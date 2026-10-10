package com.pipeline;

import com.pipeline.Models.CommandResult;
import com.pipeline.Models.Reason;
import com.pipeline.Models.State;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandsTest {

    private void assertOk(CommandResult result, String expectedMessage) {
        assertTrue(result.ok, () -> "expected ok with \"" + expectedMessage + "\", got ok=" + result.ok + " message=\"" + result.message + "\"");
        assertEquals(expectedMessage, result.message);
    }

    private void assertError(CommandResult result, Reason expectedReason) {
        assertFalse(result.ok, () -> "expected error " + expectedReason + ", got ok=" + result.ok + " reason=" + result.reason);
        assertEquals(expectedReason, result.reason);
    }

    private void assertPipelineJson(State state, String expectedJson) {
        String got = FileIo.pipelineToJson(state);
        assertEquals(expectedJson, got, "pipeline state mismatch");
    }

    /// Build a State from a data/pipeline.json document.
    private State makeState(String pipelineDoc) {
        return Parser.parsePipelineJson(pipelineDoc);
    }

    /// start appends a running run at the first stage with no results, reports an id that is
    /// already on file whether that run is still going, has passed, or has failed, and treats
    /// a shorter id as its own run.
    @Test
    void test_start_opens_a_run_at_the_first_stage() {
        State state = makeState("""
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R42","stage":"test","status":"failed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"fail"}]},
    {"run":"R410","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"pass"}]}
  ]
}""");

        CommandResult opened = Commands.start(state, "R41");           // a new id -> running at the first stage
        CommandResult shorter = Commands.start(state, "R4");           // R410 only begins with this id
        CommandResult whileRunning = Commands.start(state, "R41");    // the same id while that run is still going
        CommandResult alreadyPassed = Commands.start(state, "R410");  // the same id as the run that has passed
        CommandResult alreadyFailed = Commands.start(state, "R42");   // the same id as the run that failed

        assertOk(opened, "R41 started: build");
        assertOk(shorter, "R4 started: build");
        assertError(whileRunning, Reason.RUN_EXISTS);
        assertError(alreadyPassed, Reason.RUN_EXISTS);
        assertError(alreadyFailed, Reason.RUN_EXISTS);

        assertPipelineJson(state, """
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R42","stage":"test","status":"failed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"fail"}]},
    {"run":"R410","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"pass"}]},
    {"run":"R41","stage":"build","status":"running","results":[]},
    {"run":"R4","stage":"build","status":"running","results":[]}
  ]
}""");
    }

    /// A passed stage is added after the results already on file and the run moves on to the
    /// stage that follows it, leaving every other run where it was.
    @Test
    void test_stage_pass_moves_the_run_to_the_next_stage() {
        State state = makeState("""
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R43","stage":"test","status":"running","results":[{"stage":"build","result":"fail"},{"stage":"build","result":"pass"}]},
    {"run":"R44","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"fail"},{"stage":"deploy","result":"fail"}]},
    {"run":"R46","stage":"build","status":"running","results":[]}
  ]
}""");

        CommandResult afterAFailure = Commands.stage(state, "R43", "pass");  // build took two attempts, test passes first time
        CommandResult nothingOnFile = Commands.stage(state, "R46", "pass");  // a run with no results yet

        assertOk(afterAFailure, "R43: test passed, now deploy");
        assertOk(nothingOnFile, "R46: build passed, now test");

        assertPipelineJson(state, """
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R43","stage":"deploy","status":"running","results":[{"stage":"build","result":"fail"},{"stage":"build","result":"pass"},{"stage":"test","result":"pass"}]},
    {"run":"R44","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"fail"},{"stage":"deploy","result":"fail"}]},
    {"run":"R46","stage":"test","status":"running","results":[{"stage":"build","result":"pass"}]}
  ]
}""");
    }

    /// A pass on the last stage reports the run passed, keeps the run on that stage, and the
    /// finished run takes no further results.
    @Test
    void test_stage_pass_on_the_last_stage_finishes_the_run() {
        State state = makeState("""
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R44","stage":"test","status":"running","results":[{"stage":"build","result":"pass"}]},
    {"run":"R47","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"}]}
  ]
}""");

        CommandResult ontoTheLast = Commands.stage(state, "R44", "pass");  // test passes -> moves on to the last stage
        CommandResult finishes = Commands.stage(state, "R47", "pass");       // the last stage passes
        CommandResult afterwards = Commands.stage(state, "R47", "pass");     // the run is finished, not running

        assertOk(ontoTheLast, "R44: test passed, now deploy");
        assertOk(finishes, "R47: deploy passed, run passed");
        assertError(afterwards, Reason.NOT_RUNNING);

        assertPipelineJson(state, """
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R44","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"}]},
    {"run":"R47","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"pass"}]}
  ]
}""");
    }

    /// A failed stage is recorded, the run is reported failed and stays on that stage, and it
    /// takes no further results until someone retries it.
    @Test
    void test_stage_fail_stops_the_run() {
        State state = makeState("""
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R43","stage":"test","status":"running","results":[{"stage":"build","result":"pass"}]},
    {"run":"R44","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"}]}
  ]
}""");

        CommandResult failed = Commands.stage(state, "R43", "fail");      // test fails -> the run stops here
        CommandResult afterwards = Commands.stage(state, "R43", "pass");  // a stopped run is not running

        assertOk(failed, "R43: test failed");
        assertError(afterwards, Reason.NOT_RUNNING);

        assertPipelineJson(state, """
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R43","stage":"test","status":"failed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"fail"}]},
    {"run":"R44","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"}]}
  ]
}""");
    }

    /// stage records only pass or fail, checks the run's status before the result it was handed,
    /// reports an id that is not on file, and leaves every run untouched on each refusal.
    @Test
    void test_stage_rejects_a_result_that_is_not_pass_or_fail() {
        State state = makeState("""
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R41","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"pass"}]},
    {"run":"R46","stage":"build","status":"running","results":[]}
  ]
}""");

        CommandResult typo = Commands.stage(state, "R46", "passed");   // a near miss for pass
        CommandResult shorthand = Commands.stage(state, "R46", "ok");  // not a result the pipeline records
        CommandResult blank = Commands.stage(state, "R46", "");        // no result given at all
        CommandResult shouted = Commands.stage(state, "R46", "PASS");  // right length, wrong case: still not a result

        assertError(typo, Reason.INVALID_RESULT);
        assertError(shorthand, Reason.INVALID_RESULT);
        assertError(blank, Reason.INVALID_RESULT);
        assertError(shouted, Reason.INVALID_RESULT);

        assertPipelineJson(state, """
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R41","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"pass"}]},
    {"run":"R46","stage":"build","status":"running","results":[]}
  ]
}""");

        CommandResult stopped = Commands.stage(state, "R41", "passed");  // the status is checked before the result
        CommandResult unknown = Commands.stage(state, "R4", "pass");      // no run has this id

        assertError(stopped, Reason.NOT_RUNNING);
        assertError(unknown, Reason.RUN_NOT_FOUND);

        assertPipelineJson(state, """
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R41","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"pass"}]},
    {"run":"R46","stage":"build","status":"running","results":[]}
  ]
}""");
    }

    /// retry puts a failed run back on the stage it failed with every earlier result kept,
    /// refuses a run that is not failed or not on file, and works again after a second failure.
    @Test
    void test_retry_runs_the_failed_stage_again() {
        State state = makeState("""
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R45","stage":"deploy","status":"failed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"fail"}]},
    {"run":"R46","stage":"build","status":"running","results":[]},
    {"run":"R51","stage":"test","status":"failed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"fail"},{"stage":"test","result":"fail"}]}
  ]
}""");

        CommandResult retried = Commands.retry(state, "R45");               // the failed deploy runs again
        CommandResult whileRunning = Commands.retry(state, "R45");         // it is running now, nothing to retry
        CommandResult neverFailed = Commands.retry(state, "R46");          // still working through its first stage
        CommandResult unknown = Commands.retry(state, "R99");               // no run has this id
        CommandResult retriedAgain = Commands.retry(state, "R51");         // test has failed twice -> it runs again
        CommandResult finallyPassed = Commands.stage(state, "R45", "pass");  // the retried deploy finally works
        CommandResult afterPassing = Commands.retry(state, "R45");         // a passed run has nothing to retry

        assertOk(retried, "R45: retrying deploy");
        assertError(whileRunning, Reason.NOT_FAILED);
        assertError(neverFailed, Reason.NOT_FAILED);
        assertError(unknown, Reason.RUN_NOT_FOUND);
        assertOk(retriedAgain, "R51: retrying test");
        assertOk(finallyPassed, "R45: deploy passed, run passed");
        assertError(afterPassing, Reason.NOT_FAILED);

        assertPipelineJson(state, """
{
  "stages": ["build","test","deploy"],
  "runs": [
    {"run":"R45","stage":"deploy","status":"passed","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"fail"},{"stage":"deploy","result":"pass"}]},
    {"run":"R46","stage":"build","status":"running","results":[]},
    {"run":"R51","stage":"test","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"fail"},{"stage":"test","result":"fail"}]}
  ]
}""");
    }
}
