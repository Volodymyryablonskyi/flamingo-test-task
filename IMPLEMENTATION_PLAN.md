
# Flamingo QA Automation Assignment — Implementation Plan

> Working document that drives implementation. Source of truth for the assignment is
> `docs/Flamingo Home Assignment - QA Engineer.pdf`.
>
> **Status:** 
>plan complete, external dependencies probed and de-risked (see §11).
> Every version number, HTTP contract and error shape below was **verified against the live
> services**, not recalled — the ✅ markers point at the evidence in §11.

> ### ▶ Resume point — last updated 2026-09-18
>
> **Phase 0 is COMPLETE** (§2.5). Build is green: `mvn clean test` → 3/3, `mvn allure:report`
> renders, tag filtering verified. 3 commits on `main`, working tree clean.
>
> **No git remote is configured and nothing has been pushed yet.**
>
> **Next: Phase 1** — `Config` / `ConfigLoader` (§3.4) plus the `BaseApiTest` / `BaseUiTest`
> base classes; then Phase 2 (REST CRUD). Work the §9 roadmap in order; each phase ends in a
> green build plus the commit message drafted in that table.
>
> Outstanding manual steps for the user: re-import the project in IntelliJ as a Maven project
> (the old `.iml` was deleted), and run `gh auth login` before the repo can be created.

---

## 1. At a glance

| | |
|---|---|
| **Deliverable** | Maven/Java test suite: REST (Restful Booker) + GraphQL + UI (DemoQA/Playwright), Allure report, GitHub Actions CI |
| **Grading weights** | Framework Architecture **40%** · Code Quality **30%** · Test Design **20%** · Documentation **10%** |
| **Consequence** | The framework is worth more than the tests. Infrastructure is front-loaded; test classes stay thin. |
| **Scope** | 10 REST · 8 GraphQL · 9 UI (minimums are 3 / 5 / 2), each tagged P0/P1/P2 so scope can be cut without losing compliance |
| **Effort** | ~8.5 h across 9 phases, each ending in a green build + a commit |
| **Hard constraint** | `mvn clean test` must be **green from a cold clone with no secrets and no manual setup** (§5) |

---

## 2. Phase 0 — Prerequisites: what to install

### 2.1 Already present on this machine (verified)

| Tool | Version found | Status |
|---|---|---|
| JDK | 21.0.8 LTS (`C:\Program Files\Java\jdk-21`) | ✅ OK |
| Maven | 3.9.11 | ✅ OK |
| Node.js / npm | 24.16.0 / 11.13.0 | ✅ OK (only needed for optional MCP) |
| Git | 2.54.0.windows.1 | ✅ OK |

### 2.2 Must do before coding

| # | Action | Command | Why |
|---|---|---|---|
| 1 | Set `JAVA_HOME` (currently empty) | `setx JAVA_HOME "C:\Program Files\Java\jdk-21"` then reopen the shell | Maven works without it, but the Playwright CLI invoked through `exec-maven-plugin` expects it |
| 2 | Initialise the git repo | `git init && git branch -M main` | Repo is **not** under version control yet; "commit frequently to show your development process" is graded |
| 3 | Install Playwright browsers | `mvn exec:java -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"` | Run **after** `pom.xml` exists (Phase 1). Downloads to `%USERPROFILE%\AppData\Local\ms-playwright` |
| 4 | Enable annotation processing | IntelliJ → Settings → Build → Compiler → Annotation Processors → *Enable* | Lombok ships with IntelliJ 2023.1+; only the toggle is needed |

### 2.3 Optional

| Tool | Install | Needed? |
|---|---|---|
| GitHub CLI (`gh`) | `winget install --id GitHub.cli` | **Recommended.** Create the repo and tail Actions runs from the terminal. The web UI works too. |
| Allure CLI | `scoop install allure` | **No.** We use the `allure-maven` plugin — `mvn allure:serve` generates and opens the report with zero global installs. |

### 2.4 MCP servers — honest assessment

**No MCP server is required.** This is a plain Maven/Java build; standard file, shell and web
tools cover all of it. Two are genuinely useful, both optional:

| MCP / tool | Install | What it buys us |
|---|---|---|
| **Playwright MCP** | `claude mcp add playwright -- npx @playwright/mcp@latest` | Live DOM/accessibility-tree inspection of DemoQA while authoring Page Objects instead of guessing selectors. Cuts the slowest part of Phase 7. |
| **Claude-in-Chrome** *(already installed as a skill)* | — | Same purpose via your existing Chrome session. Use this **instead of** Playwright MCP if you'd rather not add a server. |

Explicitly **not** needed: GitHub MCP (`gh` suffices), filesystem MCP (built-in tools), any
HTTP/database MCP.

### 2.5 Phase 0 exit criteria — ✅ COMPLETE (commits `190a70e`, `4e6bdfb`)

