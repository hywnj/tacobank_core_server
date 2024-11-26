package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.common.exception.InvalidVerificationException;
import com.almagest_dev.tacobank_core_server.common.utils.ValidationUtil;
import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.infrastructure.sms.SmsAuthUtil;
import com.almagest_dev.tacobank_core_server.presentation.dto.member.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final SmsAuthUtil smsAuthUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * ID로 Member 조회
     */
    public MemberResponseDto findMemberById(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("회원 정보가 없습니다.");
        }
        Member member = memberRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        return new MemberResponseDto(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getTel(),
                member.getUserFinanceId()
        );
    }

    /**
     * 전화번호로 멤버 이메일 조회
     */
    public String findEmailByTel(FindEmailRequestDto requestDto) {
        Member member = memberRepository.findByTel(requestDto.getTel())
                .orElseThrow(() -> new IllegalArgumentException("등록된 이메일을 찾을 수 없습니다."));

        return member.getEmail();
    }


    /**
     * 비밀번호 재설정: 본인 인증 & 인증번호 발송
     */
    public void findPasswordAndSendSmsAuth(FindPasswordRequestDto requestDto) {
        // 1. 멤버 확인
        Member member = memberRepository.findByEmailAndTel(requestDto.getEmail(), requestDto.getTel())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 요청 member ID랑 조회한 ID랑 다른 경우
        log.info("GetId: {} , request get ID: {}", member.getId(), requestDto.getMemberId());
        if (member.getId() != requestDto.getMemberId()) {
            throw new IllegalArgumentException("회원 정보가 잘못되었습니다.");
        }

        // 2. 문자 인증번호 발송 및 인증 요청
        smsAuthUtil.sendVerificationCode(member.getTel());
    }

    /**
     * 비밀번호 재설정: 인증번호 검증 & 새로운 비밀번호로 설정
     */
    public void confirmPassword(ResetPasswordRequestDto requestDto) {
        // 1. 멤버 확인
        Member member = memberRepository.findByIdAndDeleted(requestDto.getMemberId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 2. 인증 여부 확인 & 인증번호 검증
        // @TODO 인증여부 확인 어떻게? : DB Log? Redis?
        if (!smsAuthUtil.verifyCode(requestDto.getTel(), requestDto.getInputCode())) {
            throw new InvalidVerificationException("인증 번호가 일치하지 않습니다.");
        }
        if (!smsAuthUtil.isVerified(requestDto.getTel())) {
            throw new InvalidVerificationException("인증 세션이 만료되었습니다. 다시 시도해주세요.");
        }

        // 3. 비밀번호 규칙 검사
        if (!ValidationUtil.validatePassword(requestDto.getNewPassword(), 8, member.getBirth(), member.getTel())) {
            throw new InvalidVerificationException("비밀번호를 규칙에 맞게 설정해주세요.");
        }

        // 4. 비밀번호 Update & 저장
        String encodedPassword = passwordEncoder.encode(requestDto.getNewPassword());
        member.changePassword(encodedPassword);

        memberRepository.save(member);

        // 관련 세션 모두 삭제
        smsAuthUtil.cleanupAllSmsSession(requestDto.getTel());
    }

    /**
     * 회원정보 수정
     */
    public void updateMember(Long id, UpdateMemberRequestDto requestDto) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("회원 정보가 없습니다.");
        }
        Member member = memberRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 요청 DTO에 필드가 있는 경우에만 업데이트
        boolean hasChanges = false;
        if (requestDto.getName() != null && !requestDto.getName().isBlank()) {
            member.changeName(requestDto.getName());
            hasChanges = true;
        }
        if (requestDto.getTel() != null && !requestDto.getTel().isBlank()) {
            member.changeTel(requestDto.getTel());
            hasChanges = true;
        }
        // 수정 값이 있을때만 업데이트
        if (!hasChanges) throw new IllegalArgumentException("수정 사항이 없습니다.");
        memberRepository.save(member);
    }

    /**
     * 회원 탈퇴
     */
    public void deactivateMember(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("회원 정보가 없습니다.");
        }
        Member member = memberRepository.findByIdAndDeleted(id, "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 이미 탈퇴한 회원입니다."));

        member.deactivate(); // 회원상태 비활성화
        memberRepository.save(member);
    }

    /**
     * 출금 비밀번호 설정
     */
    public void setTransferPin(SetPinRequestDto requestDto) {
        log.info("MemberService::setTransferPin START");

        // Member 조회
        Member member = memberRepository.findByIdAndDeleted(requestDto.getMemberId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 출금 비밀번호 유효성 검사
        if (!ValidationUtil.validateTransferPin(requestDto.getTransferPin())) {
            throw new InvalidVerificationException("출금 비밀번호를 규칙에 맞게 설정해주세요.");
        }

        // 비밀번호 해싱 및 저장
        String encodedPin = passwordEncoder.encode(requestDto.getTransferPin());
        member.changeTransferPin(encodedPin);
        memberRepository.save(member);

        log.info("MemberService::setTransferPin END - Transfer PIN 설정 완료");
    }

    /**
     * 출금 비밀번호 수정
     */
    public void changeTransferPin(ChangePinRequestDto requestDto) {
        log.info("MemberService::changeTransferPin START");
        // Member 조회
        Member member = memberRepository.findByIdAndDeleted(requestDto.getMemberId(), "N")
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 기존 비밀번호 확인
        if (!passwordEncoder.matches(requestDto.getCurrentPin(), member.getTransferPin())) {
            throw new InvalidVerificationException("비밀번호가 일치하지 않습니다");
        }
        // 새로운 비밀번호와 확인 비밀번호 일치여부
        if (!requestDto.getNewPin().equals(requestDto.getConfirmPin())) {
            throw new InvalidVerificationException("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
        }
        // 새로운 비밀번호 유효성 검사
        if (!ValidationUtil.validateTransferPin(requestDto.getNewPin())) {
            throw new InvalidVerificationException("출금 비밀번호를 규칙에 맞게 설정해주세요.");
        }

        // 새로운 비밀번호 해싱 및 저장
        String encodedPin = passwordEncoder.encode(requestDto.getNewPin());
        member.changeTransferPin(encodedPin);
        memberRepository.save(member);

        log.info("MemberService::changeTransferPin END");
    }


    /**
     * 이메일로 친구 검색
     */
    public MemberSearchResponseDto searchMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일로 회원을 찾을 수 없습니다."));

        // Member 데이터를 MemberResponseDto로 변환하여 반환
        return new MemberSearchResponseDto(
                member.getId(),
                member.getName(),
                member.getEmail()
        );
    }
}
