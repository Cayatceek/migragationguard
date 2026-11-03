package dev.cayatceek.migrationguard;

import java.util.Objects;

/** Lexically separated SQL with content excluded from strings/comments for safer rule matching. */
public record SqlStatement(String original, String normalized, int startLine) {
    public SqlStatement {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(normalized, "normalized");
        if (startLine < 1) throw new IllegalArgumentException("startLine must be positive");
    }
}
