package com.toletter.Entity;

import com.toletter.DTO.user.Request.UserKaKaoUpdateRequest;
import com.toletter.DTO.user.Request.UserUpdateRequest;
import com.toletter.Enums.LoginType;
import javax.persistence.*;

import com.toletter.Enums.UserRole;
import com.toletter.Service.LongListConverter;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor // 기본 생성자를 자동으로 추가.
@AllArgsConstructor // 필드 값을 파라미터로 받는 생성자 추가.
@Builder
@Table(name = "user")
public class User {

    @ApiModelProperty(value = "이메일(메일보낼 때 사용)", example = "test@gmail.com")
    @Id
    @Column(unique = true, nullable = false)
    private String email;

    @ApiModelProperty(value = "비밀번호(암호화)", example = "testPW")
    private String password;

    @ApiModelProperty(value = "카카오 회원 번호", example = "123456789")
    private Long kakaoId;

    @ApiModelProperty(value = "닉네임", example = "testNickname")
    @Column(unique = true)
    private String nickname;

    @ApiModelProperty(value = "주소(편지를 받을 집주소)", example = "경기도 군포시")
    private String address;

    // 로그인타입
    @ApiModelProperty(value = "로그인 타입", example = "local/kakao")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    // 2차 인증 확인
    @ApiModelProperty(value = "2차 인증 확인", example = "T / F")
    @Column(nullable = false)
    private boolean secondConfirmed;

    // 유저 권한
    @ApiModelProperty(value = "유저 권한", example = "admin / user")
    @Column(nullable = false)
    private UserRole userRole;

    // 받은 메일
    @ApiModelProperty(value = "받은 메일", example = "admin / user")
    @Convert(converter = LongListConverter.class)
    private List<Long> receivedBox;

    // 보낸 메일
    @ApiModelProperty(value = "보낸 메일", example = "admin / user")
    @Convert(converter = LongListConverter.class)
    private List<Long> sentBox;

    public void updateUser(UserUpdateRequest userUpdateRequest){
        this.address = userUpdateRequest.getAddress();
        this.nickname = userUpdateRequest.getNickname();
    }

    public void updateKakaoUser(UserKaKaoUpdateRequest userKaKaoUpdateRequest){
        this.address = userKaKaoUpdateRequest.getAddress();
        this.nickname = userKaKaoUpdateRequest.getNickname();
    }
}
