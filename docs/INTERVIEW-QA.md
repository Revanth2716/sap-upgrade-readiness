# Interview questions and answers

The questions this project invites, and the answers that are actually true.

---

## 1. The domain

### "What is a support package, and how does a system end up on a patch level?"

> "A support package bundles the patches for a set of software components. The
> components are the units SAP delivers in: SAP_BASIS, SAP_ABA, SAP_KERNEL,
> SAP_HANA, SAP_WEBAS and so on.
>
> "A system is *on* a patch level when every component in it is on that
> level. And that is exactly the problem — the patch level is a number, and
> the number hides everything that matters. A system can say 'level 3' and
> have a component whose product was replaced two years ago, which no patch
> level can fix."

### "What is an obsolete component and what do you do about it?"

> "It is a component whose product SAP has replaced. You cannot patch it —
> there is no new version of it to install. The only way forward is to
> **migrate** the functionality to the replacement product.
>
> "It is the one thing that stops an upgrade outright, and it is invisible
> in the patch level. That is why the analyzer treats it as the critical
> finding and names the replacement product in the recommendation."

### "Why does SUM refuse to run if one component is behind?"

> "Because the Software Update Manager moves the components of a system
> together. It applies them as a unit and computes the downtime from that
> unit. A component left behind inside its own system is not a small
> inconsistency — it holds the whole upgrade up, and SUM will either refuse
> the combination or refuse to estimate the downtime for it.
>
> "That is the reason the tool has a rule for component-level consistency
> and a separate rule for system-level drift. They are different problems."

### "How do you find out that a patch failed?"

> "From the SPAM patch log. And the thing about a failed patch is that
> nobody notices, because the system still starts and the application still
> runs. The system is simply in a state nobody can reason about — half the
> components moved, half did not.
>
> "So the analyzer treats an unrecorded failure as the risk it is: until
> somebody records it, the next upgrade attempt finds out the hard way."

### "What is the difference between the classic and the new status model?"

> "The classic status model tracks the status of a request or a component as
> a code in a status table. The new model — which came in with the newer
> support package manager — tracks it as a set of statuses per object and
> per user.
>
> "It matters for a tool like this one because the table and field names
> differ between them, and between releases. That is why the ABAP in this
> project is written against one layout and the documentation says so rather
> than pretending it is universal."

### "What do you read before opening a maintenance window?"

> "Three things, in this order. What blocks the upgrade — the obsolete
> components, because they turn a window into a project. What the support
> package breaks — the incompatible changes, because those are the entries
> that break custom code in a system that was tested three months ago. And
> what is left behind — the components that will hold the system up.
>
> "This tool answers the first and the third, and shows the second."

---

## 2. The design

### "Why is the severity not derived from the score?"

> "Because that is how problems hide. QA scores 45 out of 100 — 'high' on a
> score band — and it is **critical**, because it has an obsolete component
> that cannot be patched at all.
>
> "If the severity came from the score, the blocker would be filed next to a
> stale patch window and both would be equally loud. That is exactly how an
> obsolete component stays in a landscape for two years: it is in a list,
> somewhere, among other things that are also slightly wrong."

### "Why are drift and component consistency separate rules?"

> "Because they are different problems with different fixes. A system three
> patch levels behind the landscape needs *time* in a maintenance window. A
> component two levels behind its own system is a *consistency* problem, and
> it holds the upgrade up on its own.
>
> "If you report them as one number you hide the second behind the first —
> and the second one is the one that stops the upgrade."

### "Why do you report one finding for two obsolete components?"

> "One problem, one action. The fix for two obsolete components is the same
> piece of work — migrate the replaced product — so reporting it twice
> makes the report longer without making it clearer. The finding names both
> of them, and the recommendation names the replacement for each."

### "Why is the changelog a separate screen?"

> "Because it is the part of the upgrade that everybody skips and everybody
> needs. A support package does not only bring features — it brings
> incompatible changes, and those are the entries that break custom code in
> a system that was tested three months ago.
>
> "A changelog viewer that shows them mixed in with the fixes is a changelog
> nobody reads, so the incompatible ones are pulled out on their own and
> counted."

### "Why do you draw the charts on the server?"

> "Two reasons. Corporate networks block outbound internet, and a chart
> library from a CDN renders as an empty box in exactly the environment where
> this has to work. And an upgrade board has to be able to save the page and
> still see the evidence.
>
> "The component state matrix is plain SVG — a row per component, a column
> per system, and the cell is the patch level coloured by the state. It is
> the one picture that makes the landscape legible, and it needs no
> JavaScript at all."

---

## 3. The implementation

### "Walk me through the request path for a system detail screen."

