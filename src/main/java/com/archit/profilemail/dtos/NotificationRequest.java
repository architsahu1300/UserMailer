package com.archit.profilemail.dtos;

import com.archit.profilemail.notification.strategy.NotificationType;
import com.archit.profilemail.notification.strategy.structures.Message;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class NotificationRequest {
    public static final String UNIVERSAL_FROM_ADDRESS = "architsahu1300@gmail.com";
    private NotificationType notificationType;
    private Message message;
}
