package com.emailai.responder.model;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class Email {
    private String id;
    private String source; // "GMAIL" or "OUTLOOK"
    private String sender;
    private String senderEmail;
    private List<String> recipients;
    private String subject;
    private String body;
    private LocalDateTime receivedAt;
    private boolean isRead;
    private boolean hasBeenRepliedTo;

    // Additional fields for tracking
    private String threadId;
    private String conversationId;
}