# Architecture

## Why this architecture exists

An integration team does not have a "what is broken" problem. It has a
**"there are three hundred red rows and I have an hour"** problem. That one
sentence decides the architecture: the tool has to compress noise into
problems, and every problem has to arrive with its cause and its next step
attached.

That goal produces the four rules below.

---

## Rule 1 — Triage groups failures into problems, not rows

Forty failures of the same error code on the same interface are **one**
incident. The naive presentation - a filtered log - makes the reader do the
grouping themselves, which is exactly the work the tool should be doing.

`ErrorTriageService` groups the window by `(interface, error code)` and for
each group produces:

| Field | Why it matters |
|---|---|
| `occurrences` | Is this a spike or a trend? |
| `distinctHosts` | One bad host, or every host? |
| `firstSeen` / `lastSeen` | When did it start, and is it still happening? |
| `cause` | From the error catalogue — the *why* |
| `remediation` | From the error catalogue — the *what now* |
| `successRate` of the interface | How much of the traffic is affected |

The clusters are ordered by severity first and by size second, so the screen
answers "what do I fix first" rather than "what happened most often".

---

## Rule 2 — The error catalogue is what turns a log into a runbook

A message log tells you **that** something broke. An error catalogue tells
you **what to do**:

| Code | Cause | Remediation |
|---|---|---|
| `ERR_AUTH_403` | The target system rejected the call — the service user has no authorisation | Grant the missing authorisation in `SU01` (`S_RFC` / `S_BAPI`) and check the RFC destination in `SM59` |
| `ERR_TIMEOUT` | The RFC call exceeded the timeout on the destination | Check `SM50` / `SM66` for a blocked work process, then raise the timeout in `SM59` |
| `ERR_LOCK_OBJECT` | The target object was locked by an open business transaction | Find the lock in `SM12` and release it once the owning transaction has ended |
| `ERR_FIELD_MAPPING` | The IDoc segment does not match the target structure | Compare the IDoc structure with the target in `WE02` and correct the mapping |
| `ERR_NO_ROUTE` | The destination is not in the routing table | Check the routing table in `SM56` |
| `ERR_DUP_KEY` | Rejected as a duplicate key on the target table | Make the posting idempotent — look up the key and skip instead of failing |
| `ERR_MANDANT` | The target client does not exist or does not match | Check the client in the destination and the `MANDT` field in the IDoc |
| `WARN_SLOW` | Succeeded, but slower than the interface threshold | Check `ST12` for the slow statement and consider the index |

The `COMMON` flag marks a code as a known recurring cause, which is what
distinguishes "this is how this interface always fails" from "this is new".

Without this table the cockpit would be a prettier log. With it, the triage
row is the first line of a fix.

---

## Rule 3 — The alerting rules are questions, not thresholds

Four rules, each one a question an integration team asks out loud:

| Rule | Question | Severity |
|---|---|---|
| `FAILURE_BURST` | Is one interface failing repeatedly? | CRITICAL |
| `SLA_BREACH` | Is an interface below its agreed success rate? | MEDIUM |
| `LATENCY_BREACH` | Is an interface too slow? | MEDIUM |
| `AGEING` | Is a failure nobody has touched? | MEDIUM |
| `INTERFACE_DOWN` | Is an interface completely dead? | CRITICAL |
| `RECURRING_CAUSE` | Is the same cause repeating? | HIGH |

Alerts are keyed on **interface + rule** (a unique constraint in the
schema), so re-running the evaluation refreshes the existing alert instead
of raising a second one. That matters: a monitor that raises the same alert
every five minutes gets switched off, and then it protects nothing.

The thresholds are configuration, not code (`cockpit.monitoring.*`), because
the first conversation with an integration team is always about numbers.

---

## Rule 4 — The replay is simulated honestly

The project cannot call a customer system, so the replay is simulated. The
simulation is honest about what actually happens:

* an error caused by a **transient condition** — a timeout, a lock —
  *succeeds* when it is retried, because the condition has cleared;
* an error caused by **configuration** — a missing authorisation, a wrong
  mapping — *fails again*, because retrying cannot fix it.

