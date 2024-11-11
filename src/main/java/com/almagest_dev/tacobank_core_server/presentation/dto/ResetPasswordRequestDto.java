package com.almagest_dev.tacobank_core_server.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class ResetPasswordRequestDto {
    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;
    @NotBlank(message = "전화번호를 입력해주세요.")
    private String tel;
}
