package com.archit.profilemail.notification;

import com.archit.profilemail.dtos.NotificationRequest;
import com.archit.profilemail.notification.strategy.NotificationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationDispatcher {
    private List<NotificationStrategy> strategies;

    @Autowired
    public NotificationDispatcher(List<NotificationStrategy> strategies) {
        this.strategies=strategies;
    }
    public void dispatch(NotificationRequest request){
        for(NotificationStrategy strategy:strategies){
            if(strategy.supports(request.getNotificationType())){
                strategy.sendMessage(request);
                break;
            }
        }
        throw new IllegalArgumentException("No strategy found for type: " + request.getNotificationType());
    }
}
