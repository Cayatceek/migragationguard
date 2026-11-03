package dev.cayatceek.migrationguard;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Offline CLI. Exit code 2 signals HIGH/CRITICAL risks; 1 signals bad invocation/I/O. */
public final class MigrationGuardCli {
    private MigrationGuardCli() { }

    public static void main(String[] args) {
        int result = run(args, System.out, System.err);
        if (result != 0) System.exit(result);
    }

    public static int run(String[] args, PrintStream out, PrintStream err) {
        if (args == null || args.length == 0) {
            err.println("Usage: java -jar migrationguard.jar <migration.sql> [other.sql ...]");
            return 1;
        }
        List<Finding> findings = new ArrayList<>();
        for (String argument : args) {
            Path path = Path.of(argument);
            try {
                String sql = Files.readString(path, StandardCharsets.UTF_8);
                findings.addAll(MigrationAnalyzer.analyze(path.toString(), sql));
            } catch (IOException | SecurityException ex) {
                err.println("Cannot read " + path + ": " + ex.getClass().getSimpleName());
                return 1;
            }
        }
        if (findings.isEmpty()) {
            out.println("MigrationGuard: no known risky patterns found (not a correctness guarantee).");
            return 0;
        }
        for (Finding finding : findings) {
            out.printf("%s:%d [%s] %s: %s%n", finding.file(), finding.line(),
                finding.severity(), finding.ruleId(), finding.message());
            out.println("  Advice: " + finding.suggestion());
        }
        boolean dangerous = findings.stream().anyMatch(
            f -> f.severity() == Severity.HIGH || f.severity() == Severity.CRITICAL);
        return dangerous ? 2 : 0;
    }
}
