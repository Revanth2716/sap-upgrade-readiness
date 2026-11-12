# Architecture

Why the SAP Upgrade & Support Package Readiness analyzer is built the way it
is.

---

## 1. The shape of the problem

An upgrade project is decided by facts that are scattered across three
different places, and none of them is the patch level:

| Question | Where the answer lives | How it fails |
|---|---|---|
| Can this system be upgraded at all? | The SUM component list | An obsolete component is invisible in the patch level — the system says "level 3" and nothing more |
| Did the last patch work? | The SPAM patch log | Nobody records the failure, so the system is quietly in an unknown state |
| Are the components consistent with each other? | Nowhere | A component patched ahead of the one it requires, which SUM simply refuses |
| How far behind is the system? | The SPAM transaction | "Level 3" says nothing about where the landscape is |
| What does the package break? | The SAP changelog | Everybody skips it, and the incompatible changes are the ones that matter |

The value of this application is that it puts those five answers **on one
screen, for one system, and turns them into an ordered list of things to do**.

---

## 2. The layers

```
web/          Controllers, session auth, role gating, REST API
service/      The rules: analysis, changelog, audit
repo/         JdbcTemplate over portable ANSI SQL
domain/       Plain Java objects, no framework annotations
support/      Server-side SVG rendering
integration/  The boundary to a live SAP system
seed/         The demo landscape and the demo accounts
```

The dependency rule is one-directional and never bent: `web → service → repo
→ domain`.

### Why `JdbcTemplate` and not JPA

1. **Portability.** The SQL has to run unchanged on H2 (the demo) and on SAP
   HANA (production). Hand-written ANSI SQL is the only way to guarantee it.
2. **No `LIMIT`.** `LIMIT` is not valid on SAP HANA. Every "top N" query here
   fetches the rows and cuts the list **in Java**
   (`PlatformRepository.findRecentAudit`).
3. **The schema is seven tables.** An ORM would add more configuration than the
   schema itself.

---

## 3. The domain model

```
UpgradeSystem         one system: role, patch level, last patch, who by
   │
   ├── ComponentStatus     one component in one system: level and state
   │                          PATCHED / NEW / OBSOLETE / RELEASED
   ├── PatchError          one failed patch attempt, from the SPAM log
   │
   └── UpgradeAssessment   the assessment: score, severity, findings, blockers
            │
            └── UpgradeFinding  severity + category + message + action

SoftwareComponent    the catalogue: what a component is, and what replaced it
ComponentDependency  which components have to move together
SupportPackage       a support package and the patch level it carries
ChangeEntry          one changelog entry, categorised
```

### The states, and why they are not all the same

| State | Meaning | Consequence |
|---|---|---|
| `PATCHED` | The component is on the level of the system | None |
| `NEW` | The component was added after the last patch | None — it has simply never been patched |
| `OBSOLETE` | The product was replaced | **Blocks the upgrade.** It has to be migrated |
| `RELEASED` | SAP no longer delivers the component | **Blocks the upgrade.** There is no patch at all |

`OBSOLETE` and `RELEASED` are different words for the same consequence, and
both are kept because the *message* differs: one says "migrate this", the
other says "there is nothing left to install".

---

## 4. The rules

`UpgradeAnalysisService.assess()` runs seven checks per system.

| # | Condition | Finding | Severity | Penalty |
|---|---|---|---|---|
| 1 | A component is `OBSOLETE` or `RELEASED` | `OBSOLETE_COMPONENT` | CRITICAL | 30 |
| 2 | The patch log holds a failure | `PATCH_FAILED` | CRITICAL | 25 |
| 3 | A component is at a higher level than the one it requires | `COMPONENT_DEPENDENCY_BROKEN` | CRITICAL | 20 |
| 4 | The system is more than the policy limit behind the landscape | `VERSION_DRIFT` | HIGH | 20 |
| 5 | The system has never been patched | `NO_PATCH_HISTORY` | HIGH | 20 |
| 6 | A component is more than the policy limit behind **its own system** | `COMPONENT_BEHIND` | MEDIUM | 15 |
| 7 | The system has not been patched inside the window | `STALE_PATCH_WINDOW` | MEDIUM | 10 |

### The three rules that are easy to get wrong

**One blocker, one finding.** A system with two obsolete components produces
**one** `OBSOLETE_COMPONENT` finding naming both, not two findings saying
the same thing twice. One problem, one action.

**A blocked component is left out of the dependency check.** A component that
is being replaced is not going to move with the rest of the system anyway,
so comparing its patch level with its dependency's produces a finding about
a component that is about to disappear.

**System drift and component consistency are different rules.** A system three
levels behind the landscape is a **drift** problem. A component two levels
behind *its own system* is a **consistency** problem — and it is the one that
holds the upgrade up, because SUM moves the components of a system together.
Conflating them would hide the second behind the first.

