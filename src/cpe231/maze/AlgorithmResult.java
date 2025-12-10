package cpe231.maze;

import java.util.List;

public class AlgorithmResult {
    public String algoName;
    public List<int[]> path;
    public int totalCost;
    public long executionTimeNs;
    public long nodesExpanded;

    public AlgorithmResult(String algoName, List<int[]> path, int totalCost, long executionTimeNs, long nodesExpanded) {
        this.algoName = algoName;
        this.path = path;
        this.totalCost = totalCost;
        this.executionTimeNs = executionTimeNs;
        this.nodesExpanded = nodesExpanded;
    }
}