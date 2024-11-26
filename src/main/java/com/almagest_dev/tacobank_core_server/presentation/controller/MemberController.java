package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.MemberService;
import com.almagest_dev.tacobank_core_server.common.dto.CoreResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.member.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/taco/core/members")
public class MemberController {
    private final MemberService memberService;

    /**
     * ID로 특정 Member 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> findMemberById(@PathVariable Long id) {
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "사용자 조회 성공", memberService.findMemberById(id)));
    }

    /**
     * 이메일 찾기
     */
    @PostMapping("/email-recovery")
    public ResponseEntity<?> findMemberEmailByTel(@RequestBody @Valid FindEmailRequestDto requestDto) {
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "이메일 찾기 성공", memberService.findEmailByTel(requestDto)));
    }

    /**
     * 비밀번호 재설정: 본인 인증 & 인증번호 발송
     */
    @PostMapping("/password-reset/verify")
    public ResponseEntity<?> findMemberPasswordAndSendSmsAuth(@RequestBody @Valid FindPasswordRequestDto requestDto) {
        memberService.findPasswordAndSendSmsAuth(requestDto);
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "인증 번호를 발송했습니다."));
    }
    /**
     * 비밀번호 재설정: 인증번호 검증 & 새로운 비밀번호로 설정
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmMemberPassword(@RequestBody @Valid ResetPasswordRequestDto requestDto) {
        memberService.confirmPassword(requestDto);
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "비밀번호 재설정이 완료되었습니다."));
    }


    /**
     * 회원정보 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMember(@PathVariable Long id, @RequestBody UpdateMemberRequestDto requestDto) {
        memberService.updateMember(id, requestDto);
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "회원 정보가 성공적으로 수정되었습니다."));
    }

    /**
     * 회원 탈퇴
     */
    @PutMapping("/{id}/deactivation")
    public ResponseEntity<?> deleteMember(@PathVariable Long id) {
        memberService.deactivateMember(id);
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "회원 탈퇴가 완료되었습니다."));
    }

    /**
     * 출금 비밀번호 설정
     */
    @PostMapping("/transfer-pin")
    public ResponseEntity<?> setTransferPin(@RequestBody @Valid SetPinRequestDto requestDto) {
        memberService.setTransferPin(requestDto);
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "출금 비밀번호가 성공적으로 설정되었습니다."));
    }

    /**
     * 출금 비밀번호 수정
     */
    @PutMapping("/transfer-pin")
    public ResponseEntity<?> changeTransferPin(@RequestBody @Valid ChangePinRequestDto requestDto) {
        memberService.changeTransferPin(requestDto);
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "출금 비밀번호가 성공적으로 변경되었습니다."));
    }

    /**
     * 이메일로 친구 검색
     */
//    @GetMapping("/search")
//    public ResponseEntity<MemberSearchResponseDto> searchMemberByEmailUsingRequestBody(
//            @RequestBody MemberSearchRequestDto requestDto) {
//        MemberSearchResponseDto memberInfo = memberService.searchMemberByEmail(requestDto.getEmail());
//        return ResponseEntity.ok(memberInfo);
//    }
    @GetMapping("/search/{email}")
    public ResponseEntity<CoreResponseDto<MemberSearchResponseDto>> searchMemberByEmail(@PathVariable String email) {
        MemberSearchResponseDto memberInfo = memberService.searchMemberByEmail(email);
        return ResponseEntity.ok(
                new CoreResponseDto<>("SUCCESS", "회원 검색 성공", memberInfo)
        );
    }
}
