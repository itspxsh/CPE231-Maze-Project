package cpe231.maze;

import java.util.*;

public class AStar {
    private static class Node implements Comparable<Node> {
        int index, g, f, h;
        public Node(int index, int g, int h) {
            this.index = index; this.g = g; this.h = h; this.f = g + h;
        }
        public int compareTo(Node o) {
            return (this.f == o.f) ? Integer.compare(this.h, o.h) : Integer.compare(this.f, o.f);
        }
    }

    // ===============================================================
    //           HEAP HELPERS (Optimized with Bitwise Ops)
    // ===============================================================
    private static void siftUp(int[] heapIndex, int[] heapF, int idx, int newF, int newIdx) {
        while (idx > 0) {
            int p = (idx - 1) >>> 1;
            if (heapF[p] <= newF) break;
            heapIndex[idx] = heapIndex[p];
            heapF[idx] = heapF[p];
            idx = p;
        }
        heapIndex[idx] = newIdx;
        heapF[idx] = newF;
    }

    private static void siftDown(int[] heapIndex, int[] heapF, int size, int idx) {
        int nIdx = heapIndex[idx];
        int nF = heapF[idx];

        int half = size >>> 1;
        while (idx < half) {
            int left = (idx << 1) + 1;
            int small = left;
            int right = left + 1;

            if (right < size && heapF[right] < heapF[left]) {
                small = right;
            }

            if (nF <= heapF[small]) break;

            heapIndex[idx] = heapIndex[small];
            heapF[idx] = heapF[small];
            idx = small;
        }

        heapIndex[idx] = nIdx;
        heapF[idx] = nF;
    }

    // ===============================================================
    //           PATH RECONSTRUCTION
    // ===============================================================
    private static List<int[]> reconstruct(int[] parent, int cols, int curr) {
        List<int[]> path = new ArrayList<>();
        while (curr != -1) { path.add(new int[]{curr / cols, curr % cols}); curr = parent[curr]; }
        Collections.reverse(path);
        return path;
    }
    
