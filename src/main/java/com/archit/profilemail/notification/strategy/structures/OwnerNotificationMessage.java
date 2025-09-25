package com.archit.profilemail.notification.strategy.structures;

import lombok.Builder;

@Builder
public class OwnerNotificationMessage implements OwnerMessage{

    private String fromAddress;
    private String toAddress;
    private String subject;
    private String body;

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public String getFromAddress() {
        return fromAddress;
    }

    @Override
    public String getToAddress() {
        return toAddress;
    }
}
