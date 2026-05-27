package com.synlee.qforge.agent;

import com.synlee.qforge.agent.TestRunnerAgent.AgentRunResult;

/**
 * QForge — AI Agent: Main Entry Point
 *
 * Wires together the TestRunnerAgent and AgentReportService
 * to run the full AI agent workflow in one command.
 *
 * Workflow:
 *   1. Run the Maven test suite
 *   2. Detect any failures
 *   3. Send failures to Claude for analysis
 *   4. Generate an HTML report with AI insights
 *
 * How to run:
 *   Option A — IntelliJ: Right-click QForgeAgent.java → Run 'QForgeAgent.main()'
 *   Option B — Terminal:
 *     mvn exec:java -Dexec.mainClass="com.synlee.qforge.agent.QForgeAgent"
 *
 * The HTML report will be saved to:
 *   target/qforge-agent-report.html
 */

public class QForgeAgent {

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║           QFORGE AI AGENT — STARTING                ║");
        System.out.println("║     AI-Powered Test Automation by Syn H. Lee        ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // ── Detect project path ──────────────────────────────────────
        // Uses current working directory so it works from both
        // IntelliJ run config and terminal/Maven exec
        String projectPath = System.getProperty("user.dir");
        System.out.println("\n[QForge Agent] Project path: " + projectPath);

        // ── Allow override via command line arg ───────────────────────
        // Example: java QForgeAgent /home/user/workspace/MyProject
        if (args != null && args.length > 0 && !args[0].isBlank()) {
            projectPath = args[0];
            System.out.println("[QForge Agent] Using provided path: " + projectPath);
        }

        try {
            // Step 1 — Initialize the agent and reporter
            TestRunnerAgent agent = new TestRunnerAgent(projectPath);
            AgentReportService reporter = new AgentReportService(projectPath);

            // Step 2 — Run tests and analyze failures with Claude
            AgentRunResult result = agent.runAndAnalyze();

            // Step 3 — Print summary to console
            System.out.println(result.getSummary());

            // Step 4 — Generate HTML report
            reporter.generateReport(result);

            // Step 5 — Final status message
            System.out.println("\n╔══════════════════════════════════════════════════════╗");
            if (!result.hasFailures()) {
                System.out.println("║  ✓ ALL TESTS PASSED — No failures to analyze         ║");
            } else {
                int analyzed = result.getAnalyses().size();
                int highConf = result.getHighConfidenceFixCount();
                System.out.println("║  AI AGENT COMPLETE                                   ║");
                System.out.printf( "║  Failures analyzed  : %-31d║%n", analyzed);
                System.out.printf( "║  High confidence    : %-31d║%n", highConf);
                System.out.printf( "║  Needs human review : %-31d║%n", analyzed - highConf);
            }
            System.out.println("║  Report: target/qforge-agent-report.html             ║");
            System.out.println("╚══════════════════════════════════════════════════════╝");
            System.exit(0); // cleanly terminates all SDK background threads

        } catch (Exception e) {
            System.err.println("\n[QForge Agent] ✗ Agent encountered an error:");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

}
