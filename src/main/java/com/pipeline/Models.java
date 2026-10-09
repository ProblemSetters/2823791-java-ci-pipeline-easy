package com.pipeline;

import java.util.ArrayList;
import java.util.List;

public final class Models {
    private Models() {}

    public enum Status {
        RUNNING("running"),
        FAILED("failed"),
        PASSED("passed");

        private final String text;

        Status(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static Status parseStatus(String text) {
            if ("running".equals(text)) return RUNNING;
            if ("failed".equals(text)) return FAILED;
            if ("passed".equals(text)) return PASSED;
            throw new IllegalArgumentException("unknown run status: " + text);
        }
    }

    public enum Outcome {
        PASS("pass"),
        FAIL("fail");

        private final String text;

        Outcome(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }

        public static Outcome parseOutcome(String text) {
            if ("pass".equals(text)) return PASS;
            if ("fail".equals(text)) return FAIL;
            throw new IllegalArgumentException("unknown stage result: " + text);
        }
    }

    public static class StageResult {
        public String stage;
        public Outcome result = Outcome.PASS;

        public StageResult() {}

        public StageResult(String stage, Outcome result) {
            this.stage = stage;
            this.result = result;
        }
    }

    public static class Run {
        public String runId;
        public String stage;
        public Status status = Status.RUNNING;
        public List<StageResult> results = new ArrayList<>();

        public Run() {}

        public Run(String runId, String stage, Status status, List<StageResult> results) {
            this.runId = runId;
            this.stage = stage;
            this.status = status;
            this.results = results != null ? results : new ArrayList<>();
        }
    }

    public static class State {
        public List<String> stages = new ArrayList<>();
        public List<Run> runs = new ArrayList<>();

        public State() {}

        public State(List<String> stages, List<Run> runs) {
            this.stages = stages != null ? stages : new ArrayList<>();
            this.runs = runs != null ? runs : new ArrayList<>();
        }
    }

    public enum Reason {
        NONE,
        RUN_EXISTS,
        RUN_NOT_FOUND,
        NOT_RUNNING,
        INVALID_RESULT,
        NOT_FAILED;

        @Override
        public String toString() {
            if (this == NONE) return "";
            return name();
        }
    }

    public static class CommandResult {
        public boolean ok = false;
        public String message = "";
        public Reason reason = Reason.NONE;

        public CommandResult() {}

        public CommandResult(boolean ok, String message, Reason reason) {
            this.ok = ok;
            this.message = message != null ? message : "";
            this.reason = reason != null ? reason : Reason.NONE;
        }
    }
}
