package com.almagest_dev.tacobank_core_server.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TransferPasswordRequestDto {
    @NotBlank(message = "송금 정보를 보내주세요.")
    private String idempotencyKey; // 중복 방지 키

    @NotNull(message = "회원 정보를 보내주세요.")
    private Long withdrawalMemberId;      // 출금 멤버 아이디

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String transferPin; // 해시화 한 출금 비밀번호

    @NotNull(message = "송금액을 보내주세요.")
    private int amount;                 // 송금액
}
