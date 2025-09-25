package com.archit.profilemail.notification.strategy;

import com.archit.profilemail.notification.strategy.structures.Message;

public interface NotificationStrategy<T extends Message>{
    boolean supports(NotificationType type);
    void sendMessage(T message);
}
