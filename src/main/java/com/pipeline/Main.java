package com.pipeline;

import com.pipeline.Models.CommandResult;
import com.pipeline.Models.State;

import java.io.PrintStream;

public final class Main {
    private static final String PIPELINE_PATH = "data/pipeline.json";
    private static final int MIN_ARGC = 1;     // args[0] is the command
    private static final int START_ARGC = 2;   // start <run>
    private static final int STAGE_ARGC = 3;   // stage <run> <pass|fail>
    private static final int RETRY_ARGC = 2;   // retry <run>

    /// Print the supported commands and their arguments.
    private static void printUsage(PrintStream out) {
        out.println("Usage:");
        out.println("  pipeline start <run>");
        out.println("  pipeline stage <run> <pass|fail>");
        out.println("  pipeline retry <run>");
    }

    /// Load the CI Pipeline state, dispatch the requested command, and write any changes back.
    public static void main(String[] args) {
        if (args.length < MIN_ARGC) {
            printUsage(System.err);
            System.exit(1);
        }

        String command = args[0];

        if ("start".equals(command)) {
            if (args.length != START_ARGC) {
                printUsage(System.err);
                System.exit(1);
            }
        } else if ("stage".equals(command)) {
            if (args.length != STAGE_ARGC) {
                printUsage(System.err);
                System.exit(1);
            }
        } else if ("retry".equals(command)) {
            if (args.length != RETRY_ARGC) {
                printUsage(System.err);
                System.exit(1);
            }
        } else {
            System.err.println("Unknown command: " + command);
            printUsage(System.err);
            System.exit(1);
        }

        try {
            State state = Parser.parsePipelineJson(FileIo.readFileToString(PIPELINE_PATH));

            CommandResult result;
            if ("start".equals(command)) {
                result = Commands.start(state, args[1]);
            } else if ("stage".equals(command)) {
                result = Commands.stage(state, args[1], args[2]);
            } else {
                result = Commands.retry(state, args[1]);
            }

            if (result.ok) {
                FileIo.writePipelineJson(PIPELINE_PATH, state);
                System.out.println(result.message);
            } else {
                System.out.println("Error: " + result.reason);
            }
        } catch (Exception error) {
            System.err.println("Error: " + error.getMessage());
            System.exit(1);
        }
    }
}
