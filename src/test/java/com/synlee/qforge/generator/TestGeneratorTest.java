package com.synlee.qforge.generator;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * QForge — Module 1: AI Test Generator Tests
 *
 * Runs the TestGeneratorService with real endpoint descriptions
 * and validates that Claude returns proper Java test code.
 */

public class TestGeneratorTest {
    private TestGeneratorService generatorService;

    @BeforeClass
    public void setUp() {
        System.out.println("[QForge] Initializing TestGeneratorTest...");
        generatorService = new TestGeneratorService();
    }

    /**
     * Test 1 — Basic GET endpoint
     * Asks Claude to generate tests for a simple GET /users/{id} endpoint
     */
    @Test
    public void testGenerateForGetUserById() {
        String endpointDescription = """
                GET /users/{id}
                - Returns a single user object
                - Response fields: id (integer), name (string), email (string), age (integer)
                - Returns 200 OK when user exists
                - Returns 404 Not Found when user does not exist
                - Base URL: https://reqres.in/api
                """;

        String generatedCode = generatorService.generateTests(endpointDescription);

        System.out.println("\n========== GENERATED TEST CODE ==========");
        System.out.println(generatedCode);
        System.out.println("=========================================\n");

        // Validate the generated code contains expected Java/Rest Assured elements
        Assert.assertNotNull(generatedCode, "Generated code should not be null");
        Assert.assertFalse(generatedCode.isBlank(), "Generated code should not be blank");
        Assert.assertTrue(generatedCode.contains("import io.restassured"),
                "Generated code should import Rest Assured");
        Assert.assertTrue(generatedCode.contains("@Test"),
                "Generated code should contain @Test annotations");
        Assert.assertTrue(generatedCode.contains("given()") || generatedCode.contains("RestAssured"),
                "Generated code should use Rest Assured syntax");
        Assert.assertTrue(generatedCode.contains("statusCode"),
                "Generated code should validate status codes");

        System.out.println("[QForge] testGenerateForGetUserById — PASSED ✓");
    }

    /**
     * Test 2 — POST endpoint
     * Asks Claude to generate tests for a POST /users endpoint
     */
    @Test
    public void testGenerateForPostCreateUser() {
        String endpointDescription = """
                POST /users
                - Creates a new user
                - Request body: name (string, required), job (string, required)
                - Response fields: id (string), name (string), job (string), createdAt (string)
                - Returns 201 Created on success
                - Returns 400 Bad Request when required fields are missing
                - Base URL: https://reqres.in/api
                """;

        String generatedCode = generatorService.generateTests(endpointDescription);

        System.out.println("\n========== GENERATED TEST CODE ==========");
        System.out.println(generatedCode);
        System.out.println("=========================================\n");

        Assert.assertNotNull(generatedCode, "Generated code should not be null");
        Assert.assertFalse(generatedCode.isBlank(), "Generated code should not be blank");
        Assert.assertTrue(generatedCode.contains("@Test"),
                "Generated code should contain @Test annotations");
        Assert.assertTrue(generatedCode.contains("201") || generatedCode.contains("CREATED"),
                "Generated code should validate 201 Created status");
        Assert.assertTrue(generatedCode.contains("body") || generatedCode.contains("Body"),
                "Generated code should include request body");

        System.out.println("[QForge] testGenerateForPostCreateUser — PASSED ✓");
    }

    /**
     * Test 3 — DELETE endpoint
     * Asks Claude to generate tests for a DELETE /users/{id} endpoint
     */
    @Test
    public void testGenerateForDeleteUser() {
        String endpointDescription = """
                DELETE /users/{id}
                - Deletes a user by ID
                - Returns 204 No Content on success
                - Returns 404 Not Found when user does not exist
                - Base URL: https://reqres.in/api
                """;

        String generatedCode = generatorService.generateTests(endpointDescription);

        System.out.println("\n========== GENERATED TEST CODE ==========");
        System.out.println(generatedCode);
        System.out.println("=========================================\n");

        Assert.assertNotNull(generatedCode, "Generated code should not be null");
        Assert.assertFalse(generatedCode.isBlank(), "Generated code should not be blank");
        Assert.assertTrue(generatedCode.contains("@Test"),
                "Generated code should contain @Test annotations");
        Assert.assertTrue(generatedCode.contains("204") || generatedCode.contains("delete"),
                "Generated code should reference 204 or delete");

        System.out.println("[QForge] testGenerateForDeleteUser — PASSED ✓");
    }
}
