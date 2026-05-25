package com.synlee.qforge.validator;

import com.synlee.qforge.validator.LlmValidatorService.LlmResponse;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * QForge — Module 2: LLM Response Validator Tests
 *
 * Validates Claude API responses against quality rules:
 *   - Response is not null or empty
 *   - Response contains required keywords
 *   - Response length is within acceptable bounds
 *   - Response time is within acceptable threshold
 *   - Response follows requested format or structure
 *   - Response handles edge cases gracefully
 */

public class LlmValidatorTest {
    private LlmValidatorService validatorService;

    // Maximum acceptable response time in milliseconds (30 seconds)
    private static final long MAX_RESPONSE_TIME_MS = 30_000;

    // Minimum acceptable response length in characters
    private static final int MIN_RESPONSE_LENGTH = 20;

    // Maximum acceptable response length in characters
    private static final int MAX_RESPONSE_LENGTH = 5000;

    @BeforeClass
    public void setUp() {
        System.out.println("[QForge] Initializing LlmValidatorTest...");
        validatorService = new LlmValidatorService();
    }

    // ═══════════════════════════════════════════════════════════
    // Test 1 — Basic Response Quality
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that Claude returns a non-empty response
     * within an acceptable response time.
     */
    @Test(priority = 1, description = "Claude response is not null, not empty, and returned within time limit")
    public void testResponseIsNotEmptyAndWithinTimeLimit() {
        LlmResponse response = validatorService.sendPrompt(
                "What is REST API testing? Answer in 2-3 sentences."
        );

        System.out.println("\n--- Response ---");
        System.out.println(response.getText());
        System.out.println("Response time: " + response.getResponseTimeMs() + "ms");
        System.out.println("Response length: " + response.getLength() + " chars");

        // Validate response is not empty
        Assert.assertTrue(response.isNotEmpty(),
                "Response should not be null or empty");

        // Validate response time is within threshold
        Assert.assertTrue(response.getResponseTimeMs() <= MAX_RESPONSE_TIME_MS,
                "Response time " + response.getResponseTimeMs() +
                        "ms exceeded limit of " + MAX_RESPONSE_TIME_MS + "ms");

        // Validate response has meaningful length
        Assert.assertTrue(response.getLength() >= MIN_RESPONSE_LENGTH,
                "Response too short: " + response.getLength() + " chars");

        System.out.println("[QForge] testResponseIsNotEmptyAndWithinTimeLimit — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 2 — Keyword Validation
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that Claude's response contains expected keywords
     * relevant to the topic asked about.
     */
    @Test(priority = 2, description = "Claude response contains expected topic-relevant keywords")
    public void testResponseContainsExpectedKeywords() {
        LlmResponse response = validatorService.sendPrompt(
                "Explain what HTTP status codes 200, 404, and 500 mean in REST APIs."
        );

        System.out.println("\n--- Response ---");
        System.out.println(response.getText());

        Assert.assertTrue(response.isNotEmpty(), "Response should not be empty");

        // Validate response contains key HTTP status code references
        Assert.assertTrue(response.containsKeyword("200"),
                "Response should mention status code 200");
        Assert.assertTrue(response.containsKeyword("404"),
                "Response should mention status code 404");
        Assert.assertTrue(response.containsKeyword("500"),
                "Response should mention status code 500");

        // Validate response contains relevant API terminology
        Assert.assertTrue(
                response.containsKeyword("success") ||
                        response.containsKeyword("ok") ||
                        response.containsKeyword("found"),
                "Response should describe 200 meaning");

        Assert.assertTrue(
                response.containsKeyword("not found") ||
                        response.containsKeyword("missing") ||
                        response.containsKeyword("exist"),
                "Response should describe 404 meaning");

        Assert.assertTrue(
                response.containsKeyword("server") ||
                        response.containsKeyword("error") ||
                        response.containsKeyword("internal"),
                "Response should describe 500 meaning");

        System.out.println("[QForge] testResponseContainsExpectedKeywords — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 3 — Response Length Validation
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that Claude respects length constraints in its response.
     * Tests both a short answer request and a detailed answer request.
     */
    @Test(priority = 3, description = "Claude response length is within acceptable bounds")
    public void testResponseLengthIsWithinBounds() {
        // Request a short response
        LlmResponse shortResponse = validatorService.sendPrompt(
                "In exactly one sentence, what is Selenium WebDriver?"
        );

        System.out.println("\n--- Short Response ---");
        System.out.println(shortResponse.getText());
        System.out.println("Length: " + shortResponse.getLength() + " chars");

        Assert.assertTrue(shortResponse.getLength() >= MIN_RESPONSE_LENGTH,
                "Short response too short: " + shortResponse.getLength() + " chars");
        Assert.assertTrue(shortResponse.getLength() <= MAX_RESPONSE_LENGTH,
                "Short response too long: " + shortResponse.getLength() + " chars");

        // Request a detailed response
        LlmResponse detailedResponse = validatorService.sendPrompt(
                "Explain the Page Object Model design pattern in test automation. " +
                        "Include its benefits and a brief example."
        );

        System.out.println("\n--- Detailed Response ---");
        System.out.println(detailedResponse.getText());
        System.out.println("Length: " + detailedResponse.getLength() + " chars");

        // Detailed response should be longer than short response
        Assert.assertTrue(detailedResponse.getLength() > shortResponse.getLength(),
                "Detailed response should be longer than short response");

        Assert.assertTrue(detailedResponse.getLength() <= MAX_RESPONSE_LENGTH,
                "Detailed response too long: " + detailedResponse.getLength() + " chars");

        System.out.println("[QForge] testResponseLengthIsWithinBounds — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 4 — System Prompt Behavior Validation
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that Claude respects a system prompt that sets
     * a specific role and behavior constraint.
     */
    @Test(priority = 4, description = "Claude respects system prompt role and behavior instructions")
    public void testSystemPromptBehavior() {
        String systemPrompt = """
                You are a QA automation expert who only answers questions related to
                software testing, test automation, and quality assurance.
                Always include at least one practical example in your answers.
                Keep your answers concise and focused.
                """;

        LlmResponse response = validatorService.sendPrompt(
                "What is the difference between functional and non-functional testing?",
                systemPrompt
        );

        System.out.println("\n--- System Prompt Response ---");
        System.out.println(response.getText());

        Assert.assertTrue(response.isNotEmpty(),
                "Response with system prompt should not be empty");

        // Validate response contains testing-related content
        Assert.assertTrue(
                response.containsKeyword("functional") ||
                        response.containsKeyword("testing") ||
                        response.containsKeyword("performance"),
                "Response should contain testing-related content");

        // Validate response has meaningful length
        Assert.assertTrue(response.getLength() >= MIN_RESPONSE_LENGTH,
                "Response too short: " + response.getLength() + " chars");

        System.out.println("[QForge] testSystemPromptBehavior — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 5 — Edge Case: Short/Vague Prompt
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that Claude handles a very short or vague prompt
     * gracefully and still returns a meaningful response.
     */
    @Test(priority = 5, description = "Claude handles short or vague prompts gracefully")
    public void testEdgeCaseShortPrompt() {
        LlmResponse response = validatorService.sendPrompt("Testing.");

        System.out.println("\n--- Edge Case Response ---");
        System.out.println(response.getText());

        // Even with a vague prompt, Claude should return something meaningful
        Assert.assertTrue(response.isNotEmpty(),
                "Response to short prompt should not be empty");
        Assert.assertTrue(response.getLength() >= MIN_RESPONSE_LENGTH,
                "Response to short prompt too short: " + response.getLength() + " chars");
        Assert.assertTrue(response.getResponseTimeMs() <= MAX_RESPONSE_TIME_MS,
                "Response time exceeded limit");

        System.out.println("[QForge] testEdgeCaseShortPrompt — PASSED ✓");
    }

    // ═══════════════════════════════════════════════════════════
    // Test 6 — QA Domain Knowledge Validation
    // ═══════════════════════════════════════════════════════════

    /**
     * Validates that Claude demonstrates accurate QA domain knowledge
     * relevant to your expertise as a Senior Test Automation Engineer.
     */
    @Test(priority = 6, description = "Claude demonstrates accurate QA automation domain knowledge")
    public void testQaDomainKnowledgeAccuracy() {
        LlmResponse response = validatorService.sendPrompt(
                "List the key components of a Rest Assured API test in Java. " +
                        "Include: given, when, then structure."
        );

        System.out.println("\n--- QA Domain Knowledge Response ---");
        System.out.println(response.getText());

        Assert.assertTrue(response.isNotEmpty(), "Response should not be empty");

        // Validate Rest Assured specific terminology is present
        Assert.assertTrue(response.containsKeyword("given"),
                "Response should mention 'given'");
        Assert.assertTrue(response.containsKeyword("when"),
                "Response should mention 'when'");
        Assert.assertTrue(response.containsKeyword("then"),
                "Response should mention 'then'");

        // Validate Java/Rest Assured context
        Assert.assertTrue(
                response.containsKeyword("request") ||
                        response.containsKeyword("response") ||
                        response.containsKeyword("assertion"),
                "Response should contain API testing concepts");

        System.out.println("[QForge] testQaDomainKnowledgeAccuracy — PASSED ✓");
    }
}
