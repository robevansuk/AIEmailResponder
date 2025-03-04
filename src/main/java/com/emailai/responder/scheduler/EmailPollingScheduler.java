package com.emailai.responder.scheduler;

import com.emailai.responder.model.Email;
import com.emailai.responder.service.ai.AiService;
import com.emailai.responder.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class EmailPollingScheduler {

    private final List<EmailService> emailServices;
    private final AiService aiService;

    @Value("${email.polling.enabled}")
    private boolean pollingEnabled;

    @Scheduled(fixedDelayString = "${gmail.api.polling.interval}")
    public void pollGmailEmails() {
        if (!pollingEnabled) {
            return;
        }

        emailServices.stream()
                .filter(service -> "GMAIL".equals(service.getProviderName()))
                .findFirst()
                .ifPresent(this::processEmails);
    }

    // Uncomment this if you want to integrate with outlook's api for emails.
//    @Scheduled(fixedDelayString = "${outlook.api.polling.interval}")
//    public void pollOutlookEmails() {
//        if (!pollingEnabled) {
//            return;
//        }
//
//        emailServices.stream()
//                .filter(service -> "OUTLOOK".equals(service.getProviderName()))
//                .findFirst()
//                .ifPresent(this::processEmails);
//    }

    private void processEmails(EmailService emailService) {
        try {
            log.info("Polling for new emails from {}", emailService.getProviderName());
            List<Email> unreadEmails = emailService.fetchUnreadEmails();

            log.info("Found {} unread emails from {}", unreadEmails.size(), emailService.getProviderName());

            for (Email email : unreadEmails) {
                try {
                    // Generate AI response
                    String responseText = aiService.generateResponse(email);

                    // Send the response
                    boolean sent = emailService.saveResponseToDrafts(email, responseText);

                    if (sent) {
                        log.info("Successfully sent AI response to email: {}", email.getId());
                        // Mark as read
                        emailService.markAsRead(email.getId());
                    } else {
                        log.error("Failed to send AI response to email: {}", email.getId());
                    }
                } catch (Exception e) {
                    log.error("Error processing email: {}", email.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error polling for emails from {}", emailService.getProviderName(), e);
        }
    }
}