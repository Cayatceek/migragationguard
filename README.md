# MigrationGuard (repository: migragationguard)

Static PostgreSQL migration **risk analyzer and command-line tool**, written in Java 21. Designed for developers reviewing Flyway/Liquibase SQL migrations before deployment.

**Status:** v0.1.0-SNAPSHOT, developer preview. It is a **heuristic linter**, not an SQL parser and not a guarantee of zero downtime.

## Build and run

```bash
mvn verify
java -jar target/migrationguard-0.1.0-SNAPSHOT.jar V12__add_columns.sql V13__drop_legacy.sql
```

Exit codes: `0` no HIGH/CRITICAL alerts; `2` high or critical risk; `1` I/O or invalid command. Medium warnings print but do not fail CI.

Example diagnostic:

```text
V12__add_columns.sql:1 [HIGH] PG003: New NOT NULL column without DEFAULT may fail on populated tables
  Advice: Add nullable column, backfill rows in batches, then enforce NOT NULL.
```

## Initial rules

- PG001: `DROP TABLE` (critical)
- PG002: `ALTER TABLE DROP COLUMN`
- PG003: new `NOT NULL` column without default
- PG004: `ALTER COLUMN TYPE` rewrite/lock risk
- PG005: `CREATE INDEX` without `CONCURRENTLY`
- PG006: `CREATE INDEX CONCURRENTLY` requires non-transactional migration
- PG007/PG008: unconditional `DELETE` / `UPDATE`

The lexer respects single/double quoted text, comments and dollar-quoted bodies for statement splitting. Rules are **heuristic**: they may miss hazards in advanced SQL and produce false positives in procedural statements, complex ALTER TABLE clauses or unconventional dialects. Inspect findings, don't auto-edit SQL.

## Roadmap

See [architecture](docs/ARCHITECTURE.md) and [roadmap](docs/ROADMAP.md).

This repository was created as `migragationguard` on GitHub; public artifact coordinates use the correctly spelled `migrationguard`. GPL-3.0 (LICENSE).
