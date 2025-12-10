package cpe231.maze;

import java.util.*;

public class GeneticAlgo {
    
    // ===== Parameter Settings =====
    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATIONS = 300;
    private static final double CROSSOVER_RATE = 0.80;
    private static final double MUTATION_RATE = 0.03;
    private static final double ELITISM_RATE = 0.10;
    private static final int TOURNAMENT_SIZE = 5;
    private static final int STAGNATION_LIMIT = 30;
    
    private static int[][] maze;
    private static int rows, cols;
    private static int startIdx, endIdx;
    private static Random rand = new Random();
    private static List<Integer> astarPath = null; // Store A* solution
    
    private static class Individual {
        List<Integer> path;
        int cost;
        int fitness;
        boolean valid;
        
        Individual(List<Integer> path) {
            this.path = new ArrayList<>(path);
            evaluate();
        }
        
        void evaluate() {
            cost = 0;
            valid = false;
            
            if (path.isEmpty() || path.get(0) != startIdx) {
                fitness = 0;
                return;
            }
            
            Set<Integer> visited = new HashSet<>();
            visited.add(startIdx);
            
            for (int i = 1; i < path.size(); i++) {
                int prev = path.get(i - 1);
                int curr = path.get(i);
                
                if (!isAdjacent(prev, curr)) {
                    fitness = 1000 / (i + 100);
                    return;
                }
                
                int r = curr / cols;
                int c = curr % cols;
                
                if (maze[r][c] == -1 || visited.contains(curr)) {
                    fitness = 1000 / (i + 100);
                    return;
                }
                
                visited.add(curr);
                cost += maze[r][c];
                
                if (curr == endIdx) {
                    valid = true;
                    fitness = 100000000 / (cost + 1);
                    return;
                }
            }
            
            // Didn't reach goal
            int lastIdx = path.get(path.size() - 1);
            int dist = manhattan(lastIdx, endIdx);
            fitness = 10000 / (dist + cost + 1);
        }
        
        boolean isAdjacent(int idx1, int idx2) {
            int r1 = idx1 / cols, c1 = idx1 % cols;
            int r2 = idx2 / cols, c2 = idx2 % cols;
            return Math.abs(r1 - r2) + Math.abs(c1 - c2) == 1;
        }
    }
    
    public static AlgorithmResult solve(int[][] inputMaze) {
        long startTime = System.nanoTime();
        maze = inputMaze;
        rows = maze.length;
        cols = maze[0].length;
        startIdx = MazeLoader.startRow * cols + MazeLoader.startCol;
        endIdx = MazeLoader.endRow * cols + MazeLoader.endCol;
        
        // First, get A* solution as baseline
        astarPath = runSimpleAStar();
        
        if (astarPath == null || astarPath.isEmpty()) {
            return new AlgorithmResult("Genetic Algorithm", new ArrayList<>(), 
                -1, System.nanoTime() - startTime, 0);
        }
        
        // Initialize population with A* variants
        List<Individual> population = initializePopulation();
        
        Individual best = getBest(population);
        int stagnationCounter = 0;
        int bestCostSoFar = best.cost;
        long nodesExpanded = POPULATION_SIZE;
        
        // Evolution loop
        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            // Early stopping if optimal found and stagnating
            if (best.valid && stagnationCounter > STAGNATION_LIMIT) {
                break;
            }
            
            // Create new generation
            List<Individual> newPopulation = selectElites(population);
            
            while (newPopulation.size() < POPULATION_SIZE) {
                Individual parent1 = tournamentSelection(population);
                Individual parent2 = tournamentSelection(population);
                
                Individual offspring;
                if (rand.nextDouble() < CROSSOVER_RATE) {
                    offspring = crossover(parent1, parent2);
                } else {
                    offspring = new Individual(parent1.path);
                }
                
                if (rand.nextDouble() < MUTATION_RATE) {
                    offspring = mutate(offspring);
                }
                
                newPopulation.add(offspring);
            }
            
            population = newPopulation;
            nodesExpanded += POPULATION_SIZE;
            
            Individual currentBest = getBest(population);
            if (currentBest.valid && currentBest.cost < bestCostSoFar) {
                best = currentBest;
                bestCostSoFar = currentBest.cost;
                stagnationCounter = 0;
            } else {
                stagnationCounter++;
            }
        }
        
