package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class GeneticSolverV9 implements MazeSolver {

    // --- Core Parameters (Base values from your successful "Genetic" run) ---
    private static final int POPULATION_SIZE = 500;
    private static final int MAX_GENERATIONS = 1000;
    private static final double CROSSOVER_RATE = 0.85; 
    private static final double BASE_MUTATION_RATE = 0.05; // Start low
    private static final int ELITISM_COUNT = 20; // Ensure top 20 survive

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private static class Individual implements Comparable<Individual> {
        List<int[]> path;
        int cost;
        double fitness;

        public Individual(List<int[]> path, int cost) {
            this.path = new ArrayList<>(path);
            this.cost = cost;
            this.fitness = 1.0 / (cost + 1); 
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness);
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        long nodesExpanded = 0;

        // 1. Initialize with Pure Randomness (Restored to ensure diversity)
        List<Individual> population = initializePopulation(context);
        if (population.isEmpty()) return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);

        Individual bestSolution = population.get(0);
        int stagnantGens = 0;
        double currentMutationRate = BASE_MUTATION_RATE;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            Collections.sort(population);
            
            // Check for improvement
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
                stagnentGens = 0;
                currentMutationRate = BASE_MUTATION_RATE; // Reset mutation on success
            } else {
                stagnentGens++;
            }

            // 2. Adaptive Mutation: If stuck for 50 gens, increase mutation heat
            if (stagnentGens > 50) currentMutationRate = 0.20; // 4x mutation shock
            else if (stagnentGens > 20) currentMutationRate = 0.10; // 2x mutation heat

            List<Individual> nextGen = new ArrayList<>();

            // Elitism: Keep the best ones safe
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            // Breeding Loop
            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                Individual child;
                if (Math.random() < CROSSOVER_RATE) {
                    child = twoPointCrossover(p1, p2, context);
                } else {
                    child = p1;
                }

                if (Math.random() < currentMutationRate) {
                    child = mutate(child, context);
                }

                nextGen.add(child);
                nodesExpanded++;
            }
            population = nextGen;
        }

        // 3. Deterministic "Corner Cutting" Optimization
        // This guarantees the path is at least as good, usually better.
        List<int[]> optimizedPath = optimizeCorners(bestSolution.path, context);
        int finalCost = calculateCost(optimizedPath, context);

        long duration = System.nanoTime() - startTime;
        return new AlgorithmResult("Success", optimizedPath, finalCost, duration, nodesExpanded);
    }

    // --- Optimization Logic (The "Free Lunch") ---
    private List<int[]> optimizeCorners(List<int[]> path, MazeContext ctx) {
        if (path.size() < 3) return path;
        List<int[]> optimized = new ArrayList<>(path);
        boolean improved = true;
        
        // Loop until no more improvements can be made
        while (improved) {
            improved = false;
            for (int i = 0; i < optimized.size() - 2; i++) {
                int[] p1 = optimized.get(i);
                int[] p2 = optimized.get(i + 1);
                int[] p3 = optimized.get(i + 2);

                // Check if this is a "turn" (not a straight line)
                // A turn exists if x changes then y changes, or vice versa.
                // p1=(0,0), p2=(0,1), p3=(1,1) -> This is a turn.
                
                // Identify the "Alternative" corner node
                // If p2 is (p1.r, p3.c), the alt is (p3.r, p1.c)
                int altR = -1, altC = -1;
                
                if (p1[0] == p2[0] && p2[1] == p3[1]) { // Moving Row then Col
                    altR = p3[0]; altC = p1[1];
                } else if (p1[1] == p2[1] && p2[0] == p3[0]) { // Moving Col then Row
                    altR = p1[0]; altC = p3[1];
                }

                if (altR != -1 && isValid(altR, altC, ctx)) {
                    int costCurrent = ctx.getGridDirect()[p2[0]][p2[1]];
                    int costAlt = ctx.getGridDirect()[altR][altC];

                    if (costAlt < costCurrent) {
                        // Found a cheaper corner! Swap it.
                        optimized.set(i + 1, new int[]{altR, altC});
                        improved = true;
                    }
                }
            }
        }
        return optimized;
    }

    // --- Standard GA Methods (Restored to Original Logic) ---

    private Individual selectParent(List<Individual> pop) {
        Individual best = null;
        for (int i = 0; i < 5; i++) {
            Individual ind = pop.get((int) (Math.random() * pop.size()));
            if (best == null || ind.fitness > best.fitness) best = ind;
        }
        return best;
    }

    private Individual twoPointCrossover(Individual p1, Individual p2, MazeContext ctx) {
        Set<String> p1Map = new HashSet<>();
        List<int[]> intersections = new ArrayList<>();
        for(int[] p : p1.path) p1Map.add(key(p));
        for(int i = 1; i < p2.path.size() - 1; i++) {
            if (p1Map.contains(key(p2.path.get(i)))) intersections.add(p2.path.get(i));
        }
        
        if (intersections.isEmpty()) return p1;

        int[] cut = intersections.get((int)(Math.random() * intersections.size()));
        List<int[]> newPath = new ArrayList<>();
        
        for(int[] p : p1.path) {
            newPath.add(p);
            if(p[0] == cut[0] && p[1] == cut[1]) break;
        }
        
        boolean recording = false;
        for(int[] p : p2.path) {
            if(p[0] == cut[0] && p[1] == cut[1]) recording = true;
            if(recording && (p[0] != cut[0] || p[1] != cut[1])) newPath.add(p);
        }
        return new Individual(newPath, calculateCost(newPath, ctx));
    }

    private Individual mutate(Individual ind, MazeContext ctx) {
        List<int[]> path = ind.path;
        if (path.size() < 5) return ind;
        int idx1 = (int) (Math.random() * (path.size() - 2));
        int idx2 = (int) (Math.random() * (path.size() - idx1 - 1)) + idx1 + 1;
        
        // BFS Shortcut (Same as original, works well)
        Queue<List<int[]>> queue = new LinkedList<>();
        List<int[]> init = new ArrayList<>();
        init.add(path.get(idx1));
        queue.add(init);
        Set<String> visited = new HashSet<>();
        visited.add(key(path.get(idx1)));
        int limit = 150; 
        
        while (!queue.isEmpty() && limit-- > 0) {
            List<int[]> currPath = queue.poll();
            int[] currPos = currPath.get(currPath.size() - 1);
            if (currPos[0] == path.get(idx2)[0] && currPos[1] == path.get(idx2)[1]) {
                 if (currPath.size() < (idx2 - idx1 + 1) + 5) { // Looser check to allow cost improvements
                    List<int[]> newPath = new ArrayList<>();
                    for (int i = 0; i <= idx1; i++) newPath.add(path.get(i));
                    for (int i = 1; i < currPath.size() - 1; i++) newPath.add(currPath.get(i));
                    for (int i = idx2; i < path.size(); i++) newPath.add(path.get(i));
                    return new Individual(newPath, calculateCost(newPath, ctx));
                }
            }
            List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(dirs);
            for (int d : dirs) {
                int nr = currPos[0] + DR[d], nc = currPos[1] + DC[d];
                if (isValid(nr, nc, ctx) && !visited.contains(nr + "," + nc)) {
                    visited.add(nr + "," + nc);
                    List<int[]> nextPath = new ArrayList<>(currPath);
                    nextPath.add(new int[]{nr, nc});
                    queue.add(nextPath);
                }
            }
        }
        return ind;
    }

    private List<Individual> initializePopulation(MazeContext ctx) {
        List<Individual> pop = new ArrayList<>();
        int attempts = 0;
        while(pop.size() < POPULATION_SIZE && attempts < POPULATION_SIZE * 20) {
            List<int[]> path = generateRandomValidPath(ctx);
            if (path != null) pop.add(new Individual(path, calculateCost(path, ctx)));
            attempts++;
        }
        return pop;
    }

    private List<int[]> generateRandomValidPath(MazeContext ctx) {
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[ctx.rows][ctx.cols];
        Map<String, int[]> parentMap = new HashMap<>();
        int[] start = {ctx.startRow, ctx.startCol};
        stack.push(start);
        visited[start[0]][start[1]] = true;
        parentMap.put(key(start), null);

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            if (curr[0] == ctx.endRow && curr[1] == ctx.endCol) return reconstructPath(parentMap, curr);
            List<Integer> directions = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(directions); 
            for (int dir : directions) {
                int nr = curr[0] + DR[dir];
                int nc = curr[1] + DC[dir];
                if (isValid(nr, nc, ctx) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parentMap.put(key(new int[]{nr, nc}), curr);
                    stack.push(new int[]{nr, nc});
                }
            }
        }
        return null; 
    }

    private boolean isValid(int r, int c, MazeContext ctx) {
        return r >= 0 && r < ctx.rows && c >= 0 && c < ctx.cols && ctx.getGridDirect()[r][c] != -1;
    }
    private int calculateCost(List<int[]> path, MazeContext ctx) {
        if (path == null || path.size() <= 1) return 0;
        int sum = 0;
        int[][] grid = ctx.getGridDirect();
        for (int i = 1; i < path.size() - 1; i++) sum += grid[path.get(i)[0]][path.get(i)[1]];
        return sum;
    }
    private List<int[]> reconstructPath(Map<String, int[]> parentMap, int[] end) {
        List<int[]> path = new ArrayList<>();
        int[] curr = end;
        while (curr != null) { path.add(curr); curr = parentMap.get(key(curr)); }
        Collections.reverse(path);
        return path;
    }
    private String key(int[] p) { return p[0] + "," + p[1]; }
}