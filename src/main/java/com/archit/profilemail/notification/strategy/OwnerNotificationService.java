package com.archit.profilemail.notification.strategy;

import com.archit.profilemail.dtos.NotificationRequest;
import org.springframework.stereotype.Service;

@Service
public class OwnerNotificationService implements NotificationStrategy {
    @Override
    public boolean supports(NotificationType type) {
        return type==NotificationType.OWNER_EMAIL;
    }

    @Override
    public void sendMessage(NotificationRequest request) {

    }
}
