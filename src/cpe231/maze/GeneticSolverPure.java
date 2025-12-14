package cpe231.maze;

import java.util.*;

public class GeneticSolverPure implements MazeSolver {

    // --- GA Parameters Settings (ตามที่คุณกำหนดเป๊ะๆ) ---
    private static final int POPULATION_SIZE = 50;   // ลดลงเพื่อให้รันไวขึ้น
    private static final int MAX_GENERATIONS = 200;  // รอบวิวัฒนาการ
    private static final double CROSSOVER_RATE = 0.8; 
    private static final double MUTATION_RATE = 0.1; 
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
            this.fitness = 1.0 / (cost + 1); // Fitness Logic
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness);
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        
        // ดึงข้อมูลจาก Context (เพื่อให้เข้ากับ Interface)
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

        // Main Evolution Loop
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            
            // 3. Evaluate & Sort
            Collections.sort(population);

            // Update Best Solution
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
            }

            List<Individual> nextGen = new ArrayList<>();

            // 7. Elitism
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            // Create Next Generation
            while (nextGen.size() < POPULATION_SIZE) {
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

                nextGen.add(child);
            }

            population = nextGen;
        }

        long duration = System.nanoTime() - startTime;
        // คืนค่าผลลัพธ์
        return new AlgorithmResult("Genetic Algorithm (Pure)", bestSolution.path, bestSolution.cost, duration, POPULATION_SIZE * MAX_GENERATIONS);
    }

    // --- 1. Initial population fn. ---
    private static List<Individual> initializePopulation(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
        List<Individual> pop = new ArrayList<>();
        // พยายามสร้าง Path สุ่มให้ครบจำนวนประชากร
        int attempts = 0;
        while (pop.size() < POPULATION_SIZE && attempts < POPULATION_SIZE * 5) {
            attempts++;
            List<int[]> path = generateRandomValidPath(maze, rows, cols, sr, sc, er, ec);
            if (path != null) {
                pop.add(new Individual(path, calculateCost(path, maze)));
            }
        }
        return pop;
    }

    // Helper: Randomized DFS
    private static List<int[]> generateRandomValidPath(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
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

            // Shuffle neighbors
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
        return null; 
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

    // --- 5. Crossover fn. ---
    private static Individual crossover(Individual p1, Individual p2, int[][] maze) {
        List<int[]> commonPoints = new ArrayList<>();
        Set<String> p1Set = new HashSet<>();
        
        for (int[] pos : p1.path) p1Set.add(pos[0] + "," + pos[1]);
        
        for (int i = 1; i < p2.path.size() - 1; i++) { 
            int[] pos = p2.path.get(i);
            if (p1Set.contains(pos[0] + "," + pos[1])) {
                commonPoints.add(pos);
            }
        }

        if (commonPoints.isEmpty()) return p1;

        int[] cutPoint = commonPoints.get((int) (Math.random() * commonPoints.size()));
        List<int[]> newPath = new ArrayList<>();
        
        // Head from P1
        for (int[] pos : p1.path) {
            newPath.add(pos);
            if (pos[0] == cutPoint[0] && pos[1] == cutPoint[1]) break;
        }

        // Tail from P2
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
    private static Individual mutate(Individual ind, int[][] maze, int rows, int cols) {
        List<int[]> path = ind.path;
        if (path.size() < 5) return ind;

        int idx1 = (int) (Math.random() * (path.size() - 2));
        int idx2 = (int) (Math.random() * (path.size() - idx1 - 1)) + idx1 + 1;

        int[] startNode = path.get(idx1);
        int[] endNode = path.get(idx2);

        List<int[]> subPath = findSubPath(startNode, endNode, maze, rows, cols);

        if (subPath != null) {
            List<int[]> newPath = new ArrayList<>();
            for (int i = 0; i <= idx1; i++) newPath.add(path.get(i));
            for (int i = 1; i < subPath.size() - 1; i++) newPath.add(subPath.get(i));
            for (int i = idx2; i < path.size(); i++) newPath.add(path.get(i));

            return new Individual(newPath, calculateCost(newPath, maze));
        }

        return ind;
    }

    // --- Helpers ---
    private static List<int[]> findSubPath(int[] start, int[] end, int[][] maze, int rows, int cols) {
        Queue<List<int[]>> queue = new LinkedList<>();
        List<int[]> init = new ArrayList<>();
        init.add(start);
        queue.add(init);
        Set<String> visited = new HashSet<>();
        visited.add(start[0] + "," + start[1]);

        int limit = 500; 
        int count = 0;

        while (!queue.isEmpty() && count < limit) {
            List<int[]> currPath = queue.poll();
            int[] currPos = currPath.get(currPath.size() - 1);
            count++;

            if (currPos[0] == end[0] && currPos[1] == end[1]) {
                return currPath;
            }

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