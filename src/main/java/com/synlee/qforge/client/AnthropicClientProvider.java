package com.synlee.qforge.client;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import io.github.cdimascio.dotenv.Dotenv;

/**
 * QForge — Anthropic API Client
 *
 * Singleton client that loads your ANTHROPIC_API_KEY from the .env file
 * and provides a shared AnthropicClient instance for all modules.
 *
 * Usage in any module:
 *   AnthropicClient client = AnthropicClientProvider.getClient();
 */
public class AnthropicClientProvider {

    private static AnthropicClient client;

    // Private constructor — no one should instantiate this class directly
    private AnthropicClientProvider() {}

    /**
     * Returns the shared AnthropicClient instance.
     * Initializes it on first call (lazy singleton pattern).
     */
    public static AnthropicClient getClient() {
        if (client == null) {
            client = buildClient();
        }
        return client;
    }

    /**
     * Loads the API key from .env and builds the Anthropic client.
     * Fails fast with a clear error message if the key is missing.
     */
    private static AnthropicClient buildClient() {
        // Load .env file from project root
        Dotenv dotenv = Dotenv.configure()
                .filename(".env")
                .ignoreIfMissing() // won't crash if .env missing (e.g. CI environment)
                .load();

        // Try .env first, then fall back to system environment variable
        // (useful for Jenkins / CI pipelines where key is set as env var)
        String apiKey = dotenv.get("ANTHROPIC_API_KEY",
                System.getenv("ANTHROPIC_API_KEY"));

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "[QForge] ANTHROPIC_API_KEY not found. " +
                            "Add it to your .env file or set it as an environment variable."
            );
        }

        System.out.println("[QForge] Anthropic client initialized successfully.");

        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}