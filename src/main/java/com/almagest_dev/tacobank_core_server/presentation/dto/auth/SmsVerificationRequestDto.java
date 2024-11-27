package com.almagest_dev.tacobank_core_server.presentation.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SmsVerificationRequestDto {
    private String type; // 인증 유형 (회원가입: JOIN, 비번관련: PW, 출금비번: PIN, 통합계좌조회: MYDATA)

    @NotBlank(message = "전화번호를 입력해주세요.")
    private String tel;
}
