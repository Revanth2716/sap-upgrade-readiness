# Learning log

The decisions behind the code, and the mistakes behind some of them. Written
for the person reading the repository in six months — including me.

---

## 1. The order the work happened in

1. The schema, the domain classes and the repository.
2. The rules, written as a plain method with no framework in sight.
3. The seed — and only then, the screens.

The rules are the product; the screens are the report of the rules. Writing
the screens first produces a dashboard that shows what is easy to show.

---

## 2. The decision that shaped the whole project

**The patch level is a number, and the number hides everything that matters.**

The whole tool exists because of that. A system can say "level 3" and have a
component whose product was replaced two years ago — and no patch level can
fix it, because there is no level 4 of a product that no longer exists.

That reframing is what produced the rules. If the tool were about patch
levels, the rules would be "is the level high enough". Because the tool is
about *whether the upgrade can happen*, the rules are about blockers,
dependencies, consistency and drift.

> The general lesson: a metric that is easy to read is often not the one that
> matters. Ask what the metric is *for* before you build a screen around it.

---

## 3. Two rules that look like one rule and are not

**System drift** — the system is behind the landscape.
**Component consistency** — a component is behind *its own system*.

The first version had only the first. It produced a perfectly reasonable
report that said *"PRD is four levels behind"* — and completely missed the
case that actually stops the upgrade: a system on the latest level with one
component still on an old one.

The Software Update Manager moves the components of a system **together**. So
that one lagging component holds the whole upgrade up, even though the
system's own level looks fine. Two rules, two fixes, two different owners.

> The general lesson: when a number is an aggregate, the interesting cases are
> usually *inside* it, and they need their own rule.

---

## 4. The component matrix came from a screenshot

The first version of the dashboard had a table of systems and a table of
components, and the answer to "which component is in which state in which
system" was to read both tables and hold it in your head.

That did not work. With three systems and nine components it was already
too much, and it would be hopeless with ten systems.

The matrix — one row per component, one column per system, the cell is the
patch level coloured by the state — came out of trying to answer that
question for myself while looking at the screenshot of the first version.

> The general lesson: the picture you need is often the picture you have to
> build. If the answer takes a spreadsheet to hold, it wants a screen.

---

## 5. One blocker, one finding

The first version emitted one finding per obsolete component, so a system with
two of them produced two findings saying the same thing in different words.

The fix was to emit **one** finding naming all the blockers, with a
recommendation that names the replacement for each. One problem, one action.

This is the same lesson that came up in the sibling transport project — a
failed import producing both "the import failed" and "there is no evidence of
an import". The rule is simple and easy to forget: **a rule whose condition
is a superset of another rule's condition will double-report.**

---

## 6. A blocking component is left out of the dependency check

The dependency rule compares a component's patch level with the level of the
component it requires. It reported a conflict for a component that was
**obsolete** — a component that is about to be migrated away and is not going
to move with the rest of the system anyway.

The fix was one condition: skip the comparison when either side is being
replaced. It is not a special case bolted on; it follows from what the check
is for.

> The general lesson: when a rule produces a finding about something that is
> about to disappear, the rule is asking the wrong question.

---

## 7. The start-up gate — carried over, not rediscovered

Spring Boot opens the web server before the `CommandLineRunner` beans have
run, so the application serves a half-built landscape for a few hundred
milliseconds.

I found this the hard way in the *previous* project: my own smoke test read
the health endpoint the instant the port opened and got an average score
computed over three systems instead of five. The wrong number, from a
correct calculation, over a half-built landscape.

The fix went into that project, and it went into this one from the start —
the `StartupGate` with the highest runner order, and the interceptor
answering 503 until it runs.

> The general lesson: the bug you fix in one place is a bug you should not
> reproduce in the next one. Port the fix, not just the lesson.

---

## 8. The bug the screenshot caught

**PRD was missing `SAP_KERNEL`** — the very component whose patch had failed
in the seeded patch log. The demo data said "the patch of SAP_KERNEL failed"
in a system that did not have SAP_KERNEL.

No test failed. The rules were all correct. The data was not.

It was caught by reading the screenshot of the system detail screen, where
the component table and the patch log sat next to each other and disagreed.

> The general lesson: the demo data is part of the product. If it is not
> internally consistent, the story it tells is a lie — and the screenshot is
> where you find out.

---

## 9. Decisions that were easy

* **Plain Java domain classes.** No Lombok, no records (Java 8), no
  annotations.
* **JdbcTemplate.** Decided in the first ten minutes, for portability.
* **Server-side SVG.** Decided when the first chart was drawn and the CDN
  dependency was written down. It was never tempting.
* **The severity is the worst finding.** Carried over from the sibling
  projects, and it is right every time.
* **A separate changelog screen.** The incompatible changes have to be
  pullable on their own, or nobody reads them.

---

## 10. Things I would do differently

* **Write the landscape test first.** I wrote the rules, then the seed, then
  the landscape test. The seed was supposed to demonstrate the rules, and it
  took three rounds of adjustment before it demonstrated all of them.
  Writing `UpgradeLandscapeTest` first would have made the intended story
  explicit before the data was written.
* **Read the SUM component list.** The component state is the heart of this
  project and it is currently a copy maintained by hand. Reading it from
  the application server would turn the demo into a tool.
* **Model the upgrade route.** The systems are a list; a real landscape
  transports along a route, and the checklist should show the whole path a
  change has to travel, not just the last hop.
