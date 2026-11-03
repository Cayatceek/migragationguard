# MigrationGuard design

The pipeline is an offline analyzer:
1. Lex SQL into statements without splitting inside common quoted strings, comments or PostgreSQL dollar-quoted bodies.
2. Mask strings and comments to reduce matching SQL keywords in literals.
3. Apply risk rules and attach file path + source line.
4. Emit human-readable diagnostics; fail CI on HIGH/CRITICAL severity.

This is deliberately static and dependency-free: it neither connects to production nor executes migrations. PostgreSQL-specific lock semantics should eventually be verified with controlled Testcontainers workloads. Rule coverage is limited; users must continue reviewing migration plans and backups.
