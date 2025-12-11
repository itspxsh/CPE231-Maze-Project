package cpe231.maze;

import java.util.*;

public class Dijkstra {

    // รับ int[][] และพิกัด Start/Goal เป็น Parameter
    public static AlgorithmResult solve(int[][] maze, int startR, int startC, int goalR, int goalC) {
        long start = System.nanoTime();
        
        int rows = maze.length;
        int cols = maze[0].length;
        int numNodes = rows * cols;
        
        // ใช้ Parameter startR/startC
        int startIdx = startR * cols + startC;
        // ใช้ Parameter goalR/goalC
        int endIdx = goalR * cols + goalC; 

        // 1. Flatten Maze: แปลง 2D เป็น 1D Array เพื่อความเร็วสูงสุด (Cache Locality)
        int[] flatMaze = new int[numNodes];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(maze[i], 0, flatMaze, i * cols, cols);
        }

        // Validation Check
        if (flatMaze[startIdx] == -1 || flatMaze[endIdx] == -1) {
            return new AlgorithmResult("Dijkstra (Optimized)", new ArrayList<>(), -1, 0, 0);
        }

        // 2. ใช้ int[] แทน PriorityQueue<Object> เพื่อลดภาระ Garbage Collector
        int[] dist = new int[numNodes];
        int[] parent = new int[numNodes];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        // 3. Custom Binary Min-Heap (Implementation บน int[])
        // ขนาด Heap เผื่อไว้ (worst case ของ Lazy Dijkstra คือจำนวน Edge ~ 4*Node)
        int[] heapIndex = new int[numNodes * 4]; 
        int[] heapCost = new int[numNodes * 4];
        int heapSize = 0;

        // Add Start Node
        dist[startIdx] = 0;
        heapIndex[0] = startIdx;
        heapCost[0] = 0;
        heapSize++;

        long nodesExp = 0;

        // --- Main Loop ---
        while (heapSize > 0) {
            // 3.1 Heap Poll (Extract Min) แบบ Manual
            int currIdx = heapIndex[0];
            int currCost = heapCost[0];
            
            // ย้ายตัวท้ายสุดมาแทนราก แล้ว Sift Down
            heapSize--;
            if (heapSize > 0) {
                int lastIndex = heapIndex[heapSize];
                int lastCost = heapCost[heapSize];
                
                int i = 0;
                int half = heapSize >>> 1; // หาร 2 ด้วย Bitwise
                while (i < half) {
                    int child = (i << 1) + 1; // คูณ 2 + 1 ด้วย Bitwise
                    int right = child + 1;
                    if (right < heapSize && heapCost[right] < heapCost[child]) {
                        child = right;
                    }
                    if (lastCost <= heapCost[child]) break;
                    heapIndex[i] = heapIndex[child];
                    heapCost[i] = heapCost[child];
                    i = child;
                }
                heapIndex[i] = lastIndex;
                heapCost[i] = lastCost;
            }

            nodesExp++;

            // Lazy Deletion: ถ้าเจอทางที่ดีกว่าไปแล้ว ให้ข้าม
            if (currCost > dist[currIdx]) continue;

            // ✅ Found Goal
            if (currIdx == endIdx) {
                long duration = System.nanoTime() - start;
                return new AlgorithmResult("Dijkstra (Optimized)", 
                        reconstruct(parent, cols, endIdx), dist[endIdx], duration, nodesExp);
            }

            // 3.2 Neighbor Exploration (Unrolled Loop เพื่อลด Branching Overhead)
            // Up
            int nUp = currIdx - cols;
            if (currIdx >= cols && flatMaze[nUp] != -1) {
                int newCost = currCost + flatMaze[nUp];
                if (newCost < dist[nUp]) {
                    dist[nUp] = newCost;
                    parent[nUp] = currIdx;
                    // Heap Push Inline
                    int i = heapSize++;
                    while (i > 0) {
                        int p = (i - 1) >>> 1;
                        if (heapCost[p] <= newCost) break;
                        heapIndex[i] = heapIndex[p];
                        heapCost[i] = heapCost[p];
                        i = p;
                    }
                    heapIndex[i] = nUp;
                    heapCost[i] = newCost;
                }
            }
            // Down
            int nDown = currIdx + cols;
            if (currIdx < numNodes - cols && flatMaze[nDown] != -1) {
                int newCost = currCost + flatMaze[nDown];
                if (newCost < dist[nDown]) {
                    dist[nDown] = newCost;
                    parent[nDown] = currIdx;
                    // Heap Push Inline
                    int i = heapSize++;
                    while (i > 0) {
                        int p = (i - 1) >>> 1;
                        if (heapCost[p] <= newCost) break;
                        heapIndex[i] = heapIndex[p];
                        heapCost[i] = heapCost[p];
                        i = p;
                    }
                    heapIndex[i] = nDown;
                    heapCost[i] = newCost;
                }
            }
            // Left
            if (currIdx % cols != 0 && flatMaze[currIdx - 1] != -1) {
                int nLeft = currIdx - 1;
                int newCost = currCost + flatMaze[nLeft];
                if (newCost < dist[nLeft]) {
                    dist[nLeft] = newCost;
                    parent[nLeft] = currIdx;
                    // Heap Push Inline
                    int i = heapSize++;
                    while (i > 0) {
                        int p = (i - 1) >>> 1;
                        if (heapCost[p] <= newCost) break;
                        heapIndex[i] = heapIndex[p];
                        heapCost[i] = heapCost[p];
                        i = p;
                    }
                    heapIndex[i] = nLeft;
                    heapCost[i] = newCost;
                }
            }
            // Right
            if ((currIdx + 1) % cols != 0 && flatMaze[currIdx + 1] != -1) {
                int nRight = currIdx + 1;
                int newCost = currCost + flatMaze[nRight];
                if (newCost < dist[nRight]) {
                    dist[nRight] = newCost;
                    parent[nRight] = currIdx;
                    // Heap Push Inline
                    int i = heapSize++;
                    while (i > 0) {
                        int p = (i - 1) >>> 1;
                        if (heapCost[p] <= newCost) break;
                        heapIndex[i] = heapIndex[p];
                        heapCost[i] = heapCost[p];
                        i = p;
                    }
                    heapIndex[i] = nRight;
                    heapCost[i] = newCost;
                }
            }
        }

        return new AlgorithmResult("Dijkstra (Optimized)", new ArrayList<>(), -1, System.nanoTime() - start, nodesExp);
    }

    private static List<int[]> reconstruct(int[] parent, int cols, int curr) {
        List<int[]> path = new ArrayList<>();
        while (curr != -1) {
            path.add(new int[]{curr / cols, curr % cols});
            curr = parent[curr];
        }
        Collections.reverse(path);
        return path;
    }
}