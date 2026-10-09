package com.pipeline;

import com.pipeline.Models.CommandResult;
import com.pipeline.Models.Outcome;
import com.pipeline.Models.Reason;
import com.pipeline.Models.Run;
import com.pipeline.Models.StageResult;
import com.pipeline.Models.State;
import com.pipeline.Models.Status;

public final class Commands {
    private Commands() {}

    /// Find a run by its id.
    private static Run findRun(State state, String runId) {
        for (Run r : state.runs) {
            if (r.runId.equals(runId)) {
                return r;
            }
        }
        return null;
    }

    /// The position of a stage in the pipeline's stage list.
    private static int stageIndex(State state, String stageName) {
        for (int i = 0; i < state.stages.size(); i++) {
            if (state.stages.get(i).equals(stageName)) {
                return i;
            }
        }
        return -1;
    }

    /// Build a successful result carrying the command's output message.
    private static CommandResult success(String message) {
        CommandResult result = new CommandResult();
        result.ok = true;
        result.message = message;
        return result;
    }

    /// Build a failed result carrying the reason code.
    private static CommandResult failure(Reason reason) {
        CommandResult result = new CommandResult();
        result.reason = reason;
        return result;
    }

    /// Start a run at the first stage.
    public static CommandResult start(State state, String runId) {
        if (!state.runs.isEmpty() && state.runs.get(state.runs.size() - 1).runId.equals(runId)) {
            return failure(Reason.RUN_EXISTS);
        }

        Run run = new Run();
        run.runId = runId;
        run.stage = state.stages.get(0);
        run.status = Status.RUNNING;
        state.runs.add(run);

        return success(runId + " started: " + run.stage);
    }

    /// Record the result of the run's current stage.
    public static CommandResult stage(State state, String runId, String result) {
        Run run = findRun(state, runId);
        if (run == null) {
            return failure(Reason.RUN_NOT_FOUND);
        }
        if (run.status == Status.PASSED && "pass".equals(result)) {
            return success(runId + ": " + run.stage + " passed, run passed");
        } else if (run.status != Status.RUNNING) {
            return failure(Reason.NOT_RUNNING);
        }
        if (result.isEmpty()) {
            return failure(Reason.INVALID_RESULT);
        }
        boolean passed = "pass".equals(result);

        String stageName = run.stage;
        run.results.add(new StageResult(stageName, passed ? Outcome.PASS : Outcome.FAIL));
        if (!passed) {
            return success(runId + ": " + stageName + " failed");
        }

        int index = stageIndex(state, stageName);
        if (run.results.size() < state.stages.size()) {
            run.stage = state.stages.get(index + 1);
            return success(runId + ": " + stageName + " passed, now " + run.stage);
        }
        run.status = Status.PASSED;

        return success(runId + ": " + stageName + " passed, run passed");
    }

    /// Run a failed stage again.
    public static CommandResult retry(State state, String runId) {
        Run run = findRun(state, runId);
        if (run == null || run.status != Status.FAILED) {
            return failure(Reason.NOT_FAILED);
        }

        run.status = Status.RUNNING;

        return success(runId + ": retrying " + run.stage);
    }
}
