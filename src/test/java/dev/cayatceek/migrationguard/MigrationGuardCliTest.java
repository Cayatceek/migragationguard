package dev.cayatceek.migrationguard;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class MigrationGuardCliTest {
    @TempDir Path dir;

    @Test
    void reportsRiskAndNonzeroExit() throws Exception {
        Path file = Files.writeString(dir.resolve("V1.sql"), "DROP TABLE users;");
        var out = new ByteArrayOutputStream();
        var err = new ByteArrayOutputStream();
        int status = MigrationGuardCli.run(new String[] {file.toString()},
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8));
        assertEquals(2, status);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("PG001"));
        assertEquals("", err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void cleanSqlAndInvalidArguments() throws Exception {
        Path file = Files.writeString(dir.resolve("V2.sql"), "SELECT 1;");
        var out = new PrintStream(new ByteArrayOutputStream());
        var err = new PrintStream(new ByteArrayOutputStream());
        assertEquals(0, MigrationGuardCli.run(new String[] {file.toString()}, out, err));
        assertEquals(1, MigrationGuardCli.run(new String[0], out, err));
    }
}
