package com.synlee.qforge.chatbot;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.Model;
import com.synlee.qforge.client.AnthropicClientProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * QForge — Module 3: Chatbot Service
 *
 * A Claude-backed chatbot that maintains full conversation history
 * across multiple turns, enabling real multi-turn dialogue.
 *
 * Each ChatbotService instance maintains its own independent
 * conversation history — create a new instance to start a fresh chat.
 *
 * Usage:
 *   ChatbotService chatbot = new ChatbotService();
 *   String reply1 = chatbot.chat("What is Selenium WebDriver?");
 *   String reply2 = chatbot.chat("How does it compare to Playwright?");
 *   // Claude remembers the first message when answering the second
 */
public class ChatbotService {
    private static final int MAX_TOKENS = 1024;

    // System prompt that gives the chatbot its persona and focus
    private static final String SYSTEM_PROMPT = """
            You are QForge Assistant, an expert QA automation chatbot built into the
            QForge AI-powered test automation toolkit.
            
            Your expertise covers:
            - API test automation (Rest Assured, Postman, Swagger)
            - UI automation (Selenium WebDriver, Playwright, Healenium)
            - Mobile automation (Appium, UiAutomator2)
            - Performance testing (JMeter)
            - CI/CD pipelines (Jenkins, Maven, GitHub Actions)
            - Test frameworks (TestNG, JUnit, Cucumber)
            - Defect management (JIRA, TestRail)
            - Java programming for test automation
            
            Guidelines:
            - Keep answers concise, practical, and focused on QA/testing topics
            - If asked about something outside QA/testing, politely redirect
            - Always be helpful, professional, and encouraging
            - When giving code examples, use Java
            """;

    private final AnthropicClient client;

    // Conversation history — grows with each turn
    private final List<MessageParam> conversationHistory;

    // Track turn count for reporting
    private int turnCount;

    public ChatbotService() {
        this.client = AnthropicClientProvider.getClient();
        this.conversationHistory = new ArrayList<>();
        this.turnCount = 0;
    }

    /**
     * Sends a message to the chatbot and returns its response.
     * Conversation history is automatically maintained between calls.
     *
     * @param userMessage The user's message
     * @return The chatbot's response text
     */
    public String chat(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("[QForge] User message cannot be null or blank");
        }

        turnCount++;
        System.out.println("\n[QForge] Turn " + turnCount + " — User: " + userMessage);

        // Add the user's message to conversation history
        conversationHistory.add(
                MessageParam.builder()
                        .role(MessageParam.Role.USER)
                        .content(userMessage)
                        .build()
        );

        // Build the API request with full conversation history
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_SONNET_4_6)
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .messages(conversationHistory)
                .build();

        // Call Claude API
        Message message = client.messages().create(params);

        // Extract the response text
        String responseText = extractText(message);

        // Add Claude's response to conversation history for next turn
        conversationHistory.add(
                MessageParam.builder()
                        .role(MessageParam.Role.ASSISTANT)
                        .content(responseText)
                        .build()
        );

        System.out.println("[QForge] Turn " + turnCount + " — Assistant: " +
                responseText.substring(0, Math.min(120, responseText.length())) + "...");

        return responseText;
    }

    /**
     * Resets the conversation history, starting a fresh chat session.
     * The system prompt and configuration are preserved.
     */
    public void resetConversation() {
        conversationHistory.clear();
        turnCount = 0;
        System.out.println("[QForge] Conversation history reset.");
    }

    /**
     * Returns the number of conversation turns so far.
     */
    public int getTurnCount() {
        return turnCount;
    }

    /**
     * Returns an unmodifiable view of the conversation history.
     */
    public List<MessageParam> getConversationHistory() {
        return Collections.unmodifiableList(conversationHistory);
    }

    /**
     * Returns true if there is at least one completed exchange
     * (one user message + one assistant response).
     */
    public boolean hasHistory() {
        return conversationHistory.size() >= 2;
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
}
