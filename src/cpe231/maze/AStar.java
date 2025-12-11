package cpe231.maze;

import java.util.*;

public class AStar {

    // ===============================================================
    //           HEAP HELPERS (Optimized with Tie-Breaking)
    // ===============================================================
    /**
     * Helper สำหรับการ Tie-breaking ในกรณีที่ f เท่ากัน: เลือกโหนดที่มี g น้อยกว่า (Shallow)
     * หรืออาจเลือกโหนดที่มี g มากกว่า (Deep, เพื่อกระตุ้นให้ค้นหาไปข้างหน้า)
     * เราจะเลือกโหนดที่มี g น้อยกว่า เพื่อรักษา Consistent (f ที่เท่ากัน)
     */
    private static void siftUp(int[] heapIndex, int[] heapF, int[] heapG, int idx, int newF, int newIdx, int newG) {
        while (idx > 0) {
            int p = (idx - 1) / 2; // ปรับปรุงเพื่อความชัดเจน (ใช้ / 2 แทน >>> 1)
            
            // Comparison: f[p] > newF OR (f[p] == newF AND g[p] > newG)
            boolean shouldSwap = heapF[p] > newF || (heapF[p] == newF && heapG[p] > newG);

            if (!shouldSwap) break;
            
            // Swap: ย้าย Parent ลงมา
            heapIndex[idx] = heapIndex[p];
            heapF[idx] = heapF[p];
            heapG[idx] = heapG[p]; // Swap G array ด้วย
            idx = p;
        }
        heapIndex[idx] = newIdx;
        heapF[idx] = newF;
        heapG[idx] = newG;
    }

    private static void siftDown(int[] heapIndex, int[] heapF, int[] heapG, int size, int idx) {
        int nIdx = heapIndex[idx];
        int nF = heapF[idx];
        int nG = heapG[idx]; // ค่า G ของโหนดปัจจุบัน

        int half = size / 2; // ปรับปรุงเพื่อความชัดเจน (ใช้ / 2 แทน >>> 1)
        while (idx < half) {
            int left = (idx * 2) + 1; // ปรับปรุงเพื่อความชัดเจน (ใช้ * 2 + 1 แทน << 1 + 1)
            int small = left;
            int right = left + 1;

            if (right < size) {
                // Tie-breaking: ถ้า f เท่ากัน ให้เลือกโหนดที่มี g น้อยกว่า (เพื่อรักษา Order)
                boolean rightIsSmaller = heapF[right] < heapF[left] || 
                                         (heapF[right] == heapF[left] && heapG[right] < heapG[left]);
                                         
                if (rightIsSmaller) {
                    small = right;
                }
            }

            // Comparison: nF > f[small] OR (nF == f[small] AND nG > g[small])
            boolean shouldSwap = nF > heapF[small] || (nF == heapF[small] && nG > heapG[small]);
            
            if (!shouldSwap) break;

            // Swap: ย้าย Child ขึ้นมา
            heapIndex[idx] = heapIndex[small];
            heapF[idx] = heapF[small];
            heapG[idx] = heapG[small]; // Swap G array ด้วย
            idx = small;
        }

        heapIndex[idx] = nIdx;
        heapF[idx] = nF;
        heapG[idx] = nG;
    }

    // ===============================================================
    //           PATH RECONSTRUCTION (เหมือนเดิม)
    // ===============================================================
    private static List<int[]> reconstruct(int[] parent, int cols, int curr) {
        List<int[]> path = new ArrayList<>();
        while (curr != -1) { path.add(new int[]{curr / cols, curr % cols}); curr = parent[curr]; }
        Collections.reverse(path);
        return path;
    }
    
    // ===============================================================
    //                       A* (Lazy Heap with G-Tie-Breaking)
    // ===============================================================
    public static AlgorithmResult solve(int[][] maze, int startR, int startC, int goalR, int goalC) {

        long startTime = System.nanoTime();

        int rows = maze.length, cols = maze[0].length;
        int N = rows * cols;

        int startIdx = startR * cols + startC;
        int goalIdx = goalR * cols + goalC;
        
        

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
        int[] heapG = new int[maxHeapSize]; // NEW: ต้องเก็บค่า G ไว้ใน Heap ด้วยเพื่อ Tie-breaking
        int heapSize = 0;
        
        g[startIdx] = 0;
        heapIndex[0] = startIdx;
        heapF[0] = Math.abs(goalR - startR) + Math.abs(goalC - startC);
        heapG[0] = 0; // G = 0
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
                heapG[0] = heapG[heapSize]; // ต้องย้าย G
                siftDown(heapIndex, heapF, heapG, heapSize, 0); // ต้องส่ง heapG
            }

