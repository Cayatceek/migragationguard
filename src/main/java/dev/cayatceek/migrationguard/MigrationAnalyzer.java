package dev.cayatceek.migrationguard;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Static, heuristic PostgreSQL migration linter. Findings are risk signals,
 * not a guarantee that a statement will lock/fail on a particular DB instance.
 */
public final class MigrationAnalyzer {
    private static final Pattern DROP_TABLE = Pattern.compile("\\bDROP\\s+TABLE\\b");
    private static final Pattern DROP_COLUMN = Pattern.compile("\\bALTER\\s+TABLE\\b[\\s\\S]*?\\bDROP\\s+COLUMN\\b");
    private static final Pattern ADD_REQUIRED = Pattern.compile("\\bALTER\\s+TABLE\\b[\\s\\S]*?\\bADD\\s+COLUMN\\b[\\s\\S]*?\\bNOT\\s+NULL\\b");
    private static final Pattern HAS_DEFAULT = Pattern.compile("\\bDEFAULT\\b");
    private static final Pattern CHANGE_TYPE = Pattern.compile("\\bALTER\\s+TABLE\\b[\\s\\S]*?\\bALTER\\s+COLUMN\\b[\\s\\S]*?\\bTYPE\\b");
    private static final Pattern CREATE_INDEX = Pattern.compile("\\bCREATE\\s+(?:UNIQUE\\s+)?INDEX\\b");
    private static final Pattern CONCURRENTLY = Pattern.compile("\\bCONCURRENTLY\\b");
    private static final Pattern DELETE = Pattern.compile("^\\s*DELETE\\s+FROM\\b");
    private static final Pattern UPDATE = Pattern.compile("^\\s*UPDATE\\s+");
    private static final Pattern WHERE = Pattern.compile("\\bWHERE\\b");

    private MigrationAnalyzer() { }

    public static List<Finding> analyze(String file, String sql) {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(sql, "sql");
        List<Finding> findings = new ArrayList<>();
        for (SqlStatement statement : SqlStatementSplitter.split(sql)) {
            String clean = statement.normalized().toUpperCase(Locale.ROOT);
            int line = statement.startLine();
            if (DROP_TABLE.matcher(clean).find()) {
                add(findings, file, line, Severity.CRITICAL, "PG001", "DROP TABLE may destroy production data",
                    "Require a reviewed backup and a staged expand/contract migration.");
            }
            if (DROP_COLUMN.matcher(clean).find()) {
                add(findings, file, line, Severity.HIGH, "PG002", "Dropping a column can break older application versions",
                    "Deprecate reads/writes first; drop only after all clients are upgraded.");
            }
            if (ADD_REQUIRED.matcher(clean).find() && !HAS_DEFAULT.matcher(clean).find()) {
                add(findings, file, line, Severity.HIGH, "PG003", "New NOT NULL column without DEFAULT may fail on populated tables",
                    "Add nullable column, backfill rows in batches, then enforce NOT NULL.");
            }
            if (CHANGE_TYPE.matcher(clean).find()) {
                add(findings, file, line, Severity.HIGH, "PG004", "ALTER COLUMN TYPE may rewrite data and hold strong locks",
                    "Measure on representative data and evaluate an expand/contract approach.");
            }
            if (CREATE_INDEX.matcher(clean).find()) {
                if (CONCURRENTLY.matcher(clean).find()) {
                    add(findings, file, line, Severity.MEDIUM, "PG006",
                        "CREATE INDEX CONCURRENTLY cannot run inside a transaction block",
                        "Mark this migration non-transactional in your migration tool.");
                } else {
                    add(findings, file, line, Severity.MEDIUM, "PG005",
                        "Non-concurrent CREATE INDEX can block writes",
                        "Use CREATE INDEX CONCURRENTLY where appropriate.");
                }
            }
            if (DELETE.matcher(clean).find() && !WHERE.matcher(clean).find()) {
                add(findings, file, line, Severity.HIGH, "PG007", "DELETE without WHERE affects all matching rows",
                    "Verify this is intentional and constrain the operation if not.");
            }
            if (UPDATE.matcher(clean).find() && !WHERE.matcher(clean).find()) {
                add(findings, file, line, Severity.HIGH, "PG008", "UPDATE without WHERE affects all rows",
                    "Add an explicit predicate or document the full-table update.");
            }
        }
        return List.copyOf(findings);
    }

    private static void add(List<Finding> items, String file, int line, Severity severity,
                            String id, String message, String suggestion) {
        items.add(new Finding(file, line, severity, id, message, suggestion));
    }
}
