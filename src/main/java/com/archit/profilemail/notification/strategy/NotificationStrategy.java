package com.archit.profilemail.notification.strategy;

import com.archit.profilemail.dtos.NotificationRequest;

public interface NotificationStrategy {
    boolean supports(NotificationType type);
    void sendMessage(NotificationRequest request);
}
