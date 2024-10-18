package com.toletter.Service;

import com.toletter.DTO.ResponseDTO;
import com.toletter.DTO.user.Request.*;
import com.toletter.DTO.user.Response.*;
import com.toletter.Entity.User;
import com.toletter.Enums.UserRole;
import com.toletter.JWT.*;
import com.toletter.Repository.*;
import com.toletter.Service.Jwt.CustomUserDetails;
import com.toletter.Service.Jwt.RedisJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.*;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor // 초기화되지 않은 final 필드에 대해 생성자를 생성해줌.
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private  final JwtTokenProvider jwtTokenProvider;
    private final RedisJwtService redisJwtService;
    private final AlarmService alarmService;

    // 아이디 중복 확인
    public ResponseDTO confirmEmail(String userEmail){
        if(userRepository.existsByEmail(userEmail)){
            return ResponseDTO.res(401, "이메일 중복", "");
        }
        return ResponseDTO.res(200, "이메일 중복 없음", "");
    }

    // 닉네임 중복 확인
    public ResponseDTO confirmNickname(String userNickname){
        if(userRepository.existsByNickname(userNickname)){
            return ResponseDTO.res(401, "닉네임 중복", "");
        }
        return ResponseDTO.res(200, "닉네임 중복 없음", "");
    }

    // 회원가입
    @Transactional
    public ResponseDTO signup(UserSignupRequest userSignupRequest){
        User user = userSignupRequest.toEntity();

        if(userRepository.existsByEmail(userSignupRequest.getEmail())){
            return ResponseDTO.res(401, "같은 이메일이 존재", "");
        }

        if(userRepository.existsByNickname(userSignupRequest.getNickname())){
            return ResponseDTO.res(401, "같은 닉네임이 존재", "");
        }

        // 비밀번호 암호화
        user.setPassword(passwordEncoder.encode(userSignupRequest.getPassword()));
        user.setSecondConfirmed(false);
        user.setUserRole(UserRole.User);
        user.setReceivedBox(new ArrayList<>());
        user.setSentBox(new ArrayList<>());
        userRepository.save(user);
        return ResponseDTO.res(200, "회원가입 성공", "");
    }

    // 로그인
    public ResponseDTO login(UserLoginRequest userLoginRequest, HttpServletResponse httpServletResponse){
        // 아이디가 존재하지 않으면
        if(!userRepository.existsByEmail(userLoginRequest.getEmail())){
            return ResponseDTO.res(400, "로그인 실패 / 이메일 없음", "");
        }
        User user = userRepository.findByEmail(userLoginRequest.getEmail()).orElseThrow();

        // 비밀번호 틀리면
        if(!passwordEncoder.matches(userLoginRequest.getPassword(), user.getPassword())){
            return ResponseDTO.res(401, "로그인 실패 / 비밀번호 틀림", "");
        }
        // 2차 인증 안하면
        if(!user.isSecondConfirmed()){
            return ResponseDTO.res(403, "로그인 실패 / 2차 인증 안됨", "");
        }
        this.setJwtTokenInHeader(userLoginRequest.getEmail(), user.getUserRole(), httpServletResponse);
        return ResponseDTO.res(200, "로그인 성공", "");
    }

    // 유저 정보 보여주기
    public ResponseDTO viewUser(CustomUserDetails userDetails){
        User user =  userDetails.getUser();

        return  ResponseDTO.res(200, "유저 정보 보여주기 성공", UserViewResponse.res(user.getAddress(), user.getNickname(), user.getEmail()));
    }

    // 유저 정보 수정
    @Transactional
    public ResponseDTO updateUser(UserUpdateRequest userUpdateRequest, CustomUserDetails userDetails){
        User user =  userDetails.getUser();
        user.updateUser(userUpdateRequest);
        userRepository.save(user);
        UserUpdateResponse userUpdateResponse = UserUpdateResponse.res(user.getEmail(), user.getNickname(), user.getAddress());
        return ResponseDTO.res(200, "유저 정보 수정 성공", userUpdateResponse);
    }

    // 로그아웃
    public ResponseDTO logout(HttpServletRequest httpServletRequest, CustomUserDetails userDetails){
        User user =  userDetails.getUser();
        alarmService.delete(user.getNickname());
        redisJwtService.deleteValues(user.getEmail());
        jwtTokenProvider.expireToken(httpServletRequest);
        return ResponseDTO.res(200, "로그아웃 성공", "");
    }

    // 유저 탈퇴
    public ResponseDTO userDelete(HttpServletRequest httpServletRequest, UserDeleteRequest userDeleteRequest, CustomUserDetails userDetails){
        // 유저의 아이디가 존재하지 않으면
        if(!userRepository.existsByEmail(userDeleteRequest.getEmail())){
            return ResponseDTO.res(401, "유저 이메일이 없음.", "");
        }
        User user =  userDetails.getUser();
        if(!passwordEncoder.matches(userDeleteRequest.getPassword(), user.getPassword())){
            return ResponseDTO.res(400, "비밀번호가 틀림", "");
        }
        alarmService.delete(user.getNickname());
        redisJwtService.deleteValues(userDeleteRequest.getEmail());
        jwtTokenProvider.expireToken(httpServletRequest);
        userRepository.delete(user);
        return ResponseDTO.res(200, "탈퇴 성공", "");
    }

    // 토큰 헤더에 저장
    public void setJwtTokenInHeader(String email, UserRole userRole, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.createAccessToken(email, userRole);
        String refreshToken = jwtTokenProvider.createRefreshToken(email, userRole);

        jwtTokenProvider.setHeaderAccessToken(response, accessToken);
        jwtTokenProvider.setHeaderRefreshToken(response, refreshToken);
        redisJwtService.setValues(email, accessToken,refreshToken);
    }
}
