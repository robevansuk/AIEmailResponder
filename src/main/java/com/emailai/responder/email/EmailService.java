package com.emailai.responder.email;

import com.emailai.responder.model.Email;

import java.util.List;

public interface EmailService {
    /**
     * Get unread emails that need a response
     */
    List<Email> fetchUnreadEmails();

    /**
     * Send a response to an email
     */
    boolean sendResponse(Email originalEmail, String responseText);

    boolean saveResponseToDrafts(Email originalEmail, String responseText);

    /**
     * Mark an email as read
     */
    boolean markAsRead(String emailId);

    /**
     * Check if an email has already been responded to
     */
    boolean hasBeenRepliedTo(String emailId);

    /**
     * Get the email provider name (GMAIL, OUTLOOK)
     */
    String getProviderName();
}