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
	private static final String CHARSET = "utf-8";
	private static final String SUBTYPE = "html";
	private static final String SENDER = "CtrlU";
	private static final String BODY =
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

	@Value("${spring.mail.username}")
	private String email;
	@Value("${spring.mail.request-uri}")
	private String requestUri;

	private final JavaMailSender mailSender;

	public void sendEmail(User user) {
		String receiverMail = user.getEmail();
		MimeMessage message = mailSender.createMimeMessage();

		try {
			message.addRecipients(MimeMessage.RecipientType.TO, receiverMail); // 보내는 대상
			message.setSubject(EMAIL_CERTIFICATION_SUBJECT); // 제목
			message.setText(getEmailCertificationBody(user), CHARSET, SUBTYPE); // 내용, charset 타입, subtype
			message.setFrom(new InternetAddress(email, SENDER)); // 보내는 사람의 이메일 주소, 보내는 사람 이름
			mailSender.send(message); // 메일 전송
		} catch (MessagingException | UnsupportedEncodingException e) {
			throw new AuthException(FAILED_SEND_EMAIL);
		}
	}

	private String getEmailCertificationBody(User user) {
		return BODY.formatted(requestUri + user.getVerifyToken());
	}
}
