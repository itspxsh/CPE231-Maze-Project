package cpe231.maze;

import java.util.*;

public class GeneticAlgo {

// --- Optimized Hyperparameters for Speed (< 10s) ---
    private static final int POPULATION_SIZE = 60;    // เดิม 150 (ลดภาระการคำนวณต่อรุ่น)
    private static final int MAX_GENERATIONS = 1000;  // เดิม 3000
    private static final double CROSSOVER_RATE = 0.9; // เพิ่มนิดหน่อยเพื่อให้เจอคำตอบไวขึ้น
    private static final double MUTATION_RATE = 0.1;  // เดิม 0.15
    private static final int ELITISM_COUNT = 6;       // ปรับตามสัดส่วนประชากร
    private static final int TOURNAMENT_SIZE = 5;
    
    // สำคัญ! ลดเวลาการรอคอย
    private static final int STAGNATION_THRESHOLD = 15; // เดิม 80 (ถ้านิ่ง 15 รุ่นก็ถือว่าตันแล้ว)
    
    // Local Search Parameters (ตัวกินเวลาหลัก)
    private static final int LOCAL_SEARCH_INTENSITY = 5;   // เดิม 25 (หาแค่ 5 รอบพอ)
    private static final double LOCAL_SEARCH_PROB = 0.05;  // เดิม 0.4 (ทำน้อยลงมากๆ เน้นเฉพาะตัวเก่งจริง)

    // Direction arrays
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};
    
    private static int rows, cols;
    private static Random rand = new Random();

    // --- Individual Class ---
    private static class Individual implements Comparable<Individual> {
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
            // Penalize duplicate nodes
            int penalty = (path.size() - visited.size()) * 100;
            this.cost += penalty;
            
            this.fitness = 1_000_000.0 / (this.cost + 1);
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness);
        }
    }

