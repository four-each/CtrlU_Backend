package org.example.ctrlu.domain.auth.application;

import static org.example.ctrlu.domain.auth.exception.AuthErrorCode.*;

import java.util.Optional;

import org.example.ctrlu.domain.auth.dto.request.DeleteUserRequest;
import org.example.ctrlu.domain.auth.dto.request.SigninRequest;
import org.example.ctrlu.domain.auth.dto.request.SignupRequest;
import org.example.ctrlu.domain.auth.dto.response.TokenInfo;
import org.example.ctrlu.domain.auth.exception.AuthException;
import org.example.ctrlu.domain.auth.repository.RedisTokenRepository;
import org.example.ctrlu.domain.auth.util.JWTUtil;
import org.example.ctrlu.domain.user.entity.User;
import org.example.ctrlu.domain.user.entity.UserStatus;
import org.example.ctrlu.domain.user.exception.UserException;
import org.example.ctrlu.domain.user.repository.UserRepository;
import org.example.ctrlu.global.s3.AwsS3Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private static final Long ACCESSTOKEN_EXPIRATION_TIME = (60 * 1000L) * 30; // 30분
	private static final Long REFRESHTOKEN_EXPIRATION_TIME = (60 * 1000L) * 60 * 24 * 7; // 7일
	private static final Long VERIFYTOKEN_EXPIRATION_TIME = 300000L; // 5분

	private final UserRepository userRepository;
	private final RedisTokenRepository redisTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final AwsS3Service awsS3Service;
	private final MailService mailService;
	private final JWTUtil jwtUtil;

	@Transactional
	public void signup(SignupRequest signupRequest, MultipartFile file) {
		Optional<User> optionalUser = userRepository.findByEmail(signupRequest.email());

		if (optionalUser.isPresent()) {
			User user = optionalUser.get();
			handleExistingUser(signupRequest, file, user);
			return;
		}

		createNewUser(signupRequest, file);
	}

	private void handleExistingUser(SignupRequest signupRequest, MultipartFile file, User user) {
		if (user.getStatus() == UserStatus.ACTIVE) {
			throw new AuthException(ALREADY_EXIST_EMAIL);
		}

		if (user.getStatus() == UserStatus.NONCERTIFIED && !jwtUtil.isExpired(user.getVerifyToken())) {
			throw new AuthException(TRY_EMAIL_VERIFICATION);
		}

		// 사용자 정보 갱신 후 이메일 전송
		restoreAndSendEmail(signupRequest, file, user);
	}

	private void restoreAndSendEmail(SignupRequest signupRequest, MultipartFile file, User user) {
		String imageUrl = awsS3Service.uploadImage(file);
		String encodedPassword = passwordEncoder.encode(signupRequest.password());
		user.restore(encodedPassword, signupRequest.nickname(), imageUrl, jwtUtil.createVerifyToken(VERIFYTOKEN_EXPIRATION_TIME));
		mailService.sendEmail(user);
	}

	private void createNewUser(SignupRequest request, MultipartFile file) {
		String imageUrl = awsS3Service.uploadImage(file);
		String encodedPassword = passwordEncoder.encode(request.password());

		User newUser = User.builder()
			.email(request.email())
			.password(encodedPassword)
			.nickname(request.nickname())
			.image(imageUrl)
			.verifyToken(jwtUtil.createVerifyToken(VERIFYTOKEN_EXPIRATION_TIME))
			.build();

		userRepository.save(newUser);
		mailService.sendEmail(newUser);
	}

	@Transactional
	public boolean verifyEmail(String verifyToken) {
		if (jwtUtil.isExpired(verifyToken)) {
			return false;
		}

		User user = userRepository.findByVerifyToken(verifyToken)
			.orElseThrow(() -> new AuthException(NOT_FOUND_USER));

		user.changeUserStatusToActive();
		return true;
	}

	@Transactional
	public TokenInfo signin(SigninRequest request) {
		User user = userRepository.findByEmailAndStatus(request.email(), UserStatus.ACTIVE)
			.orElseThrow(() -> new AuthException(NOT_FOUND_USER));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new AuthException(INVALID_PASSWORD);
		}

		return getTokenInfo(user);
	}

	@Transactional
	public TokenInfo reissue(Cookie refreshTokenCookie) {
		if (refreshTokenCookie == null || !StringUtils.hasText(refreshTokenCookie.getValue())) {
			throw new AuthException(NOT_FOUND_REFRESHTOKEN_IN_COOKIE);
		}

		String refreshToken = refreshTokenCookie.getValue();
		validateRefreshToken(refreshToken);
		Long userId = redisTokenRepository.getUserIdFromRefreshToken(refreshToken);
		User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
			.orElseThrow(() -> new AuthException(NOT_FOUND_USER));
		redisTokenRepository.deleteRefreshToken(refreshToken);

		return getTokenInfo(user);
	}

	private void validateRefreshToken(String refreshToken) {
		if (jwtUtil.isExpired(refreshToken)) {
			throw new AuthException(EXPIRED_REFRESHTOKEN);
		}
	}

	private TokenInfo getTokenInfo(User user) {
		String accessToken = jwtUtil.createAccessToken(user.getId(), ACCESSTOKEN_EXPIRATION_TIME);
		String refreshToken = jwtUtil.createRefreshToken(REFRESHTOKEN_EXPIRATION_TIME);
		redisTokenRepository.saveRefreshToken(refreshToken, user.getId(), REFRESHTOKEN_EXPIRATION_TIME);
		return new TokenInfo(accessToken, refreshToken);
	}

	@Transactional
	public void logout(Cookie refreshTokenCookie) {
		if (refreshTokenCookie == null || !StringUtils.hasText(refreshTokenCookie.getValue())) {
			throw new AuthException(NOT_FOUND_REFRESHTOKEN_IN_COOKIE);
		}
		String refreshToken = refreshTokenCookie.getValue();
		redisTokenRepository.deleteRefreshToken(refreshToken);
	}

	@Transactional
	public void deleteUser(Long userId, DeleteUserRequest request, Cookie refreshTokenCookie) {
		User user = userRepository.findByIdAndStatus(userId, UserStatus.ACTIVE)
			.orElseThrow(() -> new AuthException(NOT_FOUND_USER));

		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new UserException(INVALID_PASSWORD);
		}

		logout(refreshTokenCookie);
		user.softdelete();
	}
}