- [x] `java -version` and `mvn -version` agree on JDK 21; `JAVA_HOME` set to `C:\Program Files\Java\jdk-21`
- [x] `git status` works inside the project — repo initialised on `main`
- [x] `src/Main.java` deleted; `flamingo-test-task.iml` deleted (replaced by `pom.xml`); PDF moved to `docs/` and the emoji stripped from its filename
- [x] `.gitignore` extended: `target/`, `allure-results/`, `allure-report/`, `.env`, `*.local.properties`, `test-output/`, traces/screenshots
- [x] `.gitattributes` added — `eol=lf` normalisation for the Ubuntu CI runner
- [x] GitHub CLI **2.101.0** installed; Playwright MCP registered and connected
- [x] Playwright **Chromium 153.0.8010.12** (build v1243) installed
- [x] `pom.xml` validates; all dependencies resolved at the pinned versions
- [x] `ToolchainSmokeTest` green (3/3) — Lombok, AssertJ, REST Assured and a real Chromium launch all verified
- [x] `mvn allure:report` renders successfully
- [x] `-Dgroups="smoke"` runs 3; `-Dgroups="api"` runs 0 **without failing the build**

> ⚠️ **IntelliJ re-import required.** The plain-Java `.iml` module was removed. Open
> `pom.xml` and choose *Add as Maven Project* (or File → Open → select `pom.xml`) so the IDE
> picks up the Maven structure and the test classpath.

---

## 3. Architecture

### 3.1 Guiding principles

1. **Tests declare intent, never mechanics.** A test method is assemble-data → call
   client/page-object → assert. No HTTP verbs, no CSS selectors, no waits inside a test class.
2. **Green from a cold clone.** A reviewer runs `git clone && mvn clean test` and gets green
   with **no secrets, no accounts, no manual configuration**. This is a hard constraint and it
   drives the endpoint decision in §5. A suite that only passes on the author's machine fails
   the assignment regardless of code quality.
3. **One layer of indirection, not three.** Page Objects and API Clients *are* the abstraction.
   No extra "steps" or "business" layer — over-abstracting a 27-test suite is a smell, not
   architecture.
4. **Configuration is injected, never hardcoded.** Every URL, timeout and toggle resolves
   through one `Config` facade with a documented precedence chain.
5. **Cross-cutting behaviour lives in JUnit 5 extensions**, not base-class `@BeforeEach` soup.
   Browser lifecycle, screenshot-on-failure and service health checks are all extensions.
   This is the highest-leverage architectural decision in the project.
6. **Every test cleans up after itself.** No test depends on another's side effects or on
   ordering — mandatory, since Restful Booker resets data and we run in parallel.
7. **Resilience belongs at the transport layer, not the test layer.** Retries live in an HTTP
   filter, so a retry never re-executes assertions (§3.5).

### 3.2 Module strategy

**Single Maven module.** A `framework` + `tests` split would be architecture theatre at this
size and slows the reviewer down.

All framework code lives in **`src/test/java`** alongside the tests. Rationale: this artifact is
never published or consumed as a library, so every dependency stays in `test` scope and the
build cannot leak test tooling into a `main` jar. `src/main/java` is deleted.

### 3.3 Package structure

```
qa-automation-assignment/
├── .github/workflows/ci.yml
├── docs/
│   ├── Flamingo Home Assignment - QA Engineer.pdf
│   └── report-screenshots/            # evidence for the "Test Report" deliverable
├── IMPLEMENTATION_PLAN.md             # this file
├── README.md
├── pom.xml
└── src/test/
    ├── java/com/flamingo/qa/
    │   ├── core/
    │   │   ├── config/
    │   │   │   ├── Config.java                # typed accessors
    │   │   │   └── ConfigLoader.java          # precedence chain (§3.4)
    │   │   ├── extension/
    │   │   │   ├── ServiceHealthExtension.java    # skip-with-reason if the SUT is down
    │   │   │   ├── PlaywrightExtension.java       # browser/context/page lifecycle
    │   │   │   └── ScreenshotOnFailureExtension.java
    │   │   └── util/
    │   │       ├── TestDataFactory.java       # DataFaker-backed builders
    │   │       └── ResourceReader.java        # classpath text/JSON/GraphQL loader
    │   ├── api/
    │   │   ├── client/
    │   │   │   ├── AuthClient.java
    │   │   │   ├── BookingClient.java
    │   │   │   ├── GraphQlClient.java
    │   │   │   └── TokenProvider.java         # thread-safe, caches the token per JVM
    │   │   ├── filter/
    │   │   │   └── TransientFailureRetryFilter.java   # §3.5
    │   │   ├── model/
    │   │   │   ├── booking/  Booking, BookingDates, BookingResponse, AuthRequest, AuthResponse
    │   │   │   └── graphql/  GraphQlRequest, GraphQlResponse, GraphQlError
    │   │   └── spec/
    │   │       ├── RequestSpecs.java          # RequestSpecBuilder factories
    │   │       └── ResponseSpecs.java         # reusable ResponseSpecification
    │   ├── ui/
    │   │   ├── pages/
    │   │   │   ├── BasePage.java
    │   │   │   ├── PracticeFormPage.java
    │   │   │   ├── WebTablesPage.java
    │   │   │   └── component/  SubmissionModal, DatePickerComponent, ReactSelectComponent
    │   │   └── support/
    │   │       ├── BrowserFactory.java        # ThreadLocal<Playwright>/<Browser>
    │   │       └── AdBlocker.java             # DemoQA ad/iframe suppression (§8)
    │   └── tests/
    │       ├── BaseApiTest.java
    │       ├── BaseUiTest.java
    │       ├── api/      AuthTest, BookingCrudTest, BookingSearchTest, BookingNegativeTest
    │       ├── graphql/  GraphQlPositiveTest, GraphQlNegativeTest
    │       └── ui/       PracticeFormTest, WebTablesTest
    └── resources/
        ├── config.properties
        ├── junit-platform.properties          # parallel execution config
        ├── allure.properties
        ├── graphql/                           # *.graphql files, one per operation
        ├── testdata/                          # JSON fixtures for data-driven tests
        └── upload/sample-upload.png           # file-upload fixture
```

