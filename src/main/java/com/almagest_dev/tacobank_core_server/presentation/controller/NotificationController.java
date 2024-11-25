package com.almagest_dev.tacobank_core_server.presentation.controller;

import com.almagest_dev.tacobank_core_server.application.service.NotificationService;
import com.almagest_dev.tacobank_core_server.common.dto.CoreResponseDto;
import com.almagest_dev.tacobank_core_server.presentation.dto.notify.NotificationResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("core/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<CoreResponseDto<List<NotificationResponseDto>>> getNotifications(@PathVariable Long memberId) {
        List<NotificationResponseDto> notifications = notificationService.getNotificationsForMember(memberId);
        return ResponseEntity.ok(
                new CoreResponseDto<>("success", "알림 조회 성공", notifications)
        );
    }
}
