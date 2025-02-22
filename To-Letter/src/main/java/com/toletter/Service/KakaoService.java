package com.toletter.Service;

import com.toletter.DTO.ResponseDTO;
import com.toletter.DTO.user.KaKaoDTO;
import com.toletter.DTO.user.Request.UserKaKaoSignupRequest;
import com.toletter.DTO.user.Request.UserKaKaoUpdateRequest;
import com.toletter.Entity.User;
import com.toletter.Enums.LoginType;
import com.toletter.Enums.UserRole;
import com.toletter.Error.ErrorCode;
import com.toletter.Error.ErrorException;
import com.toletter.JWT.JwtTokenProvider;
import com.toletter.Repository.ReceivedBoxRepository;
import com.toletter.Repository.SentBoxRepository;
import com.toletter.Repository.UserRepository;
import com.toletter.Service.Jwt.CustomUserDetails;
import com.toletter.Service.Jwt.RedisJwtService;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KakaoService {
    @Value("${kakao.apiKey}")
    String kakaoApiKey;

    @Value("${kakao.redirectUrl}")
    String redirectUrl;

    @Value("${kakao.redirectDeleteUrl}")
    String redirectDeleteUrl;

    String authUrl = "https://kauth.kakao.com/oauth/authorize?";
    String tokenUrl = "https://kauth.kakao.com/oauth/token";
    String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
    String unLinkUrl = "https://kapi.kakao.com/v1/user/unlink";
    private final UserRepository userRepository;
    private final ReceivedBoxRepository receivedBoxRepository;
    private final SentBoxRepository sentBoxRepository;
    private final UserService userService;
    private final RedisJwtService redisJwtService;
    private final AlarmService alarmService;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입을 위한 url
    public ResponseDTO getAuthCode(){
        StringBuffer url = new StringBuffer();
        url.append(authUrl) .append("client_id="+kakaoApiKey).append("&redirect_uri="+redirectUrl).append("&response_type=code");
        return ResponseDTO.res(200, "url 전달 성공",url.toString());
    }

    // 탈퇴를 위한 url
    public ResponseDTO getDeleteAuthCode(){
        StringBuffer url = new StringBuffer();
        url.append(authUrl) .append("client_id="+kakaoApiKey).append("&redirect_uri="+redirectDeleteUrl).append("&response_type=code");
        return ResponseDTO.res(200, "url 전달 성공",url.toString());
    }

    // 토큰 발급하기
    public Map getTokenUrl(String code, String type) throws ParseException {
        String access_Token = "";
        String refresh_Token = "";
        String refresh_token_expires_in = "";
        String expires_in = "";

        // http header 생성
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded;charset=utf-8");

        // body 생성
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code"); //고정값
        params.add("client_id", kakaoApiKey);
        if(type.equals("login")){
            params.add("redirect_uri", redirectUrl);
        } else if(type.equals("delete")){
            params.add("redirect_uri", redirectDeleteUrl);
        }
        params.add("code", code);

        // header + body
        HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(params, httpHeaders);

        // 4. http 요청하기
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                httpEntity,
                String.class
        );

        if(response.getStatusCode().equals(HttpStatus.OK)){
            //Response 데이터 파싱
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObj    = (JSONObject) jsonParser.parse(response.getBody());
            access_Token = String.valueOf(jsonObj.get("access_token"));
            expires_in = String.valueOf(jsonObj.get("expires_in"));
            refresh_Token = String.valueOf(jsonObj.get("refresh_token"));
            refresh_token_expires_in = String.valueOf(jsonObj.get("refresh_token_expires_in"));
        } else {
            throw new ErrorException( response.getStatusCode()+"카카오 토큰이 발급이 안됩니다.", 404, ErrorCode.NOT_FOUND_EXCEPTION);
        }

        Map<String, Object> tokenMap = new HashMap<String, Object>();
        tokenMap.put("access_Token", access_Token);
        tokenMap.put("expires_in", expires_in);
        tokenMap.put("refresh_Token", refresh_Token);
        tokenMap.put("refresh_token_expires_in", refresh_token_expires_in);

        return tokenMap;
    }

    // 회원가입 혹은 로그인 처리
    public ResponseDTO getUserInfo(Map token, HttpServletResponse httpServletResponse) throws ParseException {
        //access_token을 이용하여 사용자 정보 조회
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.get("access_Token").toString());

        HttpEntity request = new HttpEntity(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                userInfoUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        KaKaoDTO kakaoUser;
        if (response.getStatusCode() == HttpStatus.OK) {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response.getBody());
            Long userId = Long.parseLong(jsonObject.get("id").toString());
            JSONObject kakaoAccount = (JSONObject) jsonObject.get("kakao_account");

            kakaoUser = new KaKaoDTO(kakaoAccount.get("email").toString(), userId);

            if(userRepository.existsByEmail(kakaoUser.getEmail())){
                User user = userRepository.findByEmail(kakaoUser.getEmail()).orElseThrow();

                if(!user.isSecondConfirmed()){
                    return ResponseDTO.res(400, "카카오 로그인 실패/ 2차 회원가입(닉네임, 주소)이 제대로 진행이 되지 않음.", kakaoUser);
                }
                if (user.getLoginType().equals(LoginType.localLogin)) {
                    return ResponseDTO.res(403, "카카오 회원가입 실패 / 동일한 이메일 존재", "");
                } else if(user.getLoginType().equals(LoginType.kakaoLogin)){// 만약, 이미 회원가입이 된 카카오톡 유저라면
                    userService.setJwtTokenInHeader(user.getEmail(), user.getUserRole(), httpServletResponse);
                    return ResponseDTO.res(201, "카카오 로그인 성공", kakaoUser);
                }
            }
        } else {
            throw new ErrorException( response.getStatusCode()+"카카오 인증 실패 / 토큰이 이상하거나 만료됨.", 401 ,ErrorCode.UNAUTHORIZED_EXCEPTION);
        }
        UserKaKaoSignupRequest userKaKaoSignupRequest = new UserKaKaoSignupRequest(kakaoUser.getEmail(), kakaoUser.getUserId(), LoginType.kakaoLogin, false, UserRole.User);

        User newUser = userKaKaoSignupRequest.toEntity();
        userRepository.save(newUser);

        return ResponseDTO.res(200, "카카오 회원가입 성공", kakaoUser);
    }

    public ResponseDTO kakaoSignup(UserKaKaoUpdateRequest userKaKaoUpdateRequest){
        User user = userRepository.findByEmail(userKaKaoUpdateRequest.getEmail()).orElseThrow();

        if(!userKaKaoUpdateRequest.getEmail().equals(user.getEmail())){
            return ResponseDTO.res(401, "카카오 회원가입 실패/유저가 다름", "");
        }
        if(user.getLoginType().equals(LoginType.localLogin)){
            return ResponseDTO.res(403, "카카오 회원가입 실패/로컬 유저", "");
        }
        user.updateKakaoUser(userKaKaoUpdateRequest);
        user.setSecondConfirmed(true);
        userRepository.save(user);

        return ResponseDTO.res(200, "카카오 회원가입 성공", "");
    }

    public ResponseDTO userKaKaoDelete(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Map token, CustomUserDetails userDetails) throws ParseException {
        //access_token을 이용하여 사용자 정보 조회
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.get("access_Token").toString());

        HttpEntity request = new HttpEntity(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                unLinkUrl,
                HttpMethod.POST,
                request,
                String.class
        );

        if(response.getStatusCode().equals(HttpStatus.OK)){
            User user = userDetails.getUser();

            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response.getBody());
            Long userId = Long.parseLong(jsonObject.get("id").toString());

            // 카카오 로그인인지와 탈퇴 유저가 맞는지 확인
            if(user.getLoginType().equals(LoginType.kakaoLogin) && user.getKakaoId().equals(userId)){
                receivedBoxRepository.deleteAllByLetterByUserEmail(user.getEmail());
                sentBoxRepository.deleteAllByLetterByUserEmail(user.getEmail());
                alarmService.delete(user.getEmail());
                redisJwtService.deleteValues(user.getEmail());
                jwtTokenProvider.expireToken(httpServletRequest, httpServletResponse);
                userRepository.delete(user);
            }
            return ResponseDTO.res(200,"탈퇴 성공","");
        } else {
            throw new ErrorException(response.getStatusCode()+"카카오 유저 탈퇴 실패", 401, ErrorCode.UNAUTHORIZED_EXCEPTION);
        }
    }
}
