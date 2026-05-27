package com.synlee.qforge.agent;

import com.synlee.qforge.agent.FailureAnalyzerService.FailureAnalysis;
import com.synlee.qforge.agent.TestRunnerAgent.AgentRunResult;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * QForge — AI Agent: Report Service
 *
 * Generates a professional HTML report summarizing the AI agent's run:
 *   - Test execution summary (passed, failed, skipped)
 *   - Claude's analysis for each failure
 *   - Fix suggestions with confidence levels
 *   - Human review flags
 *
 * The report is saved to: target/qforge-agent-report.html
 *
 * Usage:
 *   AgentReportService reporter = new AgentReportService("/path/to/project");
 *   reporter.generateReport(agentRunResult);
 */

public class AgentReportService {

    private final String projectPath;
    private static final String REPORT_PATH = "/target/qforge-agent-report.html";
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AgentReportService(String projectPath) {
        this.projectPath = projectPath;
    }

    /**
     * Generates an HTML report from the agent run result
     * and saves it to target/qforge-agent-report.html
     *
     * @param result The AgentRunResult from TestRunnerAgent.runAndAnalyze()
     */
    public void generateReport(AgentRunResult result) {
        String reportPath = projectPath + REPORT_PATH;
        String timestamp = LocalDateTime.now().format(FORMATTER);

        System.out.println("\n[QForge Agent] Generating HTML report...");

        String html = buildHtml(result, timestamp);

        try (FileWriter writer = new FileWriter(reportPath)) {
            writer.write(html);
            System.out.println("[QForge Agent] ✓ Report saved to: " + reportPath);
        } catch (IOException e) {
            System.err.println("[QForge Agent] Could not write report: " + e.getMessage());
        }
    }

