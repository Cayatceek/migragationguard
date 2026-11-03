package dev.cayatceek.migrationguard;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MigrationAnalyzerTest {
    @Test
    void detectsDataLossAndAvailabilityHazards() {
        String sql = """
            ALTER TABLE customers ADD COLUMN country VARCHAR(2) NOT NULL;
            ALTER TABLE customers DROP COLUMN legacy;
            CREATE INDEX ix_customers_name ON customers(name);
            ALTER TABLE customers ALTER COLUMN balance TYPE NUMERIC(20,2);
            DELETE FROM customers;
            UPDATE customers SET active = false;
            DROP TABLE legacy_accounts;
            """;
        var findings = MigrationAnalyzer.analyze("V1.sql", sql);
        Set<String> ids = findings.stream().map(Finding::ruleId).collect(Collectors.toSet());
        assertEquals(Set.of("PG001", "PG002", "PG003", "PG004", "PG005", "PG007", "PG008"), ids);
        assertEquals(1, findings.getFirst().line());
    }

    @Test
    void detectsConcurrentIndexTransactionConstraint() {
        var findings = MigrationAnalyzer.analyze("V2.sql",
            "CREATE UNIQUE INDEX CONCURRENTLY idx_name ON users(name);");
        assertEquals("PG006", findings.getFirst().ruleId());
    }

    @Test
    void avoidsMatchingSqlTextInCommentsAndQuotedLiterals() {
        String sql = """
            -- DROP TABLE accounts;
            INSERT INTO audit(message) VALUES ('DROP TABLE accounts;');
            UPDATE accounts SET status = 'ok' WHERE id = 1;
            ALTER TABLE accounts ADD COLUMN flag BOOLEAN NOT NULL DEFAULT false;
            """;
        assertTrue(MigrationAnalyzer.analyze("V3.sql", sql).isEmpty());
    }

    @Test
    void givesActionableWarningsWithLocations() {
        var findings = MigrationAnalyzer.analyze("V4.sql", "SELECT 1;\nDROP TABLE users;");
        assertEquals(2, findings.getFirst().line());
        assertEquals(Severity.CRITICAL, findings.getFirst().severity());
        assertFalse(findings.getFirst().suggestion().isBlank());
    }
}