### 3.4 Configuration management

`config.properties` holds committed defaults; everything is overridable without touching code.
`ConfigLoader` resolves in this order, first hit wins:

```
1. System property   -Dapi.base.url=...     ← ad-hoc local override
2. Environment var   API_BASE_URL           ← CI overrides
3. config.properties                        ← committed defaults (complete: no key is missing)
4. Fail fast, naming the missing key
```

Key mapping is mechanical: `api.base.url` ↔ `API_BASE_URL`. ~40 lines, no extra dependency.
Typed accessors (`Config.apiBaseUrl()`, `Config.headless()`, `Config.timeoutMs()`) so a typo is
a compile error, not a null.

Per principle #2, **step 3 alone is sufficient for a green run**. No key requires a secret.

### 3.5 API layer design

- **`RequestSpecs`** builds a `RequestSpecification` per flavour (`unauthenticated()`,
  `authenticated()`, `graphql()`) with base URI, `Content-Type`, the `AllureRestAssured` filter,
  and a **failure-only logging filter** (`LogDetail.ALL` only when validation fails). Quiet on
  green, fully diagnostic on red — and it honours the brief's "don't overload these services".
- **`TransientFailureRetryFilter`** — a REST Assured `Filter` that retries **5xx / 429 /
  connection timeouts** twice with exponential backoff. Putting retry here rather than in a
  JUnit `TestTemplate` matters: a test-level retry re-runs assertions and can mask a real
  intermittent bug, and it needs a non-trivial custom extension. An HTTP-layer filter is ~30
  lines, retries only genuinely transient transport failures, and leaves assertion failures
  fatal on the first attempt. *(Revised from the first draft, which put this at test level.)*
- **`TokenProvider`** fetches `POST /auth` **once per JVM** and caches it behind a
  `Supplier`-memoising holder (thread-safe — required, since classes run concurrently).
  Re-authenticating per test would mean ~20 pointless calls to a shared public service.
- **Clients return typed models** for happy paths, so assertions read
  `assertThat(booking.getFirstname())`. Negative tests use a raw-`Response` overload, because
  there the status code and error body *are* the subject.
- **Models use Lombok** `@Value @Builder @Jacksonized`. `@Jacksonized` is mandatory — without
  it Jackson silently cannot populate a Lombok builder.

### 3.6 GraphQL layer design

- Queries live in **`src/test/resources/graphql/*.graphql`**, loaded by `ResourceReader`. Out of
  Java string literals means IDE highlighting and validation, and the malformed-query negative
  test becomes just another file.
- `GraphQlRequest { String query; Map<String,Object> variables; String operationName; }`
  serialised by Jackson. **Variables are always a map** — string interpolation is explicitly
  called out as a failure mode in the brief.
- `GraphQlResponse` exposes `JsonNode data` + `List<GraphQlError> errors` with helpers
  `hasErrors()` and `dataAt("character.name")`.
- **Error contracts are asserted, not assumed.** The brief asks us to "verify which" shape the
  API returns. We have already measured it (§11.2): non-existent ID → **HTTP 200** with
  `data.character: null` and *no* `errors`; malformed query and unknown field → **HTTP 400**
  with an `errors[]` array and no `data`. The negative tests assert exactly this, including
  `extensions.code` (`GRAPHQL_VALIDATION_FAILED`), and the finding goes in the README's
  Challenges section — the "HTTP 200 for everything" assumption in the brief is **not**
  universally true, and demonstrating that we checked is the point of the exercise.

### 3.7 UI layer design

- **Lifecycle:** `Playwright` and `Browser` are created once per **thread** in a `ThreadLocal`
  (expensive, reusable). A fresh `BrowserContext` + `Page` per **test** (cheap; guarantees
  cookie/storage isolation and makes parallel execution safe). All of it in
  `PlaywrightExtension`, registered on `BaseUiTest`.
- **Waits:** Playwright's built-in auto-waiting plus `PlaywrightAssertions.assertThat(locator)`
  web-first assertions, which retry until timeout. **`Thread.sleep` is banned in review.** For
  a genuinely custom condition (e.g. row count settled after filtering), `page.waitForFunction`
  or `locator.waitFor(...)`. Verified as necessary: DemoQA serves a **436-byte HTML shell** and
  renders everything client-side (§11.3), so nothing is present on first paint.
- **Locators:** prefer `getByRole` / `getByLabel` / `getByPlaceholder`; fall back to `#id` only
  where DemoQA gives no accessible name. No XPath chains.
