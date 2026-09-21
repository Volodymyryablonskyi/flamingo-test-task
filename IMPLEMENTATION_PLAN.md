
# Flamingo QA Automation Assignment — Implementation Plan

> Working document that drives implementation. Source of truth for the assignment is
> `docs/Flamingo Home Assignment - QA Engineer.pdf`.
>
> **Status:** 
>plan complete, external dependencies probed and de-risked (see §11).
> Every version number, HTTP contract and error shape below was **verified against the live
> services**, not recalled — the ✅ markers point at the evidence in §11.

> ### ▶ Resume point — last updated 2026-09-20
>
> **Phases 0–9 are COMPLETE**, on the `spribe_api_test` / `avenga-api-test` architecture
> (§3.2). API work is done (19 REST + 8 GraphQL, against minimums of 3 and 5) and the UI
> suite is in: `-Dgroups=ui` → **14 green headless and headed**, covering the practice form
> (date picker, subjects autocomplete, file upload, React-Select state→city cascade, modal
> verified with `SoftAssertions`) and web-tables CRUD-over-a-grid. DemoQA has been rewritten
> since the plan was drafted — see **§11.1i** before touching any UI locator.
>
> **No git remote is configured and nothing has been pushed yet.**
>
> **All nine phases are done.** `mvn clean test` → **41 green in ~28 s** at `parallelism = 4`;
> `mvn allure:report` renders with request/response, screenshot and trace attachments; README
> and `.github/workflows/ci.yml` are written; §7 traceability is fully ticked.
>
> **The one open item is the remote**: the CI workflow has never run, because there is nowhere
> to push it. Everything else in §10 is verified.
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

**Single Maven module, two source roots.** `src/main/java` is the reusable framework;
`src/test/java` holds only tests, their fixtures and their data. Dependencies the framework
itself needs are compile-scoped; JUnit, the Allure JUnit integration and DataFaker stay
test-scoped.

> **Revised 2026-09-20.** The first draft put everything in `src/test/java` to keep every
> dependency test-scoped. Changed on Vladimir's instruction to follow the conventions of his
> `spribe_api_test` and `avenga-api-test` projects, which both split this way. The split is
> also the clearer statement of the graded criterion — the framework is a thing that exists
> on its own, not a folder inside the tests.

Conventions adopted from those projects, in full:

| Piece | What it does |
|---|---|
| `StatusCode` enum | No magic numbers. Carries `describe(int)` so a mismatch reads `expected 200 OK but got 418 IM_A_TEAPOT` |
| `ResponseWrapper` | Wraps the REST Assured `Response`; every client method returns one, positive or negative |
| `ResponseVerifier` | Fluent AssertJ checks — `.verify().hasStatusCode(...).hasBodyEqualTo(...)`; `.and()` returns the wrapper |
| `HttpMethod` + `RequestBuilder` | The verb switch and the REST Assured chain exist exactly once |
| `BaseApiClient<T extends Endpoints>` | Owns the spec, turns method + path + body into a `ResponseWrapper` |
| `Endpoints` classes | Every URI built in one place per resource |
| `CustomLogger` | slf4j wrapper that knows how to log a request and a response |

One deliberate departure: `CustomLogger` does **not** also push Allure steps the way the
reference version does. `AllureRestAssured` already attaches the full request and response to
the report, so doing both would double the noise.

### 3.3 Package structure

