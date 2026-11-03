package dev.cayatceek.migrationguard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lightweight SQL lexer that protects single/double quotes, line/block comments
 * and PostgreSQL dollar-quoted bodies against false semicolon splitting.
 * It is NOT a complete PostgreSQL grammar parser.
 */
public final class SqlStatementSplitter {
    private SqlStatementSplitter() { }

    private enum State { NORMAL, SINGLE, DOUBLE, LINE_COMMENT, BLOCK_COMMENT, DOLLAR }

    public static List<SqlStatement> split(String sql) {
        Objects.requireNonNull(sql, "sql");
        List<SqlStatement> output = new ArrayList<>();
        StringBuilder original = new StringBuilder();
        StringBuilder normalized = new StringBuilder();
        int line = 1;
        int startLine = -1;
        State state = State.NORMAL;
        String dollarTag = null;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (startLine < 0 && !Character.isWhitespace(c) && c != ';') startLine = line;

            if (state == State.NORMAL) {
                if (c == ';') {
                    flush(output, original, normalized, startLine < 0 ? line : startLine);
                    startLine = -1;
                    if (c == '\n') line++;
                    continue;
                }
                if (c == '-' && next == '-') {
                    state = State.LINE_COMMENT;
                } else if (c == '/' && next == '*') {
                    state = State.BLOCK_COMMENT;
                } else if (c == '\'') {
                    state = State.SINGLE;
                } else if (c == '"') {
                    state = State.DOUBLE;
                } else if (c == '$') {
                    String found = dollarTagAt(sql, i);
                    if (found != null) {
                        state = State.DOLLAR;
                        dollarTag = found;
                    }
                }
                original.append(c);
                normalized.append(state == State.NORMAL ? c : ' ');
            } else {
                original.append(c);
                normalized.append(c == '\n' ? '\n' : ' ');
                if (state == State.LINE_COMMENT && c == '\n') {
                    state = State.NORMAL;
                } else if (state == State.BLOCK_COMMENT && c == '*' && next == '/') {
                    original.append(next);
                    normalized.append(' ');
                    i++;
                    state = State.NORMAL;
                } else if (state == State.SINGLE && c == '\'') {
                    if (next == '\'') {
                        original.append(next);
                        normalized.append(' ');
                        i++;
                    } else state = State.NORMAL;
                } else if (state == State.DOUBLE && c == '"') {
                    if (next == '"') {
                        original.append(next);
                        normalized.append(' ');
                        i++;
                    } else state = State.NORMAL;
                } else if (state == State.DOLLAR && dollarTag != null && sql.startsWith(dollarTag, i)) {
                    // The opening '$' was already copied; consume the remainder of the closing delimiter.
                    for (int j = 1; j < dollarTag.length(); j++) {
                        original.append(sql.charAt(++i));
                        normalized.append(' ');
                    }
                    dollarTag = null;
                    state = State.NORMAL;
                }
            }
            if (c == '\n') line++;
        }
        flush(output, original, normalized, startLine < 0 ? line : startLine);
        return List.copyOf(output);
    }

    private static String dollarTagAt(String text, int from) {
        int end = text.indexOf('$', from + 1);
        if (end < 0) return null;
        String inner = text.substring(from + 1, end);
        if (inner.isEmpty() || inner.matches("[A-Za-z_][A-Za-z_0-9]*")) {
            return text.substring(from, end + 1);
        }
        return null;
    }

    private static void flush(List<SqlStatement> result, StringBuilder original,
                              StringBuilder normalized, int line) {
        if (!normalized.toString().isBlank()) {
            result.add(new SqlStatement(original.toString().trim(), normalized.toString().trim(), line));
        }
        original.setLength(0);
        normalized.setLength(0);
    }
}