    /**
     * Builds the full HTML report string.
     */
    private String buildHtml(AgentRunResult result, String timestamp) {
        TestRunnerAgent.MavenRunOutput maven = result.getMavenOutput();
        List<FailureAnalysis> analyses = result.getAnalyses();

        int testsRun = maven != null ? maven.getTestsRun() : 0;
        int failures = maven != null ? maven.getFailureCount() : 0;
        int errors = maven != null ? maven.getErrorCount() : 0;
        int skipped = maven != null ? maven.getSkippedCount() : 0;
        int passed = testsRun - failures - errors;

        String statusBadge = failures == 0 && errors == 0
                ? "<span class='badge pass'>ALL PASSED</span>"
                : "<span class='badge fail'>FAILURES DETECTED</span>";

        StringBuilder html = new StringBuilder();

        html.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>QForge AI Agent Report</title>
                    <style>
                        * { box-sizing: border-box; margin: 0; padding: 0; }
                        body {
                            font-family: 'Segoe UI', Arial, sans-serif;
                            background: #0f1117;
                            color: #e0e0e0;
                            padding: 30px;
                        }
                        .header {
                            background: linear-gradient(135deg, #1a1f2e, #2d3561);
                            border-radius: 12px;
                            padding: 30px;
                            margin-bottom: 24px;
                            border-left: 5px solid #4f8ef7;
                        }
                        .header h1 {
                            font-size: 28px;
                            color: #4f8ef7;
                            margin-bottom: 6px;
                        }
                        .header p {
                            color: #8892b0;
                            font-size: 14px;
                        }
                        .badge {
                            display: inline-block;
                            padding: 6px 16px;
                            border-radius: 20px;
                            font-size: 13px;
                            font-weight: bold;
                            margin-top: 10px;
                        }
                        .badge.pass { background: #1a3a2a; color: #4caf82; border: 1px solid #4caf82; }
                        .badge.fail { background: #3a1a1a; color: #f47c7c; border: 1px solid #f47c7c; }
                        .stats-grid {
                            display: grid;
                            grid-template-columns: repeat(4, 1fr);
                            gap: 16px;
                            margin-bottom: 24px;
                        }
                        .stat-card {
                            background: #1a1f2e;
                            border-radius: 10px;
                            padding: 20px;
                            text-align: center;
                            border-top: 3px solid #2d3561;
                        }
                        .stat-card.passed { border-top-color: #4caf82; }
                        .stat-card.failed { border-top-color: #f47c7c; }
                        .stat-card.skipped { border-top-color: #f0c040; }
                        .stat-card.total { border-top-color: #4f8ef7; }
                        .stat-number {
                            font-size: 36px;
                            font-weight: bold;
                            margin-bottom: 6px;
                        }
                        .stat-card.passed .stat-number { color: #4caf82; }
                        .stat-card.failed .stat-number { color: #f47c7c; }
                        .stat-card.skipped .stat-number { color: #f0c040; }
                        .stat-card.total .stat-number { color: #4f8ef7; }
                        .stat-label { color: #8892b0; font-size: 13px; }
                        .section-title {
                            font-size: 18px;
                            color: #4f8ef7;
                            margin-bottom: 16px;
                            padding-bottom: 8px;
                            border-bottom: 1px solid #2d3561;
                        }
                        .analysis-card {
                            background: #1a1f2e;
                            border-radius: 10px;
                            padding: 24px;
                            margin-bottom: 16px;
                            border-left: 4px solid #f47c7c;
                        }
                        .analysis-card.high { border-left-color: #4caf82; }
                        .analysis-card.medium { border-left-color: #f0c040; }
                        .analysis-card.low { border-left-color: #f47c7c; }
                        .analysis-header {
                            display: flex;
                            justify-content: space-between;
                            align-items: center;
                            margin-bottom: 16px;
                        }
                        .test-name {
                            font-size: 15px;
                            font-weight: bold;
                            color: #e0e0e0;
                            font-family: monospace;
                        }
                        .confidence-badge {
                            padding: 4px 12px;
                            border-radius: 12px;
                            font-size: 12px;
                            font-weight: bold;
                        }
                        .confidence-badge.HIGH { background: #1a3a2a; color: #4caf82; }
                        .confidence-badge.MEDIUM { background: #3a3010; color: #f0c040; }
                        .confidence-badge.LOW { background: #3a1a1a; color: #f47c7c; }
                        .analysis-field {
                            margin-bottom: 12px;
                        }
                        .field-label {
                            font-size: 11px;
                            color: #4f8ef7;
                            text-transform: uppercase;
                            letter-spacing: 1px;
                            margin-bottom: 4px;
                        }
                        .field-value {
                            font-size: 14px;
                            color: #c0c8d8;
                            line-height: 1.5;
                        }
                        .code-block {
                            background: #0d1117;
                            border-radius: 6px;
                            padding: 14px;
                            font-family: monospace;
                            font-size: 13px;
                            color: #79b8ff;
                            white-space: pre-wrap;
                            overflow-x: auto;
                            border: 1px solid #2d3561;
                        }
                        .human-review-flag {
                            background: #3a2010;
                            border: 1px solid #f0a040;
                            color: #f0a040;
                            padding: 8px 14px;
                            border-radius: 6px;
                            font-size: 13px;
                            margin-top: 12px;
                            display: inline-block;
                        }
                        .all-passed-msg {
                            background: #1a3a2a;
                            border: 1px solid #4caf82;
                            border-radius: 10px;
                            padding: 24px;
                            text-align: center;
                            color: #4caf82;
                            font-size: 18px;
                        }
                        .footer {
                            text-align: center;
                            color: #4a5568;
                            font-size: 12px;
                            margin-top: 30px;
                            padding-top: 16px;
                            border-top: 1px solid #2d3561;
                        }
                        .timing {
                            color: #8892b0;
                            font-size: 13px;
                            margin-top: 8px;
                        }
                    </style>
                </head>
                <body>
                """);

        // Header
        html.append("<div class='header'>");
        html.append("<h1>🔨 QForge AI Agent Report</h1>");
        html.append("<p>Generated: ").append(timestamp).append("</p>");
        html.append("<p class='timing'>Total agent run time: ")
                .append(result.getTotalTimeMs()).append("ms</p>");
        html.append("<br>").append(statusBadge);
        html.append("</div>");

        // Stats grid
        html.append("<div class='stats-grid'>");
        html.append(statCard("total", String.valueOf(testsRun), "Tests Run"));
        html.append(statCard("passed", String.valueOf(passed), "Passed"));
        html.append(statCard("failed", String.valueOf(failures + errors), "Failed"));
        html.append(statCard("skipped", String.valueOf(skipped), "Skipped"));
        html.append("</div>");

        // AI Analyses section
        html.append("<h2 class='section-title'>🤖 AI Failure Analyses</h2>");

        if (analyses == null || analyses.isEmpty()) {
            html.append("<div class='all-passed-msg'>")
                    .append("✓ All tests passed — no AI analysis needed!")
                    .append("</div>");
        } else {
            for (FailureAnalysis analysis : analyses) {
                String confidenceClass = analysis.getConfidence() != null
                        ? analysis.getConfidence().toUpperCase() : "LOW";
                html.append("<div class='analysis-card ").append(confidenceClass).append("'>");

                // Card header
                html.append("<div class='analysis-header'>");
                html.append("<span class='test-name'>").append(escapeHtml(analysis.getTestName())).append("</span>");
                html.append("<span class='confidence-badge ").append(confidenceClass).append("'>")
                        .append(confidenceClass).append(" CONFIDENCE</span>");
                html.append("</div>");

                // Root cause
                html.append("<div class='analysis-field'>");
                html.append("<div class='field-label'>Root Cause</div>");
                html.append("<div class='field-value'>").append(escapeHtml(analysis.getRootCause())).append("</div>");
                html.append("</div>");

                // Fix suggestion
                html.append("<div class='analysis-field'>");
                html.append("<div class='field-label'>Fix Suggestion</div>");
                html.append("<div class='field-value'>").append(escapeHtml(analysis.getFixSuggestion())).append("</div>");
                html.append("</div>");

                // Fixed code (if available)
                if (analysis.hasFixedCode()) {
                    html.append("<div class='analysis-field'>");
                    html.append("<div class='field-label'>Suggested Fix Code</div>");
                    html.append("<div class='code-block'>").append(escapeHtml(analysis.getFixedCode())).append("</div>");
                    html.append("</div>");
                }

                // Human review flag
                if (analysis.requiresHumanReview()) {
                    html.append("<div class='human-review-flag'>")
                            .append("⚠ Human review recommended for this failure")
                            .append("</div>");
                }

                html.append("</div>"); // end analysis-card
            }
        }

        // Footer
        html.append("<div class='footer'>");
        html.append("QForge AI Agent — Powered by Anthropic Claude | ");
        html.append("Built by Syn H. Lee | github.com/conqueringlion111");
        html.append("</div>");

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * Builds a stat card HTML block.
     */
    private String statCard(String cssClass, String number, String label) {
        return "<div class='stat-card " + cssClass + "'>" +
                "<div class='stat-number'>" + number + "</div>" +
                "<div class='stat-label'>" + label + "</div>" +
                "</div>";
    }

    /**
     * Escapes HTML special characters to prevent rendering issues.
     */
    private String escapeHtml(String text) {
        if (text == null) return "N/A";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

}
