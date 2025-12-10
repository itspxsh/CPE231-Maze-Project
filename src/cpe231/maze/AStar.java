package cpe231.maze;

import java.util.*;

public class AStar {

    public static AlgorithmResult solve(int[][] maze) {
        long start = System.nanoTime();
        
        int rows = maze.length;
        int cols = maze[0].length;
        int numNodes = rows * cols;
        int startIdx = MazeLoader.startRow * cols + MazeLoader.startCol;
        int endIdx = MazeLoader.endRow * cols + MazeLoader.endCol;

        // 1. Flatten Maze: แปลง 2D เป็น 1D Array และ Pre-calculate Heuristic
        int[] flatMaze = new int[numNodes];
        int[] hScore = new int[numNodes]; // Array สำหรับเก็บ h(n) ที่คำนวณไว้ล่วงหน้า
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                flatMaze[idx] = maze[r][c];
                // 2. Pre-calculate Manhattan Heuristic
                hScore[idx] = manhattan(r, c, MazeLoader.endRow, MazeLoader.endCol);
            }
        }

        // Validation Check
        if (flatMaze[startIdx] == -1 || flatMaze[endIdx] == -1) {
            return new AlgorithmResult("A* Search (Optimized)", new ArrayList<>(), -1, 0, 0);
        }

        // 3. Custom Binary Min-Heap (Implementation บน int[])
        // Heap สำหรับ A* จะต้องเปรียบเทียบ f = g + h
        // heapIndex[i] = Node Index
        // heapCost[i] = F-Score (g + h)
        int[] heapIndex = new int[numNodes * 4]; 
        int[] heapCost = new int[numNodes * 4];
        int heapSize = 0;

        int[] gScore = new int[numNodes];
        int[] parent = new int[numNodes];
        Arrays.fill(gScore, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        // Add Start Node
        gScore[startIdx] = 0;
        int startH = hScore[startIdx];
        int startF = 0 + startH;
        
        heapIndex[0] = startIdx;
        heapCost[0] = startF;
        heapSize++;

        long nodesExp = 0;

        // --- Main Loop ---
        while (heapSize > 0) {
            // 3.1 Heap Poll (Extract Min) แบบ Manual (ตาม F-score)
            int currIdx = heapIndex[0];
            int currF = heapCost[0];
            
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
                    
                    // Note: A* Optimization: ถ้า f เท่ากัน อาจใช้ h เป็น Tie-breaker
                    // แต่ในโค้ดนี้เราเน้นความเร็วสูงสุด จึงใช้ f-score เท่านั้นในการเปรียบเทียบ
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
            // (ตรวจสอบ g-score เนื่องจาก g-score เท่านั้นที่บอกต้นทุนจริง)
            if (currF > gScore[currIdx] + hScore[currIdx]) continue;
            
            // ✅ Found Goal
            if (currIdx == endIdx) {
                long duration = System.nanoTime() - start;
                return new AlgorithmResult("A* Search (Optimized)", 
                        reconstruct(parent, cols, endIdx), gScore[endIdx], duration, nodesExp);
            }

            int currR = currIdx / cols;
            int currC = currIdx % cols;

            // 3.2 Neighbor Exploration (Unrolled Loop)
            int[] dr = {-1, 1, 0, 0}; // Up, Down
            int[] dc = {0, 0, -1, 1}; // Left, Right
            
            for (int i = 0; i < 4; i++) {
                int nr = currR + dr[i];
                int nc = currC + dc[i];
                
                // ตรวจสอบขอบเขต
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    int nIdx = nr * cols + nc;
                    int cost = flatMaze[nIdx];
                    
                    // ตรวจสอบกำแพง
                    if (cost != -1) {
                        int newG = gScore[currIdx] + cost;
                        
                        if (newG < gScore[nIdx]) {
                            gScore[nIdx] = newG;
                            parent[nIdx] = currIdx;
                            
                            // คำนวณ F-score ใหม่: F = G + H
                            int newF = newG + hScore[nIdx];
                            
                            // Heap Push Inline
                            int j = heapSize++;
                            while (j > 0) {
                                int p = (j - 1) >>> 1;
                                if (heapCost[p] <= newF) break;
                                heapIndex[j] = heapIndex[p];
                                heapCost[j] = heapCost[p];
                                j = p;
                            }
                            heapIndex[j] = nIdx;
                            heapCost[j] = newF;
                        }
                    }
                }
            }
        }

        return new AlgorithmResult("A* Search (Optimized)", new ArrayList<>(), -1, System.nanoTime() - start, nodesExp);
    }

    // Heuristic function: ใช้ Manhattan Distance
    private static int manhattan(int r, int c, int er, int ec) { 
        return Math.abs(r - er) + Math.abs(c - ec); 
    }
    
    // Reconstruct Path (เหมือนเดิม)
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