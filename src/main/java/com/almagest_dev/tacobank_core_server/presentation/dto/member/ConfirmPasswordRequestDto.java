package com.almagest_dev.tacobank_core_server.presentation.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class ConfirmPasswordRequestDto {
    @NotNull(message = "회원 정보가 없습니다.")
    private Long memberId;

    @NotNull(message = "인증 정보가 없습니다.")
    private Long verificationId;

    @NotBlank(message = "전화번호가 없습니다.")
    private String tel;

    @NotBlank(message = "인증번호를 입력해주세요.")
    @Pattern(regexp = "^\\d{6}$", message = "인증번호는 6자리이어야 합니다.")
    private String inputCode;

    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
    private String confirmPassword;
}
