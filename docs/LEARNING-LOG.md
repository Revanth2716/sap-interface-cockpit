# Learning log

A running record of what was learned while building this project. It is
also the evidence behind the resume bullets — a learning log is what
separates "I took a course" from "I built something".

Format: one entry per working session, newest first.

---

## 2026-11-06 — Building the SAP Interface Cockpit

### What I set out to do
Build a complete, demonstrable SAP BASIS portfolio project on the SAP Java
stack in one session, at no cost, without a live SAP system — and without
pretending to have had one.

### What I learned

**1. Triage is the product.**
The first version of the error analysis was a filtered message log: every
failed row, with its text. Technically correct, operationally useless —
the reader still had to do the grouping. Grouping by interface and error
code, counting the occurrences and the distinct hosts, and attaching the
cause and the remediation turns three hundred rows into seven lines. That
transformation is the difference between a log and a cockpit.

**2. An error catalogue is a runbook.**
"Authorisation missing on the target system for service user FI_INTF" tells
you what happened. *"Grant the missing authorisation in SU01 (`S_RFC` /
`S_BAPI`) and check the RFC destination in SM59"* tells you what to do. The
second one is what an integration team actually needs, and it is the part
that is usually missing from every monitoring tool.

**3. Data volume and incident shape are different things.**
The first traffic generator picked the interface and the outcome from the
same random number, which made forty-five percent of *all* traffic a bank
interface failure. The chart looked like a bug rather than an incident,
because a broken interface does not take over the whole landscape. Rewriting
it so the interface is chosen first, by volume, and the outcome follows that
interface's own profile produced the thing that makes the demo work: a quiet
interface that started failing at 02:00 yesterday and has failed every call
since — 18 of 18, 0% success, `INTERFACE_DOWN`.

**4. Simulating honestly is harder than not simulating.**
The replay queue has to call a customer system to be real, and it cannot.
The first instinct was to mark every replay as successful. The honest
version distinguishes a transient cause (a timeout, a lock) — which succeeds
when retried, because the condition has cleared — from a configuration
cause, which fails again. That distinction is the whole point of the queue,
and it would have been lost by pretending.

**5. The tests caught a missing file, not a wrong file.**
The `/replay` screen was returning HTTP 500 because the template had never
been written — and no service-level test could have found it, because every
service was correct. `PageRenderTest` renders each page the way a browser
would. That test now exists in all three of the Java projects in this
portfolio, and it is the cheapest guard against a release-blocking defect
there is.

**6. Two of my own writes silently did not land.**
A repository class and an application class were missing from the tree, and
the build failed with a message that pointed at the service layer rather
than at the gap. The lesson is dull but real: verify the file inventory
before you trust a build error, and read the *first* error rather than the
last one.

---

## Notes on keeping this log

Update it whenever something is learned that is not obvious from the code.
An interview question about "why is it done this way" is answered by this
file, not by the source.
