package org.example.ctrlu.domain.auth.application;

import static org.example.ctrlu.domain.auth.exception.AuthErrorCode.*;

import java.io.UnsupportedEncodingException;

import org.example.ctrlu.domain.auth.exception.AuthException;
import org.example.ctrlu.domain.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailService {
	private static final String EMAIL_CERTIFICATION_SUBJECT = "CtrlU 회원가입 이메일 인증";
	private static final String FIND_PASSWORD_SUBJECT = "CtrlU 비밀번호 재설정";
	private static final String CHARSET = "utf-8";
	private static final String SUBTYPE = "html";
	private static final String SENDER = "CtrlU";
	private static final String VERIFY_EMAIL_BODY =
        """
        <body>
            <div style="width: 592px">
                <img src="https://inandout-bucket.s3.ap-northeast-2.amazonaws.com/logo.svg" />
                <div style="margin: 0 20px">
                    <div style="font-size: 40px; font-weight: 700; margin-top: 32px">
                        회원가입을 위한<br />
                        가장 마지막 절차예요!
                    </div>
                    <div style="
                        color: #b4b4b4;
                        font-size: 22px;
                        font-weight: 500;
                        margin-top: 16px;">
                        메일 확인과 개인정보보호를 위해 인증절차를 진행하고 있어요.<br />
                        인증 버튼을 눌러 회원가입을 완료해주세요.
                    </div>
                    <a href="%s">
                        <button style="
                            margin-top: 80px;
                            background-color: black;
                            color: white;
                            border: none;
                            border-radius: 99px;
                            width: 100%%;
                            height: 80px;
                            font-size: 22px;
                            cursor: pointer;">
                            메일 인증하기
                        </button>
                    </a>
                    <div style="
                        color: #b4b4b4;
                        font-size: 22px;
                        font-weight: 500;
                        margin-top: 120px;">
                        * 본 메일은 발신전용으로 회신이 불가능합니다.
                    </div>
                </div>
            </div>
        </body>
        """;

	private static final String FIND_PASSWORD_BODY =
		"""
		<body>
			<div style="width: 592px">
				<img src="https://inandout-bucket.s3.ap-northeast-2.amazonaws.com/logo.svg" />
				<div style="margin: 0 20px">
					<div style="font-size: 40px; font-weight: 700; margin-top: 32px">
						비밀번호 재설정
					</div>
					<div style="
						color: #b4b4b4;
						font-size: 22px;
						font-weight: 500;
						margin-top: 16px;">
						요청하신 비밀번호 재설정을 위해 아래 버튼을 클릭해주세요.
					</div>
					<a href="%s">
						<button style="
							margin-top: 80px;
							background-color: black;
							color: white;
							border: none;
							border-radius: 99px;
							width: 100%%;
							height: 80px;
							font-size: 22px;
							cursor: pointer;">
							비밀번호 재설정하기
						</button>
					</a>
					<div style="
						color: #b4b4b4;
						font-size: 22px;
						font-weight: 500;
						margin-top: 120px;">
						* 본 메일은 발신전용으로 회신이 불가능합니다.
					</div>
				</div>
			</div>
		</body>
		""";

	@Value("${spring.mail.username}")
	private String email;
	@Value("${spring.mail.request-uri.verify}")
	private String verifyRequestUri;
	@Value("${spring.mail.request-uri.reset-password}")
	private String resetPasswordRequestUri;

	private final JavaMailSender mailSender;

	public void sendVerifyEmail(User user) {
		String mailBody = VERIFY_EMAIL_BODY.formatted(verifyRequestUri + user.getVerifyToken());
		sendEmail(user.getEmail(), EMAIL_CERTIFICATION_SUBJECT, mailBody);
	}

	public void sendFindPasswordEmail(User user) {
		String mailBody = FIND_PASSWORD_BODY.formatted(resetPasswordRequestUri + user.getVerifyToken());
		sendEmail(user.getEmail(), FIND_PASSWORD_SUBJECT, mailBody);
	}

	private void sendEmail(String receiverMail, String subject, String body) {
		MimeMessage message = mailSender.createMimeMessage();
		try {
			message.addRecipients(MimeMessage.RecipientType.TO, receiverMail);
			message.setSubject(subject);
			message.setText(body, CHARSET, SUBTYPE);
			message.setFrom(new InternetAddress(email, SENDER));
			mailSender.send(message);
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new AuthException(FAILED_SEND_EMAIL);
		}
	}
}
