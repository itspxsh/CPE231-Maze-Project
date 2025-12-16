package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Pure Genetic Algorithm implementation for maze pathfinding.
 * 
 * IMPROVEMENTS FROM ORIGINAL:
 * - Truly pure GA with random-only mutations (no BFS/pathfinding in mutation)
 * - Progressive evolution tracking for real-time visualization
 * - Better fitness function that penalizes incomplete paths
 * - Biased random walk for faster initial population generation
 * - Evolution metrics logging for validation
 * 
 * Thread-safe: Each instance maintains independent state.
 * Safe for concurrent benchmark execution.
 * 
 * @version 2.0
 */
public class PureGASolver implements MazeSolver {

    // === CONFIGURATION ===
    private static final int POPULATION_SIZE = 500;
    private static final int MAX_GENERATIONS = 1000;
    
    // Dynamic mutation rate (adaptive evolution)
    private static final double START_MUTATION = 0.20;  // High exploration early
    private static final double END_MUTATION = 0.02;    // Fine-tuning late
    
    private static final double ELITISM_RATE = 0.15;    // Keep top 15%
    private static final double CROSSOVER_RATE = 0.85;  // 85% chance of crossover
    
    // Directional movement arrays
    private static final int[] ROW_DELTAS = {-1, 1, 0, 0};
    private static final int[] COL_DELTAS = {0, 0, -1, 1};
    
    // Progress callback for real-time UI updates
    private Consumer<AlgorithmResult> progressCallback = null;

    /**
     * Individual chromosome representing a candidate path solution.
     */
    private static class Individual implements Comparable<Individual> {
        List<int[]> path;
        int cost;
        double fitness;

