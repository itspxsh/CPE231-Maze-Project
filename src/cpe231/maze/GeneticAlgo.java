package cpe231.maze;

import java.util.*;

public class GeneticAlgo {

    // --- GA Parameters Settings ---
    private static final int POPULATION_SIZE = 50;   // จำนวนประชากร (ลดลงหน่อยเพื่อให้รันไวขึ้นในเขาวงกตใหญ่)
    private static final int MAX_GENERATIONS = 200;  // จำนวนรุ่นสูงสุด (Termination Criteria)
    private static final double CROSSOVER_RATE = 0.8; // 80% โอกาสเกิดลูกใหม่
    private static final double MUTATION_RATE = 0.1;  // 10% โอกาสกลายพันธุ์ (หาเส้นทางลัดใหม่)
    private static final int ELITISM_COUNT = (int) (POPULATION_SIZE * (1 - CROSSOVER_RATE)); // เก็บตัวเทพไว้

    // ทิศทางเดิน (บน, ล่าง, ซ้าย, ขวา)
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    // Class สำหรับเก็บข้อมูลโครโมโซม (เส้นทาง 1 แบบ)
    private static class Individual implements Comparable<Individual> {
        List<int[]> path; // Genotype: ลำดับพิกัดการเดิน
        int cost;         // Phenotype: ผลรวมเวลา
        double fitness;   // Fitness Value

        public Individual(List<int[]> path, int cost) {
            this.path = new ArrayList<>(path);
            this.cost = cost;
            this.fitness = 1.0 / (cost + 1); // Fitness ยิ่ง Cost น้อย Fitness ยิ่งมาก
        }

        @Override
        public int compareTo(Individual other) {
            // เรียงจาก Fitness มาก -> น้อย (Cost น้อย -> มาก)
            return Double.compare(other.fitness, this.fitness);
        }
    }

    public static AlgorithmResult solve(int[][] maze) {
        long startTime = System.nanoTime();
        int rows = maze.length;
        int cols = maze[0].length;
        int startR = MazeLoader.startRow, startC = MazeLoader.startCol;
        int endR = MazeLoader.endRow, endC = MazeLoader.endCol;

        // 1. Initial population fn.
        List<Individual> population = initializePopulation(maze, rows, cols, startR, startC, endR, endC);

        if (population.isEmpty()) {
            return new AlgorithmResult("Genetic Algorithm", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);
        }

        Individual bestSolution = population.get(0);

        // Main Evolution Loop
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            
            // 3. Evaluate fitness fn. (ทำใน Constructor แล้ว แต่เรียกดูเพื่อ Sort)
            Collections.sort(population);

            // Update Best Solution
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
            }

            // Termination Criteria: ถ้าเจอ Cost ที่ดีมากๆ หรือหมดเวลา (Optional logic here)
            // ในที่นี้ใช้ Fixed Generation ตามโจทย์

            List<Individual> nextGen = new ArrayList<>();

            // 7. Select elitism fn.
            // เก็บตัวที่ดีที่สุดไว้ตามจำนวน ELITISM_COUNT
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            // สร้างลูกหลานจนเต็ม Population
            while (nextGen.size() < POPULATION_SIZE) {
                // 4. Select parent fn. (Tournament Selection)
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                Individual child;
                // 5. Crossover fn.
                if (Math.random() < CROSSOVER_RATE) {
                    child = crossover(p1, p2, maze);
                } else {
                    child = p1; // ไม่เกิด Crossover ก็ copy พ่อแม่มา
                }

                // 6. Mutation fn.
                if (Math.random() < MUTATION_RATE) {
                    child = mutate(child, maze, rows, cols);
                }

                nextGen.add(child);
            }

