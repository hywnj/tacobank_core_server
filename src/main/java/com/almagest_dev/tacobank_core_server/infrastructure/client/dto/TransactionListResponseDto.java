package com.almagest_dev.tacobank_core_server.infrastructure.client.dto;
import lombok.Data;
import java.util.List;

@Data
public class TransactionListResponseDto {

    private String apiTranId;
    private String apiTranDtm;
    private String rspCode;
    private String rspMessage;
    private String bankCodeTran;
    private String bankName;
    private String fintechUseNum;
    private String balanceAmt;
    private List<TransactionDetailDto> resList;
}
