package com.archit.profilemail.notification;

import com.archit.profilemail.dtos.NotificationRequest;
import com.archit.profilemail.notification.strategy.NotificationStrategy;
import com.archit.profilemail.notification.strategy.structures.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationDispatcher {
    private final List<NotificationStrategy<? extends Message>> strategies;

    @Autowired
    public NotificationDispatcher(List<NotificationStrategy<? extends Message>> strategies) {
        this.strategies=strategies;
    }
    public void dispatch(NotificationRequest request){
        for(NotificationStrategy<? extends Message> strategy:strategies){
            if(strategy.supports(request.getNotificationType())) {
                NotificationStrategy<Message> castedStrategy = (NotificationStrategy<Message>) strategy;
                castedStrategy.sendMessage(request.getMessage());
                return;
            }
        }
        throw new IllegalArgumentException("No strategy found for type: " + request.getNotificationType());
    }
}
