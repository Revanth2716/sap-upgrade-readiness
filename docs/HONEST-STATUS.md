# Honest status

What in this repository is **verified working**, what is **written but not yet
executed**, and what is **modelled rather than measured**. Read this before
describing the project in an interview.

---

## 1. Verified working today

| Item | Evidence |
|---|---|
| The application builds | `mvn clean package` → `BUILD SUCCESS`, executable JAR produced |
| The whole test suite passes | `mvn test` → **56 tests, 0 failures, 0 errors** |
| The application starts and serves | `java -jar target/sap-upgrade-readiness.jar` → HTTP 200 on `/api/v1/health` |
| Every screen renders | Seven screens captured from the running application into `docs/screenshots/` |
| The obsolete-component rule works | `UpgradeAnalysisServiceTest` — an obsolete component and a released component both block the upgrade, and the recommendation names the replacement |
| The dependency rule works | The same test — a component ahead of the one it requires is critical, the same level is fine, and a component that is being replaced is left out of the check |
| The drift rules work | Drift inside the policy is not a finding, drift outside it is; a component behind its own system is a separate rule |
| The severity rule holds | `severityIsTheWorstFindingNotTheScoreBand` — a failed patch stays critical at a score of 75 |
| The landscape tells the story | `UpgradeLandscapeTest` — PRD 5 critical, QA 45 critical, DEV 100 clean, three blockers, average 50 |
| The matrix shows the truth | The same test — `SAP_MDM` is a blocker in PRD and QA and migrated in DEV; `SAP_HANA` is ahead of `SAP_BASIS` in PRD |
| The changelog works | 16 entries across 7 packages, 4 of them incompatible changes, and the incompatible ones can be pulled out on their own |
| The API works | `ApiControllerTest` — health, readiness, one system, findings, the changelog, one package |
| The API is authenticated | Every endpoint except `/api/v1/health` returns 401 for an anonymous caller |
| Role gating works | `PageRenderTest` — a viewer is blocked from the changelog and the report, an analyst cannot record a patch failure |
| The start-up gate works | `StartupGateTest` — the application is closed until the seeders have run |
| The log is clean | No exception or error in the application log during a full pass |

**Reproduce all of it:**

```bash
mvn clean package
java -jar target/sap-upgrade-readiness.jar
```

---

## 2. Written but not yet executed on a live SAP system

| Item | Status | Why |
|---|---|---|
| `abap/Z_SPAM_STATUS.abap` | **Not yet compiled on NetWeaver** | No SAP system was available in the build environment. The code follows NetWeaver 7.5 / ABAP 7.40+ syntax and the Remote-Enabled flag is documented in the header, but it has not been through the ABAP syntax check. |
| `abap/Z_UPG_TYPES.abap` | **Not yet created in SE11** | These are the DDIC structures the function module imports and exports. |
| `src/extra/java/.../RfcUpgradeGateway.java` | **Not yet compiled** | The SAP Java Connector (`com.sap.jco:sapjco3`) is published on SAP's own Maven repository, not Maven Central, and that host was unreachable from the build environment. The `with-jco` profile declares the exact coordinates needed. |
| The HANA JDBC path | **Configured, not exercised** | The `hana` profile and the datasource configuration are in place; no HANA instance was available. |
| The WAR deployment on NetWeaver 7.5 | **Designed, not executed** | `DEPLOYMENT.md` describes it; the project is built as a JAR, and the `pom.xml` change to `war` is documented but not committed. |
| The `SPHIST` / `SPDIR` / `SPBUF` field names | **Unverified against a live release** | The tables are stable, but the status model and some columns differ between the classic and the new release, and some columns exist only in newer releases. |

**What this means in an interview:** say *"the SAP-side artifacts are written
against NetWeaver 7.5 semantics and are ready to compile, but I could not run
them because I did not have a system available."*

