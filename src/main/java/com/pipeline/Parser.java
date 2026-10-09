package com.pipeline;

import com.pipeline.Models.Outcome;
import com.pipeline.Models.Run;
import com.pipeline.Models.StageResult;
import com.pipeline.Models.State;
import com.pipeline.Models.Status;

import java.util.function.Consumer;

public final class Parser {
    private Parser() {}

    private static class JsonParser {
        private final String json;
        private int pos;

        public JsonParser(String json) {
            this.json = json != null ? json : "";
            this.pos = 0;
        }

        /// Parse the CI Pipeline state document into the in-memory State.
        public State parsePipelineDoc() {
            State state = new State();
            skipWs();
            expect('{');
            while (true) {
                skipWs();
                if (peek() == '}') {
                    pos++;
                    break;
                }
                String key = parseString();
                skipWs();
                expect(':');
                skipWs();
                if ("stages".equals(key)) {
                    parseArray(() -> state.stages.add(parseString()));
                } else if ("runs".equals(key)) {
                    parseArray(() -> state.runs.add(parseRunObject()));
                }
                skipWs();
                if (peek() == ',') {
                    pos++;
                }
            }
            return state;
        }

        /// Run parseElement once per item in a JSON array.
        private void parseArray(Runnable parseElement) {
            expect('[');
            skipWs();
            if (peek() == ']') {
                pos++;
                return;
            }
            while (true) {
                parseElement.run();
                skipWs();
                if (peek() == ',') {
                    pos++;
                    skipWs();
                    continue;
                }
                expect(']');
                break;
            }
        }

        /// Parse a single run object.
        private Run parseRunObject() {
            Run run = new Run();
            skipWs();
            expect('{');
            while (true) {
                skipWs();
                if (peek() == '}') {
                    pos++;
                    break;
                }
                String key = parseString();
                skipWs();
                expect(':');
                skipWs();
                if ("run".equals(key)) {
                    run.runId = parseString();
                } else if ("stage".equals(key)) {
                    run.stage = parseString();
                } else if ("status".equals(key)) {
                    run.status = Status.parseStatus(parseString());
                } else if ("results".equals(key)) {
                    parseArray(() -> run.results.add(parseResultObject()));
                }
                skipWs();
                if (peek() == ',') {
                    pos++;
                }
            }
            return run;
        }

        /// Parse a single stage result object.
        private StageResult parseResultObject() {
            StageResult result = new StageResult();
            skipWs();
            expect('{');
            while (true) {
                skipWs();
                if (peek() == '}') {
                    pos++;
                    break;
                }
                String key = parseString();
                skipWs();
                expect(':');
                skipWs();
                if ("stage".equals(key)) {
                    result.stage = parseString();
                } else if ("result".equals(key)) {
                    result.result = Outcome.parseOutcome(parseString());
                }
                skipWs();
                if (peek() == ',') {
                    pos++;
                }
            }
            return result;
        }

        private char peek() {
            return pos < json.length() ? json.charAt(pos) : '\0';
        }

        private void expect(char c) {
            if (peek() != c) {
                throw new RuntimeException("JSON parse error: expected '" + c + "' at position " + pos);
            }
            pos++;
        }

        private void skipWs() {
            while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
                pos++;
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (pos < json.length() && json.charAt(pos) != '"') {
                if (json.charAt(pos) == '\\' && pos + 1 < json.length()) {
                    pos++;
                    char escaped = json.charAt(pos++);
                    switch (escaped) {
                        case '"':  value.append('"');  break;
                        case '\\': value.append('\\'); break;
                        case '/':  value.append('/');  break;
                        case 'n':  value.append('\n'); break;
                        case 'r':  value.append('\r'); break;
                        case 't':  value.append('\t'); break;
                        default:   value.append(escaped);
                    }
                } else {
                    value.append(json.charAt(pos++));
                }
            }
            expect('"');
            return value.toString();
        }
    }

    /// Parse the CI Pipeline state JSON into the in-memory State.
    public static State parsePipelineJson(String doc) {
        JsonParser parser = new JsonParser(doc);
        return parser.parsePipelineDoc();
    }
}