```
qa-automation-assignment/
├── .github/workflows/ci.yml
├── docs/
│   ├── Flamingo Home Assignment - QA Engineer.pdf
│   └── report-screenshots/            # evidence for the "Test Report" deliverable
├── IMPLEMENTATION_PLAN.md             # this file
├── README.md
├── lombok.config                      # stopBubbling only
├── pom.xml
├── src/main/                          # ===== the framework =====
│   ├── java/com/flamingo/qa/
│   │   ├── config/      Config · ConfigLoader · ConfigurationException · RestAssuredConfigurator
│   │   ├── clients/     BaseApiClient · AuthApiClient · BookingApiClient · GraphQlApiClient · TokenProvider
│   │   ├── endpoints/   Endpoints · AuthEndpoints · BookingEndpoints
│   │   ├── http/
│   │   │   ├── request/   HttpMethod · RequestBuilder
│   │   │   ├── response/  StatusCode · ResponseWrapper · ResponseVerifier
│   │   │   └── retry/     TransientFailureRetry
│   │   ├── pojo/        auth/ · booking/ · graphql/
│   │   ├── ui/          pages/ · components/ · BrowserFactory
│   │   └── util/        CustomLogger · Json · ResourceReader
│   └── resources/
│       └── config.properties          # committed defaults
└── src/test/                          # ===== the tests =====
    ├── java/com/flamingo/qa/
    │   ├── base/        BaseApiTest · BaseRestTest · BaseGraphQlTest · BaseUiTest
    │   ├── extensions/  RequiresService · ServiceHealthExtension · SystemUnderTest
    │   │                PlaywrightExtension · ScreenshotOnFailureExtension
    │   ├── data/        BookingDataGenerator · StudentDataGenerator
    │   ├── fixtures/    BookingFixture
    │   ├── tests/       api/ · graphql/ · ui/
    │   └── <mirrors>    config/ · http/retry/ · pojo/   — framework unit tests, in the
    │                    same package as what they test so package-private stays testable
    └── resources/
        ├── junit-platform.properties  # parallel execution config
        ├── allure.properties
        ├── graphql/                   # *.graphql files, one per operation
        ├── testdata/                  # JSON fixtures for data-driven tests
        └── upload/sample-upload.png   # file-upload fixture
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

- **`RestAssuredConfigurator`** builds a `RequestSpecification` per flavour (`restSpec()`,
  `authenticatedRestSpec(token)`, `graphqlSpec()`) with base URI, `Content-Type`, the
  `AllureRestAssured` filter, and a **failure-only logging filter** (`LogDetail.ALL` only when
  validation fails). Quiet on green, fully diagnostic on red — and it honours the brief's
  "don't overload these services".
- **`TransientFailureRetry`** — retries **5xx / 429 / connection and read failures** with
  exponential backoff. Putting retry at the transport layer rather than in a JUnit
  `TestTemplate` matters: a test-level retry re-runs assertions and can mask a real
  intermittent bug. This wrapper is invoked by the clients, *below* the response-spec
  validation, so a retried request never re-runs an assertion and an assertion failure stays
  fatal on the first attempt.
  > ⚠️ **Corrected during Phase 2.** The first two drafts specified a REST Assured `Filter`.
  > That cannot work: `FilterContext.next()` walks a **single-use iterator** over the filter
  > chain, so the second call runs off the end and returns `null` instead of re-sending — a
  > retry filter looks right, turns the first retry into a `NullPointerException`, and is
  > measurably worse than no retry at all. Evidence in §11.1d; regression test in
  > `TransportResilienceTest`.
- **`TokenProvider`** fetches `POST /auth` **once per JVM** and caches it behind a
  `Supplier`-memoising holder (thread-safe — required, since classes run concurrently).
  Re-authenticating per test would mean ~20 pointless calls to a shared public service.
- **Clients return a `ResponseWrapper` for every call**, positive and negative alike, and the
  test asserts the status at the call site through the fluent verifier:
  `client.getById(id).verify().hasStatusCode(STATUS_200_OK).hasBodyEqualTo(Booking.class, expected)`.
  > **Revised 2026-09-20.** The first implementation split each client method into a typed
  > happy-path version and a raw-`Response` negative version. That doubles the client surface
  > and, worse, hides the status code behind a method name — the very thing a negative test is
  > about. One return type plus an explicit `StatusCode` assertion is smaller and says more.
- **Models are Java `record`s** (converted 2026-09-21, §11.1k). Jackson binds to the canonical
  constructor, so a deserialised model needs no annotation at all; Lombok `@Builder` stays only
  on the six models whose builders are actually called. `@Jacksonized` is gone from the project —
  it exists to make Jackson populate a *Lombok* builder, which records no longer need.

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
- **Locators:** held as `private final Locator` **fields** exposed through Lombok `@Getter`,
  never methods that build one per call — a Playwright `Locator` is a lazy description
  re-resolved on each action, so a field cannot go stale and the page object reads as a
  declaration of the screen. *(Convention set by Vladimir on 2026-09-20; applies to every
  page object and component added from here.)* Prefer `getByRole` / `getByLabel` /
  `getByPlaceholder`; fall back to `#id` only where DemoQA gives no accessible name. No XPath
  chains.
- **Failure capture:** `PlaywrightExtension` writes a full-page PNG to `target/screenshots/`,
  attaches it to Allure, and saves the per-test Playwright **trace** to `target/traces/`, so a
  failed CI run is debuggable offline via `npx playwright show-trace`.
  > ⚠️ **Corrected during Phase 6.** The plan specified a separate `ScreenshotOnFailureExtension
  > implements TestWatcher`. That cannot work: `TestWatcher.testFailed` fires *after* every
  > `AfterEachCallback`, so the page is already closed and there is nothing left to photograph.
  > Capture therefore lives in the lifecycle extension, driven by `getExecutionException()`.
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

Tags: `@Tag("regression")`, `@Tag("api")`, `@Tag("graphql")`, `@Tag("ui")`, plus `@Tag("smoke")`
on the critical path. Surefire is configured so `mvn test -Dgroups="api"` works verbatim as the
README promises. GraphQL tests carry **both** `graphql` and `api`, so `-Dgroups="api"` runs them
too — matching the brief's own framing of GraphQL as Part 1 API testing.

| Tag | Tests | Declared on |
|---|---|---|
| `regression` | 41 | `BaseApiTest` + `BaseUiTest` |
| `api` | 27 | `BaseApiTest` |
| `graphql` | 8 | `BaseGraphQlTest` |
| `ui` | 14 | `BaseUiTest` |
| `smoke` | 8 | individual methods |

