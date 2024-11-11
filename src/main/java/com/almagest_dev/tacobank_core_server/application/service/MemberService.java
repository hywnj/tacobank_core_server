package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.member.repository.MemberRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.FindEmailRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.MemberResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.ResetPasswordRequestDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.UpdateMemberRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;

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
     * 비밀번호 재설정 확인
     */
    public void resetPassword(ResetPasswordRequestDto requestDto) {
        // 1. 멤버 확인
        Member member = memberRepository.findByEmailAndTel(requestDto.getEmail(), requestDto.getTel())
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
        // @TODO 문자 인증번호 발송 및 인증 절차

        // @TODO 인증 성공시, 기존 비밀번호를 새로운 비밀번호로 재설정 허용 (기존 비밀번호 삭제?)

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
}
