package com.synlee.qforge.validator;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.synlee.qforge.client.AnthropicClientProvider;

/**
 * QForge — Module 2: LLM Response Validator
 *
 * Sends prompts to Claude and captures the response along with
 * quality metrics (response text, length, response time).
 *
 * The LlmValidatorTest class then validates these metrics against
 * configurable rules to pass or fail the test.
 *
 * Usage:
 *   LlmValidatorService validator = new LlmValidatorService();
 *   LlmResponse response = validator.sendPrompt("Explain what an API is in one sentence.");
 *   System.out.println(response.getText());
 *   System.out.println(response.getResponseTimeMs());
 */

public class LlmValidatorService {
    private static final int MAX_TOKENS = 1024;

    private final AnthropicClient client;

    public LlmValidatorService() {
        this.client = AnthropicClientProvider.getClient();
    }

    /**
     * Sends a prompt to Claude and returns an LlmResponse object
     * containing the response text and performance metrics.
     *
     * @param prompt The prompt to send to Claude
     * @return LlmResponse containing text, length, and response time
     */
    public LlmResponse sendPrompt(String prompt) {
        return sendPrompt(prompt, null);
    }

    /**
     * Sends a prompt to Claude with an optional system prompt.
     *
     * @param prompt       The user prompt to send
     * @param systemPrompt Optional system prompt (pass null to skip)
     * @return LlmResponse containing text, length, and response time
     */
    public LlmResponse sendPrompt(String prompt, String systemPrompt) {
        System.out.println("[QForge] Sending prompt: " + prompt);

        // Record start time for response time measurement
        long startTime = System.currentTimeMillis();

        // Build the message params
        MessageCreateParams.Builder paramsBuilder = MessageCreateParams.builder()
                .model(Model.CLAUDE_SONNET_4_6)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(prompt);

        // Add system prompt if provided
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            paramsBuilder.system(systemPrompt);
        }

        // Call Claude API
        Message message = client.messages().create(paramsBuilder.build());

        // Record end time
        long responseTimeMs = System.currentTimeMillis() - startTime;

        // Extract response text
        String responseText = extractText(message);

        System.out.println("[QForge] Response received in " + responseTimeMs + "ms");
        System.out.println("[QForge] Response length: " + responseText.length() + " characters");

        return new LlmResponse(responseText, responseTimeMs);
    }

    /**
     * Extracts plain text from the Claude API message response.
     */
    private String extractText(Message message) {
        StringBuilder result = new StringBuilder();
        for (ContentBlock block : message.content()) {
            if (block.isText()) {
                result.append(block.asText().text());
            }
        }
        return result.toString().trim();
    }

    // ═══════════════════════════════════════════════════════════
    // Inner class: LlmResponse
    // Holds the response text and quality metrics
    // ═══════════════════════════════════════════════════════════

    /**
     * Immutable data container for a Claude API response.
     * Holds the response text and performance metrics for validation.
     */
    public static class LlmResponse {

        private final String text;
        private final long responseTimeMs;

        public LlmResponse(String text, long responseTimeMs) {
            this.text = text;
            this.responseTimeMs = responseTimeMs;
        }

        /** The full text of Claude's response */
        public String getText() {
            return text;
        }

        /** How long the API call took in milliseconds */
        public long getResponseTimeMs() {
            return responseTimeMs;
        }

        /** Length of the response in characters */
        public int getLength() {
            return text != null ? text.length() : 0;
        }

        /** Whether the response contains a specific keyword (case-insensitive) */
        public boolean containsKeyword(String keyword) {
            return text != null && text.toLowerCase().contains(keyword.toLowerCase());
        }

        /** Whether the response starts with a specific prefix (case-insensitive) */
        public boolean startsWith(String prefix) {
            return text != null && text.toLowerCase().startsWith(prefix.toLowerCase());
        }

        /** Whether the response is not null and not blank */
        public boolean isNotEmpty() {
            return text != null && !text.isBlank();
        }

        @Override
        public String toString() {
            return "LlmResponse{" +
                    "responseTimeMs=" + responseTimeMs +
                    ", length=" + getLength() +
                    ", text='" + (text != null ? text.substring(0, Math.min(100, text.length())) + "..." : "null") + "'" +
                    '}';
        }
    }
}
