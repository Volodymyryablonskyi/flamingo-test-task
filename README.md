# QA Automation Test Suite

API (REST + GraphQL) and UI test suite for the Flamingo home assignment.
**41 tests — 19 REST, 8 GraphQL, 14 UI — green from a cold clone with no setup and no secrets.**

| | |
|---|---|
| **Stack** | Java 21 · JUnit 5 · REST Assured · Playwright · AssertJ · Jackson · Lombok · Allure |
| **Systems under test** | [Restful Booker](https://restful-booker.herokuapp.com) · [Rick and Morty GraphQL](https://rickandmortyapi.com/graphql) · [DemoQA](https://demoqa.com) |
| **Run time** | ~28 s for the whole suite at `parallelism = 4` (83 s sequential) |
| **CI** | [`.github/workflows/ci.yml`](.github/workflows/ci.yml) — API and UI as separate jobs, no secrets required |

---

## Prerequisites

- **Java 21+** — the brief says Java 11+, but this project targets **21** deliberately: it uses
  `SequencedCollection.getFirst()`, records and text blocks. `java -version` must report 21 or newer.
- **Maven 3.6+**
- **Chromium** — installed by Playwright itself, see below. No system Chrome needed.
- An internet connection. All three systems under test are public services.

Playwright downloads its own browser on first use. If your environment blocks that, install it
explicitly:

```bash
mvn compile exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install chromium"
```

**No configuration step.** `src/main/resources/config.properties` is committed complete, including
the Restful Booker credentials — those are the published demo credentials from its own
documentation, not secrets. Any value can be overridden without editing the file:

```bash
mvn test -Dui.headless=false            # -D system property  (highest precedence)
UI_HEADLESS=false mvn test              # environment variable (key upper-cased, dots to underscores)
                                        # config.properties    (committed default)
```

## How to Run

```bash
# Run all tests
mvn clean test

# Run only API tests  (REST + GraphQL = 27)
mvn test -Dgroups="api"

# Run only UI tests   (14)
mvn test -Dgroups="ui"
```

Useful variations:

```bash
mvn test -Dgroups="regression"    # the full suite by tag — every test (41)
mvn test -Dgroups="graphql"       # GraphQL only — these carry both `graphql` and `api` tags
mvn test -Dgroups="smoke"         # the critical path across all three layers
mvn test -Dui.headless=false      # watch the UI suite drive a real browser
mvn allure:serve                  # open the Allure report (downloads the renderer on first use)
```

### Tags

| Tag | Tests | Selects |
|---|---|---|
| `regression` | 41 | **Every test.** The full suite, addressed by tag rather than by absence of a filter |
| `api` | 27 | Everything that talks HTTP — REST **and** GraphQL |
| `graphql` | 8 | The GraphQL subset only |
| `ui` | 14 | The Playwright suite |
| `smoke` | 8 | The critical path across all three layers |

Tags combine with JUnit's expression syntax, so the axes compose:

```bash
mvn test -Dgroups="regression & !ui"        # the whole suite minus the browser
mvn test -Dgroups="smoke | graphql"         # union
```

Two design points worth naming:

- **`regression` is inherited, not repeated.** It sits on `BaseApiTest` and `BaseUiTest`, and
  JUnit inherits `@Tag` from superclasses — so every test class picks it up through the hierarchy
  it already extends. Nothing has to be remembered when a test is added: a new class extending a
  base is in the regression suite by construction, which is the only way a "run everything" tag
  stays honest.
- **`api` and `graphql` overlap on purpose.** `BaseGraphQlTest extends BaseApiTest`, so the
  GraphQL tests carry both — matching the brief's own framing of GraphQL as part of API testing.
  `-Dgroups="api"` therefore runs all 27, and `-Dgroups="graphql"` narrows to 8.

`regression` and an unfiltered `mvn test` currently select the same 41 tests. The tag still earns
its place: it is the stable name for "the full suite" once tags that should *not* run by default
exist — `flaky`, `slow`, or a `wip` tag excluded via `-Dgroups="regression & !wip"`.

The Allure report needs no globally installed Allure CLI — `allure-maven` unpacks the renderer
into `.allure/` on first run, so `mvn clean test` followed by `mvn allure:serve` works from a cold
clone.

## Project Structure

```
src/main/java/com/flamingo/qa/          the reusable framework
├── clients/      AuthApiClient, BookingApiClient, GraphQlApiClient, TokenProvider
├── config/       ConfigLoader (3-source precedence), Config, RestAssuredConfigurator
├── endpoints/    path constants, one class per resource
├── http/         RequestBuilder · ResponseWrapper/ResponseVerifier · TransientFailureRetry
├── pojo/         Jackson + Lombok models: auth, booking, graphql, ui
├── ui/           BrowserFactory, pages/ (BasePage + page objects), components/
└── util/         Json, ResourceReader, CustomLogger

src/test/java/com/flamingo/qa/          only tests and their fixtures
├── base/         BaseApiTest · BaseRestTest · BaseGraphQlTest · BaseUiTest
├── data/         BookingDataGenerator, StudentDataGenerator (DataFaker)
├── extensions/   PlaywrightExtension · ServiceHealthExtension · AllureEnvironmentListener
└── tests/        api/ · graphql/ · ui/
```

The split is the point: **`src/main/java` is a framework someone else could depend on**, and
`src/test/java` holds nothing but tests, their fixtures and their JUnit wiring. That is why the
framework's dependencies are compile-scoped while JUnit and DataFaker stay test-scoped.

## Test Strategy

**Risk first, coverage second.** The brief asks for quality over quantity, so every test earns its
place by covering a distinct risk rather than a distinct field permutation. Each is tagged P0–P2
in [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md) §6, and the P0s are the ones whose failure
would mean the feature is broken for everyone.

**Round-trips over field matrices.** `POST` then `GET` then `PUT` then `DELETE` against the same
booking proves the resource actually persists. Twenty variations of an invalid `firstname` would
add test count without adding information. The one place data-driving *does* pay is where the
input space genuinely changes behaviour — guest-name edge cases (`testdata/guest-name-cases.json`),
mobile-number validation, and search terms — so those three are `@ParameterizedTest`.

**Every test creates what it needs.** The brief warns that Restful Booker resets periodically, so
nothing depends on pre-existing data and cleanup tolerates a 404. A reset mid-run degrades to a
retry, not a red build.

**A service being down is not a test failure.** `ServiceHealthExtension` pings each system once per
run and, if it is unreachable, aborts its tests with `Assumptions.abort` and a reason — skipped
with an explanation, not failed. `TransientFailureRetry` separately covers 5xx, 429 and transport
errors with exponential backoff. Both exist because a red build should mean *our* code is wrong.

**Assertions carry the diagnosis.** AssertJ throughout, and `SoftAssertions` wherever a single
action produces several facts to check — the registration modal's ten rows are verified in one
pass, so one wrong field does not hide the other nine. That turns a three-iteration debug loop
into one.

**UI waits are declared, never slept.** Playwright's auto-waiting plus web-first
`assertThat(locator)` assertions, which retry until timeout. There is not a single `Thread.sleep`
in the UI layer (the only one in the repo is the API retry's backoff). This matters more than
usual here: DemoQA serves a **436-byte HTML shell** and renders everything client-side, so nothing
at all is present on first paint.

**Page objects expose behaviour, not widgets.** `form.fillAndSubmit(student)` returns a
`SubmissionModal`. Locators are held as `private final Locator` **fields** exposed via Lombok
`@Getter`, never rebuilt per call — safe specifically because a Playwright `Locator` is a lazy
description re-resolved on each action, so a field cannot go stale the way a Selenium
`WebElement` would. The composite widgets (the registration modal, the submission modal) are
their own components, reused across pages.

**Configuration is layered, not hardcoded.** `-D` beats environment variable beats
`config.properties`. There is no URL or credential anywhere in a test class, which is what lets
the same suite point at a different GraphQL schema by changing one property.

**Parallel but polite.** Classes run concurrently, methods within a class sequentially, capped at
four threads — most of the wall-clock win (83 s → 28 s), trivially safe intra-class ordering, and
a bounded load on public services that the brief explicitly asks us not to overload.

### Versions are pinned, and three are deliberate downgrades

"Latest" is a trap on this stack today. JUnit **5.14.4** rather than 6.x (a different major line
with new coordinates), Allure **2.35.5** rather than 3.0.0 (a rewritten report pipeline whose
JUnit 5 and REST Assured integrations are unproven), AssertJ **3.27.7** rather than 4.0.0-M1 (a
milestone, not a release). Rationale in `pom.xml` and §4 of the plan.

## Challenges & Solutions

Every item below is something this suite **measured**, not a generic anecdote.

**1. `POST /auth` answers bad credentials with `200`, not `401`.**
The body is `{"reason":"Bad credentials"}` with a success status. A test asserting `401` would be
asserting a spec the service does not implement. `AuthTest` asserts the measured contract —
`200` plus the `reason` field and **no** `token` — and the mismatch is flagged as a product
defect rather than quietly normalised away.

**2. GraphQL returns `400` for a malformed query, and the message is not a GraphQL error.**
The brief predicts "HTTP 200 with an errors array". Measured, the Rick and Morty API answers
**400**, and the body comes from the **Stellate CDN** in front of the API — note the nested
`extensions.stellate.code`, not a standard `extensions.code`. The document never reaches the
GraphQL server. Since that wording belongs to an edge provider that can change it at will, the
test asserts status `400`, the absence of `data`, and a non-blank `errors[0].message` — meeting
the brief's requirement exactly without pinning text we do not control. A missing required
variable is labelled `INTERNAL_SERVER_ERROR` while the identical unknown-field failure is
correctly labelled `GRAPHQL_VALIDATION_FAILED`; asserted as measured, flagged as a defect.

**3. The brief's GraphQL endpoint could not be used.**
`hygraph.com/graphql-playground` is a client-rendered playground: scraping it for a
`*.hygraph.com/v2/…` URL or an `endpoint` reference returns **zero** matches, and a Hygraph
project URL is per-account, so anyone cloning this repo would need their own key. The brief also
says "select any available schema". The suite therefore points at the public Rick and Morty
GraphQL API through the `graphql.url` property — it satisfies every required scenario
(pagination, query by ID, variables, nested fields across types) and keeps the repo runnable by a
reviewer with **no secrets**. Repointing it is a one-property change.

**4. `DELETE` intermittently returned `403` mid-run.**
Restful Booker expires tokens when it resets. The booking client now refreshes the token and
retries once on a `403`, which is visible in the logs as
`DELETE /booking/3839 was rejected with 403 - refreshing the auth token and retrying once`.

**5. DemoQA has been rewritten, and the widely-copied locators for it are wrong.**
It has moved from Bootstrap 4 + ReactTable to Bootstrap 5 + a plain `<table>`. Gender and hobby
labels are `form-check-label`, not `custom-control-label`; the web table has no "No rows found"
message, it simply empties its `<tbody>`. Locators were derived from the live DOM via the
Playwright MCP server rather than from tutorials.

**6. `Pattern.quote` silently breaks a Playwright `hasText` regex.**
Playwright serialises a Java `Pattern` into a **JavaScript** `RegExp`, and JavaScript has no
`\Q…\E` quoting. `Pattern.quote("Other")` produces `\QOther\E`, which JS reads as the literal
`QOtherE` — it matches nothing, and the click dies on a 15 s actionability timeout whose log
unhelpfully reads *"waiting for locator"*. Fixed by matching exact text scoped to the field's
wrapper, which also avoids the subtler trap that `"Male"` is a substring of `"Female"`.

**7. An unscoped `getByRole(OPTION)` is ambiguous — and it only failed under parallelism.**
A native `<select>`'s `<option>` elements carry the implicit `option` role, so while the date
picker is open the practice form holds **213** of them: its month and year dropdowns. Selecting
the subject with `page.getByRole(OPTION).first()` therefore resolved to `<option>January</option>`
whenever the picker had not finished closing — an element inside a closed `<select>`, which can
never be clicked. At four threads the picker closes more slowly, and this failed **one run in
three**. Every react-select lookup is now scoped to its own container and matches the option by
exact accessible name rather than by position. Three consecutive parallel runs green afterwards.

**8. The brief asks for sorting validation, and DemoQA no longer sorts.**
The Age header is now `<th style="width: 40px;">Age</th>` — no class, no `aria-sort`, no click
handler. Measured: clicking it repeatedly leaves the column at `39, 45, 29`. Rather than write a
test that asserts nothing or fake it by sorting client-side, that slot covers the grid control
DemoQA *does* have — **pagination**: eight added records push the table to 11 rows, page 1 holds
10 with Previous disabled and the indicator reading `1 of 2`, Next leaves 1 row with Next
disabled, and `Show 20` collapses it back to `1 of 1`.

**9. Screenshot-on-failure cannot live in a `TestWatcher`.**
The obvious design — a separate extension implementing `TestWatcher` — cannot work: `testFailed`
fires *after* every `AfterEachCallback`, so the browser page is already closed and there is
nothing left to photograph. Capture therefore lives in the lifecycle extension itself, driven by
`getExecutionException()` while the page is still alive. Proved by **inducing** a failure rather
than assuming: a 79 KB full-page PNG and a 1.9 MB trace, both attached to the report.

**10. Surefire silently clobbers `argLine`.**
Allure needs the AspectJ weaver as a `-javaagent`, and setting `<argLine>` directly would break
any future coverage agent. The `@{argLine}` late-binding form composes instead of overwriting.
Similarly, `allure-junit5` was renamed to `allure-jupiter` in 2.35.x — the old coordinate still
resolves via relocation but warns on every build.

**11. One assertion was passing vacuously.**
The empty-form test originally asserted that the first-name input did *not* carry an `is-valid`
class. DemoQA never applies that class at all, so the assertion would also have passed on a page
that submitted successfully. It now asserts the states the form really reaches on rejection:
`#userForm` gains `was-validated` and six inputs match `:invalid`. Worth naming because a green
test that cannot fail is worse than no test.

## Test Report

`mvn clean test` then `mvn allure:serve`. Captured output in
[`docs/report-screenshots/`](docs/report-screenshots):

| | |
|---|---|
| [`allure-overview.png`](docs/report-screenshots/allure-overview.png) | 41 tests, 100% passed, with the effective configuration in the Environment widget |
| [`allure-test-detail.png`](docs/report-screenshots/allure-test-detail.png) | `@Step` nesting, parameters, and the full request body / headers / curl attached to a booking test |
| [`allure-failure-capture.png`](docs/report-screenshots/allure-failure-capture.png) | an **induced** failure, with the full-page screenshot and the Playwright trace attached |

The Playwright trace is attached to the report *and* written to `target/traces/`, so a failed CI
run is debuggable offline:

```bash
npx playwright show-trace target/traces/<TestClass>.<testMethod>.zip
```

A plain Surefire HTML report is available as a fallback — `mvn surefire-report:report-only`
writes `target/reports/surefire.html`.

## CI/CD

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs on push to `main`, on every pull
request, and on demand. **API and UI run as separate parallel jobs** so a UI flake never hides API
signal and both halves report at once. The UI job caches `~/.cache/ms-playwright` keyed on
`pom.xml`, so a Playwright version bump invalidates the cache instead of running against a stale
browser. `allure-results`, Surefire reports, screenshots and traces upload with `if: always()` —
screenshots and traces exist only when something failed, which is exactly when a reviewer needs
them. A third job merges both suites' results and publishes the Allure report to GitHub Pages.

**The workflow needs no secrets**, so it is green on a fork's very first pull request.

## What I Would Add With More Time

- **WireMock stubs for the failure paths.** The suite depends on three public services. Recording
  their contracts and replaying them would make it green offline and let us assert on responses
  those services will not produce on demand — a 500, a timeout, a malformed body. It would also
  let the CDN-level GraphQL error in challenge #2 be tested as a stable contract instead of an
  observation.
- **Contract testing against an OpenAPI spec.** Restful Booker publishes documentation; validating
  responses against a schema would catch a field type changing, which no hand-written assertion
  covers.
- **Visual regression on the practice form.** Playwright's screenshot comparison catches the class
  of breakage functional assertions are blind to — challenge #5, the Bootstrap rewrite, is exactly
  that.
- **A cross-browser matrix.** `BrowserFactory.launchBrowser()` is the single place that picks
  Chromium; lifting the engine into `config.properties` turns WebKit and Firefox into a CI matrix
  axis rather than a code change.
- **Mutation-style fuzzing of the booking payload.** Systematically dropping and corrupting
  required fields would map the API's real validation boundary, which its documentation does not
  describe.
- **A k6 smoke load profile** against the read endpoints, kept deliberately small — the brief asks
  us to be respectful of these public services.
- **Flakiness tracking.** Allure history across CI runs, so a test that fails one run in three —
  challenge #7 — surfaces as a trend rather than being found by hand.