        public Individual(List<int[]> path, int cost, double fitness) {
            this.path = new ArrayList<>(path);
            this.cost = cost;
            this.fitness = fitness;
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness); // Descending
        }
    }

    // === PUBLIC API ===

    @Override
    public AlgorithmResult solve(MazeContext context) {
        return solveInternal(context, null);
    }

    /**
     * Solve with progressive updates for real-time visualization.
     * Publishes intermediate results every 10 generations.
     */
    public AlgorithmResult solveWithProgress(MazeContext context, 
                                            Consumer<AlgorithmResult> callback) {
        return solveInternal(context, callback);
    }

    // === CORE ALGORITHM ===

    private AlgorithmResult solveInternal(MazeContext context, 
                                         Consumer<AlgorithmResult> callback) {
        long startTime = System.nanoTime();
        long nodesExpanded = 0;
        this.progressCallback = callback;

        // Validate input
        if (context.startRow == context.endRow && context.startCol == context.endCol) {
            return new AlgorithmResult("Success", 
                List.of(new int[]{context.startRow, context.startCol}), 
                0, System.nanoTime() - startTime, 0);
        }

        // Initialize population with biased random walks
        List<Individual> population = initializePopulation(context);
        if (population.isEmpty()) {
            return new AlgorithmResult("Failed", new ArrayList<>(), -1, 
                System.nanoTime() - startTime, 0);
        }

        Individual bestSolution = population.get(0);
        Individual previousBest = bestSolution;
        int stallGenerations = 0;

        System.out.println("\n=== GENETIC ALGORITHM EVOLUTION ===");
        System.out.println("Population: " + POPULATION_SIZE + " | Generations: " + MAX_GENERATIONS);
        logEvolutionMetrics(0, population, bestSolution);

        // Main evolution loop
        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            
            // Adaptive mutation rate
            double currentMutationRate = START_MUTATION - 
                ((START_MUTATION - END_MUTATION) * ((double)gen / MAX_GENERATIONS));

            // Sort by fitness (best first)
            Collections.sort(population);
            
            // Update best solution
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
                stallGenerations = 0;
            } else {
                stallGenerations++;
            }

            // Log progress every 100 generations
            if (gen % 100 == 0 || gen == MAX_GENERATIONS - 1) {
                logEvolutionMetrics(gen, population, bestSolution);
            }

            // Publish progress for UI (every 10 generations)
            if (progressCallback != null && (gen % 10 == 0 || gen == MAX_GENERATIONS - 1)) {
                publishProgress(context, bestSolution, gen, startTime, nodesExpanded);
            }

            // Early stopping if converged
            if (stallGenerations > 200 && gen > 300) {
                System.out.println("→ Early convergence at generation " + gen);
                break;
            }

            // Create next generation
            List<Individual> nextGen = new ArrayList<>();

            // 1. Elitism - preserve best solutions
            int eliteCount = (int)(POPULATION_SIZE * ELITISM_RATE);
            for (int i = 0; i < eliteCount && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            // 2. Breed new individuals
            while (nextGen.size() < POPULATION_SIZE) {
                Individual parent1 = selectParent(population);
                Individual parent2 = selectParent(population);

                Individual child;
                if (Math.random() < CROSSOVER_RATE) {
                    child = crossover(parent1, parent2, context);
                } else {
                    child = parent1; // Clone parent
                }

                // Apply mutation
                if (Math.random() < currentMutationRate) {
                    child = mutate(child, context);
                }

                nextGen.add(child);
                nodesExpanded++;
            }

            population = nextGen;
            previousBest = bestSolution;
        }

        long duration = System.nanoTime() - startTime;
        System.out.println("\n=== EVOLUTION COMPLETE ===");
        System.out.printf("Final Cost: %d | Time: %.2f ms | Nodes: %,d%n", 
            bestSolution.cost, duration / 1_000_000.0, nodesExpanded);

        return new AlgorithmResult("Success", bestSolution.path, 
            bestSolution.cost, duration, nodesExpanded);
    }

    // === INITIALIZATION ===

    /**
     * Generates initial population using biased random walks.
     * 70% bias toward goal, 30% random exploration.
     */
    private List<Individual> initializePopulation(MazeContext ctx) {
        List<Individual> population = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = POPULATION_SIZE * 30;

        while (population.size() < POPULATION_SIZE && attempts < maxAttempts) {
            List<int[]> path = generateBiasedRandomPath(ctx);
            if (path != null) {
                population.add(encode(path, ctx));
            }
            attempts++;
        }

        if (population.isEmpty()) {
            System.err.println("ERROR: Failed to generate any valid paths!");
        } else if (population.size() < POPULATION_SIZE) {
            System.err.println("WARNING: Only generated " + population.size() + 
                " paths (target: " + POPULATION_SIZE + ")");
        }

        return population;
    }

    /**
     * Generates a path using biased random walk toward goal.
     * This is ONLY for initialization - mutation remains pure random.
     */
    private List<int[]> generateBiasedRandomPath(MazeContext ctx) {
        List<int[]> path = new ArrayList<>();
        boolean[][] visited = new boolean[ctx.rows][ctx.cols];
        
        int[] current = {ctx.startRow, ctx.startCol};
        path.add(current);
        visited[current[0]][current[1]] = true;

        int maxSteps = ctx.rows * ctx.cols * 2; // Prevent infinite loops
        int steps = 0;

        while (steps++ < maxSteps) {
            // Check if reached goal
            if (current[0] == ctx.endRow && current[1] == ctx.endCol) {
                return path;
            }

            // Get valid neighbors
            List<int[]> validMoves = new ArrayList<>();
            for (int d = 0; d < 4; d++) {
                int nr = current[0] + ROW_DELTAS[d];
                int nc = current[1] + COL_DELTAS[d];
                
                if (ctx.isValid(nr, nc) && !visited[nr][nc]) {
                    validMoves.add(new int[]{nr, nc});
                }
            }

            if (validMoves.isEmpty()) {
                // Dead end - backtrack
                if (path.size() <= 1) return null;
                path.remove(path.size() - 1);
                current = path.get(path.size() - 1);
                continue;
            }

            // 70% bias toward goal, 30% random
            int[] nextMove;
            if (Math.random() < 0.7) {
                // Pick move that minimizes Manhattan distance to goal
                nextMove = validMoves.get(0);
                int minDist = manhattanDistance(nextMove, ctx.endRow, ctx.endCol);
                
                for (int[] move : validMoves) {
                    int dist = manhattanDistance(move, ctx.endRow, ctx.endCol);
                    if (dist < minDist) {
                        minDist = dist;
                        nextMove = move;
                    }
                }
            } else {
                // Random move
                nextMove = validMoves.get((int)(Math.random() * validMoves.size()));
            }

            current = nextMove;
            path.add(current);
            visited[current[0]][current[1]] = true;
        }

        return null; // Failed to reach goal
    }

    // === GENETIC OPERATORS ===

    /**
     * Tournament selection - picks best from random subset.
     */
    private Individual selectParent(List<Individual> population) {
        Individual best = null;
        int tournamentSize = 5;

        for (int i = 0; i < tournamentSize; i++) {
            Individual candidate = population.get((int)(Math.random() * population.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }

        return best;
    }

    /**
     * Two-point crossover based on path intersection points.
     */
    private Individual crossover(Individual p1, Individual p2, MazeContext ctx) {
        // Find intersection points between the two paths
        Set<String> p1Set = new HashSet<>();
        for (int[] pos : p1.path) {
            p1Set.add(key(pos));
        }

        List<int[]> intersections = new ArrayList<>();
        for (int i = 1; i < p2.path.size() - 1; i++) {
            if (p1Set.contains(key(p2.path.get(i)))) {
                intersections.add(p2.path.get(i));
            }
        }

        if (intersections.isEmpty()) {
            return p1; // No crossover possible, return parent1
        }

        // Pick random intersection as crossover point
        int[] crossoverPoint = intersections.get((int)(Math.random() * intersections.size()));

        // Build child: p1 up to crossover point, then p2 after
        List<int[]> childPath = new ArrayList<>();
        
        // First segment from parent1
        for (int[] pos : p1.path) {
            childPath.add(pos);
            if (pos[0] == crossoverPoint[0] && pos[1] == crossoverPoint[1]) {
                break;
            }
        }

        // Second segment from parent2
        boolean afterCrossover = false;
        for (int[] pos : p2.path) {
            if (pos[0] == crossoverPoint[0] && pos[1] == crossoverPoint[1]) {
                afterCrossover = true;
                continue; // Skip the crossover point itself
            }
            if (afterCrossover) {
                childPath.add(pos);
            }
        }

        return encode(childPath, ctx);
    }

    /**
     * PURE RANDOM MUTATION - No pathfinding algorithms!
     * Replaces a path segment with random valid walk.
     * 
     * This is the key difference from the hybrid version:
     * - NO BFS/DFS/A* used here
     * - Only random directional choices
     * - Maintains GA purity as required by project specs
     */
    private Individual mutate(Individual ind, MazeContext ctx) {
        List<int[]> path = new ArrayList<>(ind.path);
        if (path.size() < 5) return ind;

        // Select random segment to mutate
        int startIdx = 1 + (int)(Math.random() * (path.size() - 3));
        int segmentLength = Math.min(8, path.size() - startIdx - 1);
        int endIdx = startIdx + (int)(Math.random() * segmentLength) + 1;

        int[] startPos = path.get(startIdx);
        int[] endPos = path.get(endIdx);

        // Try to create random path between startPos and endPos
        List<int[]> newSegment = randomWalkBetween(startPos, endPos, ctx, 100);

        if (newSegment != null && newSegment.size() > 0) {
            // Build mutated path
            List<int[]> mutatedPath = new ArrayList<>();
            mutatedPath.addAll(path.subList(0, startIdx));
            mutatedPath.addAll(newSegment);
            mutatedPath.addAll(path.subList(endIdx, path.size()));
            
            return encode(mutatedPath, ctx);
        }

        return ind; // Mutation failed, return unchanged
    }

    /**
     * Pure random walk between two points (no heuristics).
     * This maintains GA purity - completely random exploration.
     */
    private List<int[]> randomWalkBetween(int[] start, int[] end, 
                                         MazeContext ctx, int maxSteps) {
        List<int[]> segment = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        
        int[] current = start.clone();
        segment.add(current);
        visited.add(key(current));

        for (int step = 0; step < maxSteps; step++) {
            // Reached target
            if (current[0] == end[0] && current[1] == end[1]) {
                return segment;
            }

            // Get valid unvisited neighbors
            List<int[]> validMoves = new ArrayList<>();
            for (int d = 0; d < 4; d++) {
                int nr = current[0] + ROW_DELTAS[d];
                int nc = current[1] + COL_DELTAS[d];
                
                if (ctx.isValid(nr, nc) && !visited.contains(key(new int[]{nr, nc}))) {
                    validMoves.add(new int[]{nr, nc});
                }
            }

            if (validMoves.isEmpty()) {
                // Dead end - try backtracking
                if (segment.size() > 1) {
                    segment.remove(segment.size() - 1);
                    current = segment.get(segment.size() - 1);
                } else {
                    return null; // Failed
                }
            } else {
                // Pick completely random direction (pure GA mutation)
                current = validMoves.get((int)(Math.random() * validMoves.size()));
                segment.add(current);
                visited.add(key(current));
            }
        }

        return null; // Couldn't reach target in time
    }

    // === FITNESS & ENCODING ===

    /**
     * Encodes a path into an Individual with fitness calculation.
     */
    private Individual encode(List<int[]> path, MazeContext ctx) {
        int cost = calculateCost(path, ctx);
        double fitness = evaluateFitness(path, cost, ctx);
        return new Individual(path, cost, fitness);
    }

    /**
     * Enhanced fitness function that heavily penalizes incomplete paths.
     */
    private double evaluateFitness(List<int[]> path, int cost, MazeContext ctx) {
        if (path == null || path.isEmpty()) {
            return 0.0;
        }

        int[] lastPos = path.get(path.size() - 1);
        boolean reachedGoal = (lastPos[0] == ctx.endRow && lastPos[1] == ctx.endCol);

        if (!reachedGoal) {
            // Heavy penalty for incomplete paths
            int distToGoal = manhattanDistance(lastPos, ctx.endRow, ctx.endCol);
            return 1.0 / (cost + distToGoal * 1000.0 + 100000.0);
        }

        // Reward successful paths: lower cost = higher fitness
        // Also slightly penalize longer paths (encourage efficiency)
        double costFitness = 100000.0 / (cost + 1.0);
        double lengthPenalty = path.size() * 0.05;
        
        return costFitness - lengthPenalty;
    }

    /**
     * Calculates total path cost (excludes start and goal tiles).
     */
    private int calculateCost(List<int[]> path, MazeContext ctx) {
        if (path == null || path.size() <= 1) return Integer.MAX_VALUE;

        int sum = 0;
        int[][] grid = ctx.getGridDirect();
        
        // Sum costs of intermediate cells (skip start and end)
        for (int i = 1; i < path.size() - 1; i++) {
            int[] pos = path.get(i);
            sum += grid[pos[0]][pos[1]];
        }

        return sum;
    }

    // === METRICS & LOGGING ===

    /**
     * Logs evolution metrics for validation and debugging.
     */
    private void logEvolutionMetrics(int generation, List<Individual> population, 
                                    Individual best) {
        double avgCost = population.stream()
            .mapToInt(ind -> ind.cost)
            .average()
            .orElse(0);

        double diversity = calculateDiversity(population);
        int validPaths = (int)population.stream()
            .filter(ind -> ind.cost < Integer.MAX_VALUE)
            .count();

        System.out.printf("Gen %4d | Best: %5d | Avg: %7.1f | Valid: %3d/%3d | Diversity: %.2f%n",
            generation, best.cost, avgCost, validPaths, population.size(), diversity);
    }

    /**
     * Measures population diversity (genetic variety).
     * Higher = more exploration, Lower = converging to solution.
     */
    private double calculateDiversity(List<Individual> population) {
        Set<String> uniquePaths = new HashSet<>();
        for (Individual ind : population) {
            // Use path hash as uniqueness measure
            uniquePaths.add(pathToString(ind.path));
        }
        return (double)uniquePaths.size() / population.size();
    }

    private String pathToString(List<int[]> path) {
        StringBuilder sb = new StringBuilder();
        for (int[] pos : path) {
            sb.append(pos[0]).append(",").append(pos[1]).append("|");
        }
        return sb.toString();
    }

    /**
     * Publishes intermediate results for real-time UI updates.
     */
    private void publishProgress(MazeContext ctx, Individual best, int generation,
                                 long startTime, long nodesExpanded) {
        AlgorithmResult intermediate = new AlgorithmResult(
            "Generation " + generation + "/" + MAX_GENERATIONS,
            new ArrayList<>(best.path),
            best.cost,
            System.nanoTime() - startTime,
            nodesExpanded
        );
        progressCallback.accept(intermediate);
    }

    // === UTILITY METHODS ===

    private int manhattanDistance(int[] pos, int targetRow, int targetCol) {
        return Math.abs(pos[0] - targetRow) + Math.abs(pos[1] - targetCol);
    }

    private String key(int[] pos) {
        return pos[0] + "," + pos[1];
    }
}
