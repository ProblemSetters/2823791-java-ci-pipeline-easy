# C++: CI Pipeline

Every code push starts a pipeline run that goes through build, test, and deploy, in that order. A run moves to the next stage only when the current stage passes, and a failed stage stops the run until someone retries it, so nothing is deployed unless every earlier stage has passed. If a failed stage does not stop the run, a retry continues from the wrong stage, or a finished run can be started again, broken code can reach production and the run history no longer shows what really passed.

Your task is to fix the CI pipeline tool so that every command updates the data correctly and prints the correct success message or error reason.

## State File

**File: `data/pipeline.json`**

```json
{
  "stages": ["build","test","deploy"],
  "runs": [
    {
      "run": "R41",
      "stage": "deploy",
      "status": "passed",
      "results": [
        {"stage":"build","result":"pass"},
        {"stage":"test","result":"pass"},
        {"stage":"deploy","result":"pass"}
      ]
    },
    {
      "run": "R42",
      "stage": "test",
      "status": "failed",
      "results": [
        {"stage":"build","result":"pass"},
        {"stage":"test","result":"fail"}
      ]
    }
  ]
}
```

Fields:

- `stages` (list of strings): the stages every run goes through, in order.
- `runs` (list): every run, in the order started.
  - `run` (string): the run id, unique.
  - `stage` (string): the stage the run is at.
  - `status` (string): `running`, `failed`, or `passed`.
  - `results` (list): every stage result so far, oldest first, with `stage` and `result` (`pass` or `fail`).

When writing `data/pipeline.json` back, preserve the field order shown above, and keep runs and results in the order they appear in the file.

## Command Output

Each command prints a one-line result on success, or `Error: <REASON>` on failure. Its rules are checked in the order listed, stopping at the first failure, and on failure `data/pipeline.json` is not modified.

## Commands

### `start`

Starts a new run at the first stage.

**Usage**

```bash
./pipeline start <run>
```

**Output**

```text
<run> started: <stage>
```

**Rules**

- If a run with that id exists, fail with `RUN_EXISTS`.

On success, append the run as `running` at the first stage with no results.

### `stage`

Records whether the run's current stage passed or failed.

**Usage**

```bash
./pipeline stage <run> <pass|fail>
```

**Output**

```text
<run>: <stage> passed, now <next_stage>
<run>: <stage> passed, run passed
<run>: <stage> failed
```

**Rules**

Check these rules in order and stop at the first failure:

- If no run has that id, fail with `RUN_NOT_FOUND`.
- If the run is not `running`, fail with `NOT_RUNNING`.
- If the result is not `pass` or `fail`, fail with `INVALID_RESULT`.

On success:

- Append the result to the run.
- A pass moves the run to the next stage, or sets `passed` after the last stage.
- A fail sets `failed` and the run stays at that stage.

### `retry`

Restarts a failed run at the stage that failed.

**Usage**

```bash
./pipeline retry <run>
```

**Output**

```text
<run>: retrying <stage>
```

**Rules**

Check these rules in order and stop at the first failure:

- If no run has that id, fail with `RUN_NOT_FOUND`.
- If the run is not `failed`, fail with `NOT_FAILED`.

On success, set `running` at the same stage; earlier results are kept.

> **Note:** Refer to `README.md` for more details and examples.
