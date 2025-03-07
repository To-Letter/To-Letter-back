package com.toletter.Entity;

import com.toletter.DTO.user.Request.UserKaKaoUpdateRequest;
import com.toletter.DTO.user.Request.UserUpdateRequest;
import com.toletter.Enums.LoginType;
import javax.persistence.*;

import com.toletter.Enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Entity
@Data
@NoArgsConstructor // 기본 생성자를 자동으로 추가.
@AllArgsConstructor // 필드 값을 파라미터로 받는 생성자 추가.
@Builder
@Table(name = "user")
public class User {

    @Schema(description = "이메일(메일보낼 때 사용)", example = "test@gmail.com")
    @Id
    @Column(unique = true, nullable = false)
    private String email;

    @Schema(description = "비밀번호(암호화)", example = "testPW")
    private String password;

    @Schema(description = "카카오 회원 번호", example = "123456789")
    private Long kakaoId;

    @Schema(description = "닉네임", example = "testNickname")
    @Column(unique = true)
    private String nickname;

    @Schema(description = "주소(편지를 받을 집주소)", example = "경기도 군포시")
    private String address;

    // 로그인타입
    @Schema(description = "로그인 타입", example = "local/kakao")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    // 2차 인증 확인
    @Schema(description = "2차 인증 확인", example = "T / F")
    @Column(nullable = false)
    private boolean secondConfirmed;

    // 비밀번호 변경 확인
    @Schema(description = "비밀번호 변경 확인", example = "T / F")
    @Column(nullable = false)
    private boolean changePassWord;

    // 유저 권한
    @Schema(description = "유저 권한", example = "admin / user")
    @Column(nullable = false)
    private UserRole userRole;

    public void updateUser(UserUpdateRequest userUpdateRequest){
        this.address = userUpdateRequest.getAddress();
        this.nickname = userUpdateRequest.getNickname();
    }

    public void updateKakaoUser(UserKaKaoUpdateRequest userKaKaoUpdateRequest){
        this.address = userKaKaoUpdateRequest.getAddress();
        this.nickname = userKaKaoUpdateRequest.getNickname();
    }

    public void updatePassword(String changePassword){
        this.password = changePassword;
    }
}
