# Java: CI Pipeline

Your task is to fix the CI pipeline tool so that every command updates the data correctly and prints the correct success message or error reason. It reads the state from `data/pipeline.json`, processes the `start`, `stage` and `retry` commands, and writes the updated state back.

## Project Structure

```text
├── data/
│   └── pipeline.json           # Persistent pipeline state (stages and runs)
├── src/
│   ├── main/java/com/pipeline/
│   │   ├── Commands.java       # Command handlers
│   │   ├── FileIo.java         # State file load and save
│   │   ├── Main.java           # CLI entry point
│   │   ├── Models.java         # Data models
│   │   └── Parser.java         # JSON parsing
│   └── test/java/com/pipeline/
│       └── CommandsTest.java   # Unit tests
├── pipeline                    # CLI launcher script
├── pom.xml                     # Maven project configuration
├── run.sh                      # Setup and run script
├── reset.sh                    # Restore data/pipeline.json to its initial state
├── test.sh                     # Test script (writes junit.xml)
└── README.md
```

## Commands

### Install

```bash
mvn -q -DskipTests package
```

### Run

```bash
bash run.sh
```

### Test

```bash
bash test.sh
```

### Reset

Restore `data/pipeline.json` to its initial state.

```bash
bash reset.sh
```

## Examples

Each example below runs independently against the initial `data/pipeline.json` and shows only the records it changes.

### Example 1: Starting a run opens it at the first stage

Command:

```bash
./pipeline start R48
```

Output:

```text
R48 started: build
```

Final `data/pipeline.json`, new record appended at the end:

```json
{"run":"R48","stage":"build","status":"running","results":[]}
```

The run begins at `build`, the first name in `stages`, with no results yet.

### Example 2: A passing stage moves the run to the next one

Command:

```bash
./pipeline stage R43 pass
```

Output:

```text
R43: test passed, now deploy
```

Final `data/pipeline.json`, changed record only:

```json
{"run":"R43","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"}]}
```

The result is appended for the stage that just ran, and the run moves to `deploy`.

### Example 3: Only pass and fail are accepted, and the run is checked first

Commands:

```bash
./pipeline stage R43 passed
./pipeline stage R41 bogus
```

Outputs:

```text
Error: INVALID_RESULT
Error: NOT_RUNNING
```

`data/pipeline.json`: unchanged.

`R43` is running, so its misspelled result is rejected as invalid. `R41` is not running, and that check comes first, so the bad result is never reached.

### Example 4: Retrying puts a failed run back on the same stage

Command:

```bash
./pipeline retry R45
```

Output:

```text
R45: retrying deploy
```

Final `data/pipeline.json`, changed record only:

```json
{"run":"R45","stage":"deploy","status":"running","results":[{"stage":"build","result":"pass"},{"stage":"test","result":"pass"},{"stage":"deploy","result":"fail"}]}
```

The run is running again on `deploy`, and the failed result stays on file.

### Example 5: Only a failed run can be retried

Commands:

```bash
./pipeline retry R43
./pipeline retry R99
```

Outputs:

```text
Error: NOT_FAILED
Error: RUN_NOT_FOUND
```

`data/pipeline.json`: unchanged.

`R43` is still running, and `R99` is not on file at all.
