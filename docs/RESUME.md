# Resume and cover letter content

Copy-paste blocks. Replace the placeholders. **Only claim what you can
defend** — `INTERVIEW-QA.md` is built from this project, so an interviewer
can go deep on every line below.

---

## Project title

> **SAP Interface Cockpit — integration operations for SAP landscapes (RFC · IDoc · BAPI)**

---

## Tech stack line

> Java 8 · Spring Boot 2.7 · Spring MVC · Thymeleaf · Spring JDBC (JdbcTemplate) · Spring Security Crypto (BCrypt) · SAP HANA (JDBC) · SAP NetWeaver 7.5 · ABAP (RFC-enabled function modules) · SAP Java Connector (JCo) · JUnit 5 · Maven

---

## Resume bullets (four-bullet version)

> - Built an **integration operations cockpit for SAP landscapes** that monitors RFC, IDoc and BAPI traffic, tracks every interface against its SLA (success rate, p95 latency, oldest unresolved failure) and raises **four rule-based alerts** — failure burst, SLA breach, latency breach and ageing.
> - Implemented **error triage that groups failures into one problem per interface and cause**, enriched by an error catalogue mapping every code to its cause and its remediation — turning three hundred failed messages into one actionable line, with a server-side SVG trend chart that shows an incident as a shape rather than a spike.
> - Added a **replay queue** for failed messages that distinguishes transient causes (which succeed on retry) from configuration causes (which fail again and need a fix), with every acknowledgement and replay recorded in an **audit trail**.
> - Shipped the SAP-side artifacts — an **RFC-enabled ABAP function module** reading the interface log from `TRFC`/`EDIDC` and a **SAP Java Connector client** over an `SM59` destination — backed by a **50-case test suite** covering SLA arithmetic, triage grouping, alert de-duplication, replay semantics and screen rendering.

---

## Resume bullets (three-bullet version)

> - Built a **Spring Boot integration operations cockpit for SAP** (Java 8, JdbcTemplate, SAP HANA) that monitors RFC, IDoc and BAPI traffic, tracks each interface against its SLA and raises failure-burst, SLA-breach, latency and ageing alerts — with **50 automated tests passing**.
> - Engineered the **error triage** that turns a wall of failed messages into one problem per interface and cause, enriched by an error catalogue of cause and remediation — and a **replay queue** that knows a timeout succeeds on retry while a missing authorisation does not.
> - Delivered the SAP-side artifacts (**RFC-enabled ABAP function module** reading `TRFC`/`EDIDC`, **SAP Java Connector client**) plus an **audit trail** of every acknowledgement and replay, and a configurable rule and threshold model so an integration team can tune the cockpit to its own landscape.

---

## Professional summary (option A)

> SAP BASIS Analyst with hands-on experience in integration operations on SAP
> NetWeaver landscapes: RFC, IDoc and BAPI monitoring, SLA tracking, error
> triage and incident support, together with transport management,
> authorisations and Java development on the SAP Java stack. Built **SAP
> Interface Cockpit**, a Spring Boot application that turns interface traffic
> into ranked, actionable findings with an audit trail, and **SAP Custom Code
> & Impact Analyzer**, which answers what breaks if a custom object changes.
> Comfortable in `SM37`, `SM58`, `SM59`, `SM21`, `ST22`, `ST12`, `SM50`,
> `SE09`, `SU01` and `PFCG`, and in the ABAP and Java tooling around them.

## Professional summary (option B)

> SAP BASIS Analyst focused on the two things that decide whether an
> integration or an upgrade survives contact with production: *knowing what
> broke and why*, and *knowing what a change will affect*. Built a
> Java 8 / Spring Boot cockpit that monitors RFC, IDoc and BAPI traffic,
> groups failures into one problem per cause with the remediation attached,
> alerts on SLA and latency breaches and replays failed messages — and an
> analyzer that maps the transitive blast radius of a custom-code change.

---

## Skills section

**SAP**
SAP NetWeaver 7.5 · SAP BASIS · Integration operations · RFC · IDoc · BAPI ·
RFC statistics buffer (`TRFC`) · IDoc log (`EDIDC`) · RFC destinations and
routing (`SM59`, `SM56`) · Background jobs (`SM37`) · Error diagnosis
(`SM21`, `ST22`, `ST05`, `ST12`, `SM50`, `SM66`, `SM12`) · Transport
management (`SE09`/`SE10`/`SE01`) · Authorisations (`SU01`/`PFCG`/`SU53`) ·
ABAP development (`SE11`/`SE37`/`SE38`) · SAP HANA · SAP GUI