That distinction is the whole value of the queue. "Retry it and see" is
wasteful for a configuration error and exactly right for a timeout, and the
queue tells you which case you are in. The transient codes are an explicit,
readable set in `ReplayService` — not a hidden heuristic.

---

## Layering

```
web/          Controllers, session authentication, @RequireRole gating.
              No business logic.

service/      The analysis: SLA and trends, triage, alerting, replay.
              No SQL, no HTTP, no Spring MVC types.

repo/         JdbcTemplate queries and row mapping. ANSI SQL only.

support/      TrendChartRenderer - the hourly chart, built server-side.

config/       AppProperties - every threshold and rule that can be tuned.

seed/         The landscape and seven days of generated traffic.
```

The service layer is the interesting one, and it is deliberately free of
framework types. `ErrorTriageService` has no idea whether it is being
called from a Thymeleaf template, a REST endpoint or a scheduled job.

---

## Data model

| Table | Models | Holds |
|---|---|---|
| `IFDEF` | the interface catalogue | interface, type, direction, owner, system, target, SLA, criticality |
| `IFLOG` | the message log | one row per RFC / IDoc / BAPI call |
| `IFERR` | the error catalogue | code, severity, cause, remediation, known-cause flag |
| `IFALERT` | alerts | interface + rule, severity, message, acknowledgement |
| `IFQUEUE` | the replay queue | queued message, state, who played it, what happened |
| `SAPUSER` | user master | owner and department |
| `PORTAL_USER` / `PORTAL_AUDIT` | the portal's own | accounts and the audit trail |

In a live landscape the message log is not a table the BASIS team curates —
it is read from the RFC statistics buffer (`TRFC`) and the IDoc log
(`EDIDC`). The `Z_IF_LOG_READ` function module does exactly that, and
returns one consolidated view so the cockpit does not need to know where
each message type came from.

---

## Why the aggregates are computed in Java

The SLA figures need a **percentile**, the trend needs **hour bucketing**,
and both differ between SQLite and HANA. The SQL stays portable ANSI
(`SELECT` with `WHERE`, `ORDER BY`, `JOIN`) and every calculation happens in
one place in Java:

```java
public long percentile(List<Long> values, int percentile) {
    List<Long> sorted = new ArrayList<Long>(values);
    Collections.sort(sorted);
    int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
    ...
}
```

One behaviour that is easy to test is worth more than a query that only
runs on one engine — and it means the same numbers come out on a laptop and
on a customer system.

The same reasoning applies to the trend chart: the buckets are computed in
Java and drawn as a **server-side SVG**, with no graph library, no
client-side JavaScript and no CDN. The chart therefore works on an isolated
network, can be pasted into a document, and can be asserted on in a test.

---

## Security model

| Concern | How it is handled |
|---|---|
| Password storage | BCrypt (`spring-security-crypto`), no plaintext, no reusable hash in the repository |
| Demo credentials | Seeded at runtime from `cockpit.demo-password`; disabled with `seed-demo-users=false` |
| Session | Server-side `HttpSession`, `HttpOnly` cookie, 30-minute timeout, `SameSite=Lax` |
| Authorisation | `@RequireRole` on the controller, enforced by `AuthInterceptor` **before** the handler runs |
| Roles | `OPS_ADMIN` (everything), `ANALYST` (monitoring and triage), `VIEWER` (dashboard only) |
| Audit | Every sign-in, alert acknowledgement, alert scan and replay is written to `PORTAL_AUDIT` |

The project uses its own small interceptor instead of the full Spring
Security filter chain. With three roles and one annotation, a 60-line
interceptor says the rules more clearly than a filter chain does — and in
an interview that clarity is the point.

---

## Testing strategy

50 tests, all against the same seeded landscape the application uses, so
the tests exercise the real data rather than a hand-built fixture.

They assert **behaviour, not implementation**:

* the SLA arithmetic, the percentile and the hourly bucketing;
* failures group into one cluster per interface and cause, and the cluster
  carries the catalogue cause and remediation;
* the burst and latency rules fire, and a second evaluation does **not**
  duplicate an alert;
* a transient error succeeds on replay, a configuration error fails again;
* every screen renders.

That last one earned its place during this build: the `/replay` screen was
returning HTTP 500 because the template file had never been written, and no
service-level test could have found it.
