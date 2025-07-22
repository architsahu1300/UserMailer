package com.archit.profilemail.dtos;

import com.archit.profilemail.notification.strategy.NotificationType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class NotificationRequest {
    private NotificationType notificationType;
    private String recepient;
    private String message;
}
