# Resume content

Copy-paste blocks. Pick the length that fits the CV and delete the rest.

> **Do not paste the project title as a client engagement.** This is
> self-initiated portfolio work. Say *"a self-initiated project I built to
> practise..."* — an interviewer who asks "who was the client?" and gets a
> vague answer will stop asking the good questions.

---

## 1. The one-line version

> **SAP Upgrade & Support Package Readiness** — upgrade readiness for SAP
> landscapes: obsolete component blockers, patch level drift, dependency
> checks, failed patch detection and a per-system readiness score.

---

## 2. The three-bullet version

> **SAP Upgrade & Support Package Readiness** — *Java 8 · Spring Boot 2.7 · JdbcTemplate · SAP HANA · Thymeleaf · ABAP RFC · SAP Java Connector*
>
> - Built an **upgrade readiness analyzer** for SAP landscapes that detects **obsolete components** whose replaced product blocks an upgrade outright, verifies **component dependency consistency** against the components they require, measures **patch level drift** across the landscape, surfaces **failed patches** from the SPAM log, and flags components left behind inside their own system.
> - Designed the rules so that **system-level drift and component-level consistency are separate findings**, because the Software Update Manager moves the components of a system together — one lagging component holds the whole upgrade up even when the system itself looks current — and so that the **severity comes from the worst finding** rather than the score band, so an obsolete component is never filed next to a stale patch window.
> - Shipped the SAP-side artifacts (**RFC-enabled ABAP function module** reading `SPHIST`/`SPDIR`/`SPBUF`, **SAP Java Connector client** behind an optional Maven profile) on **portable ANSI SQL** that runs unchanged on local H2 and SAP HANA, with **56 automated tests**.

---

## 3. The two-bullet version

> **SAP Upgrade & Support Package Readiness** — upgrade readiness for SAP landscapes
> *Java 8 · Spring Boot 2.7 · JdbcTemplate · SAP HANA · ABAP RFC*
>
> - Built an **upgrade readiness analyzer** that finds the **obsolete components** that block an upgrade, the **broken component dependencies**, the **failed patches** and the **patch level drift** across the landscape, and scores every system with an actionable finding behind it.
> - Turned the analysis into a **component state matrix** — every component against every system — plus a **changelog viewer that pulls out the incompatible changes** a test team needs, with an **RFC-enabled ABAP function module** and **SAP Java Connector client** behind an optional Maven profile, on **portable ANSI SQL** for H2 and SAP HANA, with **56 tests**.

---

## 4. The one-bullet version

> Built an **upgrade readiness analyzer for SAP landscapes** (Java 8, Spring Boot, JdbcTemplate, SAP HANA, ABAP RFC) that detects obsolete-component upgrade blockers, broken component dependencies, failed patches and patch level drift, produces a per-system readiness score with an actionable finding behind it, and renders the whole landscape as a component state matrix — with an RFC-enabled ABAP function module, a SAP Java Connector client and 56 automated tests.

---

## 5. Where it fits in the CV

This is the **eighth** BASIS portfolio project. The set covers eight
distinct workstreams with no overlap, which is the point — it shows range,
not eight versions of the same CRUD app.

| # | Project | Workstream |
|---|---|---|
| 1 | `sap-basis-ops-console` | Monitoring and governance analytics (Python CLI) |
| 2 | `sap-basis-operations-portal` | Operations: leave self-service and governance |
| 3 | `sap-custom-code-analyzer` | Repository analysis: where-used and custom code quality |
| 4 | `sap-interface-cockpit` | Integration: RFC, IDoc and BAPI operations |
| 5 | `sap-performance-analyzer` | Performance: ST12 and ST03 trace analysis |
| 6 | `sap-backup-readiness` | Continuity and DR: backup, restore and capacity |
| 7 | `sap-transport-analyzer` | Change and transport: object conflicts, release trains |
| 8 | **`sap-upgrade-readiness`** | **Upgrades: obsolete blockers, drift, compatibility** |

Recommended CV section:

