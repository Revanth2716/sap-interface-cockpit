# Interview preparation

Questions an interviewer will ask about this project, with the answer it
gives you. The point of the project is not that you ran a build — it is that
you can go deeper than a build.

---

## Part 1 — questions about the project

**"Walk me through this application."**
> It monitors RFC, IDoc and BAPI traffic, tracks every interface against its
> SLA, and does three things with the failures: groups them into one problem
> per interface and cause, attaches the cause and the remediation from an
> error catalogue, and raises alerts through four rules. There is also a
> replay queue, because re-processing a failed message is what an
> integration team actually does at 07:00 after a night-time outage.

**"What does 'triage' actually mean here?"**
> Forty failures of the same error code on the same interface are one
> incident, not forty. The service groups the window by interface and error
> code and reports the count, the distinct hosts it came from, when it
> started, whether it is still happening, and the interface's success rate
> — plus the cause and the remediation from the catalogue. That last part is
> what turns a log into a runbook.

**"Why an error catalogue? Why not just show the error text?"**
> Because the error text tells you what happened and the catalogue tells you
> what to do. `ERR_AUTH_403` means the service user has no authorisation on
> the target, and the remediation is: grant `S_RFC` in `SU01` and check the
> RFC destination in `SM59`. Without that mapping the cockpit is a prettier
> log.

**"What is the replay queue for?"**
> Re-processing failed messages. The important part is that it knows which
> failures deserve a retry: a timeout or a lock clears on its own and
> succeeds when you retry it, while a missing authorisation or a wrong
> mapping fails again and needs a fix. The tool says which case you are in,
> and records who did what.

**"How do you decide something is an alert?"**
> Four rules, each one a question an integration team asks: is one interface
> failing repeatedly, is it below its agreed success rate, is it too slow, is
> there a failure nobody has touched. Plus a fifth for a completely dead
> interface. The thresholds are configuration, not code, because the first
> conversation with an integration team is always about numbers.

**"What if the same problem fires every five minutes?"**
> It refreshes the existing alert. Alerts are keyed on interface plus rule
> with a unique constraint, so re-evaluating updates the row rather than
> adding one. A monitor that raises the same alert every cycle gets
> switched off, and then it protects nothing.

**"What was the hardest part?"**
> Making the seeded data tell a story. The first version of the traffic
> generator made forty-five percent of *all* messages failures, which
> looked like a bug rather than an incident. Rewriting it so the interface
> is chosen first and the outcome follows that interface's own profile
> produced the thing I wanted: a quiet little interface that started failing
> at 02:00 yesterday and has failed every call since.

---

## Part 2 — the SAP questions behind the project

**"Where does an RFC failure show up?"**
> `SM58` on the calling side. The actual dump, if there is one, is in
> `ST22` on the callee. That asymmetry is what makes the first RFC error
> confusing — the caller says "function not found" and the callee says
> nothing at all.

**"Why would an RFC call fail with an authorisation error?"**
> The service user has no `S_RFC` (or `S_BAPI`) on the target, or the
> destination in `SM59` points at the wrong client, or the connection itself
> is refused. The catalogue entry for `ERR_AUTH_403` says exactly that, and
> the fix is in `SU01` and `SM59`.

**"How do you find out that an interface is slow?"**
> `ST12` for the SQL trace - that is where the time actually went - and
> `ST05` for the ABAP trace when the time is not in the database. Then
> compare against the job's own history: slow is only meaningful relative
> to what that interface normally does.

**"What is the difference between an IDoc error and an RFC error?"**
> An IDoc has a status: `03` is processed, anything else is a problem, and
> `EDIDC` holds the error text. An RFC call has no status of its own - it
> either returns or it raises, and the evidence is in the message log and
> in `SM58`.

**"Where would you read the message log from in a real system?"**
> The RFC statistics buffer `TRFC` and the IDoc log `EDIDC`. The function
> module in this project reads both and returns one consolidated view, so
> the cockpit does not need to know where each message type came from.

**"What is the difference between an interface being down and being
degraded?"**
> Down is no successful call in the window at all — that is the
> `INTERFACE_DOWN` rule, and it is critical. Degraded is a success rate
> below the SLA: some calls get through, so the business is quietly getting
> wrong data rather than nothing. That is the more dangerous of the two, and
> it is why the SLA is per interface rather than global.

---

## Part 3 — questions about the engineering

**"Why Java 8?"**
> Because NetWeaver 7.5 runs Java 8. Writing it to Java 8 and
> `javax.servlet` means the same artefact can be deployed on the SAP Java
> stack and run standalone next to the landscape.

**"Why not Spring Security?"**
> Deliberate. Three roles and one annotation; a 60-line interceptor reads
> more clearly than a filter chain. The password hashing still comes from
> Spring Security Crypto, so nothing is stored in the clear.

**"How do you compute a percentile?"**
> In Java, not in SQL — the percentile function differs between SQLite and
> HANA. The list is sorted and the index is `ceil(p/100 * n) - 1`. One
> behaviour that is easy to test is worth more than a query that only runs
> on one engine.

**"Why is the chart drawn on the server?"**
> No graph library, no client-side JavaScript, no CDN. The markup is built
> server-side and embedded in the page, so it works on an isolated
> network, can be pasted into a document, and can be asserted on in a test.

**"How would this scale to a real landscape?"**
> The message log is the part that needs attention: it would be read from
> `TRFC` and `EDIDC` with the window pushed into the query, and the
> aggregates computed per interface rather than by loading the window into
> memory. The services and the screens do not change, because the
> calculation stays in one place.

**"What is the weakest part of the design?"**
> The replay is simulated, so the tool cannot yet prove that a retried
> message is actually idempotent on the target. In production it would need
> a check before posting — never post a duplicate — and that check belongs
> on the roadmap.

---

## Part 4 — questions you should ask them

1. "How do you currently find out that an interface has failed?"
2. "Who owns the interfaces — the application team, a central integration
   team, or the middleware team?"
3. "How do you measure an interface's agreed SLA, and who agrees it?"
4. "When an interface fails, what happens at 03:00 — is it retried
   automatically, or does somebody do it?"
5. "How do you keep the error knowledge - the causes and the fixes - in the
   team?"
6. "Which systems are in scope, and which interface types do you see most?"

---

## Part 5 — 30-second pitch

> "I built an integration operations cockpit for SAP landscapes — a Java 8 /
> Spring Boot application that monitors RFC, IDoc and BAPI traffic, tracks
> every interface against its SLA, and does the thing that actually matters
> at 03:00: turns three hundred failed messages into one problem per
> interface and cause, with the cause and the remediation attached from an
> error catalogue. It raises four rule-based alerts, and it has a replay
> queue that knows a timeout succeeds when you retry it and a missing
> authorisation does not. On the SAP side there is an RFC-enabled ABAP
> function module reading TRFC and EDIDC and a JCo client. Fifty tests, and
> one of them caught a screen that would have shipped broken."