- **Failure capture:** `ScreenshotOnFailureExtension implements TestWatcher` — on `testFailed`
  it writes a full-page PNG to `target/screenshots/`, attaches it to Allure, and stops the
  per-test Playwright **trace** into `target/traces/`, so a failed CI run is debuggable offline
  via `npx playwright show-trace`.
- **Page Objects expose behaviour, not widgets:** `practiceFormPage.submitRegistration(student)`
  returns a `SubmissionModal`, not `void`. Composite widgets (React-Select state/city cascade,
  the date picker) are their own components, reused across pages.

### 3.8 Assertions and reporting conventions

- **All assertions go through AssertJ.** Multi-field verification (e.g. the submission modal's
  10 rows) uses `SoftAssertions` so one failing row doesn't hide the other nine — that single
  choice turns a three-iteration debug loop into one.
- Allure annotations `@Epic` / `@Feature` / `@Story` / `@Severity` and `@Step` on reusable
  helpers, so the bonus report reads as a structured spec rather than a flat method list.

### 3.9 Tagging and parallelism

Tags: `@Tag("api")`, `@Tag("graphql")`, `@Tag("ui")`, plus `@Tag("smoke")` on the critical path.
Surefire is configured so `mvn test -Dgroups="api"` works verbatim as the README promises.
GraphQL tests carry **both** `graphql` and `api`, so `-Dgroups="api"` runs them too — matching
the brief's own framing of GraphQL as Part 1 API testing.

`junit-platform.properties`:

```properties
junit.jupiter.execution.parallel.enabled = true
junit.jupiter.execution.parallel.mode.default = same_thread
junit.jupiter.execution.parallel.mode.classes.default = concurrent
junit.jupiter.execution.parallel.config.strategy = fixed
junit.jupiter.execution.parallel.config.fixed.parallelism = 4
```

Classes concurrent, methods within a class sequential. Deliberate: most of the wall-clock win,
intra-class ordering stays trivially safe, and concurrent load on the public services is capped
at 4 — respectful, as the brief asks.

---

## 4. Tech stack and pinned versions

`maven.compiler.release = 17` (brief says Java 11+; 17 is the safe modern floor and builds on
the local JDK 21). CI runs 17 **and** 21 to prove portability.

### ⚠️ "Latest" is a trap here — three deliberate downgrades

All versions below were resolved from Maven Central metadata on 2026-09-18 (§11.1). Taking the
literal newest of each would break the build or the brief:

| Library | Newest published | **We pin** | Why |
|---|---|---|---|
| JUnit | **6.1.3** | **5.14.4** | The brief says *"JUnit 5 (latest)"*. JUnit 6 is a different major line with changed coordinates and a Java 17+ floor. "Latest JUnit 5" is 5.14.4 — pinning 6.x would silently violate the stated stack. |
| Allure | **3.0.0** | **2.35.5** (`allure-maven` 2.18.0) | Allure 3 is a brand-new major with a rewritten report pipeline; its JUnit 5 and REST Assured integrations are unproven against this combination. A graded deliverable is the wrong place to debug someone else's `.0` release. |
| AssertJ | **4.0.0-M1** | **3.27.7** | 4.0.0-M1 is a milestone, not a release. |
| REST Assured | 6.0.1 | **5.5.7** | RA 6 is days-old on the 6.x line; 5.5.7 is the mature branch and its Allure integration is the one everybody ships. |

**This table goes in the README.** "Why I did *not* use the newest version" is exactly the kind
of judgement the Code Quality and Architecture criteria are looking for.

### Dependency set

| Dependency | Version | Role |
|---|---|---|
| `org.junit:junit-bom` | 5.14.4 | test platform (BOM-managed) |
| `io.rest-assured:rest-assured` | 5.5.7 | REST + GraphQL transport |
| `com.microsoft.playwright:playwright` | 1.63.0 | UI |
| `org.assertj:assertj-core` | 3.27.7 | **all** assertions |
| `com.fasterxml.jackson.core:jackson-databind` | 2.22.2 | JSON ser/de (+ `jackson-datatype-jsr310` for `LocalDate`) |
| `org.projectlombok:lombok` | 1.18.48 | boilerplate *(bonus)* |
| `io.qameta.allure:allure-bom` | 2.35.5 | `allure-junit5`, `allure-rest-assured` *(bonus)* |
| `org.aspectj:aspectjweaver` | 1.9.25 | required **by** Allure for `@Step` weaving |
| `net.datafaker:datafaker` | 2.7.0 | realistic random data → test isolation |
| `org.slf4j:slf4j-simple` | 2.0.19 | readable RestAssured/Playwright logs |

Plugins: `maven-surefire-plugin` 3.6.0 (tags, parallelism, `argLine` via **`@{argLine}`**
late-binding so the Allure agent composes instead of being clobbered), `allure-maven` 2.18.0
(`mvn allure:serve`, no CLI install), `maven-surefire-report-plugin` 3.6.0 (the plain-HTML
fallback the brief accepts), `exec-maven-plugin` 3.6.4 (Playwright browser install).

---

## 5. GraphQL endpoint — decision (resolved)

