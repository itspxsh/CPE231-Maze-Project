package cpe231.maze.core;

import java.util.List;

/**
 * A Java Record to hold the result of an algorithm run.
 * Records automatically generate getters like .path(), .cost(), .status().
 */
public record AlgorithmResult(
    String status,
    List<int[]> path,
    int cost,
    long durationNs,
    long nodesExpanded
) {}