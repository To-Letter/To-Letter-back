package com.toletter.Service;

import com.toletter.DTO.auth.Request.EmailSaveRequest;
import com.toletter.DTO.auth.Request.EmailVerifyRequest;
import com.toletter.DTO.auth.Response.EmailVerifyResponse;
import com.toletter.Entity.Auth;
import com.toletter.Entity.User;
import com.toletter.Error.ErrorCode;
import com.toletter.Error.ErrorException;
import com.toletter.Repository.AuthRepository;
import com.toletter.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender emailSender;
    private final SpringTemplateEngine templateEngine;
    private final AuthRepository authRepository;
    private final UserRepository userRepository;

    // 인증 코드 생성
    public String createCode() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            if (random.nextBoolean()) { // 랜덤으로 true, false 리턴
                key.append((char)((int)(random.nextInt(26)) + 97)); // 소문자
            } else {
                key.append(random.nextInt(10)); // 숫자
            }
        }
        System.out.println(key);
        return  key.toString();
    }

    // 타임리프 설정하는 코드
    private String setContext(String code) {
        Context context = new Context();
        context.setVariable("code", code);
        return templateEngine.process("emailAuth", context);
    }

    // 메일 보내기
    public void sendEmail(String toEmail) throws Exception {
        String randomCode = createCode(); //인증 코드 생성

        MimeMessage message = emailSender.createMimeMessage();
        message.addRecipients(MimeMessage.RecipientType.TO, toEmail); // 보낼 이메일 설정
        message.setSubject("[to-Letter] 2차 인증코드"); // 이메일 제목
        message.setFrom("to-Letter");
        message.setText(setContext(randomCode), "utf-8", "html"); // 내용 설정(Template Process)

        //실제 메일 전송
        emailSender.send(message);

        // 데베에 저장
        this.saveDB(toEmail, randomCode);
    }

    // 데베에 저장
    public void saveDB(String email, String randomCode) throws Exception {
        if(authRepository.existsByEmail(email)){
            throw new ErrorException("이미 인증 메일을 보냈습니다.", ErrorCode.NOT_FOUND_EXCEPTION);
        }
        EmailSaveRequest emailSaveRequest = new EmailSaveRequest();
        emailSaveRequest.setEmail(email);
        emailSaveRequest.setRandomCode(randomCode);
        authRepository.save(emailSaveRequest.toEntity());
    }

    // 2차 인증 검증
    public EmailVerifyResponse verifyEmail(EmailVerifyRequest emailVerifyRequest) throws Exception {
        Auth auth = authRepository.findByEmail(emailVerifyRequest.getEmail()).orElseThrow();

        // 현재 시각 가져오기
        LocalDateTime currentDateTime = LocalDateTime.now();

        // 10분 지나면 다시 보내기
        if(currentDateTime.isAfter(auth.getCreatedDate().plusMinutes(10))){
            authRepository.deleteByEmail(auth.getEmail());
            this.sendEmail(emailVerifyRequest.getEmail());
            return EmailVerifyResponse.res("401","이메일 인증 실패 / 시간 초과");
        }
        if(!emailVerifyRequest.getRandomCode().equals(auth.getRandomCode())){
            return EmailVerifyResponse.res("403","이메일 인증 실패 / 랜덤 코드 불일치");
        }
        // 인증 성공 시
        authRepository.deleteByEmail(auth.getEmail());
        User user = userRepository.findByEmail(emailVerifyRequest.getEmail()).orElseThrow();
        user.setSecondConfirmed(true);
        userRepository.save(user);
        return EmailVerifyResponse.res("200", "이메일 인증 성공");
    }

}
