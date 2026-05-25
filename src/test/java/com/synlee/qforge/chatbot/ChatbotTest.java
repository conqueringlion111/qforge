package com.synlee.qforge.chatbot;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * QForge — Module 3: Chatbot Test Suite
 *
 * Tests the ChatbotService for:
 *   - Basic single-turn response quality
 *   - Multi-turn conversation memory
 *   - System prompt persona adherence
 *   - Edge case handling (empty input, reset behavior)
 *   - QA domain knowledge accuracy
 *   - Conversation history management
 */
public class ChatbotTest {
    private ChatbotService chatbot;

    // Maximum acceptable response time per turn (30 seconds)
    private static final long MAX_RESPONSE_TIME_MS = 30_000;

    // Minimum meaningful response length
    private static final int MIN_RESPONSE_LENGTH = 20;

    /**
     * Create a fresh ChatbotService before each test
     * so every test starts with a clean conversation history.
     */
    @BeforeMethod
    public void setUp() {
        System.out.println("\n[QForge] Starting fresh chatbot session...");
        chatbot = new ChatbotService();
    }

    // ═══════════════════════════════════════════════════════════
    // Test 1 — Basic Single Turn Response
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that the chatbot returns a meaningful response
     * to a simple QA-related question.
     */
    @Test(priority = 1, description = "Chatbot returns a non-empty response to a basic QA question")
    public void testBasicSingleTurnResponse() {
        long start = System.currentTimeMillis();
        String response = chatbot.chat("What is TestNG?");
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\n--- Response ---");
        System.out.println(response);
        System.out.println("Response time: " + elapsed + "ms");

        Assert.assertNotNull(response, "Response should not be null");
        Assert.assertFalse(response.isBlank(), "Response should not be blank");
        Assert.assertTrue(response.length() >= MIN_RESPONSE_LENGTH,
                "Response too short: " + response.length() + " chars");
        Assert.assertTrue(elapsed <= MAX_RESPONSE_TIME_MS,
                "Response time exceeded limit: " + elapsed + "ms");
        Assert.assertTrue(
                response.toLowerCase().contains("testng") ||
                        response.toLowerCase().contains("test") ||
                        response.toLowerCase().contains("framework"),
                "Response should mention TestNG or testing concepts");

        System.out.println("[QForge] testBasicSingleTurnResponse — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 2 — Multi-Turn Conversation Memory
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that the chatbot remembers context from earlier
     * turns when answering follow-up questions.
     * This is the key differentiator of a real chatbot vs a single prompt.
     */
    @Test(priority = 2, description = "Chatbot remembers context from previous turns in conversation")
    public void testMultiTurnConversationMemory() {
        // Turn 1 — establish a topic
        String response1 = chatbot.chat(
                "I am working on a Rest Assured API automation framework in Java."
        );
        System.out.println("\n--- Turn 1 Response ---");
        System.out.println(response1);

        Assert.assertNotNull(response1, "Turn 1 response should not be null");
        Assert.assertFalse(response1.isBlank(), "Turn 1 response should not be blank");

        // Turn 2 — follow up without repeating context
        // Claude should remember we are talking about Rest Assured
        String response2 = chatbot.chat(
                "What is the best way to handle authentication headers in it?"
        );
        System.out.println("\n--- Turn 2 Response ---");
        System.out.println(response2);

        Assert.assertNotNull(response2, "Turn 2 response should not be null");
        Assert.assertFalse(response2.isBlank(), "Turn 2 response should not be blank");

        // Response should reference Rest Assured or headers/authentication
        Assert.assertTrue(
                response2.toLowerCase().contains("header") ||
                        response2.toLowerCase().contains("auth") ||
                        response2.toLowerCase().contains("rest assured") ||
                        response2.toLowerCase().contains("given()"),
                "Turn 2 response should reference headers or authentication in context");

        // Turn 3 — go even deeper
        String response3 = chatbot.chat(
                "Can you show me a Java code example of that?"
        );
        System.out.println("\n--- Turn 3 Response ---");
        System.out.println(response3);

        Assert.assertNotNull(response3, "Turn 3 response should not be null");
        Assert.assertFalse(response3.isBlank(), "Turn 3 response should not be blank");

        // Response should contain Java code
        Assert.assertTrue(
                response3.contains("given()") ||
                        response3.contains("header(") ||
                        response3.contains("RestAssured") ||
                        response3.contains("import "),
                "Turn 3 response should contain a Java code example");

        // Validate conversation history grew correctly
        Assert.assertEquals(chatbot.getTurnCount(), 3,
                "Turn count should be 3 after 3 exchanges");
        Assert.assertTrue(chatbot.hasHistory(),
                "Chatbot should have conversation history");

        System.out.println("[QForge] testMultiTurnConversationMemory — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 3 — Persona Adherence (QA Focus)
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that the chatbot stays in its QForge Assistant persona
     * and responds with QA-relevant content.
     */
    @Test(priority = 3, description = "Chatbot stays in QForge Assistant persona and focuses on QA topics")
    public void testPersonaAdherence() {
        String response = chatbot.chat(
                "As a QA automation engineer, what tools should I know in 2026?"
        );

        System.out.println("\n--- Persona Response ---");
        System.out.println(response);

        Assert.assertNotNull(response, "Response should not be null");
        Assert.assertFalse(response.isBlank(), "Response should not be blank");

        // Should mention QA tools from the system prompt context
        Assert.assertTrue(
                response.toLowerCase().contains("selenium") ||
                        response.toLowerCase().contains("playwright") ||
                        response.toLowerCase().contains("appium") ||
                        response.toLowerCase().contains("rest assured") ||
                        response.toLowerCase().contains("jenkins") ||
                        response.toLowerCase().contains("testng") ||
                        response.toLowerCase().contains("jmeter"),
                "Response should mention at least one QA automation tool");

        System.out.println("[QForge] testPersonaAdherence — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 4 — Conversation Reset Behavior
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that resetConversation() clears history correctly
     * and the chatbot starts fresh after reset.
     */
    @Test(priority = 4, description = "Conversation resets correctly and chatbot starts fresh after reset")
    public void testConversationResetBehavior() {
        // Have a conversation
        chatbot.chat("Tell me about Selenium WebDriver.");
        chatbot.chat("How does it handle dynamic elements?");

        // Validate history exists
        Assert.assertEquals(chatbot.getTurnCount(), 2,
                "Turn count should be 2 before reset");
        Assert.assertTrue(chatbot.hasHistory(),
                "Should have history before reset");
        Assert.assertEquals(chatbot.getConversationHistory().size(), 4,
                "History should have 4 entries (2 user + 2 assistant)");

        // Reset the conversation
        chatbot.resetConversation();

        // Validate history is cleared
        Assert.assertEquals(chatbot.getTurnCount(), 0,
                "Turn count should be 0 after reset");
        Assert.assertFalse(chatbot.hasHistory(),
                "Should have no history after reset");
        Assert.assertEquals(chatbot.getConversationHistory().size(), 0,
                "History should be empty after reset");

        // Validate chatbot still works after reset
        String response = chatbot.chat("What is JMeter used for?");
        Assert.assertNotNull(response, "Response after reset should not be null");
        Assert.assertFalse(response.isBlank(), "Response after reset should not be blank");
        Assert.assertTrue(
                response.toLowerCase().contains("jmeter") ||
                        response.toLowerCase().contains("performance") ||
                        response.toLowerCase().contains("load"),
                "Response after reset should be about JMeter");

        System.out.println("[QForge] testConversationResetBehavior — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 5 — Edge Case: Empty Input Handling
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that the chatbot throws a clear exception
     * when given a null or blank message, rather than
     * sending a bad request to the API.
     */
    @Test(priority = 5, description = "Chatbot throws IllegalArgumentException for null or blank input")
    public void testEdgeCaseEmptyInputHandling() {
        // Test null input
        try {
            chatbot.chat(null);
            Assert.fail("Should have thrown IllegalArgumentException for null input");
        } catch (IllegalArgumentException e) {
            System.out.println("[QForge] Correctly caught null input: " + e.getMessage());
            Assert.assertTrue(e.getMessage().contains("null or blank"),
                    "Exception message should mention null or blank");
        }

        // Test blank input
        try {
            chatbot.chat("   ");
            Assert.fail("Should have thrown IllegalArgumentException for blank input");
        } catch (IllegalArgumentException e) {
            System.out.println("[QForge] Correctly caught blank input: " + e.getMessage());
            Assert.assertTrue(e.getMessage().contains("null or blank"),
                    "Exception message should mention null or blank");
        }

        // Validate turn count is still 0 (bad inputs not counted)
        Assert.assertEquals(chatbot.getTurnCount(), 0,
                "Turn count should remain 0 after invalid inputs");

        System.out.println("[QForge] testEdgeCaseEmptyInputHandling — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 6 — QA Knowledge: CI/CD Pipeline Question
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that the chatbot answers a CI/CD pipeline question
     * accurately — directly relevant to your Jenkins experience.
     */
    @Test(priority = 6, description = "Chatbot accurately answers CI/CD pipeline question")
    public void testCiCdPipelineKnowledge() {
        String response = chatbot.chat(
                "How do I integrate TestNG test execution into a Jenkins pipeline?"
        );

        System.out.println("\n--- CI/CD Response ---");
        System.out.println(response);

        Assert.assertNotNull(response, "Response should not be null");
        Assert.assertFalse(response.isBlank(), "Response should not be blank");

        // Should mention Jenkins and/or Maven and/or TestNG
        Assert.assertTrue(
                response.toLowerCase().contains("jenkins") ||
                        response.toLowerCase().contains("pipeline") ||
                        response.toLowerCase().contains("maven") ||
                        response.toLowerCase().contains("mvn"),
                "Response should mention Jenkins, pipeline, or Maven");

        Assert.assertTrue(response.length() >= MIN_RESPONSE_LENGTH,
                "Response too short: " + response.length() + " chars");

        System.out.println("[QForge] testCiCdPipelineKnowledge — PASSED ✓");
    }
}
