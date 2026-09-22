# QA Automation Test Suite

API (REST + GraphQL) and UI test framework implemented as part of the Flamingo QA home assignment.
41 tests — 19 REST, 8 GraphQL, 14 UI. Runs green from a cold clone, no setup and no secrets.

Systems under test:
- [Restful Booker](https://restful-booker.herokuapp.com) — REST CRUD
- [Rick and Morty GraphQL API](https://rickandmortyapi.com/graphql) — GraphQL
- [DemoQA](https://demoqa.com) — UI

## Tools & Technologies

- Java 21
- Maven
- JUnit 5
- REST Assured
- Playwright
- AssertJ
- Jackson / Lombok / DataFaker
- Allure (reporting)
- SLF4J + Logback (logging)

## Requirements

- Java 21+
- Maven 3.6+
- Internet access (tests hit real public services)
- Chromium — downloaded by Playwright on first run, no system Chrome needed

If your environment blocks that download, install the browser explicitly:

```bash
mvn compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

No configuration step is needed — `src/main/resources/config.properties` is committed complete,
including the published Restful Booker demo credentials.

## How to Run Tests

```bash
# Run all tests
mvn clean test

# Run only API tests (REST + GraphQL)
mvn test -Dgroups=api

# Run only UI tests
mvn test -Dgroups=ui
```

Run a single class or a single test:

```bash
mvn test -Dtest=BookingCrudTest
mvn test -Dtest=BookingCrudTest#shouldRetrieveCreatedBookingById
```

### Tags

| Tag | Tests | Selects |
|---|---|---|
| `regression` | 41 | Every test |
| `api` | 27 | Everything over HTTP — REST and GraphQL |
| `graphql` | 8 | GraphQL only |
| `ui` | 14 | The Playwright suite |
| `smoke` | 8 | Critical path across all three layers |

Tags compose with JUnit expression syntax:

```bash
mvn test -Dgroups="regression & !ui"
mvn test -Dgroups="smoke | graphql"
```

## Parallel Execution

Parallel execution is **on by default**: classes run concurrently, methods within a class run
sequentially, capped at 4 threads (83 s → 28 s for the full suite). Configured in
`src/test/resources/junit-platform.properties`.

```bash
# Change the thread count
mvn test -Djunit.jupiter.execution.parallel.config.fixed.parallelism=2

# Turn parallel execution off
mvn test -Djunit.jupiter.execution.parallel.enabled=false
```

## Configuration

Every property resolves through three sources, highest precedence first:

```bash
mvn test -Dui.headless=false   # 1. -D system property
UI_HEADLESS=false mvn test     # 2. environment variable (upper-cased, dots to underscores)
                               # 3. config.properties (committed default)
```

Commonly overridden keys:

| Property | What it does | Default |
|---|---|---|
| `api.base.url` | Restful Booker base URL | `https://restful-booker.herokuapp.com` |
| `graphql.url` | GraphQL endpoint — point it at any schema | `https://rickandmortyapi.com/graphql` |
| `ui.base.url` | UI base URL | `https://demoqa.com` |
| `ui.headless` | Run the browser headless | `true` |
| `ui.timeout.ms` | Playwright timeout | `15000` |
| `health.check.enabled` | Skip a system's tests if it is unreachable | `true` |

## Allure Report

No globally installed Allure CLI is needed — `allure-maven` unpacks the renderer on first use:

```bash
mvn clean test
mvn allure:serve
```

To generate without serving:

```bash
mvn allure:report        # writes target/site/allure-maven-plugin
```

Fallback plain HTML report:

```bash
mvn surefire-report:report-only   # writes target/reports/surefire.html
```

Sample report output is committed in [`docs/report-screenshots/`](docs/report-screenshots).

On failure, a UI test attaches a full-page screenshot and a Playwright trace to the report. Traces
are also written to `target/traces/` and can be replayed offline:

```bash
npx playwright show-trace target/traces/<TestClass>.<testMethod>.zip
```

## Project Structure

```
src/main/java/com/flamingo/qa/     reusable framework
├── clients/      AuthApiClient, BookingApiClient, GraphQlApiClient, TokenProvider
├── config/       ConfigLoader (3-source precedence), Config, RestAssuredConfigurator
├── endpoints/    path constants, one class per resource
├── http/         RequestBuilder, ResponseWrapper/ResponseVerifier, TransientFailureRetry
├── pojo/         Java records: auth, booking, graphql, ui
├── ui/           BrowserFactory, pages/ (BasePage + page objects), components/
└── util/         Json, ResourceReader, CustomLogger

src/test/java/com/flamingo/qa/     tests and their fixtures only
├── base/         BaseApiTest, BaseRestTest, BaseGraphQlTest, BaseUiTest
├── data/         BookingDataGenerator, StudentDataGenerator (DataFaker)
├── extensions/   PlaywrightExtension, ServiceHealthExtension, AllureEnvironmentListener
└── tests/        api/, graphql/, ui/

src/main/resources/config.properties   committed defaults
src/test/resources/
├── graphql/      query documents, loaded from file rather than inlined
├── testdata/     guest-name-cases.json for the data-driven tests
└── upload/       sample-upload.png for the file upload test
```

## CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on push to `main`, on pull requests and
on demand. API and UI run as separate parallel jobs; Allure results, Surefire reports, screenshots
and traces upload as artifacts, and a third job publishes the merged Allure report to GitHub Pages.
The workflow needs no secrets.