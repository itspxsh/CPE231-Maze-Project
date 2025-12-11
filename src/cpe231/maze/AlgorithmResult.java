package cpe231.maze;

import java.util.List;

public class AlgorithmResult {
    public String algoName;
    // *** เปลี่ยน Type ของ Path เป็น List<int[]> เพื่อรองรับ Algorithm เดิม ***
    public List<int[]> path; 
    public int totalCost;
    public long executionTimeNs;
    public long nodesExpanded;

    // *** ต้องแก้ Constructor ให้รับ List<int[]> เช่นเดิม ***
    public AlgorithmResult(String algoName, List<int[]> path, int totalCost, long executionTimeNs, long nodesExpanded) {
        this.algoName = algoName;
        this.path = path;
        this.totalCost = totalCost;
        this.executionTimeNs = executionTimeNs;
        this.nodesExpanded = nodesExpanded;
    }
}