```markdown
### SAP Basis Analyst — Portfolio Projects

**SAP Upgrade & Support Package Readiness** · Java 8 · Spring Boot · JdbcTemplate · SAP HANA · ABAP RFC
Built an upgrade readiness analyzer for SAP landscapes that detects obsolete-component
upgrade blockers, broken component dependencies, failed patches and patch level drift, and
scores every system with an actionable finding behind it. 56 automated tests.
→ github.com/Revanth2716/sap-upgrade-readiness

**SAP Transport & Change Management Analyzer** · ...
**SAP Backup & Restore Readiness** · ...
```

---

## 6. Keywords

`SAP BASIS` · `SAP NetWeaver 7.5` · `Upgrades` · `Upgrade planning` ·
`Support Package Manager` · `SPAM` · `SAU` · `SUM` (Software Update Manager) ·
`Software components` · `Patch levels` · `Patch management` ·
`Obsolete components` · `Component migration` · `Incompatible changes` ·
`Regression testing` · `EarlyWatch Alert` · `Change and Transport Management` ·
`ABAP` · `RFC` · `SAP Java Connector (JCo)` · `SM59` · `SE37` · `SE11` ·
`SPHIST` · `SPDIR` · `SPBUF` · `Java 8` · `Spring Boot` · `JdbcTemplate` ·
`Thymeleaf` · `SAP HANA` · `Git`

---

## 7. How to talk about it in an interview

### The thirty-second version

> "I built an upgrade readiness analyzer for SAP landscapes. The patch level
> tells you almost nothing about whether a system can be upgraded — a system
> can say 'level 3' and have a component whose product was replaced two years
> ago, which no patch level can fix. The tool finds those blockers, finds the
> components left behind inside their own system, finds the patch that failed
> last night, and shows the whole landscape as a matrix of components against
> systems."

### The design decision worth defending

**Why are system drift and component consistency separate findings?**

> "Because they are different problems with different fixes. A system three
> patch levels behind the landscape is a *scheduling* problem — it needs
> time in a maintenance window. A component two levels behind its own system
> is a *consistency* problem — the Software Update Manager moves the
> components of a system together, so that one component holds the whole
> upgrade up even though the system itself looks current. If you report them
> as one number, you hide the second one behind the first, and the second one
> is the one that stops the upgrade."

### The thing to be honest about

> "The component state is not in the transport tables at all — SUM keeps it in
> its own component list on the application server's file system. So the
> portal maintains it, and the documentation says exactly which fields come
> from where. The Java side is fully tested — 56 tests, and every screenshot
> in the repository was captured from the running application."

---

## 8. What to say if you are asked "what was the hardest part?"

> "Deciding what a 'patch level' actually means. The first version compared
> the system level with the landscape level and called the difference drift.
> But that hides the case that matters: a system on the latest level with one
> component still on an old one. SUM moves the components together, so that
> one component stops the upgrade, and it is invisible if you only look at
> the system's level. Separating the two rules — drift against the
> landscape, consistency against the system's own level — is what made the
> tool useful."

---

## 9. Things not to claim

| Do not say | Say instead |
|---|---|
| "I built this for a customer" | "I built this as a self-initiated portfolio project" |
| "It is deployed in a production landscape" | "It runs locally against a seeded database; the live SAP path is written but not executed" |
| "It performs the upgrade" | "It analyses whether a system can be upgraded and produces the findings and the report" |
| "It reads the SUM component list" | "The component state is maintained in the portal, because SUM keeps that on the application server — that integration is on the roadmap" |
| "It sizes the downtime" | "The downtime estimate is on the roadmap; the tool reports what blocks the upgrade" |

---

## 10. Interview questions this project invites

Have an answer ready for these — they are the good ones:

* "What is a support package, and how does a system end up on a patch level?"
* "What is an obsolete component and what do you do about it?"
* "Why does SUM refuse to run if one component is behind?"
* "How do you find out that a patch failed?"
* "What is the difference between the classic and the new status model?"
* "What do you read before opening a maintenance window?"
* "How would you size the downtime for an upgrade?"

The full answers are in [`INTERVIEW-QA.md`](INTERVIEW-QA.md).
