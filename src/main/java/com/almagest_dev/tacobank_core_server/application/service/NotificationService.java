package com.almagest_dev.tacobank_core_server.application.service;

import com.almagest_dev.tacobank_core_server.domain.member.model.Member;
import com.almagest_dev.tacobank_core_server.domain.notification.model.Notification;
import com.almagest_dev.tacobank_core_server.domain.notification.repository.NotificationRepository;
import com.almagest_dev.tacobank_core_server.presentation.dto.notify.NotificationResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * 정산 알림 보내기
     */
    public void sendNotification(Member member, String message) {
        Notification notification = new Notification();
        notification.setMember(member);
        notification.setMessage(message);
        notificationRepository.save(notification);
    }

    public List<NotificationResponseDto> getNotificationsForMember(Long memberId) {
        List<Notification> notifications = notificationRepository.findAllByMember_IdOrderByCreatedDateDesc(memberId);
        return notifications.stream()
                .map(notification -> new NotificationResponseDto(notification.getMessage(), notification.getCreatedDate()))
                .collect(Collectors.toList());
    }
}
