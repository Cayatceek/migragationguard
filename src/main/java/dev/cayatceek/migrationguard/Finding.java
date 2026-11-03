package dev.cayatceek.migrationguard;

import java.util.Objects;

public record Finding(String file, int line, Severity severity,
                      String ruleId, String message, String suggestion) {
    public Finding {
        Objects.requireNonNull(file, "file");
        if (line < 1) throw new IllegalArgumentException("line must be positive");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(suggestion, "suggestion");
    }
}
