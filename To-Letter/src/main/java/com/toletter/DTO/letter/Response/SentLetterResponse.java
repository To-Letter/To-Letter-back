package com.toletter.DTO.letter.Response;

import com.toletter.DTO.letter.LetterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Getter
@Builder
public class SentLetterResponse {
    @Schema(description = "닉네임", example = "test")
    private String user_nickname;

    @Schema(description = "보낸 편지 list")
    private Pageable pageable;

    @Schema(description = "보낸 편지 list")
    private List<LetterDTO> listLetter;

    public static SentLetterResponse res(String user_nickname, Pageable pageable, List<LetterDTO> listLetter) {
        return SentLetterResponse.builder()
                .user_nickname(user_nickname)
                .pageable(pageable)
                .listLetter(listLetter)
                .build();
    }
}
