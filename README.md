# Spring Boot + Hibernate on ClickHouse

A small REST service that stores events in [ClickHouse](https://clickhouse.com) through
Hibernate / Spring Data JPA. It demonstrates how to run JPA on a column-oriented OLAP
database, and where ClickHouse deliberately deviates from a classic RDBMS.

| Layer     | Choice                                                    |
| --------- | --------------------------------------------------------- |
| Language  | Java 25 LTS                                               |
| Build     | Gradle 9.6.1 (wrapper included, no local Gradle needed)   |
| Framework | Spring Boot 4.1.2 (`starter-web`, `starter-data-jpa`)     |
| ORM       | Hibernate ORM 7.4.13 + custom `ClickHouseDialect`         |
| Database  | ClickHouse 26.8.36.3 (Docker Compose), JDBC driver 0.11.0 |

## Prerequisites

- **Docker** with the Compose plugin (to run ClickHouse)
- **JDK 25** on the `PATH` (Gradle uses it via the Java toolchain; set
  `org.gradle.java.home` in `gradle.properties` if it lives elsewhere)
- Internet access on first run (Gradle wrapper downloads the Gradle 9.6.1
  distribution and pulls dependencies from Maven Central)

That's it — you do _not_ need a locally installed Gradle; `./gradlew` bootstraps it.

## Build

```bash
./gradlew build
```

Compiles the app, packages the executable jar and runs the test suite.

- Fat jar: `build/libs/clickhouse-hibernate-demo-0.1.0.jar`
- Tests: the unit tests always run; the integration tests need ClickHouse
  (see below) and **skip themselves** when nothing listens on `localhost:8123`.
- Build without tests (e.g. no Docker around): `./gradlew build -x test`

## Start

**1. Start ClickHouse** (creates database `appdb`, user `app`/`app123`, exposes port 8123):

```bash
docker compose up -d
docker compose ps          # wait for STATUS "healthy" (~a few seconds)
```

**2. Start the application** — either in dev mode:

```bash
./gradlew bootRun
```

…or from the jar built in the previous step (no Gradle involved at runtime):

```bash
java -jar build/libs/clickhouse-hibernate-demo-0.1.0.jar
```

**3. Verify** — the app fails fast if ClickHouse is unreachable, so a successful
startup means the connection works. On first start it creates the `events` table
from `src/main/resources/schema.sql` and seeds ~200 demo events, then serves on
[http://localhost:8080](http://localhost:8080):

```bash
curl localhost:8080/api/stats
```

Stop the app with `Ctrl+C`.

### Configuration

Connection settings live in [`src/main/resources/application.yml`](src/main/resources/application.yml)
(`spring.datasource.*`, app port 8080). Standard Spring Boot env-var overrides work, e.g.
to point the app at another ClickHouse:

```bash
SPRING_DATASOURCE_URL='jdbc:clickhouse://ch.example.com:8123/appdb?jdbc_ignore_unsupported_values=true' \
SPRING_DATASOURCE_USERNAME=app \
SPRING_DATASOURCE_PASSWORD=secret \
java -jar build/libs/clickhouse-hibernate-demo-0.1.0.jar
```

Keep `jdbc_ignore_unsupported_values=true` in any custom URL — ClickHouse has no
transactions, and this flag makes the driver tolerate the `commit()`/`rollback()`
calls Spring issues anyway (see [notes below](#how-jpa-and-clickhouse-fit-together)).

## Stop / reset

```bash
docker compose down        # stop ClickHouse (data volume is kept)
docker compose down -v     # stop and wipe all data (re-seeds on next start)
```

## Tests

```bash
docker compose up -d   # integration tests need the database
./gradlew test
```

- `ClickHouseDialectTest` — plain unit test of the JDBC → ClickHouse type mapping, no DB needed.
- `EventApiIntegrationTest` — full round trip (create → read → update mutation → delete → stats)
  through the REST layer, Hibernate and ClickHouse.

## API

| Method   | Path                   | Description                                        |
| -------- | ---------------------- | -------------------------------------------------- |
| `POST`   | `/api/events`          | Create an event (`source`, `severity`, `message`)  |
| `GET`    | `/api/events?limit=50` | Recent events, newest first                        |
| `GET`    | `/api/events/{id}`     | Single event (`404` if unknown)                    |
| `PUT`    | `/api/events/{id}`     | Rewrite the message (ClickHouse mutation)          |
| `DELETE` | `/api/events/{id}`     | Delete (ClickHouse lightweight delete)             |
| `GET`    | `/api/stats`           | Totals, per-severity counts, hourly buckets (24 h) |

```bash
curl -X POST localhost:8080/api/events \
  -H 'Content-Type: application/json' \
  -d '{"source":"checkout-service","severity":"WARN","message":"slow query detected"}'

curl 'localhost:8080/api/events?limit=3'
curl localhost:8080/api/stats
```

To inspect ClickHouse directly:

```bash
docker exec -it clickhouse-demo clickhouse client --user app --password app123 --database appdb
```

## Troubleshooting

| Symptom                                                 | Cause / fix                                                                                               |
| ------------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| App exits at startup with a connection exception        | ClickHouse isn't up yet — `docker compose ps` must show `healthy`, then restart the app                   |
| `Port 8123 already in use` / `Port 8080 already in use` | Another container/service holds the port; change the mapping in `docker-compose.yml` or set `server.port` |
| "No matching toolchain" during build                    | Gradle can't find a JDK 25; install one or point `org.gradle.java.home` at it                             |
| Fresh `docker compose down -v` → app keeps old data?    | It doesn't: the seeder only fills an empty table, a wiped volume starts clean                             |

## How JPA and ClickHouse fit together

ClickHouse is not a JDBC/JPA-native database, so the project documents every deviation
in code. The interesting bits:

**Custom dialect** — Hibernate (core _and_ `hibernate-community-dialects`) ships no
ClickHouse dialect, so [`ClickHouseDialect`](src/main/java/com/example/clickhouse/dialect/ClickHouseDialect.java)
maps JDBC type codes to ClickHouse types (`String`, `Int64`, `DateTime64(6, 'UTC')`, `UUID`, ...).

**UUID binding** — Hibernate 7's default UUID JDBC type sends 16 raw bytes, which the
ClickHouse driver cannot serialize. `hibernate.type.preferred_uuid_jdbc_type: VARCHAR`
switches to `VarcharUUIDJdbcType`, which binds the canonical string form — the server
parses it into the native `UUID` column.

**No transactions** — ClickHouse has no transactions. The JDBC URL carries
`jdbc_ignore_unsupported_values=true` so the driver tolerates (no-ops) the
`setAutoCommit(false)` / `commit()` / `rollback()` calls made by Spring's transaction
manager. `@Transactional` boundaries still order flushing, but there is no atomicity.

**DDL lives in `schema.sql`** — ClickHouse tables require an `ENGINE` clause
(`MergeTree` with a mandatory `ORDER BY` here), which Hibernate's `hbm2ddl` cannot
express, so `spring.jpa.hibernate.ddl-auto=none` and `spring.sql.init` own the schema.

**Updates are mutations** — ClickHouse has no `UPDATE` statement. `PUT` issues
`ALTER TABLE events UPDATE ... WHERE id = ... SETTINGS mutations_sync = 1`, which blocks
until the mutation is applied. Because the bulk update bypasses the persistence context,
`@Modifying(clearAutomatically = true)` makes the follow-up read return fresh data.

**Deletes are lightweight deletes** — `DELETE FROM events WHERE id = ?` hides the row
immediately; the physical merge happens in the background.

**Aggregations stay in the database** — `/api/stats` uses native queries with
`countIf()`, `toStartOfHour()` and `now64()` so the OLAP engine does the work.

## Project layout

```text
docker-compose.yml                         ClickHouse 26.8, port 8123, user app/app123
build.gradle.kts / settings.gradle.kts     Gradle 9.6.1 build (Kotlin DSL)
src/main/resources/application.yml          datasource, JPA/Hibernate settings
src/main/resources/schema.sql              events table (MergeTree, ORDER BY (created_at, id))
src/main/.../dialect/ClickHouseDialect     Hibernate 7 dialect for ClickHouse
src/main/.../domain/Event                  JPA entity (UUID id, DateTime64 timestamp)
src/main/.../repository/EventRepository    JPA repository + native queries
src/main/.../service/EventService          API-facing service
src/main/.../web/                          REST controllers, DTOs, error handling
src/main/.../bootstrap/DemoDataSeeder      seeds ~200 events on first start
src/test/                                  unit + integration tests
```

## Bruno tests

Install:

```sh
npm install -g @usebruno/cli
```

The github action tests results: https://sco3.github.io/springboot-hibernate-clickhouse/
