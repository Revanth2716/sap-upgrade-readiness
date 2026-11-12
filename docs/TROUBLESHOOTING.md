# Troubleshooting

The problems that actually happen, and what they mean.

---

## 1. The application

### `Port 8086 was already in use`

```bash
# Windows
netstat -ano | findstr :8086
tasklist /FI "PID eq 12345"

# Linux
lsof -i :8086
```

Or start on a different port:

```bash
java -jar target/sap-upgrade-readiness.jar --server.port=9090
```

> If the application is still running from the IDE, it holds the **JAR file
> open**, and `mvn clean` then fails with *"the process cannot access the
> file because it is being used by another process"*. Stop it before cleaning.

### `503 Service Unavailable` — "the application is still starting up"

This is the **start-up gate** doing its job. Spring Boot opens the web server
before the `CommandLineRunner` beans have run, and the application refuses to
serve a half-built landscape. It opens as soon as the seeders have finished —
a fraction of a second.

If it persists, the seeder failed. Check the log for a constraint violation or
a missing table.

### `Table "SP_SYSTEM" not found`

The schema was not created. Check the log for `Copying 8 resources`, and that
`spring.sql.init.mode` is `always`.

### The application starts but every page is empty

The seeding did not run, or the data is already there:

```yaml
upgrade:
  seed-demo-data: true
```

The seeder is **idempotent** — it does nothing when `SP_SYSTEM` already has
rows. To get the demo landscape back, delete the database file:

```bash
rm -rf data/          # Linux / macOS
# Windows: delete the data folder
```

---

## 2. A finding looks wrong

### "SAP_MDM is obsolete" — but the patch level looks fine

That is the point. The patch level is a number, and it hides the fact that
the product behind it was replaced. `SAP_MDM` is at level 2 in PRD and there
is **no level 3 of SAP_MDM** — it was replaced by `SAP_MOB`, and the
component has to be **migrated**, not patched.

### "SAP_HANA is ahead of SAP_BASIS" — that is intentional

It is, sometimes. A database is often upgraded separately from the rest of
the system, and the two then sit at different levels. The finding is real
either way: SUM requires the components of a system to be consistent, and
it will refuse the combination.

### "A component behind its own system" but the system is up to date

Those are two different rules on purpose. A system can be on the latest
landscape level and still have one component left behind inside it — and
that component is what holds the upgrade up, because SUM moves the components
of a system together.

### "The system is behind the landscape" — but the policy allows it

Check `policy.max-system-drift-levels`. QA in the demo landscape is two
levels behind and the policy allows two, so it is not a finding. That number
is the agreement with the SAP account team.

### "No patch history" for a brand new system

That is exactly what the finding is for. A system with no patch history is
either brand new or nobody has been maintaining it, and both need a
decision. The finding is *high*, not *critical*, for that reason.

### The score changed without anything changing

The score depends on the **current time**: the patch window and the drift
are both measured against today. A system that was patched 89 days ago
crosses the window overnight. That is the policy working as agreed.

### Two systems have the same score but different statuses

The severity is the **worst finding**, not the score band. QA scores 45
("high" on a band) and is **critical**, because it has an obsolete component.

---

## 3. RFC and the live SAP system

### `RFC function module not found` / `not remote enabled`

**The single most common RFC problem.** The function module was created in
`SE37` but **Remote Enabled** was not ticked.

1. `SE37` → open `Z_SPAM_STATUS` → *Attributes* → tick **Remote Enabled**
2. Save and activate

> The error surfaces as an `SM58` entry on the **calling** system, not as a
> dump on the callee.

### The error only appears in `SM58`

`SM58` on the **calling** system shows the RFC errors. The long text names
the reason.

### `jco.properties not found`

The gateway reads `jco.properties` from the **working directory** of the
process. Copy it there and start the application from that directory.

### `Communication link failure` / `RFC destination not found`

The destination name in the code must match the name in `SM59` **exactly**,
including the case. The default is `SAPPRD`:

```java
public RfcUpgradeGateway() {
    this("SAPPRD");
}
```

### The call works but returns no component states

That is expected. The **component state is not in the transport tables at
all** — SUM keeps it in its own component list on the application server's
file system. The RFC function module returns the support package history, and
the component state is maintained on the portal side.

See the table in [`DEPLOYMENT.md`](DEPLOYMENT.md#6-where-the-component-state-comes-from)
for exactly what comes from where.

### The support package history is empty

Check that the field names match the release. `SPHIST` is stable, but the
status model and some columns differ between the classic and the new
release. Read the log — it says how many records were returned.

---

## 4. The build

### `Could not resolve dependencies for project ... com.sap.jco:sapjco3`

The SAP Java Connector is not on Maven Central. Either add SAP's repository to
`~/.m2/settings.xml` or build **without** the profile:

```bash
mvn clean package          # the default build, no SAP repository needed
```

### `maven-compiler-plugin` fails on a Java 8 feature

The project is compiled with `source/target = 1.8` on purpose, so it stays
deployable on the NetWeaver 7.5 Java stack. Do not raise it.

### The build fails with "the process cannot access the file because it is being used"

A running application holds the JAR open. Stop it before `mvn clean`.

---

## 5. Tests

### `PageRenderTest` fails with 500

A template expression does not resolve. The first line of the stack trace
names the template and the line.

| Symptom | Cause |
|---|---|
| `EL1008E: Property or field 'xxx' cannot be found` | The model attribute is not in the controller |
| `EL1007E: Property or field 'xxx' cannot be found on null` | A `null` object — usually a system that does not exist |
| `EL1028E: Expected to find attribute 'xxx'` | A `th:each` over a value that is not in the model |

### The landscape test fails with "expected 5 but was 0"

The seeder did not run, or the schema was rebuilt under it. The seeders run
in order: `DemoUserSeeder` (`@Order(1)`), `UpgradeSeeder` (`@Order(2)`), then
`StartupGate` (`@Order(100)`) which opens the application.

### A test fails only when the whole suite runs

Almost always the H2 database being shared between test classes. The test
profile uses `jdbc:h2:mem:upgradetest;DB_CLOSE_DELAY=-1`, so the database
survives between classes and the schema is only created once.

---

## 6. Where to look first

| Symptom | Look at |
|---|---|
| A finding is wrong | `GET /api/v1/systems/{id}` — it returns the score, the findings and the component states |
| The data is stale | `GET /api/v1/health` — it returns the system count, the blocker count and the average score |
| Something was changed | The **Recent activity** table on the dashboard |
| The application misbehaved | The log — every sign in, recorded patch failure and exported report is written to the audit trail |