    // ===============================================================
    //                       A* (Maximum Optimization)
    // ===============================================================
    public static AlgorithmResult solve(int[][] maze) {

        long startTime = System.nanoTime();

        int rows = maze.length, cols = maze[0].length;
        int N = rows * cols;

        int startIdx = MazeLoader.startRow * cols + MazeLoader.startCol;
        int goalIdx = MazeLoader.endRow * cols + MazeLoader.endCol;
        
        // Cache Goal R/C 
        int goalR = MazeLoader.endRow;
        int goalC = MazeLoader.endCol;

        // ---- Flatten ----
        int[] flat = new int[N];
        for (int r = 0; r < rows; r++)
            System.arraycopy(maze[r], 0, flat, r * cols, cols);

        if (flat[startIdx] == -1 || flat[goalIdx] == -1) {
            return new AlgorithmResult("A* Manhattan", new ArrayList<>(), -1, 0, 0);
        }

        // ---- A* DATA ----
        int[] g = new int[N];
        int[] parent = new int[N];
        boolean[] closed = new boolean[N];
        Arrays.fill(g, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        int maxHeapSize = N * 4; 
        int[] heapIndex = new int[maxHeapSize]; 
        int[] heapF = new int[maxHeapSize];
        int heapSize = 0;

        // Start node (Heuristic calculation: หาร/Modulo เพียงครั้งเดียว)
        int startR = startIdx / cols;
        int startC = startIdx % cols;
        
        g[startIdx] = 0;
        heapIndex[0] = startIdx;
        heapF[0] = Math.abs(goalR - startR) + Math.abs(goalC - startC);
        heapSize = 1;

        long expanded = 0;

        // ===============================================================
        //                          MAIN A* LOOP 
        // ===============================================================
        while (heapSize > 0) {
            // Extract Min (Poll)
            int curr = heapIndex[0]; 

            heapSize--;
            if (heapSize > 0) {
                heapIndex[0] = heapIndex[heapSize];
                heapF[0] = heapF[heapSize];
                siftDown(heapIndex, heapF, heapSize, 0);
            }

            if (closed[curr]) continue;
            closed[curr] = true;

            expanded++;

            if (curr == goalIdx) {
                long dt = System.nanoTime() - startTime;
                return new AlgorithmResult(
                    "A* Manhattan", reconstruct(parent, cols, goalIdx), g[curr], dt, expanded
                );
            }

            int baseG = g[curr];
            // คำนวณ R และ C ของโหนดปัจจุบัน (ต้องทำซ้ำทุกรอบ)
            int currR = curr / cols; 
            int currC = curr % cols;
            
            int nxt;
            int newG;
            int f;
            int cost;
            int nxtR, nxtC; 

            // ---- Neighbor Exploration (Zero-Division/Modulo Heuristic) ----
            
            // 1. UP
            nxt = curr - cols;
            if (curr >= cols) { 
                cost = flat[nxt];
                if (cost != -1 && !closed[nxt]) {
                    newG = baseG + cost; 
                    if (newG < g[nxt]) {
                        g[nxt] = newG;
                        parent[nxt] = curr;
                        
                        // INLINE MANHATTAN (ใช้ ลบ/บวก จาก currR/currC)
                        nxtR = currR - 1; 
                        nxtC = currC;
                        f = newG + Math.abs(goalR - nxtR) + Math.abs(goalC - nxtC);
                        
                        siftUp(heapIndex, heapF, heapSize, f, nxt);
                        heapSize++;
                    }
                }
            }

            // 2. DOWN
            nxt = curr + cols;
            if (curr < N - cols) { 
                cost = flat[nxt];
                if (cost != -1 && !closed[nxt]) {
                    newG = baseG + cost; 
                    if (newG < g[nxt]) {
                        g[nxt] = newG;
                        parent[nxt] = curr;
                        
                        // INLINE MANHATTAN (ใช้ ลบ/บวก จาก currR/currC)
                        nxtR = currR + 1; 
                        nxtC = currC;
                        f = newG + Math.abs(goalR - nxtR) + Math.abs(goalC - nxtC);
                        
                        siftUp(heapIndex, heapF, heapSize, f, nxt);
                        heapSize++;
                    }
                }
            }

            // 3. LEFT
            nxt = curr - 1;
            if (curr % cols != 0) { 
                cost = flat[nxt];
                if (cost != -1 && !closed[nxt]) {
                    newG = baseG + cost; 
                    if (newG < g[nxt]) {
                        g[nxt] = newG;
                        parent[nxt] = curr;
                        
                        // INLINE MANHATTAN (ใช้ ลบ/บวก จาก currR/currC)
                        nxtR = currR;
                        nxtC = currC - 1; 
                        f = newG + Math.abs(goalR - nxtR) + Math.abs(goalC - nxtC);
                        
                        siftUp(heapIndex, heapF, heapSize, f, nxt);
                        heapSize++;
                    }
                }
            }

            // 4. RIGHT
            nxt = curr + 1;
            if ((curr + 1) % cols != 0) { 
                cost = flat[nxt];
                if (cost != -1 && !closed[nxt]) {
                    newG = baseG + cost; 
                    if (newG < g[nxt]) {
                        g[nxt] = newG;
                        parent[nxt] = curr;
                        
                        // INLINE MANHATTAN (ใช้ ลบ/บวก จาก currR/currC)
                        nxtR = currR;
                        nxtC = currC + 1; 
                        f = newG + Math.abs(goalR - nxtR) + Math.abs(goalC - nxtC);
                        
                        siftUp(heapIndex, heapF, heapSize, f, nxt);
                        heapSize++;
                    }
                }
            }
        }

        // no path
        return new AlgorithmResult("A* Manhattan", new ArrayList<>(), -1, 
                System.nanoTime() - startTime, expanded);
    }
}