            // Lazy Deletion: ถ้าโหนดที่ดึงออกมามี Cost ที่สูงกว่าค่า g[curr] ที่บันทึกไว้ ให้ข้ามไป
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

            // ---- Neighbor Exploration (Relaxation Inline + Path Re-Checking) ----
            
            // 1. UP
            nxt = curr - cols;
            if (curr >= cols) { 
                cost = flat[nxt];
                if (cost != -1) { // ✅ อนุญาตให้ Relaxation เสมอ (ลบ !closed[nxt])
                    newG = baseG + cost; 
                    if (newG < g[nxt]) { // 💡 Relaxation Check
                        g[nxt] = newG;
                        parent[nxt] = curr;
                        
                        // 💡 NEW LOGIC: Path Re-Checking (Un-close/Re-open)
                        if (closed[nxt]) {
                            closed[nxt] = false;
                        }
                        
                        nxtR = currR - 1; 
                        nxtC = currC;
                        f = newG + Math.abs(goalR - nxtR) + Math.abs(goalC - nxtC);
                        
                        siftUp(heapIndex, heapF, heapG, heapSize, f, nxt, newG); 
                        heapSize++;
                    }
                }
            }

            // 2. DOWN
            nxt = curr + cols;
            if (curr < N - cols) { 
                cost = flat[nxt];
                if (cost != -1) { // ✅ อนุญาตให้ Relaxation เสมอ (ลบ !closed[nxt])
                    newG = baseG + cost; 
                    if (newG < g[nxt]) {
                        g[nxt] = newG;
                        parent[nxt] = curr;

                        // 💡 NEW LOGIC: Path Re-Checking (Un-close/Re-open)
                        if (closed[nxt]) {
                            closed[nxt] = false;
                        }
                        
                        nxtR = currR + 1; 
                        nxtC = currC;
                        f = newG + Math.abs(goalR - nxtR) + Math.abs(goalC - nxtC);
                        
                        siftUp(heapIndex, heapF, heapG, heapSize, f, nxt, newG); 
                        heapSize++;
                    }
                }
            }

            // 3. LEFT
            nxt = curr - 1;
            if (curr % cols != 0) { 
                cost = flat[nxt];
                if (cost != -1) { // ✅ อนุญาตให้ Relaxation เสมอ (ลบ !closed[nxt])
                    newG = baseG + cost; 
                    if (newG < g[nxt]) {
                        g[nxt] = newG;
                        parent[nxt] = curr;

                        // 💡 NEW LOGIC: Path Re-Checking (Un-close/Re-open)
                        if (closed[nxt]) {
                            closed[nxt] = false;
                        }
                        
                        nxtR = currR;
                        nxtC = currC - 1; 
                        f = newG + Math.abs(goalR - nxtR) + Math.abs(goalC - nxtC);
                        
                        siftUp(heapIndex, heapF, heapG, heapSize, f, nxt, newG); 
                        heapSize++;
                    }
                }
            }

            // 4. RIGHT
            nxt = curr + 1;
            if ((curr + 1) % cols != 0) { 
                cost = flat[nxt];
                if (cost != -1) { // ✅ อนุญาตให้ Relaxation เสมอ (ลบ !closed[nxt])
                    newG = baseG + cost; 
                    if (newG < g[nxt]) {
                        g[nxt] = newG;
                        parent[nxt] = curr;

                        // 💡 NEW LOGIC: Path Re-Checking (Un-close/Re-open)
                        if (closed[nxt]) {
                            closed[nxt] = false;
                        }
                        
                        nxtR = currR;
                        nxtC = currC + 1; 
                        f = newG + Math.abs(goalR - nxtR) + Math.abs(goalC - nxtC);
                        
                        siftUp(heapIndex, heapF, heapG, heapSize, f, nxt, newG); 
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