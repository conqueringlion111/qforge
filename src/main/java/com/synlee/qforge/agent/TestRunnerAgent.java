package com.synlee.qforge.agent;

import com.synlee.qforge.agent.FailureAnalyzerService.FailureAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QForge — AI Agent: Test Runner Agent
 *
 * The central coordinator of the QForge AI Agent. It:
 *   1. Runs the Maven test suite as a subprocess
 *   2. Captures and parses the output for failures
 *   3. Reads Surefire XML reports for detailed failure info
 *   4. Sends each failure to FailureAnalyzerService for Claude's analysis
 *   5. Collects all analyses into a AgentRunResult for reporting
 *
 * Usage:
 *   TestRunnerAgent agent = new TestRunnerAgent("/path/to/project");
 *   AgentRunResult result = agent.runAndAnalyze();
 *   System.out.println(result.getSummary());
 */

public class TestRunnerAgent {

    private final String projectPath;
    private final FailureAnalyzerService failureAnalyzer;

    // Surefire reports location relative to project root
    private static final String SUREFIRE_REPORTS_PATH = "target/surefire-reports";

    public TestRunnerAgent(String projectPath) {
        this.projectPath = projectPath;
        this.failureAnalyzer = new FailureAnalyzerService();
        System.out.println("[QForge Agent] Initialized for project: " + projectPath);
    }

    /**
     * Runs the full Maven test suite and analyzes any failures with Claude.
     *
     * @return AgentRunResult containing all test results and AI analyses
     */
    public AgentRunResult runAndAnalyze() {
        System.out.println("\n[QForge Agent] ═══════════════════════════════════════");
        System.out.println("[QForge Agent] Starting AI-powered test run...");
        System.out.println("[QForge Agent] ═══════════════════════════════════════");

        AgentRunResult result = new AgentRunResult();
        long startTime = System.currentTimeMillis();

        // Step 1 — Run Maven tests
        System.out.println("\n[QForge Agent] Step 1: Running Maven test suite...");
        MavenRunOutput mavenOutput = runMavenTests();
        result.setMavenOutput(mavenOutput);

        System.out.println("[QForge Agent] Maven exit code: " + mavenOutput.getExitCode());
        System.out.println("[QForge Agent] Tests run: " + mavenOutput.getTestsRun());
        System.out.println("[QForge Agent] Failures:   " + mavenOutput.getFailureCount());

        // Step 2 — If failures exist, read Surefire reports
        if (mavenOutput.getFailureCount() > 0) {
            System.out.println("\n[QForge Agent] Step 2: Reading Surefire failure reports...");
            List<TestFailure> failures = readSurefireReports();
            result.setFailures(failures);
            System.out.println("[QForge Agent] Found " + failures.size() + " failure(s) to analyze.");

            // Step 3 — Analyze each failure with Claude
            System.out.println("\n[QForge Agent] Step 3: Sending failures to Claude for analysis...");
            List<FailureAnalysis> analyses = new ArrayList<>();
            for (TestFailure failure : failures) {
                System.out.println("[QForge Agent] Analyzing: " + failure.getTestName());
                FailureAnalysis analysis = failureAnalyzer.analyze(
                        failure.getTestName(),
                        failure.getErrorMessage(),
                        failure.getStackTrace(),
                        failure.getTestCode()
                );
                analyses.add(analysis);
                System.out.println(analysis);
            }
            result.setAnalyses(analyses);

        } else {
            System.out.println("\n[QForge Agent] ✓ No failures detected — all tests passed!");
        }

        long totalTime = System.currentTimeMillis() - startTime;
        result.setTotalTimeMs(totalTime);

        System.out.println("\n[QForge Agent] Agent run complete in " + totalTime + "ms");
        return result;
    }

