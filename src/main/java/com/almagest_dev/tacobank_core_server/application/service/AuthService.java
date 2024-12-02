package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.common.exception.InvalidVerificationException;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.sms.util.SmsAuthUtil;
import com.almagest_dev.tacobank_core_server.presentation.dto.auth.SmsConfirmRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.auth.SmsVerificationRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.auth.SmsVerificationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final SmsAuthUtil smsAuthUtil;
    private final MemberRepository memberRepository;

    /**
     * 문자 인증 요청
     */
    public SmsVerificationResponseDto sendSmsVerificationCode(SmsVerificationRequestDto requestDto) {
        String type = (StringUtils.isBlank(requestDto.getType())) ? "general" : requestDto.getType();

        // member ID 가 요청에 있는 경우(=로그인 한 사용자)
        Long memberId = 0L;
        if (requestDto.getMemberId() != null && requestDto.getMemberId() > 0) {
            memberId = requestDto.getMemberId();
            // 사용자 정보 확인 (탈퇴하지 않고, 요청 전화번호 정보가 일치하는 경우에만 허용)
            memberRepository.findByIdAndDeletedAndTel(memberId, "N", requestDto.getTel())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        }

        // 문자 인증번호 발송 및 인증 요청
        long logId = smsAuthUtil.sendVerificationCode(requestDto.getTel(), type, memberId);
        return new SmsVerificationResponseDto(logId);
    }

    /**
     * 문자 인증 검증
     */
    public void confirmSmsVerificationCode(SmsConfirmRequestDto requestDto) {
        String type = (StringUtils.isBlank(requestDto.getType())) ? "general" : requestDto.getType();

        // member ID 가 요청에 있는 경우(=로그인 한 사용자)
        Long memberId = 0L;
        if (requestDto.getMemberId() != null && requestDto.getMemberId() > 0) {
            memberId = requestDto.getMemberId();
            // 사용자 정보 확인 (탈퇴하지 않고, 요청 전화번호 정보가 일치하는 경우에만 허용)
            memberRepository.findByIdAndDeletedAndTel(memberId, "N", requestDto.getTel())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        }

        // 인증 여부 확인 & 인증번호 검증
        if (!smsAuthUtil.verifyCode(requestDto.getVerificationId(), requestDto.getTel(), requestDto.getInputCode(), type, memberId)) {
            throw new InvalidVerificationException("인증 번호가 일치하지 않습니다.");
        }
    }
}
