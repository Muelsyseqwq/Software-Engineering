package com.nekocafe.recommend.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

@Component
public class OpenAiCompatibleReasonClient {

    private final AiReasonProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleReasonClient(AiReasonProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (!properties.available()) {
            throw new IllegalStateException("AI reason generation is disabled or not configured");
        }
        RestClient client = RestClient.builder()
            .baseUrl(normalizeBaseUrl(properties.getBaseUrl()))
            .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(1, properties.getTimeoutSeconds())))
                .build()))
            .build();

        ChatCompletionRequest request = new ChatCompletionRequest(
            properties.getModel(),
            List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
            ),
            properties.getTemperature(),
            properties.getMaxTokens(),
            new ResponseFormat("json_object")
        );

        String responseBody = client.post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + properties.getApiKey())
            .body(request)
            .retrieve()
            .body(String.class);

        return extractContent(responseBody);
    }

    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("AI provider returned blank HTTP body");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new IllegalStateException("AI provider response has no choices");
            }
            JsonNode message = choices.get(0).path("message");
            if (message.isMissingNode() || message.isNull()) {
                throw new IllegalStateException("AI provider response has no message");
            }
            String content = textOrBlank(message.path("content"));
            if (content.isBlank()) {
                content = textOrBlank(message.path("reasoning_content"));
            }
            if (content.isBlank()) {
                throw new IllegalStateException("AI provider message has blank content; message fields=" + message.fieldNames().toString());
            }
            return content;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("AI provider returned non-JSON response", ex);
        }
    }

    private String textOrBlank(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? "" : node.asText("").trim();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl == null || baseUrl.isBlank() ? "https://api.deepseek.com" : baseUrl.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    public record ChatCompletionRequest(
        String model,
        List<ChatMessage> messages,
        Double temperature,
        Integer max_tokens,
        ResponseFormat response_format
    ) {
    }

    public record ResponseFormat(String type) {
    }

    public record ChatMessage(String role, String content) {
    }

    public record ChatCompletionResponse(List<ChatChoice> choices) {
    }

    public record ChatChoice(ChatMessage message) {
    }
}
