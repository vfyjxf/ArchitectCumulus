package dev.vfyjxf.cumulus.impl.codec;

import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticEdge;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticGraph;
import dev.vfyjxf.cumulus.api.diagnostic.DiagnosticNode;
import dev.vfyjxf.cumulus.api.diagnostic.Severity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RuntimeAssetJsonLoader {
    public RuntimeAssetLoadResult load(List<RuntimeAssetJsonSource> sources) {
        if (sources == null) {
            throw new IllegalArgumentException("Runtime asset JSON sources must not be null");
        }
        var parsed = new ArrayList<RuntimeAssetSource>();
        var diagnostics = new ArrayList<DiagnosticNode>();
        var edges = new ArrayList<DiagnosticEdge>();
        for (var source : sources) {
            try {
                var value = new Parser(source.content()).parse();
                if (!(value instanceof Map<?, ?> raw)) {
                    addInvalidJson(diagnostics, edges, source.resourcePath());
                    continue;
                }
                parsed.add(new RuntimeAssetSource(source.resourcePath(), toObjectMap(raw)));
            } catch (IllegalArgumentException ex) {
                addInvalidJson(diagnostics, edges, source.resourcePath());
            }
        }
        if (!diagnostics.isEmpty()) {
            return new RuntimeAssetLoadResult(
                    List.of(),
                    new DiagnosticGraph("root", diagnostics, edges));
        }
        return new RuntimeAssetLoader().load(parsed);
    }

    private static Map<String, Object> toObjectMap(Map<?, ?> raw) {
        var values = new LinkedHashMap<String, Object>();
        raw.forEach((key, value) -> values.put(String.valueOf(key), value));
        return values;
    }

    private static void addInvalidJson(
            List<DiagnosticNode> diagnostics,
            List<DiagnosticEdge> edges,
            String sourcePath) {
        if (diagnostics.isEmpty()) {
            diagnostics.add(new DiagnosticNode(
                    "root",
                    Severity.info,
                    "Runtime asset JSON loading failed",
                    "diagnostic.cumulus.asset_json_load_failed",
                    List.of(),
                    "runtime/assets/json",
                    List.of()));
        }
        diagnostics.add(new DiagnosticNode(
                "INVALID_JSON",
                Severity.error,
                "Invalid runtime asset JSON",
                "diagnostic.cumulus.invalid_json",
                List.of(),
                sourcePath,
                List.of()));
        edges.add(new DiagnosticEdge("root", "INVALID_JSON", "caused_by"));
    }

    private static final class Parser {
        private final String input;
        private int cursor;

        private Parser(String input) {
            this.input = input;
        }

        private Object parse() {
            var value = value();
            whitespace();
            if (cursor != input.length()) {
                throw new IllegalArgumentException("Trailing content");
            }
            return value;
        }

        private Object value() {
            whitespace();
            if (cursor >= input.length()) {
                throw new IllegalArgumentException("Unexpected end");
            }
            var ch = input.charAt(cursor);
            if (ch == '{') {
                return object();
            }
            if (ch == '[') {
                return array();
            }
            if (ch == '"') {
                return string();
            }
            if (ch == 't' || ch == 'f') {
                return bool();
            }
            if (ch == 'n') {
                literal("null");
                return null;
            }
            if (ch == '-' || Character.isDigit(ch)) {
                return number();
            }
            throw new IllegalArgumentException("Unexpected token");
        }

        private Map<String, Object> object() {
            expect('{');
            var values = new LinkedHashMap<String, Object>();
            whitespace();
            if (peek('}')) {
                cursor++;
                return values;
            }
            while (true) {
                var key = string();
                whitespace();
                expect(':');
                values.put(key, value());
                whitespace();
                if (peek('}')) {
                    cursor++;
                    return values;
                }
                expect(',');
            }
        }

        private List<Object> array() {
            expect('[');
            var values = new ArrayList<Object>();
            whitespace();
            if (peek(']')) {
                cursor++;
                return values;
            }
            while (true) {
                values.add(value());
                whitespace();
                if (peek(']')) {
                    cursor++;
                    return values;
                }
                expect(',');
            }
        }

        private String string() {
            expect('"');
            var value = new StringBuilder();
            while (cursor < input.length()) {
                var ch = input.charAt(cursor++);
                if (ch == '"') {
                    return value.toString();
                }
                if (ch == '\\') {
                    if (cursor >= input.length()) {
                        throw new IllegalArgumentException("Invalid escape");
                    }
                    var escaped = input.charAt(cursor++);
                    value.append(switch (escaped) {
                        case '"', '\\', '/' -> escaped;
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        default -> throw new IllegalArgumentException("Unsupported escape");
                    });
                } else {
                    value.append(ch);
                }
            }
            throw new IllegalArgumentException("Unclosed string");
        }

        private Boolean bool() {
            if (input.startsWith("true", cursor)) {
                cursor += 4;
                return true;
            }
            if (input.startsWith("false", cursor)) {
                cursor += 5;
                return false;
            }
            throw new IllegalArgumentException("Invalid boolean");
        }

        private Number number() {
            var start = cursor;
            if (peek('-')) {
                cursor++;
            }
            while (cursor < input.length() && Character.isDigit(input.charAt(cursor))) {
                cursor++;
            }
            if (cursor < input.length() && input.charAt(cursor) == '.') {
                cursor++;
                while (cursor < input.length() && Character.isDigit(input.charAt(cursor))) {
                    cursor++;
                }
                return Double.parseDouble(input.substring(start, cursor));
            }
            return Integer.parseInt(input.substring(start, cursor));
        }

        private void literal(String literal) {
            if (!input.startsWith(literal, cursor)) {
                throw new IllegalArgumentException("Invalid literal");
            }
            cursor += literal.length();
        }

        private void expect(char expected) {
            whitespace();
            if (cursor >= input.length() || input.charAt(cursor) != expected) {
                throw new IllegalArgumentException("Expected " + expected);
            }
            cursor++;
        }

        private boolean peek(char expected) {
            return cursor < input.length() && input.charAt(cursor) == expected;
        }

        private void whitespace() {
            while (cursor < input.length() && Character.isWhitespace(input.charAt(cursor))) {
                cursor++;
            }
        }
    }
}
