package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class GeneticSolverAdaptive implements MazeSolver {

    // --- Hyperparameters ---
    private static final int POPULATION_SIZE = 150;
    private static final int MAX_GENERATIONS = 3000;
    private static final double CROSSOVER_RATE = 0.90; // เพิ่ม Crossover เพื่อผสมพันธุ์หาทางที่ดีที่สุด
    private static final double MUTATION_RATE = 0.25; 
    private static final int ELITISM_COUNT = 15; // เก็บตัวเก่งไว้เยอะขึ้น
    private static final int TOURNAMENT_SIZE = 5;
    private static final int STAGNATION_THRESHOLD = 40; 
    
    // Instance variables
    private int rows, cols;
    private final Random rand = new Random();

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        
        int[][] maze = context.getGridDirect();
        this.rows = context.rows;
        this.cols = context.cols;
        
        int startNode = context.startRow * cols + context.startCol;
        int endNode = context.endRow * cols + context.endCol;

        // 1. Initialize
        List<Individual> population = initializePopulation(maze, startNode, endNode);
        
        if (population.isEmpty()) {
            return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);
        }

        Individual bestSolution = population.get(0);
        int stagnationCount = 0;
        int lastBestCost = Integer.MAX_VALUE;
        int boostCounter = 0; 
        int MAX_BOOSTS = 5; 

        // 2. Evolution Loop
        int generation = 0;
        for (; generation < MAX_GENERATIONS; generation++) {
            Collections.sort(population);
            Individual currentBest = population.get(0);

            if (currentBest.fitness > bestSolution.fitness) {
                bestSolution = new Individual(currentBest);
            }

            // Stagnation Check
            if (currentBest.cost == lastBestCost) {
                stagnationCount++;
            } else {
                stagnationCount = 0;
                lastBestCost = currentBest.cost;
                boostCounter = 0; 
            }

            // Boost Mechanism
            if (stagnationCount >= STAGNATION_THRESHOLD) {
                boostCounter++;
                if (boostCounter > MAX_BOOSTS) {
                    break; // Converged
                }
                
                stagnationCount = 0;
                List<Individual> survivors = new ArrayList<>();
                
                // Keep Elites
                for (int i = 0; i < ELITISM_COUNT; i++) survivors.add(population.get(i));
                
                // Add Fresh Blood
                while (survivors.size() < POPULATION_SIZE / 2) {
                    List<Integer> newPath = generateSmartPath(maze, startNode, endNode);
                    if (!newPath.isEmpty()) survivors.add(new Individual(newPath, maze));
                }
                
                // Add Mutated Clones
                while (survivors.size() < POPULATION_SIZE) {
                    Individual mutated = new Individual(bestSolution);
                    mutated = intensiveMutation(mutated, maze);
                    survivors.add(mutated);
                }
                population = survivors;
            }

            // Next Gen
            List<Individual> nextGen = new ArrayList<>();
            for (int i = 0; i < ELITISM_COUNT; i++) {
                nextGen.add(new Individual(population.get(i)));
            }

            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);
                Individual child;
                
                if (rand.nextDouble() < CROSSOVER_RATE) {
                    child = improvedCrossover(p1, p2, maze, startNode, endNode);
                } else {
                    child = new Individual(p1);
                }

                if (rand.nextDouble() < MUTATION_RATE) {
                    child = intensiveMutation(child, maze); 
                }
                nextGen.add(child);
            }
            population = nextGen;
        }

        // 3. Final Polish: The "Perfect" String Tightening
        // วนลูปจนกว่าจะไม่มีการเปลี่ยนแปลง (Converge to Local Optima which is likely Global for Maze)
        boolean improved = true;
        int pass = 0;
        while (improved && pass < 10) {
            int beforeCost = bestSolution.cost;
            
            // ใช้เทคนิค Exhaustive Shortcutting
            bestSolution = exhaustiveRepair(bestSolution, maze); 
            
            if (bestSolution.cost < beforeCost) {
                improved = true;
            } else {
                improved = false;
            }
            pass++;
        }

        // Convert format
        List<int[]> resultPath = new ArrayList<>();
        for (int idx : bestSolution.path) {
            resultPath.add(new int[]{idx / cols, idx % cols});
        }

        return new AlgorithmResult("Success", 
            resultPath, bestSolution.cost, System.nanoTime() - startTime, 
            (long)POPULATION_SIZE * generation);
    }
    
    @Override
    public String getName() {
        return "Genetic Algorithm (Adaptive)";
    }

    // --- Individual Class ---
    private class Individual implements Comparable<Individual> {
        List<Integer> path;
        int cost;
        double fitness;

        public Individual(List<Integer> path, int[][] maze) {
            this.path = new ArrayList<>(path);
            calculateFitness(maze);
        }

        public Individual(Individual other) {
            this.path = new ArrayList<>(other.path);
            this.cost = other.cost;
            this.fitness = other.fitness;
        }

        private void calculateFitness(int[][] maze) {
            this.cost = 0;
            Set<Integer> visited = new HashSet<>();
            for (int idx : path) {
                this.cost += maze[idx / cols][idx % cols];
                visited.add(idx);
            }
            int penalty = (path.size() - visited.size()) * 1000; // Heavy penalty
            this.cost += penalty;
            this.fitness = 1_000_000.0 / (this.cost + 1);
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness);
        }
    }

    // --- Exhaustive Repair (The Fix) ---
    private Individual exhaustiveRepair(Individual ind, int[][] maze) {
        List<Integer> path = new ArrayList<>(ind.path);
        boolean changed = false;
        
        // ไล่เช็คทุกจุด i
        for (int i = 0; i < path.size() - 2; i++) {
            int startNode = path.get(i);
            
            // มองไปข้างหน้าไกลๆ (สูงสุด 40 ช่อง) เพื่อหาจุดที่เชื่อมหากันได้สั้นกว่า
            // เราเช็คย้อนกลับจากไกลสุดมาใกล้สุด (Greedy shortcut)
            int lookAheadLimit = Math.min(path.size() - 1, i + 40);
            
            for (int j = lookAheadLimit; j > i + 1; j--) {
                int endNode = path.get(j);
                
                // ลองหาทางลัดด้วย Dijkstra (แบบไม่จำกัด Depth มากนักในระยะสั้น)
                List<Integer> shortcut = dijkstraShortestPath(startNode, endNode, maze, 100);
                
                if (shortcut != null) {
                    // คำนวณ Cost ของเส้นทางเดิมในช่วงนี้
                    int currentSegmentCost = calculateSegmentCost(path.subList(i, j + 1), maze);
                    // คำนวณ Cost ของทางลัด
                    int shortcutCost = calculateSegmentCost(shortcut, maze);
                    
                    // ถ้าทางลัดดีกว่า (แม้แต่ 1 หน่วยก็เอา)
                    if (shortcutCost < currentSegmentCost) {
                        // สร้าง Path ใหม่
                        List<Integer> newPath = new ArrayList<>();
                        for (int k = 0; k <= i; k++) newPath.add(path.get(k)); // หัว
                        for (int k = 1; k < shortcut.size() - 1; k++) newPath.add(shortcut.get(k)); // กลาง (ทางลัด)
                        for (int k = j; k < path.size(); k++) newPath.add(path.get(k)); // ท้าย
                        
                        path = newPath;
                        changed = true;
                        
                        // ถอย index กลับไปหน่อยเผื่อการเปลี่ยนนี้เปิดโอกาสให้เปลี่ยนจุดก่อนหน้าได้
                        i = Math.max(-1, i - 2); 
                        break; // เจอทางลัดที่ดีที่สุดสำหรับ i แล้ว ขยับไป i ถัดไป
                    }
                }
            }
        }
        return changed ? new Individual(path, maze) : ind;
    }

    // --- Helpers ---
    
    private int calculateSegmentCost(List<Integer> segment, int[][] maze) {
        int c = 0;
        for (int node : segment) c += maze[node/cols][node%cols];
        return c;
    }

    private List<Individual> initializePopulation(int[][] maze, int start, int end) {
        List<Individual> pop = new ArrayList<>();
        // Seed with very strong greedy path
        List<Integer> greedy = generateGreedyPath(maze, start, end);
        if (!greedy.isEmpty()) pop.add(new Individual(greedy, maze));
        
        int attempts = 0;
        while (pop.size() < POPULATION_SIZE && attempts++ < POPULATION_SIZE * 10) {
            List<Integer> p = generateSmartPath(maze, start, end);
            if (!p.isEmpty()) pop.add(new Individual(p, maze));
        }
        return pop;
    }

    private List<Integer> generateGreedyPath(int[][] maze, int start, int end) {
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1])); 
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        pq.offer(new int[]{start, 0});
        parent.put(start, -1);
        
        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int node = curr[0];
            if (visited.contains(node)) continue;
            visited.add(node);
            if (node == end) return reconstructPath(parent, end);
            
            int r = node/cols, c = node%cols;
            for (int i=0; i<4; i++) {
                int nr = r+DR[i], nc = c+DC[i];
                if (isValid(nr, nc, maze)) {
                    int next = nr*cols+nc;
                    if (!visited.contains(next)) {
                        parent.put(next, node);
                        // Strong Heuristic Bias for initialization
                        int h = (Math.abs(nr - end/cols) + Math.abs(nc - end%cols)) * 2;
                        pq.offer(new int[]{next, maze[nr][nc] + h});
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private List<Integer> generateSmartPath(int[][] maze, int start, int end) {
        Stack<Integer> stack = new Stack<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        stack.push(start);
        visited.add(start);
        parent.put(start, -1);
        int er = end/cols, ec = end%cols;

        while (!stack.isEmpty()) {
            int curr = stack.peek();
            if (curr == end) return reconstructPath(parent, end);
            
            List<Integer> neighbors = getNeighbors(curr, maze);
            Collections.shuffle(neighbors, rand);
            neighbors.sort((a, b) -> {
                int distA = Math.abs(a/cols - er) + Math.abs(a%cols - ec);
                int distB = Math.abs(b/cols - er) + Math.abs(b%cols - ec);
                return Integer.compare(distA, distB);
            });
            
            boolean found = false;
            for (int n : neighbors) {
                if (!visited.contains(n)) {
                    visited.add(n);
                    parent.put(n, curr);
                    stack.push(n);
                    found = true;
                    break;
                }
            }
            if (!found) stack.pop();
        }
        return new ArrayList<>();
    }

    private Individual improvedCrossover(Individual p1, Individual p2, int[][] maze, int s, int e) {
        Set<Integer> p1Nodes = new HashSet<>(p1.path);
        List<Integer> common = new ArrayList<>();
        for (int n : p2.path) if (p1Nodes.contains(n)) common.add(n);
        
        if (common.size() < 2) return new Individual(p1);
        
        int splitNode = common.get(rand.nextInt(common.size()));
        List<Integer> newPath = new ArrayList<>();
        
        for (int n : p1.path) {
            newPath.add(n);
            if (n == splitNode) break;
        }
        boolean appending = false;
        for (int n : p2.path) {
            if (n == splitNode) appending = true;
            else if (appending) newPath.add(n);
        }
        
        if (!isPathValid(newPath)) return repairPath(newPath, maze);
        return new Individual(newPath, maze);
    }

    private Individual intensiveMutation(Individual ind, int[][] maze) {
        List<Integer> path = ind.path;
        if (path.size() < 5) return ind;
        
        int i1 = rand.nextInt(path.size()-2);
        int i2 = Math.min(path.size()-1, i1 + 2 + rand.nextInt(20));
        
        List<Integer> shortPath = dijkstraShortestPath(path.get(i1), path.get(i2), maze, 50);
        if (shortPath != null && calculateSegmentCost(shortPath, maze) < calculateSegmentCost(path.subList(i1, i2+1), maze)) {
            List<Integer> np = new ArrayList<>();
            for(int k=0; k<=i1; k++) np.add(path.get(k));
            for(int k=1; k<shortPath.size()-1; k++) np.add(shortPath.get(k));
            for(int k=i2; k<path.size(); k++) np.add(path.get(k));
            return new Individual(np, maze);
        }
        return ind;
    }

    // --- Dijkstra Utility ---
    private List<Integer> dijkstraShortestPath(int start, int end, int[][] maze, int limitExp) {
        if (start == end) return Arrays.asList(start);
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]));
        Map<Integer, Integer> dist = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        
        pq.offer(new int[]{start, 0});
        dist.put(start, 0);
        parent.put(start, -1);
        
        int explored = 0;
        int limit = limitExp > 0 ? limitExp * 20 : 5000; // Increased limit
        
        while (!pq.isEmpty() && explored++ < limit) {
            int[] curr = pq.poll();
            int u = curr[0];
            int d = curr[1];
            
            if (u == end) return reconstructPath(parent, end);
            if (d > dist.getOrDefault(u, Integer.MAX_VALUE)) continue;
            
            int r = u/cols, c = u%cols;
            for(int i=0; i<4; i++) {
                int nr = r+DR[i], nc = c+DC[i];
                if (isValid(nr, nc, maze)) {
                    int v = nr*cols+nc;
                    int newDist = d + maze[nr][nc];
                    if (newDist < dist.getOrDefault(v, Integer.MAX_VALUE)) {
                        dist.put(v, newDist);
                        parent.put(v, u);
                        pq.offer(new int[]{v, newDist});
                    }
                }
            }
        }
        return null;
    }
    
    // --- Basic Utilities ---
    private Individual repairPath(List<Integer> path, int[][] maze) {
        if (path.isEmpty()) return new Individual(new ArrayList<>(), maze);
        List<Integer> clean = new ArrayList<>();
        clean.add(path.get(0));
        for (int i=1; i<path.size(); i++) {
            int u = clean.get(clean.size()-1);
            int v = path.get(i);
            if (!areAdjacent(u, v)) {
                List<Integer> bridge = bfs(u, v, maze);
                if (bridge != null) {
                    for(int k=1; k<bridge.size(); k++) clean.add(bridge.get(k));
                }
            } else {
                clean.add(v);
            }
        }
        return new Individual(clean, maze);
    }
    
    private List<Integer> bfs(int start, int end, int[][] maze) {
        Queue<Integer> q = new LinkedList<>();
        Map<Integer, Integer> p = new HashMap<>();
        Set<Integer> v = new HashSet<>();
        q.add(start); v.add(start); p.put(start, -1);
        while(!q.isEmpty()) {
            int curr = q.poll();
            if(curr==end) return reconstructPath(p, end);
            for(int n : getNeighbors(curr, maze)) {
                if(!v.contains(n)) { v.add(n); p.put(n, curr); q.add(n); }
            }
        }
        return null;
    }

    private Individual selectParent(List<Individual> pop) {
        Individual best = pop.get(rand.nextInt(pop.size()));
        for (int i=1; i<TOURNAMENT_SIZE; i++) {
            Individual c = pop.get(rand.nextInt(pop.size()));
            if (c.fitness > best.fitness) best = c;
        }
        return best;
    }

    private List<Integer> reconstructPath(Map<Integer, Integer> parent, int curr) {
        List<Integer> path = new ArrayList<>();
        while (curr != -1) { path.add(curr); curr = parent.getOrDefault(curr, -1); }
        Collections.reverse(path);
        return path;
    }
    
    private List<Integer> getNeighbors(int u, int[][] maze) {
        List<Integer> list = new ArrayList<>();
        int r = u/cols, c = u%cols;
        for(int i=0; i<4; i++) {
            int nr = r+DR[i], nc = c+DC[i];
            if(isValid(nr, nc, maze)) list.add(nr*cols+nc);
        }
        return list;
    }
    
    private boolean areAdjacent(int u, int v) {
        return Math.abs(u/cols - v/cols) + Math.abs(u%cols - v%cols) == 1;
    }
    
    private boolean isPathValid(List<Integer> p) {
        if(p.size()<2) return false;
        for(int i=0; i<p.size()-1; i++) if(!areAdjacent(p.get(i), p.get(i+1))) return false;
        return true;
    }
    
    private boolean isValid(int r, int c, int[][] maze) {
        return r>=0 && r<rows && c>=0 && c<cols && maze[r][c] != -1;
    }
}