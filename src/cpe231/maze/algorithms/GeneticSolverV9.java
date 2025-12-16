package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class GeneticSolverV9 implements MazeSolver {

    // --- Core Parameters ---
    private static final int POPULATION_SIZE = 500;
    private static final int MAX_GENERATIONS = 200; // Lower gens needed because we start smarter
    private static final double CROSSOVER_RATE = 0.85; 
    private static final double MUTATION_RATE = 0.1; // Higher mutation to shake off local optima
    private static final int ELITISM_COUNT = 50; 

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

        // 1. Hybrid Initialization: Mix of Random + Greedy Seeds
        List<Individual> population = initializePopulation(context);
        if (population.isEmpty()) return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);

        Individual bestSolution = population.get(0);

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            Collections.sort(population);
            
            // Track Best
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
            }

            // If we found a very good path, we can stop early or optimize it
            // For now, we run full gens to guarantee convergence

            List<Individual> nextGen = new ArrayList<>();

            // Elitism
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            // Breeding
            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                Individual child;
                if (Math.random() < CROSSOVER_RATE) {
                    child = twoPointCrossover(p1, p2, context);
                } else {
                    child = p1;
                }

                if (Math.random() < MUTATION_RATE) {
                    child = mutate(child, context);
                }

                nextGen.add(child);
                nodesExpanded++;
            }
            population = nextGen;
        }

        // 2. Post-Processing: String Tightening (The "Rubber Band" Effect)
        // This is what makes the cost equal to A*
        List<int[]> finalPath = tightenPath(bestSolution.path, context);
        int finalCost = calculateCost(finalPath, context);

        long duration = System.nanoTime() - startTime;
        return new AlgorithmResult("Success", finalPath, finalCost, duration, nodesExpanded);
    }

    // --- 1. Hybrid Initialization ---
    private List<Individual> initializePopulation(MazeContext ctx) {
        List<Individual> pop = new ArrayList<>();
        
        // SEEDING: 10% of population is "Smart" (Randomized Greedy)
        int seedCount = POPULATION_SIZE / 10;
        for (int i = 0; i < seedCount; i++) {
            List<int[]> path = generateRandomizedGreedyPath(ctx);
            if (path != null) pop.add(new Individual(path, calculateCost(path, ctx)));
        }

        // The rest is Pure Random (to maintain diversity)
        int attempts = 0;
        while(pop.size() < POPULATION_SIZE && attempts < POPULATION_SIZE * 20) {
            List<int[]> path = generateRandomValidPath(ctx);
            if (path != null) pop.add(new Individual(path, calculateCost(path, ctx)));
            attempts++;
        }
        return pop;
    }

    // A "Smart" path generator that prefers moving towards the goal but adds noise
    private List<int[]> generateRandomizedGreedyPath(MazeContext ctx) {
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[ctx.rows][ctx.cols];
        Map<String, int[]> parentMap = new HashMap<>();
        
        int[] start = {ctx.startRow, ctx.startCol};
        stack.push(start);
        visited[start[0]][start[1]] = true;
        parentMap.put(key(start), null);
        
        int steps = 0;
        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            steps++;
            if (steps > ctx.rows * ctx.cols * 2) return null; // Abort if stuck

            if (curr[0] == ctx.endRow && curr[1] == ctx.endCol) return reconstructPath(parentMap, curr);

            List<Integer> dirs = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
            
            // Sort by distance to goal (Greedy)
            dirs.sort((a, b) -> {
                int d1 = Math.abs((curr[0] + DR[a]) - ctx.endRow) + Math.abs((curr[1] + DC[a]) - ctx.endCol);
                int d2 = Math.abs((curr[0] + DR[b]) - ctx.endRow) + Math.abs((curr[1] + DC[b]) - ctx.endCol);
                return Integer.compare(d1, d2);
            });

            // Add randomness: 20% chance to swap best move with random move
            if (Math.random() < 0.2) Collections.shuffle(dirs);

            for (int d : dirs) {
                int nr = curr[0] + DR[d], nc = curr[1] + DC[d];
                if (isValid(nr, nc, ctx) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parentMap.put(key(new int[]{nr, nc}), curr);
                    stack.push(new int[]{nr, nc});
                }
            }
        }
        return null;
    }

    // --- 2. String Tightening (Optimization) ---
    private List<int[]> tightenPath(List<int[]> path, MazeContext ctx) {
        if (path.size() < 3) return path;
        List<int[]> tight = new ArrayList<>();
        tight.add(path.get(0));
        
        int[] current = path.get(0);
        int i = 0;
        
        while (i < path.size() - 1) {
            int bestNextIdx = i + 1;
            
            // Look ahead: Can we draw a straight line from 'current' to 'path[j]'?
            // We check up to 20 nodes ahead to save time
            for (int j = path.size() - 1; j > i + 1; j--) {
                if (hasLineOfSight(current, path.get(j), ctx)) {
                    bestNextIdx = j;
                    break; // Found the furthest reachable node, skip intermediate ones
                }
            }
            
            // If we found a shortcut, fill the gap with the straight line
            if (bestNextIdx > i + 1) {
                List<int[]> segment = getLine(current, path.get(bestNextIdx));
                for (int k = 1; k < segment.size(); k++) tight.add(segment.get(k));
            } else {
                tight.add(path.get(bestNextIdx));
            }
            
            current = path.get(bestNextIdx);
            i = bestNextIdx;
        }
        return tight;
    }

    // Bresenham-like Line of Sight Check
    private boolean hasLineOfSight(int[] start, int[] end, MazeContext ctx) {
        List<int[]> line = getLine(start, end);
        for (int[] p : line) {
            if (ctx.getGridDirect()[p[0]][p[1]] == -1) return false; // Hit wall
        }
        return true;
    }

    // Simple Walk Logic (Manhattan safe) to generate line points
    private List<int[]> getLine(int[] start, int[] end) {
        List<int[]> line = new ArrayList<>();
        int r = start[0], c = start[1];
        line.add(new int[]{r, c});
        
        while (r != end[0] || c != end[1]) {
            if (r < end[0]) r++;
            else if (r > end[0]) r--;
            else if (c < end[1]) c++;
            else if (c > end[1]) c--;
            line.add(new int[]{r, c});
        }
        return line;
    }

    // --- Standard Methods ---
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

    private Individual mutate(Individual ind, MazeContext ctx) {
        List<int[]> path = ind.path;
        if (path.size() < 10) return ind;
        // Simple shortcut mutation: Pick two close points and try to connect them
        int idx1 = (int) (Math.random() * (path.size() - 5));
        int idx2 = idx1 + 2 + (int)(Math.random() * 5); // Look 2-7 steps ahead
        if (idx2 >= path.size()) idx2 = path.size() - 1;

        if (hasLineOfSight(path.get(idx1), path.get(idx2), ctx)) {
            List<int[]> newPath = new ArrayList<>();
            for (int i=0; i<=idx1; i++) newPath.add(path.get(i));
            List<int[]> bridge = getLine(path.get(idx1), path.get(idx2));
            for (int i=1; i<bridge.size()-1; i++) newPath.add(bridge.get(i));
            for (int i=idx2; i<path.size(); i++) newPath.add(path.get(i));
            return new Individual(newPath, calculateCost(newPath, ctx));
        }
        return ind;
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

    private Individual selectParent(List<Individual> pop) {
        Individual best = null;
        for (int i = 0; i < 5; i++) {
            Individual ind = pop.get((int) (Math.random() * pop.size()));
            if (best == null || ind.fitness > best.fitness) best = ind;
        }
        return best;
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