package com.almagest_dev.tacobank_core_server.presentation.dto.member;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ResetPasswordRequestDto {
    @NotNull(message = "계정 정보가 없습니다.")
    @Min(value = 1, message = "유효하지 않은 계정 정보입니다.")
    private Long memberId;
    @NotBlank(message = "전화번호가 없습니다.")
    private String tel;
    @NotBlank(message = "인증번호를 입력해주세요.")
    private String inputCode;
    @NotBlank(message = "새로운 비밀번호를 입력해주세요.")
    private String newPassword;
}
