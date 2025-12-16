package cpe231.maze.core;

import java.util.List;

/**
 * Immutable record to hold algorithm results.
 * Now includes helper methods for the UI and Benchmark.
 */
public record AlgorithmResult(
    String status,
    List<int[]> path,
    int cost,
    long durationNs,
    long nodesExpanded
) {
    /**
     * Returns true if the algorithm successfully found the goal.
     */
    public boolean isSuccess() {
        return "Success".equalsIgnoreCase(status);
    }

    /**
     * Returns duration in milliseconds (formatted as double).
     */
    public double getDurationMs() {
        return durationNs / 1_000_000.0;
    }
}