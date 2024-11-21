package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.AccountService;
import com.almagest_dev.tacobank_core_server.application.service.FavoriteAccountService;
import com.almagest_dev.tacobank_core_server.common.dto.CoreResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/core/accounts")
public class AccountController {

    private final AccountService accountService;
    private final FavoriteAccountService favoriteAccountService;

    public AccountController(AccountService accountService, FavoriteAccountService favoriteAccountService) {
        this.accountService = accountService;
        this.favoriteAccountService = favoriteAccountService;
    }

    @PostMapping
    public ResponseEntity<AccountMemberReponseDto> getUserAccounts(@RequestBody MemberRequestDto requestDto) {
        AccountMemberReponseDto response = accountService.getUserAccounts(requestDto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/set-main")
    public ResponseEntity<String> setMainAccount(@RequestBody MainAccountRequestDto requestDto) {
        accountService.setMainAccount(requestDto);
        return ResponseEntity.ok("메인 계좌가 설정되었습니다.");
    }

    @PutMapping("/update-main")
    public ResponseEntity<String> updateMainAccount(@RequestBody MainAccountRequestDto requestDto) {
        accountService.updateMainAccount(requestDto);
        return ResponseEntity.ok("메인 계좌가 수정되었습니다.");
    }

    @PostMapping("/favorite-account")
    public ResponseEntity<String> setAndRetrieveFavoriteAccount(@RequestBody FavoriteAccountRequestDto requestDto) {
        favoriteAccountService.setAndRetrieveFavoriteAccount(requestDto);
        return ResponseEntity.ok("즐겨찾기 계좌로 설정되었습니다.");
    }

    @DeleteMapping("/favorite-account")
    public ResponseEntity<String> cancelFavoriteAccount(@RequestBody FavoriteAccountRequestDto requestDto) {
        favoriteAccountService.cancelFavoriteAccount(requestDto);
        return ResponseEntity.ok("즐겨찾기 계좌가 성공적으로 취소되었습니다.");
    }

    @GetMapping("/favorite-account/list")
    public ResponseEntity<List<FavoriteAccountResponseDto>> getFavoriteAccounts(@RequestBody FavoriteAccountRequestDto requestDto) {
        Long memberId = requestDto.getMemberId(); // JSON 바디에서 memberId 추출
        List<FavoriteAccountResponseDto> responseDtoList = favoriteAccountService.getFavoriteAccounts(memberId);
        return ResponseEntity.ok(responseDtoList);
    }


    /**
     * 즐겨찾기, 최근 이체 계좌 조회
     */
    @GetMapping("/transfer-options/{memberId}")
    public ResponseEntity<?> getTransferOptions(@PathVariable Long memberId) {
        TransferOptionsResponseDto response = accountService.getTransferOptions(memberId);
        return ResponseEntity.ok(new CoreResponseDto<>("SUCCESS", "계좌 조회 성공", response));
    }


}