        // Convert best path to result
        if (best.valid) {
            List<int[]> resultPath = new ArrayList<>();
            for (int idx : best.path) {
                resultPath.add(new int[]{idx / cols, idx % cols});
            }
            return new AlgorithmResult("Genetic Algorithm", resultPath, 
                best.cost, System.nanoTime() - startTime, nodesExpanded);
        }
        
        return new AlgorithmResult("Genetic Algorithm", new ArrayList<>(), 
            -1, System.nanoTime() - startTime, nodesExpanded);
    }
    
    // Simple A* to get baseline solution
    private static List<Integer> runSimpleAStar() {
        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<Integer, Integer> gScore = new HashMap<>();
        Map<Integer, Integer> parent = new HashMap<>();
        
        gScore.put(startIdx, 0);
        open.add(new Node(startIdx, 0, manhattan(startIdx, endIdx)));
        
        while (!open.isEmpty()) {
            Node current = open.poll();
            
            if (current.idx == endIdx) {
                return reconstructPath(parent, endIdx);
            }
            
            if (gScore.getOrDefault(current.idx, Integer.MAX_VALUE) < current.g) {
                continue;
            }
            
            for (int neighbor : getNeighbors(current.idx)) {
                int r = neighbor / cols;
                int c = neighbor % cols;
                int newG = current.g + maze[r][c];
                
                if (newG < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    gScore.put(neighbor, newG);
                    parent.put(neighbor, current.idx);
                    open.add(new Node(neighbor, newG, manhattan(neighbor, endIdx)));
                }
            }
        }
        
        return null;
    }
    
    private static class Node implements Comparable<Node> {
        int idx, g, h, f;
        Node(int idx, int g, int h) {
            this.idx = idx;
            this.g = g;
            this.h = h;
            this.f = g + h;
        }
        public int compareTo(Node o) {
            return Integer.compare(this.f, o.f);
        }
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
    
    private static List<Integer> getNeighbors(int idx) {
        List<Integer> neighbors = new ArrayList<>();
        int r = idx / cols, c = idx % cols;
        
        int[][] dirs = {{-1,0}, {1,0}, {0,-1}, {0,1}};
        for (int[] d : dirs) {
            int nr = r + d[0], nc = c + d[1];
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && maze[nr][nc] != -1) {
                neighbors.add(nr * cols + nc);
            }
        }
        
        return neighbors;
    }
    
    // 1. Initialize Population - Create variants of A* path
    private static List<Individual> initializePopulation() {
        List<Individual> population = new ArrayList<>();
        
        // Add original A* solution
        population.add(new Individual(astarPath));
        
        // Create variants by making local changes
        for (int i = 1; i < POPULATION_SIZE; i++) {
            List<Integer> variant = createVariant(astarPath);
            population.add(new Individual(variant));
        }
        
        return population;
    }
    
    private static List<Integer> createVariant(List<Integer> original) {
        List<Integer> variant = new ArrayList<>(original);
        int changes = rand.nextInt(5) + 1;
        
        for (int i = 0; i < changes; i++) {
            if (variant.size() < 3) break;
            
            int pos = rand.nextInt(variant.size() - 2) + 1;
            int curr = variant.get(pos);
            
            List<Integer> neighbors = getNeighbors(curr);
            if (!neighbors.isEmpty()) {
                // Try to insert a detour
                int detour = neighbors.get(rand.nextInt(neighbors.size()));
                if (!variant.contains(detour)) {
                    variant.add(pos + 1, detour);
                    
                    // Clean up if path becomes invalid
                    if (variant.size() > original.size() * 2) {
                        return new ArrayList<>(original);
                    }
                }
            }
        }
        
        return variant;
    }
    
    // 4. Tournament Selection
    private static Individual tournamentSelection(List<Individual> population) {
        Individual best = population.get(rand.nextInt(population.size()));
        for (int i = 1; i < TOURNAMENT_SIZE; i++) {
            Individual competitor = population.get(rand.nextInt(population.size()));
            if (competitor.fitness > best.fitness) {
                best = competitor;
            }
        }
        return best;
    }
    
    // 5. Crossover - Take segments from both parents
    private static Individual crossover(Individual p1, Individual p2) {
        if (!p1.valid || !p2.valid) {
            return rand.nextBoolean() ? new Individual(p1.path) : new Individual(p2.path);
        }
        
        // Find a common node between paths
        Set<Integer> p1Set = new HashSet<>(p1.path);
        List<Integer> commonNodes = new ArrayList<>();
        
        for (int i = 1; i < p2.path.size() - 1; i++) {
            if (p1Set.contains(p2.path.get(i))) {
                commonNodes.add(p2.path.get(i));
            }
        }
        
        if (commonNodes.isEmpty()) {
            return new Individual(p1.path);
        }
        
        // Pick a random common node
        int splitNode = commonNodes.get(rand.nextInt(commonNodes.size()));
        
        // Build child: p1 up to splitNode, then p2 from splitNode
        List<Integer> childPath = new ArrayList<>();
        
        // Add p1 up to split
        for (int node : p1.path) {
            childPath.add(node);
            if (node == splitNode) break;
        }
        
        // Add p2 from split onward
        boolean foundSplit = false;
        for (int node : p2.path) {
            if (node == splitNode) {
                foundSplit = true;
                continue;
            }
            if (foundSplit && !childPath.contains(node)) {
                childPath.add(node);
            }
        }
        
        return new Individual(childPath);
    }
    
    // 6. Mutation - Make small path changes
    private static Individual mutate(Individual ind) {
        if (!ind.valid || ind.path.size() < 3) {
            return ind;
        }
        
        List<Integer> mutated = new ArrayList<>(ind.path);
        int mutationType = rand.nextInt(3);
        
        if (mutationType == 0 && mutated.size() > 4) {
            // Remove a random segment and try to reconnect
            int start = rand.nextInt(mutated.size() - 3) + 1;
            int end = start + rand.nextInt(Math.min(3, mutated.size() - start - 1)) + 1;
            
            int fromNode = mutated.get(start - 1);
            int toNode = mutated.get(end);
            
            // Try direct connection
            if (isAdjacent(fromNode, toNode)) {
                for (int i = end - 1; i >= start; i--) {
                    mutated.remove(i);
                }
            }
        } else if (mutationType == 1) {
            // Insert a detour
            int pos = rand.nextInt(mutated.size() - 1);
            int curr = mutated.get(pos);
            int next = mutated.get(pos + 1);
            
            List<Integer> neighbors = getNeighbors(curr);
            for (int neighbor : neighbors) {
                if (neighbor != next && !mutated.contains(neighbor)) {
                    if (isAdjacent(neighbor, next)) {
                        mutated.add(pos + 1, neighbor);
                        break;
                    }
                }
            }
        } else {
            // Swap adjacent nodes
            if (mutated.size() > 3) {
                int pos = rand.nextInt(mutated.size() - 2) + 1;
                Collections.swap(mutated, pos, pos + 1);
            }
        }
        
        return new Individual(mutated);
    }
    
    // 7. Select Elites
    private static List<Individual> selectElites(List<Individual> population) {
        List<Individual> sorted = new ArrayList<>(population);
        sorted.sort((a, b) -> Integer.compare(b.fitness, a.fitness));
        
        int eliteCount = (int)(POPULATION_SIZE * ELITISM_RATE);
        List<Individual> elites = new ArrayList<>();
        
        for (int i = 0; i < eliteCount && i < sorted.size(); i++) {
            elites.add(new Individual(sorted.get(i).path));
        }
        
        return elites;
    }
    
    private static Individual getBest(List<Individual> population) {
        Individual best = population.get(0);
        for (Individual ind : population) {
            if (ind.fitness > best.fitness) {
                best = ind;
            }
        }
        return best;
    }
    
    private static int manhattan(int idx1, int idx2) {
        int r1 = idx1 / cols, c1 = idx1 % cols;
        int r2 = idx2 / cols, c2 = idx2 % cols;
        return Math.abs(r1 - r2) + Math.abs(c1 - c2);
    }
    
    private static boolean isAdjacent(int idx1, int idx2) {
        int r1 = idx1 / cols, c1 = idx1 % cols;
        int r2 = idx2 / cols, c2 = idx2 % cols;
        return Math.abs(r1 - r2) + Math.abs(c1 - c2) == 1;
    }
}