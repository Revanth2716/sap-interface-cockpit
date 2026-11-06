# Honest status

What in this repository is **verified working**, what is **written but not
yet executed**, and what is **modelled rather than measured**. Read this
before describing the project in an interview.

---

## 1. Verified working today

| Item | Evidence |
|---|---|
| The application builds | `mvn clean package` → `BUILD SUCCESS`, executable JAR produced |
| The whole test suite passes | `mvn test` → **50 tests, 0 failures, 0 errors** |
| The application starts and serves | `java -jar target/sap-interface-cockpit.jar` → HTTP 200 on `/login` |
| Every screen renders | Eight screens captured from the running application into `docs/screenshots/` |
| The SLA arithmetic is right | `InterfaceHealthServiceTest` — success rate, percentile, hourly buckets, "worst first" ordering |
| Triage groups correctly | `ErrorTriageServiceTest` — one cluster per interface and cause, enriched from the catalogue, ordered by severity |
| The alert rules fire | `AlertAndReplayServiceTest` — failure burst and latency breach fire; a second evaluation does **not** duplicate an alert |
| The replay semantics are right | A transient error succeeds on retry; a configuration error fails again |
| The API works | `ApiControllerTest` — health, summary, interfaces, clusters, errors, alerts, per-interface report |
| Role gating works | `PageRenderTest` — an analyst sees the monitoring screens, a viewer gets `403` |

**Reproduce all of it:**

```bash
mvn clean package
java -jar target/sap-interface-cockpit.jar
```

---

## 2. Written but not yet executed on a live SAP system

This is an honest gap, and it is a gap of *environment*, not of intent.

| Item | Status | Why |
|---|---|---|
| `abap/Z_IF_LOG_READ.abap` | **Not yet compiled on NetWeaver** | No SAP system was available in the build environment. The code follows NetWeaver 7.5 / ABAP 7.40+ syntax and the Remote-Enabled flag is documented in the header, but it has not been through the ABAP syntax check. |
| `abap/ZIF_LOG_TYPES.abap` | **Not yet created in SE11** | This is the DDIC structure the function module imports. |
| `src/extra/java/.../RfcInterfaceLogGateway.java` | **Not yet compiled** | The SAP Java Connector (`com.sap.jco:sapjco3`) is published on SAP's own Maven repository, not Maven Central, and that host was unreachable from the build environment. The `with-jco` profile declares the exact coordinates needed. |
| The HANA JDBC path | **Configured, not exercised** | The `hana` profile and the datasource configuration are in place; no HANA instance was available. |
| Reading `TRFC` / `EDIDC` | **Modelled, not measured** | The message log in this project is a seeded synthetic dataset with the shape of the real tables, not a customer's traffic. |

**What this means in an interview:** say *"the SAP-side artifacts are written
against NetWeaver 7.5 semantics and are ready to compile, but I could not run
them because I did not have a system available."* That is a completely
acceptable answer for a fresher, and it is far better than implying the code
was executed.

**The plan to close the gap** is in `DEPLOYMENT.md`: install the NetWeaver AS
ABAP Developer Edition, paste the function module into `SE37` with
**Remote-Enabled** ticked, create the DDIC structure in `SE11`, and build
the JCo adapter against an RFC destination in `SM59`.

---

## 3. Modelled rather than measured

| Area | What is modelled | What production needs |
|---|---|---|
| Message log | One table with the shape of `TRFC` and `EDIDC` | The real statistics buffer and IDoc log, read through the function module |
| The landscape | 8 synthetic interfaces across a DEV/QA/PRD landscape | The customer's own interface catalogue |
| The traffic | Seven days of generated messages with per-interface profiles | The real traffic, including the interfaces nobody monitors |
| The error catalogue | Eight error codes with real causes and remediations | The customer's own runbook, which is always better than any generic one |
| The replay | Simulated against the error semantics | A real re-processing call, with an idempotency check before posting |

---

## 4. Known limitations — the ones an interviewer would find

1. **The replay does not call a customer system.** It simulates the
   outcome from the error semantics. A production version must also check
   idempotency before posting, so a retry cannot create a duplicate.
2. **Only what the middleware records is visible.** A message that never
   reached the system is invisible to any monitor — including this one.
3. **The SLA is a success rate, not a business measure.** "99.5% up" and
   "the data is right" are different questions, and only the second one
   matters to the business.
4. **The scanner has no baseline.** "Slow" is absolute (a threshold in
   milliseconds), not relative to what that interface normally does.
5. **The schema is rebuilt on every start.** `schema.sql` drops and
   recreates every table, then the seeders reload the landscape. That is
   predictable for a demo and wrong for production — migrations replace it.
6. **Single system.** The model has one landscape; a real tool compares
   DEV, QA and PRD.

---

## 5. What the project is not

* It is **not** connected to a live SAP system out of the box — it runs
  against the seeded model.
* It is **not** a middleware. It observes and explains; it does not route,
  transform or retry anything.
* It is **not** an alerting tool that pages people. It raises alerts; how
  they reach a human is the customer's own notification setup.
* It is **not** a replacement for `SM58` and `EDIDC` — it tells you what
  the failures mean, which those transactions do not.

---

## 6. Why the gaps were left open rather than faked

A portfolio project that pretends to have run against a production system is
easy to expose in an interview: the interviewer asks which release, which
client, which interface, and the story collapses. The gaps above are
specific, explained, and each one comes with the concrete step that closes
it — which is a better answer, and a better piece of engineering to talk
about.
