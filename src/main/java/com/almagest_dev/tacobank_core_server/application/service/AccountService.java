package com.almagest_dev.tacobank_core_server.application.service;


import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.AccountInfoDTO;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.IntegrateAccountRequestDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.dto.IntegrateAccountResponseDto;
import com.almagest_dev.tacobank_core_server.infrastructure.client.testbed.TestbedApiClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class AccountService {

    private final MemberRepository memberRepository;
    private final TestbedApiClient testbedApiClient;

    public AccountService(MemberRepository memberRepository, TestbedApiClient testbedApiClient) {
        this.memberRepository = memberRepository;
        this.testbedApiClient = testbedApiClient;
    }

    public IntegrateAccountResponseDto getUserAccounts(Long memberId) {
        // memberId로 Member 엔티티 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // userFinanceId 가져오기
        String userFinanceId = member.getUserFinanceId();

        // 요청 객체 생성
        IntegrateAccountRequestDto requestDto = new IntegrateAccountRequestDto();
        requestDto.setUserFinanceId(userFinanceId);
        requestDto.setInquiryBankType("A"); // 필요시 다른 값 설정

        // 외부 API 호출
        IntegrateAccountResponseDto responseDto = testbedApiClient.requestApi(
                requestDto, "/openbank/accounts", IntegrateAccountResponseDto.class
        );

        // 외부 API에서 받은 응답 객체 그대로 반환
        return responseDto;
    }


}