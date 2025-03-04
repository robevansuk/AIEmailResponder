package com.emailai.responder.service.ai;

import com.emailai.responder.model.Email;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.N;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AiService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.model}")
    private String model;

    @Value("${ai.max.tokens}")
    private int maxTokens;

    @Value("${email.response.signature}")
    private String emailSignature;

    public AiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public String generateResponse(Email email) {
        try {
            String prompt = createPrompt(email);
            String response = callAiApi(prompt);

            // Add signature
            return response + emailSignature;
        } catch (Exception e) {
            log.error("Error generating AI response", e);
            return "I've received your email and will get back to you soon." + emailSignature;
        }
    }

    private String createPrompt(Email email) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("Generate a professional and helpful response to the following email:\n\n");
        promptBuilder.append("From: ").append(email.getSender()).append(" <").append(email.getSenderEmail()).append(">\n");
        promptBuilder.append("Subject: ").append(email.getSubject()).append("\n\n");
        promptBuilder.append(email.getBody()).append("\n\n");

        promptBuilder.append("Important guidelines for your response:\n");
        promptBuilder.append("1. Be concise but thorough\n");
        promptBuilder.append("2. Maintain a professional tone\n");
        promptBuilder.append("3. Address the specific questions or requests in the email\n");
        promptBuilder.append("4. Don't make up information you don't have\n");
        promptBuilder.append("5. If you can't answer something, suggest an appropriate follow-up\n");
        promptBuilder.append("6. Do not include any greeting or signature as these will be added separately\n");

        return promptBuilder.toString();
    }

    private String callAiApi(String prompt) {
        try {
            // Build request body for OpenAI API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", maxTokens);
            requestBody.put("temperature", 0.7);

            // Make the API call
            String responseJson = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(requestBody))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // Parse the response
            final OpenAiResponse openAiResponse = objectMapper.readValue(responseJson, OpenAiResponse.class);

            if (openAiResponse != null && !openAiResponse.getChoices().isEmpty()) {
                return openAiResponse.getChoices().get(0).getMessage().getContent();
            }

            return "Thank you for your email. I'll respond to your inquiry shortly.";
        } catch (Exception e) {
            log.error("Error calling AI API", e);
            return "Thank you for your email. I'll respond to your inquiry shortly.";
        }
    }

    public OpenAiResponse parseResponse(String responseJson) throws JsonProcessingException {
        return objectMapper.readValue(responseJson, OpenAiResponse.class);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenAiResponse {
        private String id;
        private String object;
        private long created;
        private String model;
        private List<Choice> choices;
        private Usage usage;
        private String service_tier;
        private String system_fingerprint;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Choice {
            private int index;
            private Message message;
            private String logprobs;
            private String finish_reason;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Message {
                private String role;
                private String content;
                private String refusal;
            }
        }

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Usage {
            private int prompt_tokens;
            private int completion_tokens;
            private String total_tokens;
            private PromptTokensDetails prompt_tokens_details;
            private CompletionTokensDetials completion_tokens_details;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class PromptTokensDetails {
                private int cached_tokens;
                private int audio_tokens;
            }

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class CompletionTokensDetials {
                private int reasoning_tokens;
                private int audio_tokens;
                private int accepted_prediction_tokens;
                private int rejected_prediction_tokens;

            }
        }
    }
}