`regression` is declared on the two roots of the hierarchy, **not** repeated per class. JUnit
inherits `@Tag` from superclasses, so a new test class is in the regression suite by construction
rather than by someone remembering to tag it — which is the only way a "run everything" tag stays
truthful as the suite grows. *(Added 2026-09-21 at Vladimir's request.)* It currently selects the
same 41 tests as an unfiltered `mvn test`; its value is being the **stable name** for the full
suite once tags that should not run by default exist (`flaky`, `slow`, `wip`), at which point CI
becomes `-Dgroups="regression & !wip"` and the default run stays honest.

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

`maven.compiler.release = 21`.

> **Revised 2026-09-20** from 17, on Vladimir's instruction, so the suite can use modern
> idioms — `List.getFirst()` arrived with `SequencedCollection` in Java 21 and does not
> compile against release 17. The brief asks for "Java 11+", so 21 satisfies it, but the
> trade is explicit and goes in the README: **a reviewer needs JDK 21**, and the CI matrix
> drops to a single version, so cross-version portability is no longer proven by the build.

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
| 10 | `shouldFilterBookingIdsByGuestName` | P1 | + | `GET /booking?firstname=&lastname=` — **`@ParameterizedTest` over a JSON fixture**, 5 name shapes (apostrophe, accents, hyphen) ✅ all match |
| 11 | `shouldReturnNoIdsForUnknownGuest` | P1 | − | ✅ empty array, not a 404 |
| 12 | `shouldRejectDeleteWithoutAuthToken` | P1 | − | 403, and the booking is proved to have survived |
| 13 | `shouldReturnServerErrorForIncompletePayload` | P2 | − | ✅ **500, not 400 — an API defect, pinned as measured** (§11.1f) |
| 14 | `shouldStoreBookingDatesWithoutTimeZoneShift` | P2 | + | dates compared against locally built `LocalDate`s, so a symmetric serialisation bug cannot hide |

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
| 6 | `shouldReturnSyntaxErrorForMalformedQuery` | P1 | − | **400**, `errors[].message` non-blank, `data` absent. Text not pinned: the rejection comes from the **Stellate CDN**, not the GraphQL server (§11.2) |
| 7 | `shouldReturnValidationErrorForUnknownField` | P1 | − | **400**, exact message + `extensions.code` |
| 8 | `shouldReturnErrorWhenRequiredVariableIsMissing` | P2 | − | ✅ exact spec wording asserted; `extensions.code` is **`INTERNAL_SERVER_ERROR`** — a mislabelled validation failure (§11.2) |

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
| 7 | `shouldFilterRecordsBySearchTerm` | P1 | +/− | `@ParameterizedTest` incl. the no-match case |
| 8 | ~~`shouldSortRecordsByAgeAscendingAndDescending`~~ → `shouldPaginateRecordsBeyondThePageSize` | P1 | + | **Sorting no longer exists on DemoQA** (§11.1i). Replaced by the other grid control: page size, page indicator, Next/Previous enablement |
| 9 | `shouldValidateRequiredFieldsInRegistrationForm` | P2 | − | submit the empty modal form |

---

## 7. Requirements traceability

Proves nothing in the brief was missed - checked off in Phase 9. **Every row is satisfied.**

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
| UI: screenshots of failures | `PlaywrightExtension` + traces (§3.7, corrected in §11.1h); proved on an induced failure |
| AssertJ assertions | §3.8 — AssertJ only, `SoftAssertions` for multi-field |
| Jackson | models + GraphQL ser/de (§3.5, §3.6) |
| Allure *(bonus)* | §4, Phase 8; `AllureEnvironmentListener` fills the Environment widget |
| Lombok *(bonus)* | `@Builder` on the six models with builders, `@Getter` on every page object (§3.5, §3.7) |
| Clear package structure | §3.3 |
| `.gitignore` | Phase 0; `.playwright-mcp/` added in Phase 7 |
| `pom.xml` with all dependencies | §4 |
| README (all 4 required sections) | `README.md` — Prerequisites · How to Run · Test Strategy · Challenges & Solutions · What I Would Add |
| Test report | Allure (`mvn allure:serve`) + `mvn surefire-report:report-only` + 3 screenshots in `docs/report-screenshots/` |
| CI/CD *(bonus)* | `.github/workflows/ci.yml` — api-tests / ui-tests / report, no secrets |
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
| ~~DemoQA ad iframes intercept clicks~~ — **measured away, 2026-09-20** | — | **No mitigation needed.** DemoQA no longer serves ads: three runs with no blocking showed `iframe[id^='google_ads']` count **0** and a Submit click landing in ~48 ms. `AdBlocker` was deleted rather than kept for a problem that no longer exists. If ads return, Playwright's actionability checks surface it as an explicit click timeout, which is a clear signal to re-add it. |
| **Practice-form submit button sits under the sticky footer** | `click()` times out | `scrollIntoView()` in `BasePage`. The footer is in the DOM but was measured not to intercept the click |
| **Parallel execution + shared Playwright objects** | Cross-test interference | `ThreadLocal` browser, fresh `BrowserContext` per test, `classes.default=concurrent` only |
| **`TokenProvider` race under parallel classes** | Duplicate auth calls or a torn read | Memoised behind a thread-safe holder (§3.5) |
| ~~**Lombok `@Builder` + Jackson**~~ — **dissolved 2026-09-21** | Was: silent `null` fields | The models are records, so Jackson uses the canonical constructor and there is no builder to populate. The whole failure mode is gone rather than mitigated (§11.1k) |
| **Allure `argLine` clobbered by surefire** | Empty Allure report | `@{argLine}` late-binding syntax |
| **Bleeding-edge major versions** (JUnit 6, Allure 3, AssertJ 4-M1) | Build breaks on someone else's `.0` | Deliberate pins with documented rationale (§4) |
| **Rick and Morty API rate limits** | Intermittent failures | Low test count, parallelism capped at 4, retry filter covers 429 |

---

## 9. Roadmap

Nine phases, each ending in a **green build and a commit** — "commit frequently to show your
development process" is satisfied structurally, not retroactively. ~8.5 h.

| Phase | Deliverable | Exit criteria | Est. | Commit message |
|---|---|---|---|---|
| **0** ✅ | Environment + repo bootstrap | ✅ §2.5 checklist green | 0.5 h | `chore: initialise repository and project structure` |
| **1** ✅ | Config + transport layer + API base classes | ✅ 12/12 green; `ConfigLoaderTest` proves all three precedence sources; health-check skip proved both ways | 1.0 h | `feat(core): add configuration management and API transport layer` |
| **2** ✅ | REST auth + CRUD (tests 1–8) | ✅ 10 API tests green; token caching verified by test; no hardcoded URLs | 1.5 h | `test(api): cover restful-booker auth and booking CRUD` |
| **2b** ✅ | Comment trim, dead-class removal, then the §3.2 architecture move | ✅ 24/24 green, no build warnings; `-Dgroups=api` → 10 | 1.0 h | `refactor: adopt the spribe/avenga framework architecture` |
| **3** ✅ | REST negative + data-driven (9–10) | ✅ 19 API tests green; `@ParameterizedTest` wired to `testdata/guest-name-cases.json` | 0.5 h | `test(api): add negative and data-driven booking scenarios` |
| **4** ✅ | GraphQL client + positive (1–4) | ✅ 4 tests green; variables passed as a map; queries in `.graphql` files | 1.0 h | `test(graphql): add graphql client and positive query coverage` |
| **5** ✅ | GraphQL negative (5–8) | ✅ 8 GraphQL tests green against the **measured** contracts in §11.2, two of which the Phase 5 re-probe corrected | 0.5 h | `test(graphql): assert error contracts for invalid queries` |
| **6** ✅ | UI framework | ✅ all of it, plus screenshot **and** trace proved on an induced failure — a phase early | 1.0 h | `feat(ui): add playwright page-object framework with failure capture` |
| **7** ✅ | UI tests (1–9) | ✅ 14 UI executions (9 methods + parameterised cases) green headless **and** headed; zero `Thread.sleep` in UI code; 81 KB screenshot + 2.1 MB trace re-proved on an induced failure | 2.0 h | `test(ui): cover practice form and web tables via page objects` |
| **8** ✅ | Reporting + parallelism | ✅ report renders 116 request/response, 1 screenshot and 1 trace attachment; 3 consecutive parallel runs green at 29.6 / 28.8 / 27.3 s against 83.4 s sequential | 0.5 h | `ci: enable allure reporting and parallel execution` |
| **9** ✅ | CI + documentation | ✅ workflow written (3 jobs, no secrets); README complete against the brief's template; §7 traceability fully ticked; 3 report screenshots captured. **Actions cannot be verified green until a remote exists** | 0.5 h | `docs: add readme, ci workflow and execution report` |

Phases 4–5 dropped an hour versus the first draft: the endpoint question is now settled and the
error contracts are already measured, so implementation is transcription rather than discovery.

### Phase 9 detail — CI workflow

`.github/workflows/ci.yml`:
- Triggers: `push` to `main`, `pull_request`, `workflow_dispatch`
- Two parallel jobs, `api-tests` and `ui-tests` — fast feedback, and a UI flake never blocks
  API signal
- `actions/setup-java@v4` (temurin **21**) with Maven caching — single version, see §4
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

- [x] `mvn clean test` green from a **cold clone**, JDK + Maven only, **no secrets, no setup** — 41/41
- [x] `mvn test -Dgroups="api"` → 27 and `-Dgroups="ui"` → 14, each the correct subset
- [x] ≥10 REST · ≥8 GraphQL · ≥9 UI, all meaningfully asserting — 19 · 8 · 14, and the one
      vacuous assertion found in review was replaced (§11.1i)
- [x] Every assertion via AssertJ; multi-field checks use `SoftAssertions`
- [x] Zero `Thread.sleep` in UI code, zero hardcoded URLs/credentials, zero committed secrets
- [x] Screenshot **and** trace produced on an induced UI failure (demonstrated, not assumed) —
      79 KB PNG + 1.9 MB trace, both attached to Allure
- [x] Allure report renders with request/response, screenshot and trace attachments
- [ ] GitHub Actions green **on a fork**, artifacts downloadable — **blocked: no remote exists**
- [x] README complete, all sections of the brief's template filled with real content
- [x] §7 traceability matrix fully ticked
- [x] Commit history tells the story of §9, minimum one commit per phase

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

### 11.1c Phase 1 findings

| Finding | Symptom | Fix |
|---|---|---|
| **`java.net.http.HttpClient` only became `AutoCloseable` in Java 21** | `try (HttpClient client = ...)` does not compile against `maven.compiler.release=17`, even on a JDK 21 toolchain — exactly what the release flag is for | Plain instantiation plus `try`/`catch` in `ServiceHealthExtension`. Caught at compile time, but it is the class of mistake that would otherwise only surface in the CI matrix's Java 17 job |
| **Restful Booker's auth token is a cookie, not a bearer header** | Confirmed against the API docs; a `Authorization: Bearer` header is silently ignored and the `PUT` returns 403 | `RequestSpecs.authenticated(token)` sets `Cookie: token=…` once, so no test repeats the detail |

Verified in Phase 1:

- `mvn clean test` → **12/12 green** (3 toolchain smoke + 9 config).
- The precedence chain is proved by `ConfigLoaderTest`, which injects the environment and
  system-property lookups — a JVM cannot set its own environment variables, which is *why*
  `ConfigLoader` takes them as functions rather than calling `System.getenv` inline.
- `ServiceHealthExtension` proved **both ways**: against the live service the class runs;
  with `-Dapi.base.url=http://localhost:1` the class is **skipped with a reason** and the
  build stays green. Restful Booker was up on 2026-09-20, so §11.2 still holds.

### 11.1d Phase 2 findings — all three are README "Challenges" material

Every one of these was invisible by inspection and only appeared by running the suite
against the live service. Each is now covered by a regression test.

| Finding | Symptom | Fix |
|---|---|---|
| **Restful Booker answers a multi-value `Accept` header with HTTP 418 I'm a Teapot** | Every write failed with `Expected status code <200> but was <418>`. REST Assured's `ContentType.JSON` expands to `application/json, application/javascript, text/javascript, text/json` | `RequestSpecs` sets the literal string `application/json`. Measured: `application/json` → **200**, `application/json, text/json` → **418**, `text/plain` → **418** |
| **A read timeout arrives as a *checked* `SocketTimeoutException`** thrown through REST Assured's Groovy internals | `catch (RuntimeException)` let it straight past, so the retry never fired for the one failure it most needed to cover | Catch `Exception`; wrap a non-runtime failure keeping the original as cause. `TransportResilienceTest` counts accepted connections to prove N attempts really happen |
| **Retry inside a REST Assured `Filter` cannot work** | `FilterContext.next()` walks a single-use iterator; the second call runs off the end and returns `null`, so attempt 2 threw `NullPointerException: because "response" is null` | Retry moved out of the filter chain into `TransientFailureRetry`, called by the clients below response-spec validation |

What triggered the investigation: one `POST /booking` against a stalled Heroku dyno took
**372 seconds**. Two things were learned about the timeout configuration —

- The configured socket timeout **does** apply (proved: a server that accepts and never
  answers aborts at the configured 800 ms, not later).
- `SO_TIMEOUT` is a **per-read** timeout, so it cannot bound a response that trickles bytes
  forever. That is why the response-time ceiling in `ResponseSpecs` stays: it is the only
  guard on *total* duration, and it is what caught the 372-second call.

Also verified in Phase 2: `@JsonNaming(LowerCaseStrategy)` **is** carried onto the Lombok
builder by `@Jacksonized`, so the models keep idiomatic camelCase without a `@JsonProperty`
on every field. `mvn clean test` → **27/27** on two consecutive runs; `-Dgroups=api` → 10.

### 11.1e Architecture-move findings

| Finding | Symptom | Fix |
|---|---|---|
| **Jackson 3 arrives transitively**, so `@Jacksonized` cannot tell which variant to generate for | Every pojo compiled with *"Ambiguous: Jackson2 and Jackson3 exist; define which variant(s) you want in `lombok.config`"*. Only appeared once the pojos moved to `src/main/java` and the compile classpath changed | `lombok.config` with `lombok.jacksonized.jacksonVersion = 2`. Key and accepted value (`"2"`, not `TWO`) read out of `lombok-1.18.48.jar` rather than guessed |
| **Package-private framework internals are not reachable from a differently-named test package** | `ConfigLoader`'s test constructor and `environmentVariableName` became invisible when the test moved to `…framework.config` | Framework unit tests mirror the package they test (`com.flamingo.qa.config` in both source roots) — the standard Maven arrangement |
| **A verifier that logs at INFO is noisy on green** | `Verify status code is 200 OK` printed once per assertion, against the "quiet on green" rule the logging config already follows | Dropped to `debug`; only retries and cleanup anomalies log at WARN |

### 11.1f Phase 3 findings

| Finding | Evidence | Handling |
|---|---|---|
| **`POST /booking` with an incomplete body returns HTTP 500**, not 400 | `{"firstname":"OnlyThis"}` → `500` · `Internal Server Error` | Asserted as measured in `BookingNegativeTest`. A genuine defect in the SUT and the most report-worthy thing the API tests found — it belongs in the README's Challenges section as a bug, not as a quirk |
| **Name search handles apostrophes, accents and hyphens correctly** | `O'Brien`, `Renée Müller`, `Smith-Jones` all round-trip through the query string and return exactly the created id | No workaround needed. Worth keeping as the data-driven case set precisely because it is where such a lookup usually breaks |
| **An unmatched search is `[]` with 200**, not 404 | `firstname=Nobody` → `200` · `[]` | Asserted directly |
| **Restful Booker invalidates the cached token when it resets mid-run** | A live run produced six consecutive 403s on authenticated calls, with new bookings landing on a freshly restarted id sequence | `BaseApiClient` drops the token and re-sends once on a 403, but only for authenticated clients — on an unauthenticated one 403 is the expected answer and a retry would hide it |

A 5xx is retryable at the transport layer, so the incomplete-payload test sends its request
three times before the status is asserted. Three requests once per run is within what the
brief's "don't overload these services" allows, and the alternative — special-casing the
retry policy per call — would complicate the client to save two seconds.

### 11.1g Phase 4 notes

- The three documents were run against the live schema before anything was asserted:
  `characters(page:)` → `count: 826`, `pages: 42`, `prev: null`, `next: 2`; page 2 starts at
  id 21 and shares nothing with page 1; the fragment resolves and episodes come back with
  production codes such as `S01E01`.
- **`info.count` is asserted as a relation, not a number.** `pages == ceil(count / 20)` holds
  whatever the catalogue size is; pinning 826 would break the first time a character is added.
- `GraphQlEndpoints.getQueryUri()` returns an empty path on purpose — a GraphQL API has one
  URL, and `graphql.url` already carries it in full so the schema can be repointed from
  configuration.


**Post-phase correction — `AdBlocker` deleted.** The plan treated DemoQA's ad iframes as the
headline UI risk (§8). Measured instead of assumed: with blocking switched off, three runs
reported **zero** `iframe[id^='google_ads']` elements and a Submit click completing in
**48 ms**. The site no longer serves ads, so the class was solving a problem that does not
exist and was removed. The sticky footer is still in the DOM but does not intercept; the
`scrollIntoView` helper in `BasePage` covers it.

**Comments removed from `src/main/java` as well** (248 lines), on Vladimir's instruction, so
neither source root carries prose now. Nothing measured is lost — every finding lives in this
appendix and goes into the README's Challenges section in Phase 9. One place is worth knowing
about: `RestAssuredConfigurator.SINGLE_VALUE_ACCEPT_JSON` must stay a literal single value,
because `ContentType.JSON` expands to four and Restful Booker answers that with **418**. The
constant was renamed to carry that hint without a comment.

### 11.1h Phase 6 findings

| Finding | Why it matters | Handling |
|---|---|---|
| **`TestWatcher.testFailed` runs after every `AfterEachCallback`** | The plan specified a separate `ScreenshotOnFailureExtension` implementing `TestWatcher`. By the time it fires, the lifecycle extension has closed the page, so it could only photograph something that no longer exists | Lifecycle and capture merged into `PlaywrightExtension`, which reads `getExecutionException()` in `afterEach` while the page is still alive. **§3.7 is corrected accordingly** |
| **`Store.CloseableResource` is deprecated in JUnit 5.14** | Registering the browser shutdown that way logs *"Type implements CloseableResource but not AutoCloseable"* on every run | The shutdown value is a plain `AutoCloseable` record |
| **A trace on every test is waste** | Tracing every passing test writes hundreds of KB nobody opens | Tracing starts for every test but the trace is only written on failure; a pass stops it with no path, discarding it |

Verified by **inducing a failure**, not by assuming: a 94 KB full-page screenshot and a
206 KB trace were written under `target/` and attached to Allure. The screenshot also
showed the form rendering with no ad iframe at all, which is what prompted the measurement
that removed `AdBlocker` entirely. It is a good candidate for `docs/report-screenshots/`
in Phase 9.

**Observed on a live run: `mvn clean test` took 495 s**, against ~35 s normally. Restful
Booker was resetting and throwing connection resets; the log shows
`DELETE /booking/3839 was rejected with 403 - refreshing the auth token and retrying once`,
i.e. the §11.1f token fix firing and keeping the suite green. Two consequences worth naming:

- The socket timeout bounds each *read* at 30 s, not the whole call, so a service that is
  slow but progressing stretches the run rather than failing it. That is the right trade for
  a free public dyno — a false red is worse than a slow green — but it does mean a reviewer
  can hit a multi-minute run. Worth a line in the README.
- `ResponseSpecs`, which used to impose a 30 s total-duration ceiling, went away when the
  clients moved to `ResponseWrapper`. Nothing guards total duration now. Deliberate for the
  same reason, and recorded here so the removal is not mistaken for an oversight.

### 11.1i Phase 7 findings — DemoQA has been rewritten; the planned locators no longer hold

Re-probed live on **2026-09-21**. DemoQA has migrated from Bootstrap 4 + ReactTable to
Bootstrap 5 + a plain `<table>`, which invalidates several widely-copied locators.

| Finding | Why it matters | Handling |
|---|---|---|
| **`Pattern.quote` cannot be used in a Playwright `hasText` regex** | Playwright serialises a Java `Pattern` into a **JavaScript** `RegExp`. JS has no `\Q…\E` quoting, so `^\QOther\E$` compiles to a pattern matching the literal `QOtherE` — it silently matches nothing and the click dies on a 15 s actionability timeout with a misleading "waiting for locator" log | Gender/hobby resolve through `genderGroup.getByText(label, setExact(true))` scoped to `#genterWrapper` / `#hobbiesWrapper`. Exact-text matching also avoids the trap that `"Male"` is a substring of `"Female"`, which a plain `setHasText(String)` would hit |
| **The web table no longer sorts** | Planned test 8 asserted `isSortedAccordingTo` after clicking the Age header. The rewritten markup is `<th style="width: 40px;">Age</th>` — no class, no `aria-sort`, no handler. Measured: clicking it three times left the column at `39, 45, 29` | Test 8 replaced by `shouldPaginateRecordsBeyondThePageSize`, the grid control DemoQA *does* have: 8 added records push the table to 11 rows → page 1 holds 10 with Previous disabled and the indicator reading `1 of 2`; Next leaves 1 row with Next disabled; `Show 20` collapses back to `1 of 1` with all 11 |
| **There is no "No rows found" message** | §6.3 test 7 expected it. The rewritten table simply empties `<tbody>` | The parameterised case asserts `hasCount(0)` for `NoSuchRecordAnywhere`, which is what the page actually does |
| **Gender/hobby labels are `form-check-label`, not `custom-control-label`** | Every tutorial locator for this page is Bootstrap 4 | Locators key off `label[for^=…]` inside the wrapper, so the class change is irrelevant |
| **Empty submit marks `#userForm` `was-validated` and leaves 6 `input:invalid`** | The original assertion (`not().hasAttribute("class", "is-valid")`) passed vacuously — that class is never applied, so it would also pass on a page that submitted successfully | Asserts `hasClass("was-validated")` and `not().hasCount(0)` on `#userForm input:invalid` — both are states the page actually reaches only on rejection |

`#userNumber` is `type="text"` with `pattern="\d*" minlength="10" maxlength="10"`, so the
parameterised mobile cases (`123`, `abcdefghij`, empty) all fill cleanly and are rejected by
constraint validation rather than by the input refusing the keystrokes.

### 11.1j Phase 8 findings

Reporting and parallelism were already wired in Phase 1, so this phase was mostly *proving*
them — which is exactly what turned up the suite's only real flake.

| Finding | Why it matters | Handling |
|---|---|---|
| **An unscoped `getByRole(OPTION)` is ambiguous on the practice form** | A native `<select>`'s `<option>` elements carry the implicit `option` role, so while the date picker is open the page holds **213** of them — its month and year dropdowns. `selectSubject` used `page.getByRole(OPTION).first()`, which therefore resolved to `<option value="0">January</option>` whenever the picker had not finished closing. That element is inside a closed `<select>` and can never be clicked, so the test burned the full 15 s timeout. Under 4-way parallelism the picker closes more slowly and the race widens — it failed **1 run in 3** | Every react-select lookup is scoped to its own container (`#subjectsContainer`, `#state`, `#city`, where react-select renders its menu inline rather than in a portal) and matches the option by **exact accessible name** instead of `.first()`. Three consecutive parallel runs green afterwards |
| **The trace was written but never attached** | §3.7 promises the trace reaches Allure. Phase 6 wrote it to `target/traces/` only, so a reviewer reading the report offline had the screenshot but not the trace | `PlaywrightExtension` now attaches the zip as well. Verified in the generated report: 116 `text/html` request/response attachments, 1 `image/png`, 1 `application/zip` |
| **Parallelism is worth about 3×** | The claim needed a number, not an adjective | Measured on this machine: **83.4 s** sequential against **29.6 / 28.8 / 27.3 s** at `parallelism = 4`. README material |
| **`mvn allure:report` self-installs the renderer** | It unpacks allure-commandline 2.46.1 into `.allure/` on first use — no global Allure CLI, which is what makes the bonus report reproducible from a cold clone | `.allure/` was already in `.gitignore` |

### 11.1k Records conversion (2026-09-21)

Audited every type in the project against the record criteria — immutable, no superclass, no
state beyond its components — and converted the **14** that qualified. Verified by the compiler
and by a green 41/41, not by inspection.

| Converted | Kept as a class, and why |
|---|---|
| All 14 pojos: `AuthRequest` · `AuthResponse` · `Booking` · `BookingDates` · `BookingResponse` · `Character` · `CharactersPage` · `Episode` · `GraphQlError` (+ nested `Location`) · `GraphQlRequest` · `Info` · `Employee` · `Student` | **`ResponseWrapper`** — its private constructor plus `of()` factory exist so that *every* construction logs the response. A record's canonical constructor cannot be less accessible than the record, so converting would expose an unlogged `new ResponseWrapper(...)` **and** leak the wrapped REST Assured `Response` through a generated `response()` accessor, defeating the encapsulation the wrapper exists for |
| *(`GuestNameCase` and `BrowserShutdown` were already records)* | **`CustomLogger`** — same shape, same objection: `logger()` would leak the SLF4J `Logger` |
| | **`AuthEndpoints` / `BookingEndpoints` / `GraphQlEndpoints`** — stateless. A zero-component record is legal but buys nothing |
| | Clients, pages, components, config, extensions — mutable state, inheritance, or both |

Three findings worth keeping:

- **Jackson needs no help with records.** It binds to the canonical constructor, and
  `@JsonNaming(LowerCaseStrategy)` is applied to record components exactly as it was to Lombok
  fields. Proved by removing `@Jacksonized` from `Booking` and `BookingDates` and watching the
  27 API tests stay green. **This dissolves the §8 risk** *"Lombok `@Builder` + Jackson → silent
  null fields"*: there is no builder left for Jackson to fail to populate. `lombok.config`'s
  `lombok.jacksonized.jacksonVersion = 2` pin went with it — the Jackson 2/3 ambiguity warning it
  suppressed only ever came from `@Jacksonized` — and the build is still warning-free.
- **Lombok `@Builder(toBuilder = true)` works on a record** in 1.18.48, which is what let
  `Booking`, `Student` and `Employee` keep the `toBuilder()` the tests use for one-field variants.
- **The rename is the whole cost, and the compiler priced it**: 93 call sites moved from
  `getX()` to `x()`. It had to be driven off compiler positions rather than a find-and-replace,
  because `PracticeFormPage` exposes `@Getter` locators under the *same* names — `form.getFirstName()`
  returns a `Locator` while `student.getFirstName()` was the pojo. A textual replace would have
  silently broken the page object; 92 were fixed from javac's file/line/column and the last one
  was a `Character::getId` method reference, which has no `.get…()` form to match.

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
| truncated query | **`400`** · see the correction below |
| unknown field `nopeNotAField` | **`400`** · `Cannot query field "nopeNotAField" on type "Character".` · `extensions.code: GRAPHQL_VALIDATION_FAILED` · `locations` present |
| `$id: ID!` declared but not supplied | **`400`** · `Variable "$id" of required type "ID!" was not provided.` · `extensions.code:` **`INTERNAL_SERVER_ERROR`** |

`https://countries.trevorblades.com/graphql` also responded `200` — a second fallback if needed.

> **Resolved in Phase 5 — and the message is not what was assumed.** The full body is:
>
> ```json
> {"errors":[{"message":"The request did not contain a valid GraphQL request.  Batch queries
> and APQ request are not currently supported for this API. Please ensure that your request
> contains a valid query and try again.","extensions":{"stellate":{"code":"INVALID_QUERY"}}}]}
> ```
>
> That is **not a GraphQL syntax error**. The document is rejected by the **Stellate CDN**
> sitting in front of the API, before it ever reaches the GraphQL server — hence the nested
> `extensions.stellate.code` instead of a standard `extensions.code`. The wording belongs to
> an edge provider that can change it at will, so the test asserts status `400`, absence of
> `data`, and a non-blank `errors[0].message`, and does **not** pin the text. The brief's
> requirement ("assert `errors[].message` and absence of `data`") is met exactly.
>
> Second correction: a **missing required variable** carries
> `extensions.code: INTERNAL_SERVER_ERROR`. That is the same class of failure as the
> unknown-field case, which correctly reports `GRAPHQL_VALIDATION_FAILED` — so the API
> labels one of two identical validation failures a server error. Asserted as measured and
> flagged as a defect, the same way the Restful Booker 500 is.

### 11.3 DemoQA

`GET /automation-practice-form` → `200`, **436 bytes**. A client-rendered SPA shell: nothing is
in the initial HTML. Confirms that Playwright auto-waiting is load-bearing and that any
HTML-parsing shortcut would fail.