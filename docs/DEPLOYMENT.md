# Deployment

Two paths: the local demo that ships with the repository, and the live SAP
path.

---

## Path A — local demo (works today, zero cost)

### Requirements

* Java 8 or newer
* Maven 3.6+
* Nothing else

```bash
mvn clean package
java -jar target/sap-interface-cockpit.jar
```

The application starts on <http://localhost:8082>, creates a local H2
database under `./data/`, seeds the interface catalogue and seven days of
generated traffic, and waits at the login page.

### Demo accounts

| User | Portal role | Sees |
|---|---|---|
| `ops.lead` | OPS_ADMIN | Everything |
| `analyst.dev` | ANALYST | Monitoring, triage and alerts |
| `viewer` | VIEWER | Dashboard only |

Password: `cockpit.demo-password` (default `demo1234`). Turn the accounts
off in a shared environment with `cockpit.seed-demo-users: false`.

### Configuration

All in `src/main/resources/application.yml`:

| Key | Purpose |
|---|---|
| `cockpit.seed.days` | How many days of traffic to generate |
| `cockpit.seed.messages-per-day` | The traffic volume |
| `cockpit.seed.random-seed` | Fixes the dataset so the tests can assert on it |
| `cockpit.monitoring.sla-window-hours` | The window the SLA and the rules look at |
| `cockpit.monitoring.failure-burst-threshold` | Failures above this are an incident |
| `cockpit.monitoring.ageing-hours` | A failure older than this needs an owner |
| `cockpit.monitoring.latency-threshold-ms` | The p95 latency target |
| `cockpit.mail.*` | Daily digest delivery (disabled by default) |

### Tests

```bash
mvn test
```

---

## Path B — against a live SAP system

### Step 1 — read the message log over JDBC

The SQL is portable ANSI, so the datasource swap is configuration.

```yaml
# /opt/cockpit/cockpit.yml
spring:
  datasource:
    url: jdbc:sap://hana-host:39015/?databaseUrl=true&databaseName=HXE
    driver-class-name: com.sap.hana.client.jdbc.Driver
    username: SAP_BASIS_READ
    password: ${SAP_BASIS_DB_PASSWORD}
cockpit:
  seed-demo-users: false
  seed-demo-data: false
```

```bash
java -jar target/sap-interface-cockpit.jar \
     --spring.config.additional-location=file:/opt/cockpit/cockpit.yml
```

The SAP HANA JDBC driver is published on SAP's Maven repository, not Maven
Central:

```bash
mvn -Phana clean package
```

or fetch it directly:

```bash
mvn dependency:get -Dartifact=com.sap.hana.client:hana-jdbc:2.6.0 \
    -DremoteRepositories=https://repo.sap.com/nexus/content/groups/public/
```

**Use a read-only database user.** The tool only issues `SELECT` against the
message log.

### Step 2 — read the log over RFC instead

When the data must come through the SAP application layer — for example
when the monitoring user may not have direct database access:

1. Create `Z_IF_LOG_READ` from `abap/` in `SE37`, **Remote-Enabled**
   ticked.
2. Create the DDIC structure from `abap/ZIF_LOG_TYPES.abap` in `SE11`.
3. Maintain the RFC destination in `SM59`.
4. Build with the JCo adapter:

```bash
mvn -Pwith-jco clean package
```

5. Provide the connection parameters in `jco.properties` in the working
   directory — never in the repository:

```properties
sap.applicationServer=appserver.example.internal
sap.systemNumber=00
sap.client=100
sap.user=BASIS_MONITOR
sap.password=********
```

### Step 3 — point the catalogue at your interfaces

The interface catalogue (`IFDEF`) is the cockpit's own table: the name, the
type, the owner, the source and target systems and the agreed SLA. In a real
landscape it is maintained from the integration catalogue, and the SLA is
agreed with the business — which is a conversation the tool cannot have for
you.

### Step 4 — schedule the evaluation

```cron
*/15 * * * * curl -s -X POST http://localhost:8082/scan > /dev/null
```

With `cockpit.mail.enabled: true` and `spring.mail` pointed at a relay, the
new alerts can be mailed to the integration team.

---

## Path C — deploy on the SAP NetWeaver Java stack

The code targets **Java 8** and uses `javax.servlet` (Servlet 3.1), which
is what NetWeaver 7.5 supports. Two routes:

### C1 — Standalone JAR next to the landscape (simplest)

```bash
nohup java -jar sap-interface-cockpit.jar \
  --spring.config.additional-location=file:/opt/cockpit/cockpit.yml \
  > /var/log/cockpit.log 2>&1 &
```

This is how most monitoring and integration tools actually run in a customer
landscape: outside the SAP application server, with a route to the systems
it reads.

### C2 — WAR on NetWeaver Java

Switch the packaging to `war`, mark the embedded Tomcat as `provided` and
deploy through **NetWeaver Administrator → Deployments**.

**Honest caveat:** the NetWeaver Java stack has a restricted class loader
and no embedded Tomcat, so a Spring Boot application deployed there needs
its libraries supplied by the deployment or a shared class loader entry.
That is a real deployment project rather than a configuration flag — it is
on the roadmap with that caveat attached rather than claimed as done.

---

## Operating the cockpit

### Health check

```bash
curl -s http://localhost:8082/api/v1/health
```

```json
{"status":"UP","interfaces":8,"messagesInWindow":246}
```

### Useful endpoints

| Endpoint | Purpose |
|---|---|
| `GET /api/v1/summary` | The dashboard numbers, as JSON |
| `GET /api/v1/interfaces` | The SLA report per interface |
| `GET /api/v1/interfaces/{name}` | One interface in detail |
| `GET /api/v1/clusters` | The triage - one problem per interface and cause |
| `GET /api/v1/errors` | The error catalogue |
| `GET /api/v1/alerts` | The alert inbox |

### Operational notes

| Topic | Recommendation |
|---|---|
| Credentials | Keep them in an external configuration file or the environment, never in the repository |
| Database user | Read-only against the message log |
| Demo accounts | `cockpit.seed-demo-users: false` in anything shared |
| Schema changes | Replace the rebuild-on-start schema with migrations before production use |
| Alert noise | Tune the thresholds with the integration team before switching the notifications on — a monitor that cries wolf gets switched off |
