package com.archit.profilemail.notification.strategy;

import com.archit.profilemail.notification.strategy.structures.Message;
import com.archit.profilemail.notification.strategy.structures.OwnerMessage;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OwnerNotificationService implements NotificationStrategy<OwnerMessage> {

    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;

    @Override
    public boolean supports(NotificationType type) {
        return type==NotificationType.OWNER_EMAIL;
    }

    @Async("ownerNotificationExecutor")
    @Override
    public void sendMessage(OwnerMessage message) {
        Mail mail = createEmail(message);
        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            System.out.println("Headers: " + response.getHeaders());

        } catch (IOException e) {
            throw new RuntimeException("Error while trying to send completion mail to "+ message.getToAddress());
        }


    }
    private Mail createEmail(OwnerMessage message){
        Email from  = new Email(message.getFromAddress());
        Email to = new Email(message.getToAddress());
        Content content = new Content("text/plain",message.getBody());
        Mail mail = new Mail(from, message.getSubject(), to, content);
        return mail;
    }
}
