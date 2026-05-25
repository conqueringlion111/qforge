package com.synlee.qforge.generator;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.synlee.qforge.client.AnthropicClientProvider;

/**
 * QForge — Module 1: AI Test Generator
 *
 * Sends a REST API endpoint description to Claude and receives
 * a fully written Rest Assured test class in return.
 *
 * Usage:
 *   TestGeneratorService generator = new TestGeneratorService();
 *   String testCode = generator.generateTests("GET /users/{id} returns name, email, age");
 *   System.out.println(testCode);
 */

public class TestGeneratorService {

    // The Claude model to use
    private static final String MODEL = "claude-sonnet-4-6";

    // Max tokens for the response (1000 is plenty for a test class)
    private static final int MAX_TOKENS = 1024;

    // The system prompt tells Claude exactly what role to play
    private static final String SYSTEM_PROMPT = """
            You are an expert Java test automation engineer specializing in REST API testing.
            
            When given a description of a REST API endpoint, you will generate a complete,
            ready-to-run Rest Assured test class in Java using TestNG.
            
            Your generated code must always:
            - Use Rest Assured for HTTP requests
            - Use TestNG annotations (@Test, @BeforeClass)
            - Use Hamcrest matchers for assertions (e.g. equalTo, notNullValue, hasItems)
            - Follow Page Object Model naming conventions
            - Include a @BeforeClass method that sets the base URI
            - Include at least 3 test methods:
                1. A happy path / 200 OK test
                2. A validation test (checks response fields)
                3. A negative test (e.g. 404 not found, invalid input)
            - Include meaningful comments explaining each test
            - Use proper Java package: com.synlee.qforge.generator
            
            Return ONLY the Java code. No explanations, no markdown, no code fences.
            Just raw Java code that can be pasted directly into a .java file and compiled.
            """;

    private final AnthropicClient client;

    public TestGeneratorService() {
        this.client = AnthropicClientProvider.getClient();
    }

    /**
     * Generates a Rest Assured test class based on the endpoint description.
     *
     * @param endpointDescription Plain English description of the API endpoint
     *                            e.g. "GET /users/{id} returns user name, email, and age"
     * @return A complete Java test class as a String
     */
    public String generateTests(String endpointDescription) {
        System.out.println("[QForge] Generating tests for: " + endpointDescription);

        // Build the message to send to Claude
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_SONNET_4_6)
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .addUserMessage("Generate a Rest Assured TestNG test class for this endpoint:\n\n"
                        + endpointDescription)
                .build();

        // Call the Claude API
        Message response = client.messages().create(params);

        // Extract the text from the response
        return extractText(response);
    }

    /**
     * Extracts the plain text content from the Claude API response.
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