**The plan to close the gap** is in [`DEPLOYMENT.md`](DEPLOYMENT.md): create
the DDIC structures in `SE11`, paste the function module into `SE37` with
**Remote-Enabled** ticked, create the RFC destination in `SM59`, and build the
JCo adapter against SAP's Maven repository.

---

## 3. The one gap that is not a build problem

**The component state is not in the transport tables at all.**

SAP's Software Update Manager keeps its component list — which components
exist in a system, and which of them are obsolete or have been replaced — in
its own files on the application server's file system. The transport tables
hold the support package history (`SPHIST`), the patch level per directory
(`SPDIR`) and the patch buffer (`SPBUF`). They do **not** hold the component
state, and they do not hold the patch log either.

So in this project:

| Data | Where it comes from |
|---|---|
| The applied support packages | The database — read by the RFC function module |
| The component state (obsolete, replaced) | **Maintained on the portal side** |
| The patch failures | **Maintained on the portal side** — which is why the portal can record one |

The RFC adapter's `fetchComponentStates` therefore returns **nothing** rather
than guessing. A production implementation reads the SUM component list from
the application server; that is on the roadmap, and the gap is documented in
`DEPLOYMENT.md` rather than papered over.

---

## 4. Modelled rather than measured

| Area | What is modelled | What production needs |
|---|---|---|
| The patch levels | Three systems on levels 3, 5 and 7 | The real `SPDIR` of every system and every client |
| The component states | A catalogue of nine components with their states | The **SUM component list** from the application server |
| The patch log | One recorded failure | The real SPAM patch log |
| The dependencies | Six edges between components | The real dependency matrix, which SUM maintains |
| The support packages | Seven packages with release dates | The real SP level catalogue |
| The changelog | Sixteen entries, four of them incompatible | The **SAP-published changelog** for each support package — a document, not a database row |
| The landscape | Three systems | Every system in the landscape, and every client |

---

## 5. Known limitations — the ones an interviewer would find

1. **The component state is a copy, not the source.** See section 3.
2. **The changelog is hand-maintained.** The real changelog is a SAP-published
   PDF per support package. The portal models it so the *shape* of the tool —
   the categorisation, the filtering of the incompatible changes — can be
   demonstrated, but the content is synthetic.
3. **The dependency matrix is hand-maintained.** In reality SUM holds it and
   derives the upgrade plan from it.
4. **No downtime estimate.** The real maintenance window has to be sized from
   the component runtimes and the abort/commit phases, and this tool does not
   do that.
5. **No route.** The landscape is a list of systems, not a graph of the
   transport route between them, so the tool cannot show the path a change
   has to travel.
6. **The schema is rebuilt on every start.** `schema.sql` drops and recreates
   every table and the seeders reload the landscape. That is predictable for a
   demo and wrong for production — migrations replace it, and
   `spring.sql.init.mode` becomes `never`.
7. **No history.** The tool answers "is this system ready now", not "has it
   been getting worse".

---

## 6. What the project is not

* It is **not** connected to a live SAP system out of the box — it runs
  against the seeded dataset, and the RFC path needs a system that was not
  available.
* It is **not** an upgrade tool. It never patches, never moves a component
  and never books a window. It reports whether an upgrade can happen and what
  is stopping it.
* It is **not** a substitute for EarlyWatch Alert. EWA checks a system
  against SAP's own rules; this tool checks the *landscape* — the drift
  between the systems and the consistency inside them, which is the part
  EWA cannot see because EWA only ever looks at one system.
* It is **not** a changelog viewer for real support packages. The content is
  synthetic; the categorisation and the filtering are the point.

---

## 7. Why the gaps were left open rather than faked

A portfolio project that pretends to have run against a production system is
easy to expose in an interview: the interviewer asks which system, which
client, which support package, and the story collapses. The gaps above are
specific, explained, and each one comes with the concrete step that closes
it.

The same rule applies to the data. The seeded landscape is explicitly
synthetic, and the way it is seeded — one deliberate failure mode per system
— makes it a **test fixture**, not a pretence of a customer.
