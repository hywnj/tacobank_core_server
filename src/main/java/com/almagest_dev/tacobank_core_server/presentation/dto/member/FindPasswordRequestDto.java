package com.almagest_dev.tacobank_core_server.presentation.dto.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class FindPasswordRequestDto {
    @NotNull(message = "회원 정보가 없습니다.")
    private Long memberId;
    @NotBlank(message = "이메일을 입력해주세요.")
    private String email;
    @NotBlank(message = "전화번호를 입력해주세요.")
    private String tel;
}
