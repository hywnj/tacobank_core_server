package com.almagest_dev.tacobank_core_server.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExceptionResponseDTO {
    private String error;
    private String message;
}
