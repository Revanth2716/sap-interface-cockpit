# Troubleshooting

How to read the cockpit's output, and how to diagnose the cockpit itself.

---

## Part 1 — reading the analysis

### "Is this interface really down?"

Look at the success rate and the alert:

| Signal | Meaning |
|---|---|
| `INTERFACE_DOWN` alert | No successful call in the window at all. Critical. |
| `FAILURE_BURST` | Many failures of one kind - usually one cause repeating |
| `SLA_BREACH` | Some calls get through, so the business is quietly getting wrong data |
| `LATENCY_BREACH` | Everything works, but too slowly |
| `AGEING` | A failure nobody has touched |

The third one is the dangerous one. An interface that is completely dead is
noticed immediately; an interface that succeeds 94% of the time is a
business-data problem that surfaces weeks later in a reconciliation.

### "One interface, forty failures - is that one problem or forty?"

One problem, if the error code is the same. The triage groups by
**interface + error code**, so:

* one code, forty occurrences → **one** incident with one fix;
* four codes, ten occurrences each → **four** incidents, and the one with
  the highest count is probably the cause of the others (a mapping problem
  usually produces a timeout on top of it).

### "The interface is slow. Where do I look?"

1. `ST12` for the SQL trace — where the time actually went.
2. `ST05` for the ABAP trace, if the time is not in the database.
3. The job's own history — slow is only meaningful relative to what that
   interface normally does. The cockpit's p95 is absolute; a baseline would
   make it relative.

### "Should I just retry it?"

The replay queue tells you. A **transient** cause — a timeout, a lock —
clears by itself, and the retry succeeds. A **configuration** cause — a
missing authorisation, a wrong mapping — fails again, and the queue says so
explicitly: *"This cause needs a fix, not a retry."*

Never retry a duplicate-key error without checking idempotency first. The
tool does not post anything to your system; when it does, the idempotency
check has to be part of that path.

---

## Part 2 — troubleshooting the cockpit

### The build cannot resolve `com.sap.jco:sapjco3`

Expected outside SAP's network. The JCo adapter lives in `src/extra/java`
and is only compiled with the `with-jco` profile:

```bash
mvn clean package                 # default - no SAP dependencies
mvn -Pwith-jco clean package      # needs repo.sap.com reachable
```

### The message log is empty

The seed did not run, or the window is empty. Check:

```yaml
cockpit:
  seed:
    days: 7
    messages-per-day: 220
```

and confirm the data:

```sql
SELECT COUNT(*) FROM IFLOG;
SELECT COUNT(*) FROM IFDEF;
```

### Every interface breaches its SLA

Usually one thing: the SLA is compared against a success rate that counts
**warnings** as failures, or the window is empty and the percentage falls
out as zero. Check `cockpit.monitoring.sla-window-hours` and confirm that
`IFDEF.SLA_PCT` holds the agreed rate.

### An interface does not appear at all

It is in `IFDEF` but not in the window — no traffic in the last
`cockpit.monitoring.sla-window-hours` hours. That is worth noticing on its
own: an interface that has been quiet for a day may be a job that stopped
running.

### The alert list is empty

The rules are only evaluated when you press **Evaluate alert rules** or
when the scheduled evaluation runs. `GET /api/v1/alerts` returns the
persisted alerts, not a live evaluation.

### The same alert appears twice

It should not — `IFALERT` has a unique constraint on `(IFACE, RULE)`. If
it happens, the schema was created by hand instead of from `db/schema.sql`.

### A screen returns HTTP 500 but the tests are green

A template expression does not resolve. `PageRenderTest` renders every
screen and fails on a template exception — if the failing screen is not
covered, add it to the parameterised list in that test.

### The RFC call fails with "function module not found"

The function module was not created with the **Remote-Enabled** flag in
`SE37`. This is the single most common RFC error, and the symptom on the
calling side is an entry in `SM58`.

Other causes, in order of likelihood:

1. The function module exists but was not transported to the target system.
2. The RFC destination in `SM59` points at the wrong client.
3. The calling user has no `S_RFC` authorisation.
4. The connection itself fails — check `SM21` on the caller.

---

## Part 3 — the questions interviewers ask about these failures

| Question | Short answer |
|---|---|
| Where does an RFC failure appear? | `SM58` on the caller; the dump, if any, is in `ST22` on the callee |
| Why "function not found"? | The function module was not created Remote-Enabled |
| Where are the IDoc errors? | `EDIDC` — status `03` is processed, anything else is a problem |
| How do you find the slow statement? | `ST12` for the SQL, `ST05` for the ABAP trace |
| What is a lock-related failure? | `SM12` holds the lock; find the owning transaction before releasing it |
| Is a 99% success rate good? | Only against the agreed SLA — and "up" is not the same as "correct" |
| Why not retry everything? | A configuration error fails again; a timeout does not |
