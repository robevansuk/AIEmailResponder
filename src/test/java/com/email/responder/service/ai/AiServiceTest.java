package com.email.responder.service.ai;

import com.emailai.responder.service.ai.AiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AiServiceTest {

    private static final String EXPECTED_RESPONSE = "Thank you for reaching out with this opportunity. Could you please provide more details about the role, including the company, job responsibilities, location, and any specific qualifications or skills required? Additionally, information about the team and company culture would be helpful. This will allow me to evaluate the opportunity more thoroughly. Looking forward to your response.";

    private static final String response = """
                {
              "id": "chatcmpl-B7UwUrKkDT0Oob1B8EdYHS95uuycS",
              "object": "chat.completion",
              "created": 1741126938,
              "model": "gpt-4o-2024-08-06",
              "choices": [
                {
                  "index": 0,
                  "message": {
                    "role": "assistant",
                    "content": "Thank you for reaching out with this opportunity. Could you please provide more details about the role, including the company, job responsibilities, location, and any specific qualifications or skills required? Additionally, information about the team and company culture would be helpful. This will allow me to evaluate the opportunity more thoroughly. Looking forward to your response.",
                    "refusal": null
                  },
                  "logprobs": null,
                  "finish_reason": "stop"
                }
              ],
              "usage": {
                "prompt_tokens": 149,
                "completion_tokens": 67,
                "total_tokens": 216,
                "prompt_tokens_details": {
                  "cached_tokens": 0,
                  "audio_tokens": 0
                },
                "completion_tokens_details": {
                  "reasoning_tokens": 0,
                  "audio_tokens": 0,
                  "accepted_prediction_tokens": 0,
                  "rejected_prediction_tokens": 0
                }
              },
              "service_tier": "default",
              "system_fingerprint": "fp_eb9dce56a8"
            }
            """;

    AiService testObject;

    WebClient.Builder webClientBuilder;
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.webClientBuilder = WebClient.builder();
        this.objectMapper = new ObjectMapper();
        testObject = new AiService(webClientBuilder, objectMapper);
    }

    @Test
    void shouldParseJsonCorrectly() throws JsonProcessingException {
        AiService.OpenAiResponse openAiResponse = testObject.parseResponse(response);
        assertEquals(EXPECTED_RESPONSE, openAiResponse.getChoices().get(0).getMessage().getContent());
    }
}