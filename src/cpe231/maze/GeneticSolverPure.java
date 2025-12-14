package cpe231.maze;

import java.util.*;

public class GeneticSolverPure implements MazeSolver {

    // --- GA Parameters Settings (Adjusted for Performance & Requirements) ---
    // เพิ่มจำนวนประชากรเพื่อให้มีความหลากหลาย (Diversity) มากขึ้น
    private static final int POPULATION_SIZE = 100;   
    private static final int MAX_GENERATIONS = 200;  
    
    // Crossover 90%: เน้นการผสมพันธุ์เพราะเราต้องการรวม Path ที่ดีเข้าด้วยกัน
    private static final double CROSSOVER_RATE = 0.9; 
    
    // Mutation 5%: ตามโจทย์ (0-5%) ค่านี้เพียงพอสำหรับการแก้ทางตันโดยไม่ทำลายโครงสร้างที่ดี
    private static final double MUTATION_RATE = 0.05; 
    
    // Elitism: เก็บตัวท็อปไว้ 10% (ตามสูตร 1 - Crossover Rate)
    private static final int ELITISM_COUNT = (int) (POPULATION_SIZE * (1 - CROSSOVER_RATE)); 

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
            // เรียกใช้ฟังก์ชันประเมิน Fitness แยกต่างหาก
            this.fitness = evaluateFitness(cost);
        }

        @Override
        public int compareTo(Individual other) {
            // เรียงจาก Fitness มาก -> น้อย
            return Double.compare(other.fitness, this.fitness);
        }
        
        // Helper สำหรับเช็คตัวซ้ำ (Duplicate)
        public boolean isSamePath(Individual other) {
            return this.cost == other.cost && this.path.size() == other.path.size();
        }
    }

    // --- 3. Evaluate fitness fn. ---
    private static double evaluateFitness(int cost) {
        // ยิ่ง Cost น้อย Fitness ยิ่งมาก
        return 1.0 / (cost + 1);
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        
        int[][] maze = context.getGrid();
        int rows = context.rows;
        int cols = context.cols;
        int startR = context.startRow;
        int startC = context.startCol;
        int endR = context.endRow;
        int endC = context.endCol;

        // 1. Initial population
        List<Individual> population = initializePopulation(maze, rows, cols, startR, startC, endR, endC);

        if (population.isEmpty()) {
            return new AlgorithmResult("Genetic Algorithm (Pure)", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);
        }

        Individual bestSolution = population.get(0);
        int totalGenerations = 0;

        // Main Evolution Loop
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            totalGenerations++;
            
            // Sort เพื่อหาตัวที่ดีที่สุดและเตรียมทำ Elitism
            Collections.sort(population);

            // Update Best Solution found so far
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
            }

            List<Individual> nextGen = new ArrayList<>();

            // 7. Select elitism fn.
            // เก็บตัวที่ดีที่สุดไว้เสมอ เพื่อไม่ให้คำตอบที่ดีหายไป
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            // Create Next Generation
            // วนลูปจนกว่าจะได้ประชากรครบจำนวน
            int attempts = 0;
            while (nextGen.size() < POPULATION_SIZE && attempts < POPULATION_SIZE * 2) {
                attempts++;
                
                // 4. Selection
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                Individual child;
                
                // 5. Crossover
                if (Math.random() < CROSSOVER_RATE) {
                    child = crossover(p1, p2, maze);
                } else {
                    child = p1;
                }

                // 6. Mutation
                if (Math.random() < MUTATION_RATE) {
                    child = mutate(child, maze, rows, cols);
                }
                
                // Diversity Control: พยายามไม่รับตัวที่ซ้ำกับที่มีอยู่แล้วในรุ่นถัดไป
                // เพื่อบังคับให้ระบบหาทางเลือกใหม่ๆ
                boolean isDuplicate = false;
                if (nextGen.size() > ELITISM_COUNT) { // เช็คเฉพาะตัวใหม่ที่เพิ่งสร้าง
                     // สุ่มเช็คตัวท้ายๆ เพื่อประหยัดเวลา ไม่ต้องวนลูปทั้งหมด
                    int checkLimit = Math.min(nextGen.size(), 10);
                    for (int k = 0; k < checkLimit; k++) {
                        if (child.isSamePath(nextGen.get(nextGen.size() - 1 - k))) {
                            isDuplicate = true;
                            break;
                        }
                    }
                }
                
                if (!isDuplicate) {
                    nextGen.add(child);
                }
            }
            
            // ถ้าสร้างไม่ครบ (เพราะติด Duplicate เยอะ) ให้เติมด้วยตัวเก่า
            while (nextGen.size() < POPULATION_SIZE) {
                nextGen.add(population.get((int)(Math.random() * population.size())));
            }

            population = nextGen;
        }

        long duration = System.nanoTime() - startTime;
        return new AlgorithmResult("Genetic Algorithm (Pure)", bestSolution.path, bestSolution.cost, duration, POPULATION_SIZE * totalGenerations);
    }

    // --- 1. Initial population fn. ---
    private static List<Individual> initializePopulation(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
        List<Individual> pop = new ArrayList<>();
        int attempts = 0;
        // พยายามสร้าง Path จนกว่าจะครบหรือลองเกินขีดจำกัด
        while (pop.size() < POPULATION_SIZE && attempts < POPULATION_SIZE * 10) {
            attempts++;
            // ใช้ Guided DFS เพื่อให้ได้ Path ที่ดูดีตั้งแต่เริ่ม (ไม่ยึกยือมากเกินไป)
            List<int[]> path = generateGuidedRandomPath(maze, rows, cols, sr, sc, er, ec);
            if (path != null) {
                pop.add(new Individual(path, calculateCost(path, maze)));
            }
        }
        return pop;
    }

    // --- Improved: Guided Randomized DFS ---
    // แทนที่จะสุ่มทิศทางมั่วๆ 100% เราจะ "ลำเอียง" (Bias) ไปทางทิศที่เข้าใกล้เป้าหมายมากกว่า
    private static List<int[]> generateGuidedRandomPath(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[rows][cols];
        Map<String, int[]> parentMap = new HashMap<>();
        
        stack.push(new int[]{sr, sc});
        visited[sr][sc] = true;
        parentMap.put(sr + "," + sc, null);

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            int r = curr[0];
            int c = curr[1];

            if (r == er && c == ec) {
                return reconstructPath(parentMap, er, ec);
            }

            // *Key Improvement*: ใช้ทิศทางแบบ Guided (เรียงตามความน่าจะเป็นที่ดี)
            List<Integer> directions = getGuidedDirections(r, c, er, ec);

            for (int dir : directions) {
                int nr = r + DR[dir];
                int nc = c + DC[dir];

                if (isValid(nr, nc, rows, cols, maze) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parentMap.put(nr + "," + nc, new int[]{r, c});
                    stack.push(new int[]{nr, nc});
                }
            }
        }
        return null; 
    }
    
    // Helper: เรียงทิศทาง โดยให้ทิศที่ "ใกล้เป้าหมายที่สุด" ถูก Stack Push ทีหลัง (เพื่อให้ Pop ออกมาก่อน)
    private static List<Integer> getGuidedDirections(int r, int c, int tr, int tc) {
        List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
        
        // เรียงจาก "ใกล้เป้าหมาย" -> "ไกลเป้าหมาย" (Descending Distance)
        // เพราะ Stack เป็น LIFO เราจึงเอาตัวไกลใส่ไปก่อน ตัวใกล้ใส่ทีหลัง จะได้หยิบตัวใกล้มาใช้ก่อน
        dirs.sort((d1, d2) -> {
            int dist1 = Math.abs((r + DR[d1]) - tr) + Math.abs((c + DC[d1]) - tc);
            int dist2 = Math.abs((r + DR[d2]) - tr) + Math.abs((c + DC[d2]) - tc);
            // return มากไปน้อย (Distance เยอะ = ไกล)
            return Integer.compare(dist2, dist1); 
        });

        // Small Randomness: สลับ 2 ทิศที่ดีที่สุดบ้าง เพื่อไม่ให้เป็น Greedy Search เกินไป (ยังคงความเป็น Random/GA)
        if (Math.random() < 0.3) { 
            Collections.swap(dirs, dirs.size()-1, dirs.size()-2);
        }
        
        return dirs;
    }

    // --- 4. Select parent fn. (Tournament) ---
    private static Individual selectParent(List<Individual> pop) {
        int tournamentSize = 5;
        Individual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Individual ind = pop.get((int) (Math.random() * pop.size()));
            if (best == null || ind.fitness > best.fitness) {
                best = ind;
            }
        }
        return best;
    }

    // --- 5. Crossover fn. (Cut and Splice) ---
    private static Individual crossover(Individual p1, Individual p2, int[][] maze) {
        List<int[]> commonPoints = new ArrayList<>();
        Set<String> p1Set = new HashSet<>();
        
        for (int[] pos : p1.path) p1Set.add(pos[0] + "," + pos[1]);
        
        // หาจุดตัดร่วม (ยกเว้นจุดเริ่มและจบ เพื่อให้เกิดการเปลี่ยนเส้นทางจริงๆ)
        for (int i = 1; i < p2.path.size() - 1; i++) { 
            int[] pos = p2.path.get(i);
            if (p1Set.contains(pos[0] + "," + pos[1])) {
                commonPoints.add(pos);
            }
        }

        if (commonPoints.isEmpty()) return p1; // ถ้าไม่มีจุดร่วม ก็คืนพ่อไปเลย

        int[] cutPoint = commonPoints.get((int) (Math.random() * commonPoints.size()));
        List<int[]> newPath = new ArrayList<>();
        
        // ส่วนหัวจาก P1
        for (int[] pos : p1.path) {
            newPath.add(pos);
            if (pos[0] == cutPoint[0] && pos[1] == cutPoint[1]) break;
        }

        // ส่วนหางจาก P2
        boolean foundCut = false;
        for (int[] pos : p2.path) {
            if (pos[0] == cutPoint[0] && pos[1] == cutPoint[1]) foundCut = true;
            
            // ต้องเช็คเงื่อนไขป้องกัน Loop: ถ้าใส่หาง P2 แล้วมันวนกลับมาชนหัว P1
            if (foundCut && (pos[0] != cutPoint[0] || pos[1] != cutPoint[1])) {
                newPath.add(pos);
            }
        }
        
        return new Individual(newPath, calculateCost(newPath, maze));
    }

    // --- 6. Mutation fn. ---
    private static Individual mutate(Individual ind, int[][] maze, int rows, int cols) {
        List<int[]> path = ind.path;
        if (path.size() < 5) return ind;

        // เลือกจุด 2 จุดใน Path เพื่อสร้างทางลัดใหม่
        int idx1 = (int) (Math.random() * (path.size() - 2));
        int idx2 = (int) (Math.random() * (path.size() - idx1 - 1)) + idx1 + 1;

        int[] startNode = path.get(idx1);
        int[] endNode = path.get(idx2);

        // ใช้ Guided DFS ในการหา Mutation Path เช่นกัน เพื่อให้การกลายพันธุ์มีโอกาสได้ทางที่ดีขึ้นสูง
        List<int[]> subPath = findGuidedSubPath(startNode, endNode, maze, rows, cols);

        if (subPath != null) {
            List<int[]> newPath = new ArrayList<>();
            // หัวเดิม
            for (int i = 0; i <= idx1; i++) newPath.add(path.get(i));
            // ตรงกลางใหม่
            for (int i = 1; i < subPath.size() - 1; i++) newPath.add(subPath.get(i));
            // หางเดิม
            for (int i = idx2; i < path.size(); i++) newPath.add(path.get(i));

            return new Individual(newPath, calculateCost(newPath, maze));
        }

        return ind;
    }

    // Helper: Guided BFS/DFS สำหรับ Mutation (ใช้ Logic เดียวกับ Initialization)
    private static List<int[]> findGuidedSubPath(int[] start, int[] end, int[][] maze, int rows, int cols) {
        Queue<List<int[]>> queue = new LinkedList<>();
        List<int[]> init = new ArrayList<>();
        init.add(start);
        queue.add(init);
        Set<String> visited = new HashSet<>();
        visited.add(start[0] + "," + start[1]);

        int limit = 200; // Limit การค้นหาเพื่อไม่ให้เสียเวลามากไป
        int count = 0;

        while (!queue.isEmpty() && count < limit) {
            List<int[]> currPath = queue.poll();
            int[] currPos = currPath.get(currPath.size() - 1);
            count++;

            if (currPos[0] == end[0] && currPos[1] == end[1]) {
                return currPath;
            }

            // ใช้ Guided Directions เหมือนกัน (แต่สำหรับ BFS อาจต้อง Reverse Logic นิดหน่อย หรือใช้ Shuffle ปกติก็ได้)
            // แต่เพื่อให้ Mutation มีประสิทธิภาพ ขอใช้การสุ่มแบบปกติ (Shuffle) จะดีกว่าในระยะสั้น (BFS)
            // เพราะเราต้องการหา "ทางอื่น" ที่ไม่ใช่ทางเดิม
            List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(dirs);

            for (int d : dirs) {
                int nr = currPos[0] + DR[d];
                int nc = currPos[1] + DC[d];
                if (isValid(nr, nc, rows, cols, maze) && !visited.contains(nr + "," + nc)) {
                    visited.add(nr + "," + nc);
                    List<int[]> nextPath = new ArrayList<>(currPath);
                    nextPath.add(new int[]{nr, nc});
                    queue.add(nextPath);
                }
            }
        }
        return null;
    }

    private static boolean isValid(int r, int c, int rows, int cols, int[][] maze) {
        return r >= 0 && r < rows && c >= 0 && c < cols && maze[r][c] != -1;
    }

    private static int calculateCost(List<int[]> path, int[][] maze) {
        int sum = 0;
        for (int[] p : path) {
            sum += maze[p[0]][p[1]];
        }
        return sum;
    }

    private static List<int[]> reconstructPath(Map<String, int[]> parentMap, int er, int ec) {
        List<int[]> path = new ArrayList<>();
        int[] curr = {er, ec};
        while (curr != null) {
            path.add(curr);
            curr = parentMap.get(curr[0] + "," + curr[1]);
        }
        Collections.reverse(path);
        return path;
    }
}