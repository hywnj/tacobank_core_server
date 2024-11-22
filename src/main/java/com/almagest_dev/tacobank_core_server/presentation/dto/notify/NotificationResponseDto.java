package com.almagest_dev.tacobank_core_server.presentation.dto.notify;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationResponseDto {
    private String message;
    private LocalDateTime createdDate;
}