The brief points at `https://hygraph.com/graphql-playground` and says "select any available
schema: Video, Ecommerce, Marketing". **That page exposes no usable endpoint**: I scraped it for
`*.hygraph.com/v2/<project>/<stage>` URLs and `endpoint` references and found none — it is
client-rendered and the demo project's Content API URL never appears in the HTML (§11.2).
So there is nothing to copy-paste, and the brief's "documentation" link is not a working target.

Combined with principle #2 (*green from a cold clone, no secrets*), a Hygraph project gated
behind a Permanent Auth Token would mean **the reviewer's clone fails**. That is disqualifying.

**Decision — dual endpoint, config-switched:**

| | Endpoint | Auth | Role |
|---|---|---|---|
| **Default (committed)** | `https://rickandmortyapi.com/graphql` | none | Runs in CI and on a cold clone. ✅ **Verified** to satisfy every GraphQL requirement in the brief (§11.2) |
| **Optional** | Own Hygraph project from the Ecommerce starter, **public read enabled** (no token) | none | Set `GRAPHQL_URL` to switch. Demonstrates portability; committed `.graphql` files for both schemas |

Why Rick and Morty is the default — it covers the brief clause by clause, and I checked each:

| Brief requirement | Covered by | Verified |
|---|---|---|
| List with pagination/limit | `characters(page:) { info{count,pages} results{} }` | ✅ 200, `count: 826` |
| Single entity by ID | `character(id:)` | ✅ |
| Variables, not interpolation | `$page: Int`, `$id: ID!` | ✅ |
| Fragment / nested across types | `character → episode → name` (Character→Episode) | ✅ returns nested episodes |
| Invalid ID — assert the real shape | `character(id:"999999")` | ✅ **HTTP 200**, `{"data":{"character":null}}`, no `errors` |
| Malformed query — `errors[].message`, no `data` | truncated query | ✅ **HTTP 400** |
| Non-existent field — validation error | `nopeNotAField` | ✅ **HTTP 400**, `Cannot query field "nopeNotAField" on type "Character".`, `extensions.code: GRAPHQL_VALIDATION_FAILED` |

The important part architecturally: `GraphQlClient` takes the endpoint and auth header as pure
configuration, so switching schemas is a config line plus swapping `.graphql` files. **That
switchability is itself the deliverable** — and it gets written up in the README's Test Strategy
section, along with why the brief's own link could not be used as-is.

---

## 6. Test inventory

Minimums: 3 API CRUD · 5 GraphQL · 2 UI. Planned is well above, but **priority-tagged** so
scope can be cut under time pressure without dropping below compliance:
**P0** = required minimum · **P1** = strong add · **P2** = drop first.

### 6.1 REST — Restful Booker (`@Tag("api")`)

| # | Test | Pri | Type | Covers |
|---|---|---|---|---|
| 1 | `shouldIssueTokenForValidCredentials` | P0 | + | `POST /auth` ✅ verified: 200 + `{"token":"..."}` |
| 2 | `shouldNotIssueTokenForInvalidCredentials` | P1 | − | ✅ **verified quirk: HTTP 200 + `{"reason":"Bad credentials"}`**, not 401. Asserting the real contract, not the expected one |
| 3 | `shouldCreateBookingWithGeneratedData` | P0 | + | `POST /booking`, every field echoed back |
| 4 | `shouldRetrieveCreatedBookingById` | P0 | + | `GET /booking/{id}` round-trip |
| 5 | `shouldUpdateAllFieldsOfExistingBooking` | P0 | + | `PUT /booking/{id}` + auth header |
| 6 | `shouldRejectUpdateWithoutAuthToken` | P1 | − | 403 Forbidden |
| 7 | `shouldPartiallyUpdateBooking` | P2 | + | `PATCH` — untouched fields preserved |
| 8 | `shouldDeleteBookingAndReturn404OnSubsequentGet` | P0 | +/− | `DELETE`, then prove it is really gone |
| 9 | `shouldReturn404ForNonExistentBookingId` | P1 | − | ✅ verified: 404 |
| 10 | `shouldFilterBookingIdsByGuestName` | P1 | + | `GET /booking?firstname=&lastname=` — **`@ParameterizedTest`, data-driven** |

Isolation: a `BookingFixture` creates a uniquely-named booking in `@BeforeEach` and deletes it
in `@AfterEach`, tolerating 404 in case the service reset mid-run. Tests 3 and 8 manage their own.

### 6.2 GraphQL (`@Tag("graphql")`, `@Tag("api")`)

| # | Test | Pri | Type | Covers |
|---|---|---|---|---|
| 1 | `shouldReturnPaginatedListOfCharacters` | P0 | + | list + page limit; asserts `info.count` and no null rows |
| 2 | `shouldReturnSingleCharacterById` | P0 | + | entity by ID |
| 3 | `shouldPaginateUsingGraphQlVariables` | P0 | + | `$page: Int!` **as a variable**; asserts page 2 ∩ page 1 = ∅ |
| 4 | `shouldResolveNestedFieldsAcrossTypesUsingFragment` | P0 | + | `character { ...CharacterSummary episode { id name } }` |
| 5 | `shouldReturnNullDataForNonExistentId` | P0 | − | asserts the **measured** shape: 200, `data.character == null`, `errors` absent |
| 6 | `shouldReturnSyntaxErrorForMalformedQuery` | P1 | − | **400**, `errors[].message`, `data` absent |
| 7 | `shouldReturnValidationErrorForUnknownField` | P1 | − | **400**, exact message + `extensions.code` |
| 8 | `shouldReturnErrorWhenRequiredVariableIsMissing` | P2 | − | ✅ verified: `Variable "$id" of required type "ID!" was not provided.` |