---

## 5. The score, and why the severity is not the score

```
score    = max(0, 100 - sum of the penalties)
severity = the worst severity among the findings
```

QA loses 30 points for an obsolete component, 15 for a component left behind
and 10 for a stale window: score **45**. On a score band of "35–60 = high",
that is *high*. It is **critical**, because a component that cannot be
patched stops the upgrade outright.

If the severity came from the score, the blocker would be filed next to a
stale patch window, and both would be equally loud — which is exactly how an
obsolete component stays in a landscape for two years.

`UpgradeAnalysisServiceTest.severityIsTheWorstFindingNotTheScoreBand` pins
this down, and the test name says why it exists.

---

## 6. The component state matrix

The one picture that makes the landscape legible: one row per component, one
column per system, and the cell is the patch level, coloured by the state.

```
              PRD    QA    DEV
SAP_ABA        3      5      7     green   patched
SAP_BASIS      3      5      7     green   patched
SAP_HANA       4      3      7     green   PRD is ahead of SAP_BASIS
SAP_JAVA       3      5      7     green   patched
SAP_KERNEL     3      5      7     green   patched
SAP_MDM        2      3      7     red     released / obsolete / migrated
SAP_MOB        -      -      7     blue    new, never patched
SAP_ST-PI      3      5      7     red     obsolete in PRD
SAP_WEBAS      3      5      7     green   patched
```

Three things are visible at a glance that no list of patch levels shows:
`SAP_MDM` is red in two systems and green in the third, `SAP_HANA` is ahead
of `SAP_BASIS` in PRD, and `SAP_MOB` only exists in DEV because that is
where the migration has happened.

It is server-rendered SVG, like every other chart in this portfolio.

---

## 7. The changelog

`ChangelogService` serves the support package changelog, and the important
part is the **incompatible changes** pulled out on their own.

A support package does not only bring features. It brings incompatible
changes — and those are the entries that break custom code in a system that
was tested three months ago. A changelog viewer that shows them mixed in
with the fixes is a changelog nobody reads.

The categories are `FEATURE`, `FIX`, `SECURITY` and `INCOMPATIBLE`.

---

## 8. The gateway boundary

```java
public interface UpgradeGateway {
    String description();
    List<UpgradeSystem> fetchSystems();
    List<ComponentStatus> fetchComponentStates(String systemId);
}
```

| Implementation | Where | When |
|---|---|---|
| `LocalUpgradeGateway` | `src/main/java` | The default — reads the local database |
| `RfcUpgradeGateway` | `src/extra/java` | `upgrade.gateway: jco` — calls `Z_SPAM_STATUS` over RFC |

The JCo adapter lives in `src/extra/java` and is added to the build by the
`with-jco` profile, because the SAP Java Connector is published on SAP's own
Maven repository and not on Maven Central. The default build therefore
compiles and tests anywhere.

The RFC adapter's `fetchComponentStates` deliberately returns **nothing**:
the component state is not in the transport tables at all, it is in the SUM
component list on the application server. Returning nothing is honest;
returning a guess would not be.

---

## 9. The start-up gate

Spring Boot opens the web server **before** the `CommandLineRunner` beans have
run, so for the first few hundred milliseconds the application serves a
half-built landscape — a dashboard with two systems instead of three, and a
score computed over whatever happened to exist.

`StartupGate` is a `CommandLineRunner` with the highest order, so it is
always the last one, and the interceptor answers **503** with *"the
application is still starting up"* until it runs. The same gate is in every
project in this portfolio, because the same bug was found in the same way
twice.

---

## 10. Security

| Concern | Decision |
|---|---|
| Password storage | BCrypt, hashed at runtime — no reusable hash is committed |
| Sessions | Server-side `HttpSession`, 30-minute timeout, `HttpOnly` and `SameSite=Lax` |
| Authorisation | Three roles, one interceptor, one `@RequireRole` |
| The API | Authenticated like the screens; only `/api/v1/health` is open |
| Write operations | Recording a patch failure is `UPGRADE_ADMIN` only, and is audited |

Spring Security is used for the BCrypt encoder only — the filter chain is
deliberately not used, so the application stays deployable as a plain WAR.

---

## 11. Testing strategy

| Layer | What is tested | How |
|---|---|---|
| Rules | Each rule on its own | `UpgradeAnalysisServiceTest` — a mocked repository per scenario |
| Data | The story the seed has to tell | `UpgradeLandscapeTest` — the whole Spring context |
| Contract | The API shape and the authentication | `ApiControllerTest` — MockMvc |
| Rendering | Every screen renders | `PageRenderTest` — MockMvc against every URL |
| Start-up | The application opens after the seed | `StartupGateTest` |

`PageRenderTest` is not optional. A template can compile, the service layer
can be fully green, and one screen can still return HTTP 500 because a
single expression in the markup does not resolve.
