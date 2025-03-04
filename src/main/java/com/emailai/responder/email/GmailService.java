package com.emailai.responder.email;

import com.emailai.responder.model.Email;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import com.google.common.io.BaseEncoding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GmailService implements EmailService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_MODIFY);

    @Value("${gmail.api.credentials.path}")
    private Resource credentialsPath;

    @Value("${gmail.api.tokens.directory}")
    private String tokensDirectoryPath;

    @Value("${gmail.api.application.name}")
    private String applicationName;

    private Gmail gmailService;

    @Override
    public List<Email> fetchUnreadEmails() {
        try {
            if (gmailService == null) {
                initializeGmailService();
            }

            String user = "me";
            ListMessagesResponse response = gmailService.users().messages().list(user)
                    .setQ("is:unread -category:promotions -category:social")
                    .setMaxResults(10L)
                    .execute();

            List<Email> emails = new ArrayList<>();
            if (response.getMessages() != null) {
                for (Message message : response.getMessages()) {
                    Message fullMessage = gmailService.users().messages().get(user, message.getId()).execute();

                    if (!hasBeenRepliedTo(fullMessage.getId())) {
                        Email email = convertToEmail(fullMessage);
                        emails.add(email);
                    }
                }
            }

            return emails;
        } catch (Exception e) {
            log.error("Error fetching unread emails from Gmail", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean sendResponse(Email originalEmail, String responseText) {
        try {
            if (gmailService == null) {
                initializeGmailService();
            }

            // Create email
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage email = new MimeMessage(session);

            email.setFrom(new InternetAddress("me"));
            email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(originalEmail.getSenderEmail()));
            email.setSubject("Re: " + originalEmail.getSubject());
            email.setText(responseText);
            email.setHeader("In-Reply-To", originalEmail.getId());
            email.setHeader("References", originalEmail.getId());

            // Encode and wrap the MIME message into a gmail message
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] rawMessageBytes = buffer.toByteArray();
            String encodedEmail = BaseEncoding.base64Url().encode(rawMessageBytes);
            Message message = new Message();
            message.setRaw(encodedEmail);
            message.setThreadId(originalEmail.getThreadId());

            // Send the message
            message = gmailService.users().messages().send("me", message).execute();
            return message != null && message.getId() != null;
        } catch (Exception e) {
            log.error("Error sending response via Gmail", e);
            return false;
        }
    }

    @Override
    public boolean saveResponseToDrafts(Email originalEmail, String responseText) {
        try {
            if (gmailService == null) {
                initializeGmailService();
            }

            // Create email
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);
            MimeMessage email = new MimeMessage(session);

            email.setFrom(new InternetAddress("me"));
            email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(originalEmail.getSenderEmail()));
            email.setSubject("Re: " + originalEmail.getSubject());
            email.setText(responseText);
            email.setHeader("In-Reply-To", originalEmail.getId());
            email.setHeader("References", originalEmail.getId());

            // Encode and wrap the MIME message into a gmail message
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] rawMessageBytes = buffer.toByteArray();
            String encodedEmail = Base64.encodeBase64URLSafeString(rawMessageBytes);
            Message message = new Message();
            message.setRaw(encodedEmail);
            message.setThreadId(originalEmail.getThreadId());

            // Create a draft instead of sending
            Draft draft = new Draft();
            draft.setMessage(message);

            // Save the message as a draft
            Draft createdDraft = gmailService.users().drafts().create("me", draft).execute();
            return createdDraft != null && createdDraft.getId() != null;
        } catch (Exception e) {
            log.error("Error saving response to drafts via Gmail", e);
            return false;
        }
    }

    @Override
    public boolean markAsRead(String emailId) {
        try {
            if (gmailService == null) {
                initializeGmailService();
            }

            // Remove UNREAD label
            ModifyMessageRequest message = new ModifyMessageRequest();
            message.setRemoveLabelIds(Collections.singletonList("UNREAD"));
            gmailService.users().messages().modify("me", emailId, message).execute();
            return true;
        } catch (Exception e) {
            log.error("Error marking email as read in Gmail", e);
            return false;
        }
    }

    @Override
    public boolean hasBeenRepliedTo(String emailId) {
        try {
            if (gmailService == null) {
                initializeGmailService();
            }

            Message message = gmailService.users().messages().get("me", emailId).execute();
            // Check if the message has the REPLIED label
            List<String> labelIds = message.getLabelIds();
            return labelIds != null && labelIds.contains("SENT");
        } catch (Exception e) {
            log.error("Error checking if email has been replied to in Gmail", e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "GMAIL";
    }

    private void initializeGmailService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(HTTP_TRANSPORT);
        gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(applicationName)
                .build();
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets
        InputStream in = credentialsPath.getInputStream();
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private Email convertToEmail(Message message) {
        String subject = "";
        String sender = "";
        String senderEmail = "";
        List<String> recipients = new ArrayList<>();
        String body = "";
        LocalDateTime receivedDate = LocalDateTime.now();

        // Process headers
        List<MessagePartHeader> headers = message.getPayload().getHeaders();
        for (MessagePartHeader header : headers) {
            switch (header.getName()) {
                case "Subject":
                    subject = header.getValue();
                    break;
                case "From":
                    sender = header.getValue();
                    // Extract email from "Name <email>" format
                    int startIdx = sender.indexOf("<");
                    int endIdx = sender.indexOf(">");
                    if (startIdx >= 0 && endIdx > startIdx) {
                        senderEmail = sender.substring(startIdx + 1, endIdx);
                    } else {
                        senderEmail = sender; // If format is just the email
                    }
                    break;
                case "To":
                    recipients = Arrays.stream(header.getValue().split(","))
                            .map(String::trim)
                            .collect(Collectors.toList());
                    break;
                case "Date":
                    // Parse date if needed
                    break;
            }
        }

        // Extract the email body
        MessagePart payload = message.getPayload();
        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                if (part.getMimeType().equals("text/plain")) {
                    body = new String(Base64.decodeBase64(part.getBody().getData()), StandardCharsets.UTF_8);
                    break;
                }
            }
        } else if (payload.getBody() != null && payload.getBody().getData() != null) {
            body = new String(Base64.decodeBase64(payload.getBody().getData()), StandardCharsets.UTF_8);
        }

        // Convert message timestamp to LocalDateTime
        if (message.getInternalDate() != null) {
            receivedDate = LocalDateTime.ofInstant(
                    new Date(message.getInternalDate()).toInstant(),
                    ZoneId.systemDefault()
            );
        }

        return Email.builder()
                .id(message.getId())
                .source(getProviderName())
                .sender(sender)
                .senderEmail(senderEmail)
                .recipients(recipients)
                .subject(subject)
                .body(body)
                .receivedAt(receivedDate)
                .isRead(false)
                .hasBeenRepliedTo(false)
                .threadId(message.getThreadId())
                .build();
    }
}