**Development**
Java 8 · Spring Boot 2.7 · Spring MVC · Thymeleaf · Spring JDBC (JdbcTemplate) ·
Spring Security Crypto (BCrypt) · SAP Java Connector (JCo) · SQL (portable
ANSI / SAP HANA dialect) · Server-side SVG generation · Rule engines and
threshold configuration · SLA and percentile computation · JUnit 5 · Maven · Git

**Tools**
Eclipse + ADT · SQL Developer / DBeaver · Docker (SAP HANA Express) · Git ·
GitHub · Postman · Chrome DevTools

---

## Cover letter paragraph

> I am applying for the SAP BASIS Analyst role because the work I want to do
> is the work that happens when something goes wrong in an interface at 03:00
> and nobody knows yet. My portfolio project **SAP Interface Cockpit** is a
> Java 8 / Spring Boot application for the SAP Java stack that monitors RFC,
> IDoc and BAPI traffic, tracks every interface against its SLA and raises
> alerts for failure bursts, SLA breaches, latency breaches and ageing
> failures. The part I am proudest of is the triage: it groups three hundred
> failed messages into one problem per interface and cause and attaches the
> remediation from an error catalogue — *"the service user has no
> authorisation on the target: grant `S_RFC` in `SU01` and check the
> destination in `SM59`"*. It also has a replay queue that knows the
> difference between a timeout, which succeeds when you retry it, and a
> missing authorisation, which does not. On the SAP side it pairs an
> RFC-enabled ABAP function module reading `TRFC` and `EDIDC` with a SAP
> Java Connector client. Fifty tests back it up, and one of them caught a
> screen that would have returned an error page in production. I am looking
> for a team where I can apply this to real customer landscapes.

---

## LinkedIn / About section

> SAP BASIS Analyst in the making. I build the tooling that turns a wall of
> failed interface messages into one sentence with a fix attached. Current
> portfolio: **SAP Interface Cockpit** — Java 8 / Spring Boot monitoring and
> triage for SAP RFC, IDoc and BAPI traffic, with SLA tracking, rule-based
> alerting, an error catalogue that carries the remediation, and a replay
> queue that knows which failures deserve a retry. 50 tests, zero SAP
> licence required to run it locally. Learning in the open, one commit at a
> time.

---

## GitHub profile README

```
**SAP BASIS Analyst** - NetWeaver 7.5 | Integration ops | Transports | Custom code

Building SAP Interface Cockpit: a Java 8 / Spring Boot application that
monitors RFC, IDoc and BAPI traffic, tracks every interface against its SLA
and turns three hundred failed messages into one problem per cause - with
the remediation attached.

- SLA tracking: success rate, p95 latency, oldest unresolved failure
- Error triage: one cluster per interface and cause, enriched from a catalogue
- Four alerting rules: failure burst, SLA breach, latency breach, ageing
- Replay queue: transient causes succeed on retry, configuration causes do not
- RFC-enabled ABAP function module reading TRFC/EDIDC + a JCo client
- 50 automated tests
```

---

## Certification path (pick what you actually hold)

| Certification | Why it fits this project |
|---|---|
| SAP Certified Associate - SAP Integration Certification | Fits the RFC / IDoc / BAPI work directly |
| SAP Certified Associate - SAP ABAP Programming | Validates the function module and the message model |
| SAP Certified Associate - SAP Cloud Programming Fundamentals | Entry-level SAP credential, widely recognised |
| openSAP / SAP Learning badges | Free, quick, and can be listed under "Learning" |

---

## Truthfulness checklist before you send this

- [ ] Every bullet describes something you can **run and show**
- [ ] The project is presented as **self-initiated**, not as client work
- [ ] You can explain, for every bullet, the SAP table or transaction behind it
- [ ] You have run `mvn clean package` and can show the 50 green tests
- [ ] You have the application running and can demo the incident dashboard live
- [ ] You know exactly what is **not** compiled against a live SAP system yet,
      and can say so calmly (see `HONEST-STATUS.md`)