### 6.3 UI — DemoQA (`@Tag("ui")`)

We implement **both Option A and Option B**. They exercise genuinely different mechanics
(complex widget interaction vs. CRUD-over-a-grid), and reusing `BasePage`, the components and
the browser extension across both is the clearest possible demonstration of POM.

**`PracticeFormTest`** — `/automation-practice-form`

| # | Test | Pri | Type | Covers |
|---|---|---|---|---|
| 1 | `shouldSubmitCompleteRegistrationAndShowSuccessModal` | P0 | + | full happy path: text fields, gender/hobbies, **date picker**, subjects autocomplete, **file upload**, React-Select state→city cascade, **modal verified row-by-row with `SoftAssertions`** |
| 2 | `shouldNotSubmitWhenMandatoryFieldsAreEmpty` | P0 | − | no modal; invalid fields flagged |
| 3 | `shouldRejectInvalidMobileNumbers` | P1 | − | `@ParameterizedTest`: too short / non-numeric / empty |

**`WebTablesTest`** — `/webtables`

| # | Test | Pri | Type | Covers |
|---|---|---|---|---|
| 4 | `shouldAddNewRecordToTable` | P0 | + | create + row assertion |
| 5 | `shouldEditExistingRecord` | P1 | + | update; other fields unchanged |
| 6 | `shouldDeleteRecord` | P1 | + | delete + row count decremented |
| 7 | `shouldFilterRecordsBySearchTerm` | P1 | +/− | `@ParameterizedTest` incl. the **"No rows found"** case |
| 8 | `shouldSortRecordsByAgeAscendingAndDescending` | P1 | + | AssertJ `isSortedAccordingTo` on the extracted column |
| 9 | `shouldValidateRequiredFieldsInRegistrationForm` | P2 | − | submit the empty modal form |

---

## 7. Requirements traceability

Proves nothing in the brief was missed. Checked off during Phase 9.

| Brief requirement | Satisfied by |
|---|---|
| REST Assured + JUnit 5 | §4 pinned stack |
| Auth — `POST /auth`, token reused | `AuthClient` + `TokenProvider` (§3.5); tests 6.1/1–2 |
| CRUD: POST / GET / PUT / DELETE booking | tests 6.1/3, 4, 5, 8 |
| GraphQL positive ×4 | tests 6.2/1–4 |
| GraphQL negative ×3 | tests 6.2/5–7 |
| UI: Playwright | §4 |
| UI: Page Object Model | `ui/pages/**` + components (§3.7) |
| UI: dynamic waits handled properly | auto-wait + web-first assertions; `Thread.sleep` banned (§3.7) |
| UI: screenshots of failures | `ScreenshotOnFailureExtension` + traces (§3.7) |
| AssertJ assertions | §3.8 — AssertJ only, `SoftAssertions` for multi-field |
| Jackson | models + GraphQL ser/de (§3.5, §3.6) |
| Allure *(bonus)* | §4, Phase 8 |
| Lombok *(bonus)* | `@Value @Builder @Jacksonized` (§3.5) |
| Clear package structure | §3.3 |
| `.gitignore` | Phase 0 |
| `pom.xml` with all dependencies | §4 |
| README (all 4 required sections) | Phase 9 |
| Test report | Allure + surefire HTML + `docs/report-screenshots/` |
| CI/CD *(bonus)* | Phase 9 |
| Data-driven tests *(nice)* | 6.1/10, 6.3/3, 6.3/7 |
| Custom waits / retry logic *(nice)* | `TransientFailureRetryFilter` (§3.5); custom waits (§3.7) |
| Parallel execution *(nice)* | §3.9 |
| "If a service is down, document it and mock it" | `ServiceHealthExtension` skip-with-reason (§8); WireMock listed in README's "with more time" |
| Meaningful commit messages | Phase table in §9, one commit per phase |

---