            population = nextGen;
        }

        // จบการทำงาน คืนค่าตัวที่ดีที่สุด
        long duration = System.nanoTime() - startTime;
        return new AlgorithmResult("Genetic Algorithm", bestSolution.path, bestSolution.cost, duration, POPULATION_SIZE * MAX_GENERATIONS);
    }

    // --- 1. Initial population fn. ---
    // ใช้ Randomized DFS สร้างเส้นทางที่ถูกต้อง (Valid Path) หลายๆ แบบ
    private static List<Individual> initializePopulation(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
        List<Individual> pop = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<int[]> path = generateRandomValidPath(maze, rows, cols, sr, sc, er, ec);
            if (path != null) {
                pop.add(new Individual(path, calculateCost(path, maze)));
            }
        }
        return pop;
    }

    // Helper: Randomized DFS to find a valid path
    private static List<int[]> generateRandomValidPath(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[rows][cols];
        Map<String, int[]> parentMap = new HashMap<>(); // Key: "r,c", Val: parent{r,c}
        
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

            // Shuffle neighbors to get random paths
            List<Integer> directions = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(directions); 

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
        return null; // หาทางไม่เจอ (กรณีเขาวงกตปิดตาย)
    }

    // --- 2. Encode fn. ---
    // (Implied) เรา Encode เส้นทางเป็น List<int[]> ตั้งแต่ Constructor

    // --- 4. Select parent fn. ---
    // Tournament Selection: สุ่มมา 3-5 ตัว เลือกตัวที่ดีที่สุดมาเป็นพ่อพันธุ์
    private static Individual selectParent(List<Individual> pop) {
        int tournamentSize = 5;
        Individual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Individual ind = pop.get((int) (Math.random() * pop.size()));
            if (best == null || ind.fitness > best.fitness) {
                best = ind;
            }
        }
        return best; // คืนค่าตัวที่ดีที่สุดในกลุ่มที่สุ่มมา
    }

    // --- 5. Crossover fn. ---
    // Single-Point Crossover บนจุดตัด (Intersection): หาจุดที่เส้นทางทับกัน แล้วสลับหาง
    private static Individual crossover(Individual p1, Individual p2, int[][] maze) {
        // หาจุดร่วม (Common Points)
        List<int[]> commonPoints = new ArrayList<>();
        Set<String> p1Set = new HashSet<>();
        
        for (int[] pos : p1.path) p1Set.add(pos[0] + "," + pos[1]);
        
        for (int i = 1; i < p2.path.size() - 1; i++) { // ไม่เอา Start/End
            int[] pos = p2.path.get(i);
            if (p1Set.contains(pos[0] + "," + pos[1])) {
                commonPoints.add(pos);
            }
        }

        if (commonPoints.isEmpty()) return p1; // ถ้าไม่มีจุดตัดเลย ก็คืนพ่อไป

        // สุ่มจุดตัดมา 1 จุด
        int[] cutPoint = commonPoints.get((int) (Math.random() * commonPoints.size()));

        // สร้างลูกใหม่: หัวจาก P1 + หางจาก P2 (เริ่มจากจุดตัด)
        List<int[]> newPath = new ArrayList<>();
        
        // ส่วนหัวจาก P1
        for (int[] pos : p1.path) {
            newPath.add(pos);
            if (pos[0] == cutPoint[0] && pos[1] == cutPoint[1]) break;
        }

        // ส่วนหางจาก P2 (หา index ของจุดตัดใน P2 ก่อน)
        boolean foundCut = false;
        for (int[] pos : p2.path) {
            if (pos[0] == cutPoint[0] && pos[1] == cutPoint[1]) foundCut = true;
            if (foundCut && (pos[0] != cutPoint[0] || pos[1] != cutPoint[1])) {
                newPath.add(pos);
            }
        }
        
        return new Individual(newPath, calculateCost(newPath, maze));
    }

    // --- 6. Mutation fn. ---
    // พยายามหาทางลัด: สุ่มจุด 2 จุดในเส้นทาง แล้วลองเดินหาทางใหม่เชื่อมกัน (ถ้าเชื่อมได้และสั้นกว่า/หรือแค่เชื่อมได้ ก็เอา)
    private static Individual mutate(Individual ind, int[][] maze, int rows, int cols) {
        List<int[]> path = ind.path;
        if (path.size() < 5) return ind;

        // สุ่ม 2 จุดในเส้นทาง (index ห่างกันพอสมควร)
        int idx1 = (int) (Math.random() * (path.size() - 2));
        int idx2 = (int) (Math.random() * (path.size() - idx1 - 1)) + idx1 + 1;

        int[] startNode = path.get(idx1);
        int[] endNode = path.get(idx2);

        // ลองหาทางเชื่อมใหม่ด้วย Randomized DFS แบบจำกัดความลึก (Local Search)
        List<int[]> subPath = findSubPath(startNode, endNode, maze, rows, cols);

        if (subPath != null) {
            // สร้างเส้นทางใหม่: หัวเดิม + ทางเชื่อมใหม่ + หางเดิม
            List<int[]> newPath = new ArrayList<>();
            for (int i = 0; i <= idx1; i++) newPath.add(path.get(i)); // หัว
            for (int i = 1; i < subPath.size() - 1; i++) newPath.add(subPath.get(i)); // ทางเชื่อม (ตัดหัวท้ายออกเพราะซ้ำ)
            for (int i = idx2; i < path.size(); i++) newPath.add(path.get(i)); // หาง

            return new Individual(newPath, calculateCost(newPath, maze));
        }

        return ind; // ถ้าหาทางเชื่อมไม่ได้ ก็คืนตัวเดิม
    }

    // Helpers
    private static List<int[]> findSubPath(int[] start, int[] end, int[][] maze, int rows, int cols) {
        // Simple BFS/DFS limit depth เพื่อหาทางเชื่อมสั้นๆ
        Queue<List<int[]>> queue = new LinkedList<>();
        List<int[]> init = new ArrayList<>();
        init.add(start);
        queue.add(init);
        Set<String> visited = new HashSet<>();
        visited.add(start[0] + "," + start[1]);

        int limit = 500; // จำกัดรอบไม่ให้วนนานเกิน
        int count = 0;

        while (!queue.isEmpty() && count < limit) {
            List<int[]> currPath = queue.poll();
            int[] currPos = currPath.get(currPath.size() - 1);
            count++;

            if (currPos[0] == end[0] && currPos[1] == end[1]) {
                return currPath;
            }

            // Shuffle directions
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