// --- Main Solve Method ---
    public static AlgorithmResult solve(int[][] maze) {
        long startTime = System.nanoTime();
        rows = maze.length;
        cols = maze[0].length;
        int startNode = MazeLoader.startRow * cols + MazeLoader.startCol;
        int endNode = MazeLoader.endRow * cols + MazeLoader.endCol;

        // Initialize with better paths
        List<Individual> population = initializePopulation(maze, startNode, endNode);
        if (population.isEmpty()) {
            return new AlgorithmResult("Genetic Algorithm (Adaptive)", 
                new ArrayList<>(), -1, 0, 0);
        }

        Individual bestSolution = population.get(0);
        int stagnationCount = 0;
        int lastBestCost = Integer.MAX_VALUE;
        
        // ✅ ปรับแก้ตรงนี้: ลดโควตา Boost เหลือ 1 ครั้งก็พอ
        int boostCounter = 0; 
        int MAX_BOOSTS = 1; // เดิม 3 (แค่ครั้งเดียวก็รู้เรื่องแล้วสำหรับ Maze นี้)

        System.out.println(">>> GA Started: Seeking Near Optimal Solution...");

        // --- Evolution Loop ---
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            Collections.sort(population);
            Individual currentBest = population.get(0);

            if (currentBest.fitness > bestSolution.fitness) {
                bestSolution = new Individual(currentBest);
            }

            // Stagnation detection
            if (currentBest.cost == lastBestCost) {
                stagnationCount++;
            } else {
                stagnationCount = 0;
                lastBestCost = currentBest.cost;
                // ถ้าเจอคำตอบที่ดีขึ้น ให้รีเซ็ต boost counter ด้วย
                boostCounter = 0; 
            }

            boolean isBoosting = false;
            if (stagnationCount >= STAGNATION_THRESHOLD) {
                
                boostCounter++;
                if (boostCounter > MAX_BOOSTS) {
                    // เปลี่ยนข้อความให้ดู Professional
                    System.out.println(">>> Converged! Optimization complete."); 
                    break; 
                }
                
                isBoosting = true;
                stagnationCount = 0;
                
                // Diversity injection
                List<Individual> survivors = new ArrayList<>();
                for (int i = 0; i < ELITISM_COUNT; i++) {
                    survivors.add(population.get(i));
                }
                
                // Add fresh diverse individuals
                while (survivors.size() < POPULATION_SIZE / 2) {
                    List<Integer> newPath = generateSmartPath(maze, startNode, endNode);
                    if (!newPath.isEmpty()) {
                        survivors.add(new Individual(newPath, maze));
                    }
                }
                
                // Add heavily mutated elites
                while (survivors.size() < POPULATION_SIZE) {
                    Individual mutated = new Individual(bestSolution);
                    mutated = intensiveMutation(mutated, maze);
                    survivors.add(mutated);
                }
                
                population = survivors;
            }

            // Logging (เหมือนเดิม)
            if (generation % 50 == 0 || isBoosting) {
                String status = isBoosting ? "[Stuck... Boost!! 🚀]" : "[Evolving]";
                System.out.printf("Gen %-4d: Best Cost = %-6d %s%n", 
                    generation, bestSolution.cost, status);
            }

            // ... (โค้ดด้านล่างเหมือนเดิมเป๊ะ ไม่ต้องแก้) ...
            List<Individual> nextGen = new ArrayList<>();

            // Elitism with local search
            for (int i = 0; i < ELITISM_COUNT; i++) {
                Individual elite = new Individual(population.get(i));
                if (rand.nextDouble() < LOCAL_SEARCH_PROB) {
                    elite = localSearch(elite, maze);
                }
                nextGen.add(elite);
            }

            // Breeding
            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                Individual child;
                if (rand.nextDouble() < CROSSOVER_RATE) {
                    child = improvedCrossover(p1, p2, maze, startNode, endNode);
                } else {
                    child = new Individual(p1);
                }

                // Multiple mutation strategies
                if (rand.nextDouble() < MUTATION_RATE) {
                    int strategy = rand.nextInt(3);
                    switch(strategy) {
                        case 0: child = shortcutMutation(child, maze); break;
                        case 1: child = segmentReplaceMutation(child, maze); break;
                        case 2: child = localOptimizationMutation(child, maze); break;
                    }
                }

                nextGen.add(child);
            }

            population = nextGen;
        }

        // Final intensive local search on best solution
        bestSolution = intensiveLocalSearch(bestSolution, maze);
        bestSolution = greedyRepair(bestSolution, maze);

        List<int[]> resultPath = new ArrayList<>();
        for (int idx : bestSolution.path) {
            resultPath.add(new int[]{idx / cols, idx % cols});
        }

        long duration = System.nanoTime() - startTime;
        System.out.println(">>> GA Finished. Final Cost: " + bestSolution.cost);
        
        return new AlgorithmResult("Genetic Algorithm (Adaptive)", 
            resultPath, bestSolution.cost, duration, 
            (long)POPULATION_SIZE * MAX_GENERATIONS);
    }

    // --- Initialization: Smart Path Generation ---
    private static List<Individual> initializePopulation(int[][] maze, int start, int end) {
        List<Individual> pop = new ArrayList<>();
        
        // Add greedy path (similar to A*)
        List<Integer> greedyPath = generateGreedyPath(maze, start, end);
        if (!greedyPath.isEmpty()) {
            pop.add(new Individual(greedyPath, maze));
        }
        
        // Add diverse paths
        while (pop.size() < POPULATION_SIZE) {
            List<Integer> path = generateSmartPath(maze, start, end);
            if (!path.isEmpty()) {
                pop.add(new Individual(path, maze));
            }
        }
        return pop;
    }

    // Greedy path (prioritize low cost cells)
    private static List<Integer> generateGreedyPath(int[][] maze, int start, int end) {
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> 
            Integer.compare(a[1], b[1])); // [node, cost]
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        
        pq.offer(new int[]{start, 0});
        parent.put(start, -1);
        
        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int node = curr[0];
            
            if (visited.contains(node)) continue;
            visited.add(node);
            
            if (node == end) {
                return reconstructPath(parent, end);
            }
            
            int r = node / cols, c = node % cols;
            for (int i = 0; i < 4; i++) {
                int nr = r + DR[i], nc = c + DC[i];
                if (isValid(nr, nc, maze)) {
                    int next = nr * cols + nc;
                    if (!visited.contains(next)) {
                        parent.put(next, node);
                        pq.offer(new int[]{next, maze[nr][nc]});
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    // Smart random path with heuristic guidance
    private static List<Integer> generateSmartPath(int[][] maze, int start, int end) {
        Stack<Integer> stack = new Stack<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        
        stack.push(start);
        visited.add(start);
        parent.put(start, -1);
        
        int er = end / cols, ec = end % cols;

        while (!stack.isEmpty()) {
            int curr = stack.peek();
            if (curr == end) return reconstructPath(parent, end);

            List<Integer> neighbors = getNeighbors(curr, maze);
            
            // Sort by heuristic (distance to goal) with randomness
            int cr = curr / cols, cc = curr % cols;
            neighbors.sort((a, b) -> {
                int ar = a / cols, ac = a % cols;
                int br = b / cols, bc = b % cols;
                int distA = Math.abs(ar - er) + Math.abs(ac - ec);
                int distB = Math.abs(br - er) + Math.abs(bc - ec);
                return Integer.compare(distA + rand.nextInt(5), distB + rand.nextInt(5));
            });
            
            boolean foundNext = false;
            for (int next : neighbors) {
                if (!visited.contains(next)) {
                    visited.add(next);
                    parent.put(next, curr);
                    stack.push(next);
                    foundNext = true;
                    break;
                }
            }
            if (!foundNext) stack.pop();
        }
        return new ArrayList<>();
    }

    // --- Improved Crossover (Path-preserving) ---
    private static Individual improvedCrossover(Individual p1, Individual p2, 
                                               int[][] maze, int start, int end) {
        Set<Integer> p1Set = new HashSet<>(p1.path);
        List<Integer> common = new ArrayList<>();
        
        for (int node : p2.path) {
            if (p1Set.contains(node)) {
                common.add(node);
            }
        }
        
        if (common.size() < 2) {
            return new Individual(p1);
        }
        
        // Random crossover point from common nodes
        int splitIdx = rand.nextInt(common.size());
        int splitNode = common.get(splitIdx);
        
        List<Integer> newPath = new ArrayList<>();
        
        // Take from p1 until split
        for (int node : p1.path) {
            newPath.add(node);
            if (node == splitNode) break;
        }
        
        // Take from p2 after split
        boolean afterSplit = false;
        for (int node : p2.path) {
            if (node == splitNode) {
                afterSplit = true;
                continue;
            }
            if (afterSplit) {
                newPath.add(node);
            }
        }
        
        // Validate path connectivity
        if (!isPathValid(newPath)) {
            // Repair path
            newPath = repairPath(newPath, maze, start, end);
        }
        
        return new Individual(newPath, maze);
    }

    // --- Mutation Strategies ---
    
    // 1. Shortcut Mutation (Safe Bounds Fix)
    private static Individual shortcutMutation(Individual ind, int[][] maze) {
        List<Integer> path = ind.path;
        if (path.size() < 5) return ind;

        // สุ่มจุดเริ่มต้น
        int idx1 = rand.nextInt(path.size() - 2);
        
        // ✅ FIX: คำนวณขอบเขต idx2 ให้ปลอดภัย 100%
        // เราต้องการกระโดดไปข้างหน้าอย่างน้อย 2 ช่อง แต่ไม่เกิน 30 ช่อง และห้ามเกินขนาด path
        int maxLookAhead = 30;
        int minIdx2 = idx1 + 2; 
        int maxIdx2 = Math.min(path.size() - 1, idx1 + maxLookAhead);

        // ถ้าคำนวณแล้วขอบเขตผิดพลาด (เช่น path สั้นเกินไปในช่วงท้าย) ให้คืนค่าเดิม
        if (minIdx2 > maxIdx2) return ind;

        // สุ่ม idx2 ในช่วงที่ปลอดภัย [minIdx2, maxIdx2]
        int idx2 = rand.nextInt(maxIdx2 - minIdx2 + 1) + minIdx2;

        List<Integer> shortcut = dijkstraShortestPath(path.get(idx1), path.get(idx2), maze);
        
        // ถ้าหาทางลัดได้ และทางลัดมี Cost น้อยกว่าทางเดิม ให้ใช้ทางลัด
        if (shortcut != null) {
            // คำนวณ Cost เพื่อเปรียบเทียบ (ใช้ subList ต้องระวัง index +1)
            int oldCost = calculateCost(path.subList(idx1, idx2 + 1), maze);
            int newCost = calculateCost(shortcut, maze);

            if (newCost < oldCost) {
                List<Integer> newPath = new ArrayList<>();
                for (int i = 0; i <= idx1; i++) newPath.add(path.get(i));
                // add ทางลัด (ตัดหัวตัดท้ายออกเพราะซ้ำกับจุดเชื่อม)
                for (int i = 1; i < shortcut.size() - 1; i++) newPath.add(shortcut.get(i));
                for (int i = idx2; i < path.size(); i++) newPath.add(path.get(i));
                return new Individual(newPath, maze);
            }
        }
        return ind;
    }

    // 2. Segment Replace Mutation (Safe Bounds Fix)
    private static Individual segmentReplaceMutation(Individual ind, int[][] maze) {
        List<Integer> path = ind.path;
        if (path.size() < 8) return ind;

        // แบ่งครึ่ง path เพื่อหาจุดเริ่มที่ปลอดภัย
        int segStart = rand.nextInt(path.size() / 2);
        
        // ✅ FIX: คำนวณขอบเขต segEnd ให้ปลอดภัย
        // ต้องการความยาว segment อย่างน้อย 1 ช่อง และจบไม่เกิน path.size() - 1
        int maxSegEnd = path.size() - 1;
        int minSegEnd = segStart + 2; // อย่างน้อยห่างกัน 2 ช่องเพื่อให้มีตรงกลางให้ replace
        
        if (minSegEnd > maxSegEnd) return ind;

        int segEnd = rand.nextInt(maxSegEnd - minSegEnd + 1) + minSegEnd;

        List<Integer> alternate = dijkstraShortestPath(path.get(segStart), path.get(segEnd), maze);
        
        if (alternate != null) {
             int oldCost = calculateCost(path.subList(segStart, segEnd + 1), maze);
             int newCost = calculateCost(alternate, maze);

             if (newCost < oldCost) {
                List<Integer> newPath = new ArrayList<>();
                for (int i = 0; i < segStart; i++) newPath.add(path.get(i));
                newPath.addAll(alternate); // Dijkstra คืน path เต็ม รวมหัวท้าย ใส่ได้เลยแต่มักจะซ้ำ ต้องระวัง
                // หมายเหตุ: Dijkstra คืน path ที่รวม start/end node
                // ถ้าจะให้เนียน ควรลบ start ของ alternate (เพราะซ้ำกับ path[segStart]) 
                // แต่ List.addAll มันต่อง่าย ถ้าไม่ซีเรียสเรื่องโหนดซ้ำนิดหน่อย (Fitness เรามี penalty แล้ว) ก็ปล่อยได้
                // แต่เพื่อความถูกต้องเป๊ะๆ:
                /* for (int i = 1; i < alternate.size() - 1; i++) newPath.add(alternate.get(i));
                */
                // ถ้าใช้ addAll แบบเดิม ต้องระวัง Path ยาวขึ้นโดยไม่จำเป็น 
                // โค้ดเดิมของคุณใช้วิธีแทรก alternate ทั้งก้อน แล้วต่อด้วย segEnd+1 ซึ่งถูกต้องครับ
                for (int i = segEnd + 1; i < path.size(); i++) newPath.add(path.get(i));
                
                // เพื่อความชัวร์ ให้ลบตัวซ้ำออก (Clean Path)
                List<Integer> cleanPath = new ArrayList<>();
                if (!newPath.isEmpty()) cleanPath.add(newPath.get(0));
                for(int i=1; i<newPath.size(); i++) {
                    if(!newPath.get(i).equals(newPath.get(i-1))) {
                        cleanPath.add(newPath.get(i));
                    }
                }
                
                return new Individual(cleanPath, maze);
             }
        }
        return ind;
    }

    // 3. Local Optimization Mutation
    private static Individual localOptimizationMutation(Individual ind, int[][] maze) {
        List<Integer> path = new ArrayList<>(ind.path);
        
        for (int i = 0; i < Math.min(5, path.size() - 2); i++) {
            int idx = rand.nextInt(path.size() - 2) + 1;
            int curr = path.get(idx);
            
            // Try swapping with better neighbor
            List<Integer> neighbors = getNeighbors(curr, maze);
            int bestNeighbor = curr;
            int bestCost = maze[curr / cols][curr % cols];
            
            for (int n : neighbors) {
                int cost = maze[n / cols][n % cols];
                if (cost < bestCost && !path.contains(n)) {
                    bestCost = cost;
                    bestNeighbor = n;
                }
            }
            
            if (bestNeighbor != curr) {
                path.set(idx, bestNeighbor);
            }
        }
        
        if (isPathValid(path)) {
            return new Individual(path, maze);
        }
        return ind;
    }

    // Intensive mutation for diversity
    private static Individual intensiveMutation(Individual ind, int[][] maze) {
        Individual result = new Individual(ind);
        for (int i = 0; i < 3; i++) {
            int strategy = rand.nextInt(3);
            switch(strategy) {
                case 0: result = shortcutMutation(result, maze); break;
                case 1: result = segmentReplaceMutation(result, maze); break;
                case 2: result = localOptimizationMutation(result, maze); break;
            }
        }
        return result;
    }

    // --- Local Search (2-opt like improvement) ---
    private static Individual localSearch(Individual ind, int[][] maze) {
        List<Integer> path = new ArrayList<>(ind.path);
        boolean improved = true;
        int iterations = 0;
        
        while (improved && iterations < LOCAL_SEARCH_INTENSITY) {
            improved = false;
            iterations++;
            
            for (int i = 0; i < path.size() - 3; i++) {
                for (int j = i + 2; j < Math.min(i + 25, path.size() - 1); j++) {
                    // Use weighted shortest path instead of BFS
                    List<Integer> shortcut = dijkstraShortestPath(path.get(i), path.get(j), maze);
                    
                    if (shortcut != null && calculateCost(shortcut, maze) < calculateCost(path.subList(i, j + 1), maze)) {
                        List<Integer> newPath = new ArrayList<>();
                        for (int k = 0; k <= i; k++) newPath.add(path.get(k));
                        for (int k = 1; k < shortcut.size() - 1; k++) newPath.add(shortcut.get(k));
                        for (int k = j; k < path.size(); k++) newPath.add(path.get(k));
                        
                        path = newPath;
                        improved = true;
                        break;
                    }
                }
                if (improved) break;
            }
        }
        
        return new Individual(path, maze);
    }

    // Intensive local search for final polishing
    private static Individual intensiveLocalSearch(Individual ind, int[][] maze) {
        List<Integer> path = new ArrayList<>(ind.path);
        boolean improved = true;
        int iterations = 0;
        
        while (improved && iterations < 50) {
            improved = false;
            iterations++;
            
            // Try all possible shortcuts with weighted paths
            for (int i = 0; i < path.size() - 2; i++) {
                for (int j = i + 2; j < Math.min(i + 30, path.size()); j++) {
                    List<Integer> shortcut = dijkstraShortestPath(path.get(i), path.get(j), maze);
                    
                    if (shortcut != null) {
                        int currentCost = calculateCost(path.subList(i, j + 1), maze);
                        int shortcutCost = calculateCost(shortcut, maze);
                        
                        if (shortcutCost < currentCost) {
                            List<Integer> newPath = new ArrayList<>();
                            for (int k = 0; k <= i; k++) newPath.add(path.get(k));
                            for (int k = 1; k < shortcut.size() - 1; k++) newPath.add(shortcut.get(k));
                            for (int k = j; k < path.size(); k++) newPath.add(path.get(k));
                            
                            path = newPath;
                            improved = true;
                            break;
                        }
                    }
                }
                if (improved) break;
            }
        }
        
        return new Individual(path, maze);
    }

    // Greedy repair: replace each segment with optimal path
    private static Individual greedyRepair(Individual ind, int[][] maze) {
        List<Integer> path = new ArrayList<>(ind.path);
        
        for (int windowSize = 5; windowSize <= 15; windowSize += 5) {
            for (int i = 0; i < path.size() - windowSize; i++) {
                List<Integer> optimal = dijkstraShortestPath(path.get(i), path.get(i + windowSize), maze);
                
                if (optimal != null) {
                    int currentCost = calculateCost(path.subList(i, i + windowSize + 1), maze);
                    int optimalCost = calculateCost(optimal, maze);
                    
                    if (optimalCost < currentCost) {
                        List<Integer> newPath = new ArrayList<>();
                        for (int k = 0; k <= i; k++) newPath.add(path.get(k));
                        for (int k = 1; k < optimal.size() - 1; k++) newPath.add(optimal.get(k));
                        for (int k = i + windowSize; k < path.size(); k++) newPath.add(path.get(k));
                        path = newPath;
                    }
                }
            }
        }
        
        return new Individual(path, maze);
    }

    // Calculate path cost
    private static int calculateCost(List<Integer> path, int[][] maze) {
        int cost = 0;
        for (int node : path) {
            cost += maze[node / cols][node % cols];
        }
        return cost;
    }

    // Dijkstra for weighted shortest path (respects cell costs)
    private static List<Integer> dijkstraShortestPath(int start, int end, int[][] maze) {
        if (start == end) return Arrays.asList(start);
        
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]));
        Map<Integer, Integer> dist = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        
        pq.offer(new int[]{start, 0});
        dist.put(start, 0);
        parent.put(start, -1);
        
        int explored = 0;
        while (!pq.isEmpty() && explored++ < 2000) {
            int[] curr = pq.poll();
            int node = curr[0];
            int currDist = curr[1];
            
            if (node == end) return reconstructPath(parent, end);
            
            if (currDist > dist.getOrDefault(node, Integer.MAX_VALUE)) continue;
            
            for (int next : getNeighbors(node, maze)) {
                int nextCost = maze[next / cols][next % cols];
                int newDist = currDist + nextCost;
                
                if (newDist < dist.getOrDefault(next, Integer.MAX_VALUE)) {
                    dist.put(next, newDist);
                    parent.put(next, node);
                    pq.offer(new int[]{next, newDist});
                }
            }
        }
        return null;
    }

    // --- Helper Methods ---
    
    private static Individual selectParent(List<Individual> pop) {
        Individual best = pop.get(rand.nextInt(pop.size()));
        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            Individual ind = pop.get(rand.nextInt(pop.size()));
            if (ind.fitness > best.fitness) best = ind;
        }
        return best;
    }

    private static List<Integer> bfsShortestPath(int start, int end, int[][] maze) {
        if (start == end) return Arrays.asList(start);
        
        Queue<Integer> q = new LinkedList<>();
        Map<Integer, Integer> parent = new HashMap<>();
        Set<Integer> visited = new HashSet<>();
        
        q.add(start);
        visited.add(start);
        parent.put(start, -1);
        
        int depth = 0;
        while (!q.isEmpty() && depth++ < 500) {
            int curr = q.poll();
            if (curr == end) return reconstructPath(parent, end);
            
            for (int next : getNeighbors(curr, maze)) {
                if (!visited.contains(next)) {
                    visited.add(next);
                    parent.put(next, curr);
                    q.add(next);
                }
            }
        }
        return null;
    }

    private static boolean isPathValid(List<Integer> path) {
        if (path.size() < 2) return false;
        for (int i = 0; i < path.size() - 1; i++) {
            if (!areAdjacent(path.get(i), path.get(i + 1))) {
                return false;
            }
        }
        return true;
    }

    private static boolean areAdjacent(int a, int b) {
        int ar = a / cols, ac = a % cols;
        int br = b / cols, bc = b % cols;
        return Math.abs(ar - br) + Math.abs(ac - bc) == 1;
    }

    private static List<Integer> repairPath(List<Integer> path, int[][] maze, 
                                           int start, int end) {
        List<Integer> repaired = new ArrayList<>();
        repaired.add(path.get(0));
        
        for (int i = 1; i < path.size(); i++) {
            int prev = repaired.get(repaired.size() - 1);
            int curr = path.get(i);
            
            if (areAdjacent(prev, curr)) {
                repaired.add(curr);
            } else {
                List<Integer> bridge = bfsShortestPath(prev, curr, maze);
                if (bridge != null) {
                    for (int j = 1; j < bridge.size(); j++) {
                        repaired.add(bridge.get(j));
                    }
                }
            }
        }
        
        return repaired.isEmpty() ? path : repaired;
    }

    private static List<Integer> getNeighbors(int idx, int[][] maze) {
        List<Integer> neighbors = new ArrayList<>();
        int r = idx / cols, c = idx % cols;
        for (int i = 0; i < 4; i++) {
            int nr = r + DR[i], nc = c + DC[i];
            if (isValid(nr, nc, maze)) {
                neighbors.add(nr * cols + nc);
            }
        }
        return neighbors;
    }

    private static boolean isValid(int r, int c, int[][] maze) {
        return r >= 0 && r < rows && c >= 0 && c < cols && maze[r][c] != -1;
    }

    private static List<Integer> reconstructPath(Map<Integer, Integer> parent, int curr) {
        List<Integer> path = new ArrayList<>();
        while (curr != -1) {
            path.add(curr);
            curr = parent.getOrDefault(curr, -1);
        }
        Collections.reverse(path);
        return path;
    }
}