    /**
     * Runs 'mvn test' as a subprocess and captures the output.
     */
    private MavenRunOutput runMavenTests() {
        MavenRunOutput output = new MavenRunOutput();
        StringBuilder fullOutput = new StringBuilder();

        try {
            // Build the Maven command
            ProcessBuilder pb = new ProcessBuilder(
                    getMavenCommand(), "test", "-Dsurefire.failIfNoSpecifiedTests=false"
            );
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true); // merge stderr into stdout

            Process process = pb.start();

            // Read Maven output line by line
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fullOutput.append(line).append("\n");

                    // Parse key metrics from Maven output
                    if (line.contains("Tests run:")) {
                        parseMavenTestLine(line, output);
                    }
                }
            }

            output.setExitCode(process.waitFor());
            output.setFullOutput(fullOutput.toString());

        } catch (Exception e) {
            System.err.println("[QForge Agent] Error running Maven: " + e.getMessage());
            output.setExitCode(-1);
            output.setFullOutput("Error: " + e.getMessage());
        }

        return output;
    }

    /**
     * Parses a Maven "Tests run: X, Failures: Y" line to extract counts.
     */
    private void parseMavenTestLine(String line, MavenRunOutput output) {
        try {
            // Match: Tests run: 3, Failures: 1, Errors: 0, Skipped: 0
            Pattern pattern = Pattern.compile(
                    "Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)"
            );
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                output.setTestsRun(output.getTestsRun() + Integer.parseInt(matcher.group(1)));
                output.setFailureCount(output.getFailureCount() + Integer.parseInt(matcher.group(2)));
                output.setErrorCount(output.getErrorCount() + Integer.parseInt(matcher.group(3)));
                output.setSkippedCount(output.getSkippedCount() + Integer.parseInt(matcher.group(4)));
            }
        } catch (Exception e) {
            // Non-critical parsing error — continue
        }
    }

    /**
     * Reads Surefire XML reports from target/surefire-reports
     * and extracts failure details for each failed test.
     */
    private List<TestFailure> readSurefireReports() {
        List<TestFailure> failures = new ArrayList<>();
        Path reportsDir = Paths.get(projectPath, SUREFIRE_REPORTS_PATH);

        if (!Files.exists(reportsDir)) {
            System.err.println("[QForge Agent] Surefire reports directory not found: " + reportsDir);
            return failures;
        }

        try {
            Files.list(reportsDir)
                    .filter(path -> path.toString().endsWith(".xml"))
                    .filter(path -> !path.getFileName().toString().startsWith("TEST-"))
                    .forEach(path -> {
                        // Read .txt surefire reports for failure details
                    });

            // Read .txt surefire report files which contain failure stack traces
            Files.list(reportsDir)
                    .filter(path -> path.toString().endsWith(".txt"))
                    .forEach(path -> {
                        try {
                            String content = Files.readString(path);
                            if (content.contains("FAILED") || content.contains("Exception")) {
                                TestFailure failure = parseSurefireTxtReport(content, path.getFileName().toString());
                                if (failure != null) {
                                    failures.add(failure);
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[QForge Agent] Could not read report: " + path);
                        }
                    });

        } catch (Exception e) {
            System.err.println("[QForge Agent] Error reading surefire reports: " + e.getMessage());
        }

        return failures;
    }

    /**
     * Parses a Surefire .txt report file to extract test failure details.
     */
    private TestFailure parseSurefireTxtReport(String content, String fileName) {
        try {
            // Extract test class name from filename (e.g. "com.synlee.qforge.generator.TestGeneratorTest.txt")
            String testClass = fileName.replace(".txt", "");

            // Find failed test methods
            String testName = testClass;
            String errorMessage = "Unknown error";
            String stackTrace = "";

            // Look for exception or assertion error
            if (content.contains("AssertionError")) {
                int errorIndex = content.indexOf("AssertionError");
                errorMessage = content.substring(
                        Math.max(0, errorIndex - 50),
                        Math.min(content.length(), errorIndex + 200)
                ).trim();
            } else if (content.contains("Exception")) {
                int errorIndex = content.indexOf("Exception");
                errorMessage = content.substring(
                        Math.max(0, errorIndex),
                        Math.min(content.length(), errorIndex + 200)
                ).trim();
            }

            // Extract stack trace (first 1500 chars after error)
            int stackStart = content.indexOf("\tat ");
            if (stackStart != -1) {
                stackTrace = content.substring(
                        stackStart,
                        Math.min(content.length(), stackStart + 1500)
                );
            }

            return new TestFailure(testName, errorMessage, stackTrace, null);

        } catch (Exception e) {
            System.err.println("[QForge Agent] Could not parse report: " + fileName);
            return null;
        }
    }

    /**
     * Returns the correct Maven command for the current OS.
     */
    private String getMavenCommand() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows") ? "mvn.cmd" : "mvn";
    }

    // ═══════════════════════════════════════════════════════════
    // Inner class: TestFailure
    // ═══════════════════════════════════════════════════════════

    /**
     * Represents a single test failure with all context needed for analysis.
     */
    public static class TestFailure {
        private final String testName;
        private final String errorMessage;
        private final String stackTrace;
        private final String testCode;

        public TestFailure(String testName, String errorMessage,
                           String stackTrace, String testCode) {
            this.testName = testName;
            this.errorMessage = errorMessage;
            this.stackTrace = stackTrace;
            this.testCode = testCode;
        }

        public String getTestName()     { return testName; }
        public String getErrorMessage() { return errorMessage; }
        public String getStackTrace()   { return stackTrace; }
        public String getTestCode()     { return testCode; }
    }

    // ═══════════════════════════════════════════════════════════
    // Inner class: MavenRunOutput
    // ═══════════════════════════════════════════════════════════

    /**
     * Holds the output and metrics from a Maven test run.
     */
    public static class MavenRunOutput {
        private int exitCode;
        private int testsRun;
        private int failureCount;
        private int errorCount;
        private int skippedCount;
        private String fullOutput;

        public int getExitCode()       { return exitCode; }
        public int getTestsRun()       { return testsRun; }
        public int getFailureCount()   { return failureCount; }
        public int getErrorCount()     { return errorCount; }
        public int getSkippedCount()   { return skippedCount; }
        public String getFullOutput()  { return fullOutput; }

        public void setExitCode(int exitCode)         { this.exitCode = exitCode; }
        public void setTestsRun(int testsRun)         { this.testsRun = testsRun; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public void setErrorCount(int errorCount)     { this.errorCount = errorCount; }
        public void setSkippedCount(int skippedCount) { this.skippedCount = skippedCount; }
        public void setFullOutput(String fullOutput)  { this.fullOutput = fullOutput; }
    }

    // ═══════════════════════════════════════════════════════════
    // Inner class: AgentRunResult
    // ═══════════════════════════════════════════════════════════

    /**
     * Holds the complete results of an agent run including
     * Maven output, failures detected, and Claude's analyses.
     */
    public static class AgentRunResult {
        private MavenRunOutput mavenOutput;
        private List<TestFailure> failures = new ArrayList<>();
        private List<FailureAnalysis> analyses = new ArrayList<>();
        private long totalTimeMs;

        public MavenRunOutput getMavenOutput()       { return mavenOutput; }
        public List<TestFailure> getFailures()       { return failures; }
        public List<FailureAnalysis> getAnalyses()   { return analyses; }
        public long getTotalTimeMs()                 { return totalTimeMs; }

        public void setMavenOutput(MavenRunOutput mavenOutput) { this.mavenOutput = mavenOutput; }
        public void setFailures(List<TestFailure> failures)     { this.failures = failures; }
        public void setAnalyses(List<FailureAnalysis> analyses) { this.analyses = analyses; }
        public void setTotalTimeMs(long totalTimeMs)            { this.totalTimeMs = totalTimeMs; }

        public boolean hasFailures() {
            return failures != null && !failures.isEmpty();
        }

        public int getHighConfidenceFixCount() {
            if (analyses == null) return 0;
            return (int) analyses.stream()
                    .filter(FailureAnalysis::isHighConfidence)
                    .count();
        }

        /**
         * Returns a clean summary of the agent run for reporting.
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("\n╔══════════════════════════════════════════════════════");
            sb.append("\n║         QFORGE AI AGENT — RUN SUMMARY               ");
            sb.append("\n╠══════════════════════════════════════════════════════");

            if (mavenOutput != null) {
                sb.append("\n║ Tests Run    : ").append(mavenOutput.getTestsRun());
                sb.append("\n║ Failures     : ").append(mavenOutput.getFailureCount());
                sb.append("\n║ Errors       : ").append(mavenOutput.getErrorCount());
                sb.append("\n║ Skipped      : ").append(mavenOutput.getSkippedCount());
            }

            sb.append("\n║ Total Time   : ").append(totalTimeMs).append("ms");
            sb.append("\n╠══════════════════════════════════════════════════════");

            if (analyses != null && !analyses.isEmpty()) {
                sb.append("\n║ AI ANALYSES  : ").append(analyses.size()).append(" failure(s) analyzed");
                sb.append("\n║ High Confidence Fixes: ").append(getHighConfidenceFixCount());
                sb.append("\n╠══════════════════════════════════════════════════════");
                for (FailureAnalysis analysis : analyses) {
                    sb.append("\n║ ► ").append(analysis.getTestName());
                    sb.append("\n║   Root Cause : ").append(analysis.getRootCause());
                    sb.append("\n║   Fix        : ").append(analysis.getFixSuggestion());
                    sb.append("\n║   Confidence : ").append(analysis.getConfidence());
                    sb.append("\n║   Human Review: ").append(analysis.requiresHumanReview());
                    sb.append("\n║");
                }
            } else {
                sb.append("\n║ ✓ All tests passed — no AI analysis needed!");
            }

            sb.append("\n╚══════════════════════════════════════════════════════");
            return sb.toString();
        }
    }

}
