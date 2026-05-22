# QForge 🔨

**AI-Powered Test Automation Toolkit**

QForge is a Java-based QA toolkit that combines traditional test automation with
the power of large language models (LLMs). It uses the Anthropic Claude API to
generate Rest Assured test code, validate LLM responses, and test chatbot behavior —
all driven by TestNG and reported with ExtentReports.

---

## Modules

### Module 1 — AI Test Generator
Paste in a REST API endpoint or description and QForge uses Claude to automatically
generate ready-to-run Rest Assured test code in Java.

**Tech:** Anthropic Java SDK, Rest Assured, TestNG

### Module 2 — LLM Response Validator
Sends prompts to the Claude API and validates responses against configurable rules
(length, required keywords, format, response time). Reports results as a TestNG
pass/fail suite with ExtentReports output.

**Tech:** Anthropic Java SDK, AssertJ, TestNG, ExtentReports

### Module 3 — Chatbot + Automated Test Suite
A simple Claude-backed chatbot with a full TestNG regression suite around it,
validating response quality, consistency, and edge case handling.

**Tech:** Anthropic Java SDK, Rest Assured, TestNG, ExtentReports

---

## Tech Stack

| Tool                  | Version  | Purpose                          |
|-----------------------|----------|----------------------------------|
| Java                  | 17 (LTS) | Core language                    |
| Anthropic Java SDK    | 2.32.0   | Claude API integration           |
| Rest Assured          | 5.5.7    | API test execution               |
| TestNG                | 7.11.0   | Test runner and suite management |
| Jackson               | 2.18.3   | JSON parsing                     |
| ExtentReports         | 5.1.1    | HTML test reporting              |
| AssertJ               | 3.27.7   | Fluent assertions                |
| dotenv-java           | 3.2.0    | Secure API key loading           |
| SLF4J + Logback       | 2.0.18   | Logging                          |
| Maven                 | 3.x      | Build and dependency management  |
| IntelliJ IDEA         | Community| IDE                              |

---

## Project Structure

```
qforge/
├── pom.xml
├── .env                               ← your API key (never committed)
├── .gitignore
├── README.md
├── src/
│   ├── main/java/com/synlee/qforge/
│   │   ├── client/                    ← Anthropic API client setup
│   │   ├── generator/                 ← Module 1: AI Test Generator
│   │   ├── validator/                 ← Module 2: LLM Response Validator
│   │   ├── chatbot/                   ← Module 3: Chatbot
│   │   └── utils/                     ← Shared utilities
│   └── test/
│       ├── java/com/synlee/qforge/
│       │   ├── generator/             ← Module 1 tests
│       │   ├── validator/             ← Module 2 tests
│       │   └── chatbot/               ← Module 3 tests
│       └── resources/
│           ├── testng-all.xml         ← Full suite (all modules)
│           ├── testng-generator.xml   ← Module 1 only
│           ├── testng-validator.xml   ← Module 2 only
│           └── testng-chatbot.xml     ← Module 3 only
```

---

## Getting Started

### Prerequisites
- Java 17 (Eclipse Temurin recommended — [adoptium.net](https://adoptium.net))
- Maven 3.x
- IntelliJ IDEA Community Edition
- Anthropic API key — sign up at [console.anthropic.com](https://console.anthropic.com)

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/conqueringlion111/qforge.git
cd qforge
```

**2. Create your `.env` file in the project root**
```
ANTHROPIC_API_KEY=your_api_key_here
```
> ⚠️ Never commit this file. It is already listed in `.gitignore`.

**3. Install dependencies**
```bash
mvn clean install -DskipTests
```

**4. Run all tests**
```bash
mvn test
```

**5. Run a specific module**
```bash
# Module 1 only
mvn test -Dsurefire.suiteXmlFiles=src/test/resources/testng-generator.xml

# Module 2 only
mvn test -Dsurefire.suiteXmlFiles=src/test/resources/testng-validator.xml

# Module 3 only
mvn test -Dsurefire.suiteXmlFiles=src/test/resources/testng-chatbot.xml
```

---

## Test Reports

After running tests, open the ExtentReports HTML report:
```
test-output/extent-report.html
```
Open this file in any browser for a full visual breakdown of results.

---

## Author

**Syn H. Lee**
Senior Test Automation Engineer | UI, API, Mobile Automation
- GitHub: [conqueringlion111](https://github.com/conqueringlion111)
- Email: synhlee@gmail.com
- Location: Lawrenceville, GA

---

## License

This project is open source and available under the [MIT License](LICENSE).
