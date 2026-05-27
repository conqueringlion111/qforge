package com.synlee.qforge.agent;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.synlee.qforge.client.AnthropicClientProvider;

/**
 * QForge — AI Agent: Failure Analyzer Service
 *
 * Sends test failure details to Claude and receives:
 *   - Root cause analysis of the failure
 *   - Suggested fix for the test or the code
 *   - Confidence level of the fix
 *
 * This is the "brain" of the QForge AI Agent.
 */

public class FailureAnalyzerService {

    private static final int MAX_TOKENS = 1024;

    private static final String SYSTEM_PROMPT = """
            You are an expert Java test automation engineer specializing in
            Rest Assured API testing, TestNG, and Maven.
            
            When given a test failure, you will analyze it and respond with
            ONLY a JSON object in this exact format (no markdown, no explanation):
            {
              "rootCause": "Brief description of why the test failed",
              "fixSuggestion": "Specific actionable fix suggestion",
              "fixedCode": "The corrected Java code snippet if applicable, or null",
              "confidence": "HIGH | MEDIUM | LOW",
              "requiresHumanReview": true | false
            }
            
            Guidelines:
            - rootCause: be specific, not generic
            - fixSuggestion: be actionable and precise
            - fixedCode: provide the corrected method or snippet only, not the whole class
            - confidence: HIGH if the fix is clear, MEDIUM if uncertain, LOW if complex
            - requiresHumanReview: true if the fix needs human judgment or access to the app
            """;

    private final AnthropicClient client;

    public FailureAnalyzerService() {
        this.client = AnthropicClientProvider.getClient();
    }

    /**
     * Analyzes a test failure and returns a FailureAnalysis object
     * containing root cause, fix suggestion, and confidence level.
     *
     * @param testName   The name of the failing test method
     * @param errorMsg   The assertion or exception error message
     * @param stackTrace The relevant portion of the stack trace
     * @param testCode   The original test method code (optional, pass null if unavailable)
     * @return FailureAnalysis containing Claude's diagnosis and fix
     */
    public FailureAnalysis analyze(String testName,
                                   String errorMsg,
                                   String stackTrace,
                                   String testCode) {

        System.out.println("[QForge Agent] Analyzing failure: " + testName);

        String prompt = buildPrompt(testName, errorMsg, stackTrace, testCode);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_SONNET_4_6)
                .maxTokens(MAX_TOKENS)
                .system(SYSTEM_PROMPT)
                .addUserMessage(prompt)
                .build();

        Message message = client.messages().create(params);
        String responseText = extractText(message);

        System.out.println("[QForge Agent] Analysis received for: " + testName);

        return parseAnalysis(responseText, testName);
    }

    /**
     * Builds the prompt sent to Claude with all failure context.
     */
    private String buildPrompt(String testName,
                               String errorMsg,
                               String stackTrace,
                               String testCode) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze this Java TestNG test failure:\n\n");
        prompt.append("TEST NAME: ").append(testName).append("\n\n");
        prompt.append("ERROR MESSAGE:\n").append(errorMsg).append("\n\n");

        if (stackTrace != null && !stackTrace.isBlank()) {
            // Limit stack trace to first 1000 chars to stay within token limits
            String trimmedStack = stackTrace.length() > 1000
                    ? stackTrace.substring(0, 1000) + "\n... (truncated)"
                    : stackTrace;
            prompt.append("STACK TRACE:\n").append(trimmedStack).append("\n\n");
        }

        if (testCode != null && !testCode.isBlank()) {
            prompt.append("ORIGINAL TEST CODE:\n").append(testCode).append("\n\n");
        }

        prompt.append("Respond with the JSON analysis only.");
        return prompt.toString();
    }

    /**
     * Parses Claude's JSON response into a FailureAnalysis object.
     * Falls back to a safe default if parsing fails.
     */
    private FailureAnalysis parseAnalysis(String responseText, String testName) {
        try {
            // Simple JSON field extraction (avoids extra dependency)
            String rootCause = extractJsonField(responseText, "rootCause");
            String fixSuggestion = extractJsonField(responseText, "fixSuggestion");
            String fixedCode = extractJsonField(responseText, "fixedCode");
            String confidence = extractJsonField(responseText, "confidence");
            boolean requiresHumanReview = responseText.contains("\"requiresHumanReview\": true")
                    || responseText.contains("\"requiresHumanReview\":true");

            return new FailureAnalysis(
                    testName,
                    rootCause,
                    fixSuggestion,
                    fixedCode,
                    confidence,
                    requiresHumanReview,
                    responseText
            );

        } catch (Exception e) {
            System.err.println("[QForge Agent] Could not parse analysis response: " + e.getMessage());
            return new FailureAnalysis(
                    testName,
                    "Unable to parse analysis",
                    "Manual review required",
                    null,
                    "LOW",
                    true,
                    responseText
            );
        }
    }

    /**
     * Extracts a string value from a simple JSON response by field name.
     */
    private String extractJsonField(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return "N/A";

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return "N/A";

        int valueStart = json.indexOf("\"", colonIndex);
        if (valueStart == -1) return "N/A";

        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) return "N/A";

        return json.substring(valueStart + 1, valueEnd);
    }

    /**
     * Extracts plain text from Claude API response.
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
    // Inner class: FailureAnalysis
    // ═══════════════════════════════════════════════════════════

    /**
     * Immutable data container for Claude's analysis of a test failure.
     */
    public static class FailureAnalysis {

        private final String testName;
        private final String rootCause;
        private final String fixSuggestion;
        private final String fixedCode;
        private final String confidence;
        private final boolean requiresHumanReview;
        private final String rawResponse;

        public FailureAnalysis(String testName,
                               String rootCause,
                               String fixSuggestion,
                               String fixedCode,
                               String confidence,
                               boolean requiresHumanReview,
                               String rawResponse) {
            this.testName = testName;
            this.rootCause = rootCause;
            this.fixSuggestion = fixSuggestion;
            this.fixedCode = fixedCode;
            this.confidence = confidence;
            this.requiresHumanReview = requiresHumanReview;
            this.rawResponse = rawResponse;
        }

        public String getTestName()        { return testName; }
        public String getRootCause()       { return rootCause; }
        public String getFixSuggestion()   { return fixSuggestion; }
        public String getFixedCode()       { return fixedCode; }
        public String getConfidence()      { return confidence; }
        public boolean requiresHumanReview() { return requiresHumanReview; }
        public String getRawResponse()     { return rawResponse; }

        public boolean isHighConfidence()  { return "HIGH".equalsIgnoreCase(confidence); }
        public boolean isMediumConfidence(){ return "MEDIUM".equalsIgnoreCase(confidence); }
        public boolean hasFixedCode()      { return fixedCode != null && !fixedCode.isBlank()
                && !"null".equalsIgnoreCase(fixedCode); }

        @Override
        public String toString() {
            return "\n╔══════════════════════════════════════════════" +
                    "\n║ FAILURE ANALYSIS: " + testName +
                    "\n╠══════════════════════════════════════════════" +
                    "\n║ Root Cause    : " + rootCause +
                    "\n║ Fix Suggestion: " + fixSuggestion +
                    "\n║ Confidence    : " + confidence +
                    "\n║ Human Review  : " + requiresHumanReview +
                    (hasFixedCode() ? "\n║ Fixed Code    :\n" + fixedCode : "") +
                    "\n╚══════════════════════════════════════════════";
        }
    }

}
