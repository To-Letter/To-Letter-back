package com.toletter.DTO.letter;

import com.toletter.Entity.Letter;
import com.toletter.Entity.SentBox;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.time.LocalDateTime;

@Data
@RequiredArgsConstructor
public class SaveSentBox {
    @ApiModelProperty(value = "보내는 유저 닉네임")
    private String fromUserNickname;

    @ApiModelProperty(value = "메일 보낸 시간", example = "2024-03-04T14:02:10")
    private LocalDateTime sentTime;

    @ApiModelProperty(value = "letter_id(pk)", example = "1")
    private Letter letter;

    public SentBox toEntity() {
        return SentBox.builder()
                .userNickname(fromUserNickname)
                .sentTime(sentTime)
                .letter(letter)
                .build();
    }
}