> "The interceptor checks the start-up gate, the session and the role. The
> controller asks the analysis service for the assessment of that one
> system. The service loads the whole landscape once into a snapshot — the
> systems, the component states, the dependencies and the patch log —
> because the drift rule needs the highest level anywhere in the landscape
> and the dependency rule needs the level of another component. Loading that
> per system would be an N+1 query per finding.
>
> "Then the seven rules run, the findings are produced, and the view renders
> them."

### "How do you test a scoring rule?"

> "One scenario per test, with the repository mocked so that the test
> contains exactly the situation it is about — an obsolete component and
> nothing else, a dependency ahead of its requirement and nothing else. When
> a test fails it names the rule that broke.
>
> "On top of that there is a test against the whole seeded landscape, because
> the seed has to tell a specific story: PRD blocked three different ways,
> QA blocked once and drifting, DEV clean and migrated."

### "What is the `PageRenderTest` for, and why is it in every project?"

> "A template can compile, the service layer can be fully green, and one
> screen can still return HTTP 500 because a single expression in the markup
> does not resolve. Nothing else in the suite would catch that."

### "What is the start-up gate?"

> "Spring Boot opens the web server before the `CommandLineRunner` beans have
> run, so for the first few hundred milliseconds the application serves a
> half-built landscape. I found it because my own smoke test read the health
> endpoint the instant the port opened and got an average score computed over
> two systems instead of three.
>
> "The gate now holds the application closed until the seeders have finished
> and answers 503 until then. A load balancer that probes the application
> the moment it starts should see 'not ready', not an empty dashboard."

---

## 4. The SAP side

### "How does the live path work?"

> "Two artifacts. The ABAP function module `Z_SPAM_STATUS` reads the support
> package history from `SPHIST`, the patch levels per directory from `SPDIR`
> and the patch buffer from `SPBUF`. The JCo adapter calls it over RFC
> through a destination maintained in `SM59`.
>
> "The important design point is the interface. `UpgradeGateway` is the
> boundary; the local implementation reads the database, the RFC
> implementation calls the function module. Nothing above the interface knows
> that RFC exists."

### "Where does the obsolete-component state come from, then?"

> "Not from the database. The Software Update Manager keeps the component
> list in its own files on the application server, and the state of each
> component — patched, new, obsolete, released — lives there.
>
> "So the portal maintains it, and the documentation says exactly which
> fields come from where. A production implementation reads the SUM component
> list; until that is done, being explicit about the gap is better than
> pretending the database holds something it does not."

### "What goes wrong first with RFC?"

> "The **Remote Enabled** flag. If the function module is created in `SE37`
> without it, the call fails with 'function module not found / not remote
> enabled'. And the error appears as an `SM58` entry on the **calling**
> system, not as a dump on the callee — so the system you are looking at is
> the one that looks fine."

---

## 5. The honest questions

### "Has this run against a real SAP system?"

> "No. The Java side runs and is tested — 56 tests, and every screenshot in
> the repository was captured from the running application. The ABAP, the
> DDIC types and the JCo adapter are written against NetWeaver 7.5 semantics
> and are ready to compile, but there was no NetWeaver system and no access
> to SAP's Maven repository in the build environment, so they have not been
> executed. The repository says so in `HONEST-STATUS.md`, and I would rather
> say it here than have it come out in an interview."

### "Where does the data come from then?"

> "It is a seeded synthetic landscape, and the seed is deliberately a *story*
> rather than noise. Three systems, each with one specific failure mode: a
> production system with two components that cannot be patched, a failed
> patch and a component ahead of the one it depends on; a quality system with
> an obsolete component and a component left behind inside itself; and a
> development system that has already done the migration and is clean."

### "What would you change first with a real customer?"

> "The component state. In this project it is maintained in the portal,
> which is honest but wrong — it belongs to SUM. The first real integration
> is to read the SUM component list, so the obsolete-component verdict comes
> from the source instead of from somebody's typing.
>
> "Second, the downtime estimate per component, so the maintenance window can
> be sized rather than guessed."

### "What would you not build?"

> "I would not build the upgrade itself into this tool. It reports whether a
> system can be upgraded and what is stopping it; the upgrade stays a SUM
> run, with a human in front of it. A tool that decides to start an upgrade
> at 02:00 is a tool people stop trusting."

---

## 6. Questions to ask them back

* "How do you currently find out that a component is obsolete?"
* "What happens when a patch fails in the night — who finds out, and how?"
* "How do you size the maintenance window before you book it?"
* "How do you know the custom code still works after an incompatible change?"
* "Do you upgrade all systems at once, or one at a time?"
* "Who owns the upgrade plan — the BASIS team, the application team, or the
  SAP account team?"
