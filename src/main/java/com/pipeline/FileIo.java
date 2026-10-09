package com.pipeline;

import com.pipeline.Models.Run;
import com.pipeline.Models.StageResult;
import com.pipeline.Models.State;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class FileIo {
    private FileIo() {}

    /// Escape a string for safe inclusion inside JSON double quotes.
    private static String escapeJsonString(String value) {
        if (value == null) return "";
        StringBuilder escaped = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"':  escaped.append("\\\""); break;
                case '\\': escaped.append("\\\\"); break;
                case '\n': escaped.append("\\n");  break;
                case '\r': escaped.append("\\r");  break;
                case '\t': escaped.append("\\t");  break;
                default:   escaped.append(ch);
            }
        }
        return escaped.toString();
    }

    /// Format the stage list as a single-line JSON array of strings.
    private static String stagesToJson(List<String> stages) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < stages.size(); i++) {
            if (i > 0) builder.append(',');
            builder.append('"').append(escapeJsonString(stages.get(i))).append('"');
        }
        builder.append(']');
        return builder.toString();
    }

    /// Format a single stage result as a single-line JSON object.
    private static String resultToJson(StageResult result) {
        return "{\"stage\":\"" + escapeJsonString(result.stage) + "\""
                + ",\"result\":\"" + result.result + "\"}";
    }

    /// Format a single run as a single-line JSON object.
    private static String runToJson(Run run) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"run\":\"").append(escapeJsonString(run.runId)).append("\"")
                .append(",\"stage\":\"").append(escapeJsonString(run.stage)).append("\"")
                .append(",\"status\":\"").append(run.status).append("\"")
                .append(",\"results\":[");
        for (int i = 0; i < run.results.size(); i++) {
            if (i > 0) builder.append(',');
            builder.append(resultToJson(run.results.get(i)));
        }
        builder.append("]}");
        return builder.toString();
    }

    /// Read a text file as a single string.
    public static String readFileToString(String path) {
        try {
            return Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to open file: " + path, e);
        }
    }

    /// Format the CI Pipeline state as JSON for data/pipeline.json.
    public static String pipelineToJson(State state) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n  \"stages\": ").append(stagesToJson(state.stages));
        builder.append(",\n  \"runs\": [");
        for (int i = 0; i < state.runs.size(); i++) {
            builder.append(i == 0 ? "\n    " : ",\n    ");
            builder.append(runToJson(state.runs.get(i)));
        }
        if (!state.runs.isEmpty()) {
            builder.append("\n  ");
        }
        builder.append("]\n}");
        return builder.toString();
    }

    /// Write the CI Pipeline state JSON to the given file.
    public static void writePipelineJson(String path, State state) {
        try {
            Path target = Paths.get(path);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.writeString(target, pipelineToJson(state) + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Unable to open output file: " + path, e);
        }
    }
}