## 8. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **Restful Booker (Heroku) cold-starts or is down** | Whole API suite red for reasons unrelated to our code | `ServiceHealthExtension` pings `/ping` once per run; on failure the suite is **skipped with a reason** (`Assumptions.abort`), not failed. `TransientFailureRetryFilter` covers 5xx/429/timeouts. The brief explicitly sanctions this. |
| **Restful Booker resets data mid-run** | A booking vanishes between create and assert | Never rely on pre-existing data; each test creates what it needs, cleanup tolerates 404 |
| **DemoQA ad iframes intercept clicks** — the classic DemoQA flake | Non-deterministic UI failures | `AdBlocker`: `context.route()` aborts googlesyndication/doubleclick/adsbygoogle, plus an init-script hiding `#fixedban` and `footer`. Applied centrally in `PlaywrightExtension`, so no test repeats it. |
| **Practice-form submit button sits under the sticky footer** | `click()` times out | `scrollIntoViewIfNeeded()` in the page object (largely moot once the footer is hidden) |
| **Parallel execution + shared Playwright objects** | Cross-test interference | `ThreadLocal` browser, fresh `BrowserContext` per test, `classes.default=concurrent` only |
| **`TokenProvider` race under parallel classes** | Duplicate auth calls or a torn read | Memoised behind a thread-safe holder (§3.5) |
| **Lombok `@Builder` + Jackson** | Silent `null` fields | `@Jacksonized` on every deserialised model; the first API round-trip test catches it immediately |
| **Allure `argLine` clobbered by surefire** | Empty Allure report | `@{argLine}` late-binding syntax |
| **Bleeding-edge major versions** (JUnit 6, Allure 3, AssertJ 4-M1) | Build breaks on someone else's `.0` | Deliberate pins with documented rationale (§4) |
| **Rick and Morty API rate limits** | Intermittent failures | Low test count, parallelism capped at 4, retry filter covers 429 |

---

## 9. Roadmap

Nine phases, each ending in a **green build and a commit** — "commit frequently to show your
development process" is satisfied structurally, not retroactively. ~8.5 h.

| Phase | Deliverable | Exit criteria | Est. | Commit message |
|---|---|---|---|---|
| **0** | Environment + repo bootstrap | §2.5 checklist green | 0.5 h | `chore: initialise repository and project structure` |
| **1** | `pom.xml` + config + base classes | `mvn clean test` green with one placeholder test; `Config` resolves from all three sources; browsers installed | 1.0 h | `build: add maven build with junit5, rest-assured, playwright, allure` |
| **2** | REST auth + CRUD (tests 1–8) | 8 API tests green; token caching verified; no hardcoded URLs | 1.5 h | `test(api): cover restful-booker auth and booking CRUD` |
| **3** | REST negative + data-driven (9–10) | 10 API tests green; `@ParameterizedTest` wired to a JSON fixture | 0.5 h | `test(api): add negative and data-driven booking scenarios` |
| **4** | GraphQL client + positive (1–4) | 4 tests green; variables passed as a map; queries in `.graphql` files | 1.0 h | `test(graphql): add graphql client and positive query coverage` |
| **5** | GraphQL negative (5–8) | 8 GraphQL tests green against the **measured** contracts in §11.2 | 0.5 h | `test(graphql): assert error contracts for invalid queries` |
| **6** | UI framework | `PlaywrightExtension`, `BasePage`, `BrowserFactory`, `AdBlocker`, failure capture; one smoke test navigates DemoQA headless | 1.0 h | `feat(ui): add playwright page-object framework with failure capture` |
| **7** | UI tests (1–9) | 9 UI tests green headless **and** headed; zero `Thread.sleep`; screenshot verified on an **induced** failure | 2.0 h | `test(ui): cover practice form and web tables via page objects` |
| **8** | Reporting + parallelism | `mvn allure:serve` shows request/response + screenshot attachments; 3 consecutive parallel runs stable | 0.5 h | `ci: enable allure reporting and parallel execution` |
| **9** | CI + documentation | Actions green on push/PR; README complete; §7 traceability ticked; screenshots in `docs/report-screenshots/` | 0.5 h | `docs: add readme, ci workflow and execution report` |

Phases 4–5 dropped an hour versus the first draft: the endpoint question is now settled and the
error contracts are already measured, so implementation is transcription rather than discovery.

### Phase 9 detail — CI workflow

`.github/workflows/ci.yml`:
- Triggers: `push` to `main`, `pull_request`, `workflow_dispatch`
- Two parallel jobs, `api-tests` and `ui-tests` — fast feedback, and a UI flake never blocks
  API signal
- `actions/setup-java@v4` (temurin, matrix `[17, 21]` on the API job) with Maven caching
- UI job caches `~/.cache/ms-playwright`, installs with `install --with-deps chromium`
- **No secrets required** (§5) — so the workflow runs green on a fork's first PR
- `if: always()` upload of `allure-results`, `target/screenshots`, `target/traces`, surefire reports
- Bonus: publish the Allure report to GitHub Pages

### README sections (Phase 9)

Follow the brief's template verbatim, with substantive free text:
- **Test Strategy** — risk-based prioritisation; CRUD round-trips over field permutations; the
  config-driven endpoint decision (§5) and why the brief's Hygraph link could not be used as-is;
  the version-pinning rationale (§4)
- **Challenges & Solutions** — DemoQA ad iframes; Restful Booker's `200 + "Bad credentials"`;
  GraphQL returning **400**, not 200, for malformed queries; Lombok/Jackson `@Jacksonized`;
  surefire `@{argLine}`. Every one of these is a *measured* finding (§11), not a generic story.
- **What I Would Add With More Time** — WireMock stubs so the suite is green offline, contract
  testing against an OpenAPI spec, visual regression on the practice form, cross-browser matrix
  (WebKit/Firefox), k6 smoke load, mutation-style fuzzing of the booking payload

---

## 10. Definition of done

