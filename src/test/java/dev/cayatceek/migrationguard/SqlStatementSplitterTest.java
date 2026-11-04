package dev.cayatceek.migrationguard;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SqlStatementSplitterTest {
    @Test
    void ignoresSemicolonsInsideQuotesCommentsAndDollarBodies() {
        String sql = """
            -- comment; ignored
            CREATE TABLE accounts (id int, label text DEFAULT 'some; text');
            /* a; block */
            DO $$ BEGIN RAISE NOTICE 'hello;'; END $$;
            DROP TABLE accounts;
            """;
        var parts = SqlStatementSplitter.split(sql);
        assertEquals(3, parts.size());
        assertEquals(2, parts.getFirst().startLine());
        assertTrue(parts.get(2).normalized().contains("DROP TABLE"));
        assertFalse(parts.getFirst().normalized().contains("some; text"));
    }

    @Test
    void supportsPostgresqlNestedBlockComments() {
        String sql = "/* outer /* DROP TABLE accounts; */ DROP TABLE users; */ SELECT 1;";
        var parts = SqlStatementSplitter.split(sql);
        assertEquals(1, parts.size());
        assertEquals("SELECT 1", parts.getFirst().normalized().trim());
        assertTrue(MigrationAnalyzer.analyze("V1.sql", sql).isEmpty());
    }

    @Test
    void handlesEscapedQuotesAndNestedLineBreaks() {
        var parts = SqlStatementSplitter.split("SELECT 'it''s; here';\nDELETE FROM x;");
        assertEquals(2, parts.size());
        assertEquals(2, parts.get(1).startLine());
    }
}
