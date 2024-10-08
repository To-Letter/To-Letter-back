package com.toletter.DTO.user.Request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data // get, set 둘 다 됨.
@RequiredArgsConstructor
public class UserUpdateRequest {
    @ApiModelProperty(value = "닉네임")
    private String nickname;

    @ApiModelProperty(value = "주소")
    private String address;
}
