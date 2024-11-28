package com.almagest_dev.tacobank_core_server.presentation.dto.home;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRequestDto {
    private Long memberId;
    private String fromDate;
    private String toDate;
}
