package com.almagest_dev.tacobank_core_server.presentation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferSessionData {
    private String idempotencyKey;          // 중복 방지 키(클라이언트에서 생성)
    private Long withdrawalMemberId;           // 출금 멤버 아이디
    private String withdrawalUserFinanceId;    // 사용자 금융 식별번호
    private Long withdrawalAccountId;          // 출금 계좌 아이디
    private String withdrawalFintechUseNum;    // 출금 계좌 핀테크 이용번호
    private String withdrawalAccountNum;       // 출금 계좌 번호
    private String withdrawalAccountHolder;    // 출금 예금주
    private String withdrawalBankCode;         // 출금 은행 코드
    private String receiverFintechUseNum;   // 입금(수취) 계좌 핀테크 이용번호
    private String receiverAccountNum;      // 입금(수취) 계좌 번호
    private String receiverAccountHolder;   // 입금(수취) 예금주(수취인)
    private String receiverBankCode;        // 입금(수취) 은행 코드
    private int amount;                     // 송금액
    private boolean passwordVerified;       // 결제 비밀번호 검증 여부

    public void assignReceiverFintechUseNum(String receiverFintechUseNum) {
        this.receiverFintechUseNum = receiverFintechUseNum;
    }
    public void assignAmount(int amount) {
        this.amount = amount;
    }
    public void changePasswordVerified(boolean flag) {
        this.passwordVerified = flag;
    }
    public boolean isPasswordVerified() {
        return this.passwordVerified;
    }
}
