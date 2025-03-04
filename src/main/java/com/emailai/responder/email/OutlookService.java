//package com.emailai.responder.email;
//
//import com.azure.identity.ClientSecretCredential;
//import com.azure.identity.ClientSecretCredentialBuilder;
//import com.emailai.responder.model.Email;
//import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
//import com.microsoft.graph.models.BodyType;
//import com.microsoft.graph.models.ItemBody;
//import com.microsoft.graph.models.Message;
//import com.microsoft.graph.models.Recipient;
//import com.microsoft.graph.models.UserSendMailParameterSet;
//import com.microsoft.graph.requests.GraphServiceClient;
//import com.microsoft.graph.requests.MessageCollectionPage;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class OutlookService implements EmailService {
//
//    @Value("${outlook.api.client.id}")
//    private String clientId;
//
//    @Value("${outlook.api.client.secret}")
//    private String clientSecret;
//
//    @Value("${outlook.api.tenant.id}")
//    private String tenantId;
//
//    private GraphServiceClient<okhttp3.Request> graphClient;
//
//    private static final List<String> SCOPES = Arrays.asList("https://graph.microsoft.com/.default");
//
//    @Override
//    public List<Email> fetchUnreadEmails() {
//        try {
//            if (graphClient == null) {
//                initializeGraphClient();
//            }
//
//            // Query for unread emails
//            MessageCollectionPage messages = graphClient.me().messages()
//                    .buildRequest()
//                    .filter("isRead eq false")
//                    .top(10)
//                    .get();
//
//            List<Email> emails = new ArrayList<>();
//            if (messages != null && messages.getCurrentPage() != null) {
//                for (Message message : messages.getCurrentPage()) {
//                    if (!hasBeenRepliedTo(message.id)) {
//                        Email email = convertToEmail(message);
//                        emails.add(email);
//                    }
//                }
//            }
//
//            return emails;
//        } catch (Exception e) {
//            log.error("Error fetching unread emails from Outlook", e);
//            return Collections.emptyList();
//        }
//    }
//
//    @Override
//    public boolean sendResponse(Email originalEmail, String responseText) {
//        try {
//            if (graphClient == null) {
//                initializeGraphClient();
//            }
//
//            // Create a new message
//            Message message = new Message();
//            message.subject = "Re: " + originalEmail.getSubject();
//
//            // Set the body
//            ItemBody body = new ItemBody();
//            body.contentType = BodyType.TEXT;
//            body.content = responseText;
//            message.body = body;
//
//            // Set recipient (original sender)
//            Recipient recipient = new Recipient();
//            com.microsoft.graph.models.EmailAddress emailAddress = new com.microsoft.graph.models.EmailAddress();
//            emailAddress.address = originalEmail.getSenderEmail();
//            recipient.emailAddress = emailAddress;
//            message.toRecipients = Collections.singletonList(recipient);
//
//            // Set reply metadata
//            message.conversationId = originalEmail.getConversationId();
//
//            // Send the email
//            graphClient.me()
//                    .sendMail(UserSendMailParameterSet.newBuilder()
//                            .withMessage(message)
//                            .withSaveToSentItems(true)
//                            .build())
//                    .buildRequest()
//                    .post();
//
//            return true;
//        } catch (Exception e) {
//            log.error("Error sending response via Outlook", e);
//            return false;
//        }
//    }
//
//    @Override
//    public boolean saveResponseToDrafts(Email originalEmail, String responseText) {
//        try {
//            if (graphClient == null) {
//                initializeGraphClient();
//            }
//
//            // Create a new message
//            Message message = new Message();
//            message.subject = "Re: " + originalEmail.getSubject();
//
//            // Set the body
//            ItemBody body = new ItemBody();
//            body.contentType = BodyType.TEXT;
//            body.content = responseText;
//            message.body = body;
//
//            // Set recipient (original sender)
//            Recipient recipient = new Recipient();
//            com.microsoft.graph.models.EmailAddress emailAddress = new com.microsoft.graph.models.EmailAddress();
//            emailAddress.address = originalEmail.getSenderEmail();
//            recipient.emailAddress = emailAddress;
//            message.toRecipients = Collections.singletonList(recipient);
//
//            // Set reply metadata
//            message.conversationId = originalEmail.getConversationId();
//
//            // Save the email to drafts folder instead of sending it
//            Message createdMessage = graphClient.me()
//                    .messages()
//                    .buildRequest()
//                    .post(message);
//
//            return createdMessage != null && createdMessage.id != null;
//        } catch (Exception e) {
//            log.error("Error saving response to drafts via Outlook", e);
//            return false;
//        }
//    }
//
//    @Override
//    public boolean markAsRead(String emailId) {
//        try {
//            if (graphClient == null) {
//                initializeGraphClient();
//            }
//
//            Message message = new Message();
//            message.isRead = true;
//
//            graphClient.me().messages(emailId)
//                    .buildRequest()
//                    .patch(message);
//
//            return true;
//        } catch (Exception e) {
//            log.error("Error marking email as read in Outlook", e);
//            return false;
//        }
//    }
//
//    @Override
//    public boolean hasBeenRepliedTo(String emailId) {
//        try {
//            if (graphClient == null) {
//                initializeGraphClient();
//            }
//
//            Message message = graphClient.me().messages(emailId)
//                    .buildRequest()
//                    .select("isRead,flag")
//                    .get();
//
//            // Check if message has been replied to (this is a simplified check)
//            // In a real application, you might want to use a more reliable method
//            return message.isRead != null && message.isRead;
//        } catch (Exception e) {
//            log.error("Error checking if email has been replied to in Outlook", e);
//            return false;
//        }
//    }
//
//    @Override
//    public String getProviderName() {
//        return "OUTLOOK";
//    }
//
//    private void initializeGraphClient() {
//        // Create the auth provider
//        final ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
//                .clientId(clientId)
//                .clientSecret(clientSecret)
//                .tenantId(tenantId)
//                .build();
//
//        final TokenCredentialAuthProvider tokenCredentialAuthProvider =
//                new TokenCredentialAuthProvider(SCOPES, clientSecretCredential);
//
//        // Build a Graph client
//        graphClient = GraphServiceClient.builder()
//                .authenticationProvider(tokenCredentialAuthProvider)
//                .buildClient();
//    }
//
//    private Email convertToEmail(Message message) {
//        String subject = message.subject != null ? message.subject : "";
//        String sender = "";
//        String senderEmail = "";
//        List<String> recipients = new ArrayList<>();
//        String body = "";
//
//        // Extract sender info
//        if (message.from != null && message.from.emailAddress != null) {
//            sender = message.from.emailAddress.name != null ? message.from.emailAddress.name : "";
//            senderEmail = message.from.emailAddress.address;
//        }
//
//        // Extract recipients
//        if (message.toRecipients != null) {
//            recipients = message.toRecipients.stream()
//                    .filter(r -> r.emailAddress != null && r.emailAddress.address != null)
//                    .map(r -> r.emailAddress.address)
//                    .collect(Collectors.toList());
//        }
//
//        // Extract body
//        if (message.body != null) {
//            body = message.body.content;
//
//            // If body is HTML, we might want to strip HTML tags for plain text
//            if (message.body.contentType == BodyType.HTML) {
//                body = body.replaceAll("<[^>]*>", "");
//            }
//        }
//
//        // Extract received date
//        LocalDateTime receivedDate = LocalDateTime.now();
//        if (message.receivedDateTime != null) {
//            receivedDate = LocalDateTime.ofInstant(
//                    message.receivedDateTime.toInstant(),
//                    ZoneId.systemDefault()
//            );
//        }
//
//        return Email.builder()
//                .id(message.id)
//                .source(getProviderName())
//                .sender(sender)
//                .senderEmail(senderEmail)
//                .recipients(recipients)
//                .subject(subject)
//                .body(body)
//                .receivedAt(receivedDate)
//                .isRead(message.isRead != null && message.isRead)
//                .hasBeenRepliedTo(false)
//                .conversationId(message.conversationId)
//                .build();
//    }
//}