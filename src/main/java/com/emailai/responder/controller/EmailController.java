package com.emailai.responder.controller;

import com.emailai.responder.model.Email;
import com.emailai.responder.model.EmailResponse;
import com.emailai.responder.service.ai.AiService;
import com.emailai.responder.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("/api/emails")
@RequiredArgsConstructor
@Slf4j
public class EmailController {

    private final List<EmailService> emailServices;
    private final AiService aiService;

    @GetMapping
    public ResponseEntity<Map<String, List<Email>>> getUnreadEmails() {
        Map<String, List<Email>> emailsByProvider = emailServices.stream()
                .collect(Collectors.toMap(
                        EmailService::getProviderName,
                        EmailService::fetchUnreadEmails
                ));

        return ResponseEntity.ok(emailsByProvider);
    }

    @PostMapping("/{provider}/{emailId}/respond")
    public ResponseEntity<EmailResponse> respondToEmail(
            @PathVariable String provider,
            @PathVariable String emailId,
            @RequestParam(required = false) Boolean useAi) {

        // Find the email service for the given provider
        EmailService emailService = emailServices.stream()
                .filter(service -> service.getProviderName().equalsIgnoreCase(provider))
                .findFirst()
                .orElse(null);

        if (emailService == null) {
            return ResponseEntity.badRequest().body(
                    EmailResponse.builder()
                            .emailId(emailId)
                            .sent(false)
                            .errorMessage("Invalid provider: " + provider)
                            .build()
            );
        }

        try {
            // Get all unread emails
            List<Email> emails = emailService.fetchUnreadEmails();

            // Find the specific email
            Email email = emails.stream()
                    .filter(e -> e.getId().equals(emailId))
                    .findFirst()
                    .orElse(null);

            if (email == null) {
                return ResponseEntity.notFound().build();
            }

            // Generate response
            String responseText;
            if (useAi != null && useAi) {
                responseText = aiService.generateResponse(email);
            } else {
                responseText = "Thank you for your email. We have received it and will respond shortly.";
            }

            // Send response
            boolean sent = emailService.sendResponse(email, responseText);

            if (sent) {
                // Mark as read
                emailService.markAsRead(emailId);

                return ResponseEntity.ok(
                        EmailResponse.builder()
                                .emailId(emailId)
                                .responseText(responseText)
                                .sent(true)
                                .build()
                );
            } else {
                return ResponseEntity.internalServerError().body(
                        EmailResponse.builder()
                                .emailId(emailId)
                                .responseText(responseText)
                                .sent(false)
                                .errorMessage("Failed to send response")
                                .build()
                );
            }
        } catch (Exception e) {
            log.error("Error responding to email", e);
            return ResponseEntity.internalServerError().body(
                    EmailResponse.builder()
                            .emailId(emailId)
                            .sent(false)
                            .errorMessage("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/{provider}/{emailId}/mark-read")
    public ResponseEntity<Map<String, Boolean>> markAsRead(
            @PathVariable String provider,
            @PathVariable String emailId) {

        EmailService emailService = emailServices.stream()
                .filter(service -> service.getProviderName().equalsIgnoreCase(provider))
                .findFirst()
                .orElse(null);

        if (emailService == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false));
        }

        boolean success = emailService.markAsRead(emailId);
        return ResponseEntity.ok(Map.of("success", success));
    }
}