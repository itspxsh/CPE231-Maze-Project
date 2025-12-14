package cpe231.maze;

import java.util.*;

public class HybridGA {

    // --- GA Parameters ---
    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATIONS = 500;
    private static final double CROSSOVER_RATE = 0.9;
    private static final double MUTATION_RATE = 0.3; // เพิ่มโอกาสหาทางลัด
    private static final int ELITISM_COUNT = 5;

    // ทิศทางเดิน (บน, ล่าง, ซ้าย, ขวา)
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private static class Individual implements Comparable<Individual> {
        List<int[]> path;
        int cost;
        double fitness;

        public Individual(List<int[]> path, int cost) {
            this.path = new ArrayList<>(path);
            this.cost = cost;
            // Fitness สูตรเพื่อนคุณ: เน้น Cost หนักๆ
            this.fitness = 10_000_000.0 / (cost + 1.0); 
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness);
        }
    }

    public static AlgorithmResult solve(int[][] maze) {
        long startTime = System.nanoTime();
        int rows = maze.length;
        int cols = maze[0].length;
        int startR = MazeLoader.startRow, startC = MazeLoader.startCol;
        int endR = MazeLoader.endRow, endC = MazeLoader.endCol;

        // 1. Initial Population: ใช้แบบ Heuristic (เลียนแบบเพื่อน)
        List<Individual> population = initializePopulation(maze, rows, cols, startR, startC, endR, endC);

        if (population.isEmpty()) return new AlgorithmResult("Genetic Algorithm", new ArrayList<>(), -1, 0, 0);

        Individual bestSolution = population.get(0);

        // --- Main Evolution Loop ---
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            Collections.sort(population);
            
            // เก็บตัวที่ดีที่สุด
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
                
                // [Optimization Trick] ทุกครั้งที่เจอ New Best ลองเอาไปเข้าเครื่องรีดเส้นทางทันที
                List<int[]> optimizedPath = optimizePath(bestSolution.path, maze);
                int optimizedCost = calculateCost(optimizedPath, maze);
                if (optimizedCost < bestSolution.cost) {
                    bestSolution = new Individual(optimizedPath, optimizedCost);
                }
            }

            List<Individual> nextGen = new ArrayList<>();

            // Elitism
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            // Reproduction
            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                Individual child;
                // Crossover
                if (Math.random() < CROSSOVER_RATE) {
                    child = crossover(p1, p2, maze);
                } else {
                    child = p1;
                }

                // Mutation
                if (Math.random() < MUTATION_RATE) {
                    child = mutate(child, maze, rows, cols);
                }
                
                // Simplify Path เล็กน้อยทุกรอบ
                child = simplifyPathBasic(child, maze);

                nextGen.add(child);
            }
            population = nextGen;
        }

        // Final Optimization Pass (ทีเด็ดของเพื่อนคุณ)
        // ก่อนส่งคำตอบสุดท้าย เอาไปรีดเส้นทางอีกรอบ
        List<int[]> finalPath = optimizePath(bestSolution.path, maze);
        int finalCost = calculateCost(finalPath, maze);
        
        long duration = System.nanoTime() - startTime;
        return new AlgorithmResult("Genetic Algorithm", finalPath, finalCost, duration, POPULATION_SIZE * MAX_GENERATIONS);
    }

    // --- 1. Initialization (Heuristic Walk) ---
    // สร้างเส้นทางโดยเลือกเดินไปในทิศที่ Score ดีสุด (Distance + Cost)
    private static List<Individual> initializePopulation(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
        List<Individual> pop = new ArrayList<>();
        
        for (int i = 0; i < POPULATION_SIZE; i++) {
            // ใช้ความน่าจะเป็น: 70% เดินแบบฉลาด, 30% เดินสุ่ม (เพื่อความหลากหลาย)
            boolean useHeuristic = (Math.random() < 0.7); 
            List<int[]> path = generateHeuristicPath(maze, rows, cols, sr, sc, er, ec, useHeuristic);
            
            if (path != null) {
                pop.add(new Individual(path, calculateCost(path, maze)));
            }
        }
        // กันเหนียว ถ้าหาไม่ได้เลย
        if (pop.isEmpty()) {
            List<int[]> path = generateHeuristicPath(maze, rows, cols, sr, sc, er, ec, false);
            if (path != null) pop.add(new Individual(path, calculateCost(path, maze)));
        }
        return pop;
    }

    private static List<int[]> generateHeuristicPath(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec, boolean smart) {
        // ใช้ Stack DFS แบบเลือกทิศทาง
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[rows][cols];
        Map<String, int[]> parentMap = new HashMap<>();
        
        stack.push(new int[]{sr, sc});
        visited[sr][sc] = true;
        parentMap.put(sr + "," + sc, null);

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            if (curr[0] == er && curr[1] == ec) return reconstructPath(parentMap, er, ec);

            List<int[]> neighbors = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int nr = curr[0] + DR[i];
                int nc = curr[1] + DC[i];
                if (isValid(nr, nc, rows, cols, maze) && !visited[nr][nc]) {
                    neighbors.add(new int[]{nr, nc});
                }
            }

            if (smart) {
                // สูตรเพื่อนคุณ: Score = Dist * 1.3 + Cost * 1.4
                // เราต้องการ Score ต่ำสุด (ใกล้ Goal และ Cost ถูก)
                neighbors.sort((a, b) -> {
                    double scoreA = (Math.abs(a[0]-er) + Math.abs(a[1]-ec)) * 1.3 + maze[a[0]][a[1]] * 1.4;
                    double scoreB = (Math.abs(b[0]-er) + Math.abs(b[1]-ec)) * 1.3 + maze[b[0]][b[1]] * 1.4;
                    return Double.compare(scoreB, scoreA); // Reverse sort for Stack
                });
            } else {
                Collections.shuffle(neighbors);
            }

            for (int[] n : neighbors) {
                visited[n[0]][n[1]] = true;
                parentMap.put(n[0] + "," + n[1], curr);
                stack.push(n);
            }
        }
        return null; // หาทางไม่เจอ
    }

    // --- The Secret Weapon: Path Optimization (เลียนแบบ optimizePath) ---
    // พยายามหาทางลัดระหว่างจุดในเส้นทาง โดยใช้ Local A*
    private static List<int[]> optimizePath(List<int[]> path, int[][] maze) {
        if (path.size() < 3) return path;
        List<int[]> optimized = new ArrayList<>();
        optimized.add(path.get(0));

        int currentIdx = 0;
        // มองไปข้างหน้า ดูว่ามีจุดไหนที่เรา "วาร์ป" ไปได้ด้วย A* สั้นๆ ไหม
        while (currentIdx < path.size() - 1) {
            int bestNextIdx = currentIdx + 1;
            int maxLookAhead = Math.min(path.size() - 1, currentIdx + 30); // มองไกลสุด 30 ช่อง

            for (int targetIdx = maxLookAhead; targetIdx > currentIdx + 1; targetIdx--) {
                int[] curr = path.get(currentIdx);
                int[] target = path.get(targetIdx);
                
                // ถ้าระยะห่าง Manhattan ไม่เกิน 15 (วิ่ง A* ไหว)
                int dist = Math.abs(curr[0] - target[0]) + Math.abs(curr[1] - target[1]);
                if (dist <= 15) {
                    // ลองรัน Local A* หาทางลัด
                    List<int[]> shortPath = findLocalAStar(curr, target, maze);
                    if (shortPath != null) {
                        // เช็คว่าทางลัดนี้ Cost ถูกกว่าทางเดิมใน GA ไหม
                        int originalSegmentCost = calculateSegmentCost(path, currentIdx, targetIdx, maze);
                        int newSegmentCost = calculateCost(shortPath, maze);
                        
                        if (newSegmentCost < originalSegmentCost) {
                            // เจอทางลัด!
                            for (int i = 1; i < shortPath.size(); i++) optimized.add(shortPath.get(i)); // add ทางลัด
                            currentIdx = targetIdx; // กระโดดไปจุดปลายทางเลย
                            break; 
                        }
                    }
                }
            }
            
            // ถ้าไม่เจอทางลัด ก็เดินตามทางเดิม 1 ก้าว
            if (currentIdx < path.size() - 1) {
                 // แต่ต้องเช็คว่าเราไม่ได้เพิ่งกระโดดไปนะ (ถ้ากระโดด loop บนจะ update currentIdx แล้ว)
                 // Logic ง่ายๆ: ถ้า optimized ตัวสุดท้ายคือ currentIdx แล้ว ให้ใส่ตัวถัดไป
                 int[] lastAdded = optimized.get(optimized.size()-1);
                 int[] realCurr = path.get(currentIdx);
                 if (lastAdded[0] == realCurr[0] && lastAdded[1] == realCurr[1]) {
                     optimized.add(path.get(currentIdx + 1));
                     currentIdx++;
                 }
            }
        }
        return optimized;
    }

    // A* จิ๋ว สำหรับหาทางลัดระยะสั้น (หัวใจสำคัญของ Optimization)
    private static List<int[]> findLocalAStar(int[] start, int[] end, int[][] maze) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(start[0], start[1], 0, 0));
        
        Map<String, Integer> gScore = new HashMap<>();
        Map<String, int[]> parent = new HashMap<>();
        gScore.put(start[0]+","+start[1], 0);

        int nodesExpanded = 0;
        int MAX_NODES = 200; // จำกัดการค้นหา กันโปรแกรมค้าง

        while(!pq.isEmpty() && nodesExpanded < MAX_NODES) {
            Node curr = pq.poll();
            nodesExpanded++;

            if(curr.r == end[0] && curr.c == end[1]) {
                return reconstructPathMap(parent, end);
            }

            for(int i=0; i<4; i++) {
                int nr = curr.r + DR[i];
                int nc = curr.c + DC[i];
                
                if(isValid(nr, nc, maze.length, maze[0].length, maze)) {
                    int newG = curr.g + maze[nr][nc];
                    String key = nr+","+nc;
                    if(newG < gScore.getOrDefault(key, Integer.MAX_VALUE)) {
                        gScore.put(key, newG);
                        parent.put(key, new int[]{curr.r, curr.c});
                        int h = Math.abs(nr-end[0]) + Math.abs(nc-end[1]);
                        pq.add(new Node(nr, nc, newG, newG+h));
                    }
                }
            }
        }
        return null; // หาไม่เจอในเวลาที่กำหนด
    }
    
    // --- Helper Functions ---
    private static Individual simplifyPathBasic(Individual ind, int[][] maze) {
        // ฟังก์ชันตัดลูปง่ายๆ (A->B->A ตัดทิ้ง)
        List<int[]> path = new ArrayList<>(ind.path);
        boolean changed = true;
        while (changed) {
            changed = false;
            if (path.size() < 3) break;
            List<int[]> newPath = new ArrayList<>();
            newPath.add(path.get(0));
            int curr = 0;
            while (curr < path.size() - 1) {
                int jump = curr + 1;
                for (int i = path.size() - 1; i > curr + 1; i--) {
                    if (isNeighbor(path.get(curr), path.get(i))) {
                        jump = i; changed = true; break;
                    }
                }
                newPath.add(path.get(jump));
                curr = jump;
            }
            path = newPath;
        }
        return new Individual(path, calculateCost(path, maze));
    }

    private static Individual selectParent(List<Individual> pop) {
        int tournament = 5;
        Individual best = null;
        for (int i=0; i<tournament; i++) {
            Individual ind = pop.get((int)(Math.random()*pop.size()));
            if (best == null || ind.fitness > best.fitness) best = ind;
        }
        return best;
    }

    private static Individual crossover(Individual p1, Individual p2, int[][] maze) {
        Set<String> set1 = new HashSet<>();
        for(int[] p : p1.path) set1.add(p[0]+","+p[1]);
        
        List<int[]> common = new ArrayList<>();
        for(int i=1; i<p2.path.size()-1; i++) {
            if(set1.contains(p2.path.get(i)[0]+","+p2.path.get(i)[1])) common.add(p2.path.get(i));
        }
        if(common.isEmpty()) return p1;

        int[] cut = common.get((int)(Math.random()*common.size()));
        List<int[]> childP = new ArrayList<>();
        for(int[] p : p1.path) {
            childP.add(p);
            if(p[0]==cut[0] && p[1]==cut[1]) break;
        }
        boolean found=false;
        for(int[] p : p2.path) {
            if(p[0]==cut[0] && p[1]==cut[1]) found=true;
            if(found && (p[0]!=cut[0] || p[1]!=cut[1])) childP.add(p);
        }
        return new Individual(childP, calculateCost(childP, maze));
    }

    private static Individual mutate(Individual ind, int[][] maze, int rows, int cols) {
        // Mutation แบบสุ่มหาทางลัด (ใช้ Local A* มาช่วยด้วยก็ได้ถ้าอยากโหด)
        List<int[]> path = ind.path;
        if(path.size()<5) return ind;
        int idx1 = (int)(Math.random()*(path.size()-2));
        int idx2 = Math.min(path.size()-1, idx1 + 5 + (int)(Math.random()*15));
        
        // ลองหาทางใหม่ระหว่าง idx1 กับ idx2
        List<int[]> shortCut = findLocalAStar(path.get(idx1), path.get(idx2), maze);
        if(shortCut != null) {
             List<int[]> newPath = new ArrayList<>();
             for(int i=0; i<=idx1; i++) newPath.add(path.get(i));
             for(int i=1; i<shortCut.size()-1; i++) newPath.add(shortCut.get(i));
             for(int i=idx2; i<path.size(); i++) newPath.add(path.get(i));
             return new Individual(newPath, calculateCost(newPath, maze));
        }
        return ind;
    }

    private static int calculateSegmentCost(List<int[]> path, int start, int end, int[][] maze) {
        int sum=0; for(int i=start; i<=end; i++) sum += maze[path.get(i)[0]][path.get(i)[1]]; return sum;
    }
    private static boolean isNeighbor(int[] a, int[] b) { return Math.abs(a[0]-b[0]) + Math.abs(a[1]-b[1]) == 1; }
    private static boolean isValid(int r, int c, int rows, int cols, int[][] maze) { return r>=0 && r<rows && c>=0 && c<cols && maze[r][c]!=-1; }
    private static int calculateCost(List<int[]> path, int[][] maze) { int s=0; for(int[] p:path) s+=maze[p[0]][p[1]]; return s; }
    
    private static List<int[]> reconstructPath(Map<String, int[]> pm, int er, int ec) {
        List<int[]> p = new ArrayList<>(); int[] c = {er, ec};
        while(c!=null) { p.add(c); c=pm.get(c[0]+","+c[1]); }
        Collections.reverse(p); return p;
    }
    
    private static List<int[]> reconstructPathMap(Map<String, int[]> pm, int[] end) {
        List<int[]> p = new ArrayList<>(); int[] c = end;
        while(c!=null && pm.containsKey(c[0]+","+c[1])) { // Check containsKey to stop at start node
            p.add(c); c=pm.get(c[0]+","+c[1]); 
        }
        p.add(c); // Add start node
        Collections.reverse(p); return p;
    }

    private static class Node implements Comparable<Node> {
        int r, c, g, f;
        public Node(int r, int c, int g, int f) { this.r=r; this.c=c; this.g=g; this.f=f; }
        public int compareTo(Node o) { return Integer.compare(this.f, o.f); }
    }
}