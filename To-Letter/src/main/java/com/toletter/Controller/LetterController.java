package com.toletter.Controller;

import com.toletter.DTO.ResponseDTO;
import com.toletter.DTO.letter.Request.DeleteLetterRequest;
import com.toletter.DTO.letter.Request.SendLetterRequest;
import com.toletter.Service.Jwt.CustomUserDetails;
import com.toletter.Service.LetterService;
import io.swagger.annotations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Pageable;

import java.sql.SQLException;

@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/letter")
public class LetterController {
    private final LetterService letterService;

    // 메일 보내기
    @ApiResponses( value ={
            @ApiResponse(code = 200, message = "메일 보내기 성공"),
            @ApiResponse(code = 401, message = "메일 보내기 실패 / 보낼 유저가 없음(유저가 존재하지 않음)"),
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "Authorization/refreshToken", dataType = "String", paramType = "header", example = "bearer token")
    })
    @ApiOperation(value = "메일 보내기")
    @PostMapping("/send")
    public ResponseDTO sendLetter(@RequestBody SendLetterRequest sendLetterRequest, @AuthenticationPrincipal CustomUserDetails userDetails) throws SQLException {
        return letterService.sendLetter(sendLetterRequest, userDetails);
    }

    // 받은 모든 메일함 열기
    @ApiResponses( value ={
            @ApiResponse(code = 200, message = "받은 모든 메일함 열기 성공"),
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "Authorization/refreshToken", dataType = "String", paramType = "header", example = "bearer token"),
            @ApiImplicitParam(name = "page", value = "현재 페이지", dataType = "int", paramType = "query", example = "0"),
            @ApiImplicitParam(name = "size", value = "페이지 당 편지 수", dataType = "int", paramType = "query", example = "10"),
            @ApiImplicitParam(name = "sort", value = "정렬(desc)", dataType = "Sort", paramType = "query", example = "desc")
    })
    @ApiOperation(value = "받은 모든 메일함 열기")
    @GetMapping("/receive")
    public ResponseDTO receivedLetter(@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        return letterService.receivedLetter(userDetails, pageable);
    }

    // 안 읽은 메일함 열기
    @ApiResponses( value ={
            @ApiResponse(code = 200, message = "안 읽은 메일함 열기 성공"),
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "Authorization/refreshToken", dataType = "String", paramType = "header", example = "bearer token"),
            @ApiImplicitParam(name = "page", value = "현재 페이지", dataType = "int", paramType = "query", example = "0"),
            @ApiImplicitParam(name = "size", value = "페이지 당 편지 수", dataType = "int", paramType = "query", example = "10"),
            @ApiImplicitParam(name = "sort", value = "정렬(desc)", dataType = "Sort", paramType = "query", example = "desc")
    })
    @ApiOperation(value = "안 읽은 메일함 열기")
    @GetMapping("/receive/unRead")
    public ResponseDTO receivedUnReadLetter(@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        return letterService.receivedUnReadLetter(userDetails, pageable);
    }

    // 읽은 메일함 열기
    @ApiResponses( value ={
            @ApiResponse(code = 200, message = "읽은 메일함 열기 성공"),
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "Authorization/refreshToken", dataType = "String", paramType = "header", example = "bearer token"),
            @ApiImplicitParam(name = "page", value = "현재 페이지", dataType = "int", paramType = "query", example = "0"),
            @ApiImplicitParam(name = "size", value = "페이지 당 편지 수", dataType = "int", paramType = "query", example = "10"),
            @ApiImplicitParam(name = "sort", value = "정렬(desc)", dataType = "Sort", paramType = "query", example = "desc")
    })
    @ApiOperation(value = "읽은 메일함 열기")
    @GetMapping("/receive/read")
    public ResponseDTO receivedReadLetter(@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        return letterService.receivedReadLetter(userDetails, pageable);
    }

    // 메일 읽음 처리
    @ApiResponses( value ={
            @ApiResponse(code = 200, message = "메일 읽음 처리 성공"),
            @ApiResponse(code = 401, message = "메일 읽음 처리 실패 / 본인의 메일이 아님"),
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "Authorization/refreshToken", dataType = "String", paramType = "header", example = "bearer token")
    })
    @ApiOperation(value = "메일 읽음 처리")
    @GetMapping("/receive/viewCheckLetter")
    public ResponseDTO viewCheckReceivedLetter (@RequestParam Long letterID, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return letterService.viewCheckReceivedLetter(letterID, userDetails);
    }

    // 보낸 모든 메일함
    @ApiResponses( value ={
            @ApiResponse(code = 200, message = "보낸 모든 메일함 열기 성공")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "Authorization/refreshToken", dataType = "String", paramType = "header", example = "bearer token"),
            @ApiImplicitParam(name = "page", value = "현재 페이지", dataType = "int", paramType = "query", example = "0"),
            @ApiImplicitParam(name = "size", value = "페이지 당 편지 수", dataType = "int", paramType = "query", example = "10"),
            @ApiImplicitParam(name = "sort", value = "정렬(desc)", dataType = "Sort", paramType = "query", example = "desc")
    })
    @ApiOperation(value = "보낸 모든 메일함 열기")
    @GetMapping("/sent")
    public ResponseDTO viewSentBox (@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        return letterService.viewSentBox (userDetails, pageable);
    }

    // 메일 삭제
    @ApiResponses( value ={
            @ApiResponse(code = 200, message = "메일 삭제 성공"),
            @ApiResponse(code = 401, message = "메일 삭제 실패 / 메일(letterId)이 없음"),
            @ApiResponse(code = 403, message = "메일 삭제 실패 / 메일 주인이 아님")
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "request", value = "Authorization/refreshToken", dataType = "String", paramType = "header", example = "bearer token")
    })
    @ApiOperation(value = "메일 삭제")
    @DeleteMapping("/deleteLetter")
    public ResponseDTO deleteLetter (@RequestBody DeleteLetterRequest deleteLetterRequest, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return letterService.deleteLetter(deleteLetterRequest, userDetails);

    }
}
