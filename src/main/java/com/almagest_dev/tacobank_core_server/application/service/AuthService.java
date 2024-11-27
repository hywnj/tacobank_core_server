package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.common.exception.InvalidVerificationException;
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

    /**
     * 문자 인증 요청
     */
    public SmsVerificationResponseDto sendSmsVerificationCode(SmsVerificationRequestDto requestDto) {
        String type = (StringUtils.isBlank(requestDto.getType())) ? "GENERAL" : requestDto.getType();

        // 문자 인증번호 발송 및 인증 요청
        long logId = smsAuthUtil.sendVerificationCode(requestDto.getTel(), type);
        return new SmsVerificationResponseDto(logId);
    }

    /**
     * 문자 인증 검증
     */
    public void confirmSmsVerificationCode(SmsConfirmRequestDto requestDto) {
        // 인증 여부 확인 & 인증번호 검증
        if (!smsAuthUtil.verifyCode(requestDto.getVerificationId(), requestDto.getTel(), requestDto.getInputCode())) {
            throw new InvalidVerificationException("인증 번호가 일치하지 않습니다.");
        }

        // 관련 세션 모두 삭제
        smsAuthUtil.cleanupAllSmsSession(requestDto.getVerificationId(), requestDto.getTel());
    }
}