- [ ] `mvn clean test` green from a **cold clone**, JDK + Maven only, **no secrets, no setup**
- [ ] `mvn test -Dgroups="api"` and `-Dgroups="ui"` each run the correct subset
- [ ] ≥10 REST · ≥8 GraphQL · ≥9 UI, all meaningfully asserting (no `assertTrue(true)`)
- [ ] Every assertion via AssertJ; multi-field checks use `SoftAssertions`
- [ ] Zero `Thread.sleep`, zero hardcoded URLs/credentials, zero committed secrets
- [ ] Screenshot **and** trace produced on an induced UI failure (demonstrated, not assumed)
- [ ] Allure report renders with request/response and screenshot attachments
- [ ] GitHub Actions green **on a fork**, artifacts downloadable
- [ ] README complete, all four sections filled with real content
- [ ] §7 traceability matrix fully ticked
- [ ] Commit history tells the story of §9, minimum one commit per phase

---

## 11. Appendix — verified facts

Probed live on **2026-09-18**. Re-verify if implementation starts more than a few weeks later.

### 11.1 Version resolution (Maven Central metadata)

`junit-bom` latest 5.x **5.14.4** (newest overall 6.1.3) · `rest-assured` 5.x **5.5.7** (newest
6.0.1) · `playwright` **1.63.0** · `assertj-core` **3.27.7** (newest 4.0.0-M1, a milestone) ·
`jackson-databind` **2.22.2** · `lombok` **1.18.48** · `allure-bom` 2.x **2.35.5** (newest 3.0.0)
· `allure-maven` 2.x **2.18.0** · `aspectjweaver` **1.9.25** · `datafaker` **2.7.0** ·
`maven-surefire-plugin` **3.6.0** · `maven-surefire-report-plugin` **3.6.0** ·
`exec-maven-plugin` **3.6.4** · `slf4j-simple` **2.0.19**.

### 11.1b Build-time findings (discovered during Phase 0 — README "Challenges" material)

Three things only surfaced once the build actually ran. All are fixed in `pom.xml`:

| Finding | Symptom | Fix |
|---|---|---|
| **Allure library and report renderer are versioned independently** | `mvn allure:report` → `Can't install allure: Cannot resolve allure commandline dependencies … allure-commandline:zip:2.35.5 (absent)`. The Java library is 2.35.5; `allure-commandline` is at **2.46.1** and has no 2.35.5 release at all | Separate `allure.report.version` property; `<reportVersion>` points at it, not at the library version |
| **`allure-junit5` was renamed to `allure-jupiter`** in Allure 2.35.x | Build succeeds but warns *"has been relocated"* twice per run | Use the `allure-jupiter` coordinate directly |
| **`maven-compiler-plugin` 3.14.2 does not exist** | `go-offline` fails; the failure is then *cached* locally and not retried until the update interval elapses — needs `-U` to recover | Pinned to the real latest, **3.16.0**. Latest stable line: 3.13.0 → 3.14.0 → 3.14.1 → 3.15.0 → 3.16.0 |

Verified working afterwards: `mvn clean test` 3/3 green · `mvn allure:report` renders ·
`-Dgroups="smoke"` runs 3 · `-Dgroups="api"` runs 0 without failing (thanks to
`failIfNoSpecifiedTests=false`, which matters for the CI matrix job).

### 11.2 Live service contracts

**Restful Booker** — service up (`GET /ping` → 201 Created):

| Call | Result |
|---|---|
| `POST /auth` valid creds | `200` · `{"token":"1a6315d5750e68e"}` |
| `POST /auth` wrong password | **`200`** · `{"reason":"Bad credentials"}` — *not* 401 |
| `GET /booking/99999999` | `404` |

**Hygraph playground** — page scraped for `*.hygraph.com/v2/…` URLs and `endpoint` references:
**zero matches**. Client-rendered; no public endpoint is discoverable from the brief's link.

**Rick and Morty GraphQL** (`https://rickandmortyapi.com/graphql`):

| Call | Result |
|---|---|
| `characters(page:1){info{count} results{id name episode{id name}}}` | `200` · `count: 826`, nested episodes resolved |
| `character(id:"999999")` | **`200`** · `{"data":{"character":null}}`, no `errors` |
| truncated query | **`400`** |
| unknown field `nopeNotAField` | **`400`** · `Cannot query field "nopeNotAField" on type "Character".` · `extensions.code: GRAPHQL_VALIDATION_FAILED` |
| `$id: ID!` declared but not supplied | **`400`** · `Variable "$id" of required type "ID!" was not provided.` |

`https://countries.trevorblades.com/graphql` also responded `200` — a second fallback if needed.

> Note: the malformed-query body was not captured cleanly by the probe (status `400` confirmed,
> body empty in the transcript). Confirm the exact `errors[].message` in Phase 5 before
> asserting on its text; assert on status + `errors` non-empty if the message proves unstable.

### 11.3 DemoQA

`GET /automation-practice-form` → `200`, **436 bytes**. A client-rendered SPA shell: nothing is
in the initial HTML. Confirms that Playwright auto-waiting is load-bearing and that any
HTML-parsing shortcut would fail.