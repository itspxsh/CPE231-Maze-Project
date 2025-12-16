package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;
import java.util.function.Consumer;

public class HybridGASolver implements MazeSolver {

    // --- SETUP ---
    private static final int POPULATION_SIZE = 500;
    private static final int MAX_GENERATIONS = 1000;
    
    // START with V1 aggression (High exploration)
    private static final double START_MUTATION = 0.2; 
    // END with Genetic stability (High precision)
    private static final double END_MUTATION = 0.02;  
    
    private static final double ELITISM_RATE = 0.15;

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    // Callback for real-time visualization
    private Consumer<AlgorithmResult> progressCallback;

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
            return Double.compare(other.fitness, this.fitness); // Descending order
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        return solveInternal(context, null);
    }

    // NEW: Support for visualization callback
    public AlgorithmResult solveWithProgress(MazeContext context, Consumer<AlgorithmResult> callback) {
        return solveInternal(context, callback);
    }

    private AlgorithmResult solveInternal(MazeContext context, Consumer<AlgorithmResult> callback) {
        long startTime = System.nanoTime();
        long nodesExpanded = 0;
        this.progressCallback = callback;

        List<Individual> population = initializePopulation(context);
        if (population.isEmpty()) 
            return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);

        Individual bestSolution = population.get(0);

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            
            // Dynamic mutation rate
            double currentMutationRate = START_MUTATION - 
                ((START_MUTATION - END_MUTATION) * ((double)gen / MAX_GENERATIONS));

            Collections.sort(population);
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
            }

            // --- ANIMATION UPDATE ---
            // Update UI every 10 generations
            if (progressCallback != null && (gen % 10 == 0 || gen == MAX_GENERATIONS - 1)) {
                AlgorithmResult intermediate = new AlgorithmResult(
                    "Generation " + gen + "/" + MAX_GENERATIONS,
                    new ArrayList<>(bestSolution.path), // Copy path to avoid thread issues
                    bestSolution.cost,
                    System.nanoTime() - startTime,
                    nodesExpanded
                );
                progressCallback.accept(intermediate);
            }

            List<Individual> nextGen = new ArrayList<>();

            // 1. Elitism 
            int eliteCount = (int)(POPULATION_SIZE * ELITISM_RATE);
            for(int i=0; i<eliteCount && i < population.size(); i++) 
                nextGen.add(population.get(i));

            // 2. Evolution
            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                // Crossover 
                Individual child = (Math.random() < 0.85) ? crossover(p1, p2, context) : p1;

                // Mutation 
                if (Math.random() < currentMutationRate) {
                    child = mutate(child, context);
                }

                nextGen.add(child);
                nodesExpanded++;
            }
            population = nextGen;
        }

        long duration = System.nanoTime() - startTime;
        return new AlgorithmResult("Success", bestSolution.path, bestSolution.cost, duration, nodesExpanded);
    }

    // ... (Rest of the class methods remain unchanged) ...
    // Note: I will include the helper methods here to ensure the file is complete and compilable for you.

    private List<Individual> initializePopulation(MazeContext ctx) {
        List<Individual> pop = new ArrayList<>();
        int attempts = 0;
        // Limit attempts to prevent infinite loop on impossible mazes
        while (pop.size() < POPULATION_SIZE && attempts < POPULATION_SIZE * 20) {
            List<int[]> rawPath = generateRandomValidPath(ctx);
            if (rawPath != null) {
                pop.add(encode(rawPath, ctx));
            }
            attempts++;
        }
        return pop;
    }

    private Individual encode(List<int[]> path, MazeContext ctx) {
        int cost = calculateCost(path, ctx);
        double fitness = evaluateFitness(cost);
        return new Individual(path, cost, fitness);
    }

    private double evaluateFitness(int cost) {
        return 1.0 / (cost + 1);
    }

    private Individual selectParent(List<Individual> pop) {
        Individual best = null;
        int tournamentSize = 5;
        for (int i = 0; i < tournamentSize; i++) {
            Individual ind = pop.get((int) (Math.random() * pop.size()));
            if (best == null || ind.fitness > best.fitness)
                best = ind;
        }
        return best;
    }

    private Individual crossover(Individual p1, Individual p2, MazeContext ctx) {
        Set<String> p1Map = new HashSet<>();
        List<int[]> intersections = new ArrayList<>();

        for (int[] p : p1.path) p1Map.add(key(p));

        for (int i = 1; i < p2.path.size() - 1; i++) {
            if (p1Map.contains(key(p2.path.get(i))))
                intersections.add(p2.path.get(i));
        }

        if (intersections.isEmpty()) return p1;

        int[] cut = intersections.get((int) (Math.random() * intersections.size()));
        List<int[]> newPath = new ArrayList<>();

        for (int[] p : p1.path) {
            newPath.add(p);
            if (p[0] == cut[0] && p[1] == cut[1]) break;
        }

        boolean recording = false;
        for (int[] p : p2.path) {
            if (p[0] == cut[0] && p[1] == cut[1]) recording = true;
            if (recording && (p[0] != cut[0] || p[1] != cut[1])) newPath.add(p);
        }

        return encode(newPath, ctx);
    }

    private Individual mutate(Individual ind, MazeContext ctx) {
        List<int[]> path = ind.path;
        if (path.size() < 5) return ind;

        int idx1 = (int) (Math.random() * (path.size() - 2));
        int idx2 = (int) (Math.random() * (path.size() - idx1 - 1)) + idx1 + 1;

        Queue<List<int[]>> queue = new LinkedList<>();
        List<int[]> init = new ArrayList<>();
        init.add(path.get(idx1));
        queue.add(init);

        Set<String> visited = new HashSet<>();
        visited.add(key(path.get(idx1)));

        int limit = 300;
        while (!queue.isEmpty() && limit-- > 0) {
            List<int[]> currPath = queue.poll();
            int[] currPos = currPath.get(currPath.size() - 1);

            if (currPos[0] == path.get(idx2)[0] && currPos[1] == path.get(idx2)[1]) {
                List<int[]> newPath = new ArrayList<>();
                for (int i = 0; i <= idx1; i++) newPath.add(path.get(i));
                for (int i = 1; i < currPath.size() - 1; i++) newPath.add(currPath.get(i));
                for (int i = idx2; i < path.size(); i++) newPath.add(path.get(i));
                return encode(newPath, ctx);
            }

            List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(dirs);

            for (int d : dirs) {
                int nr = currPos[0] + DR[d], nc = currPos[1] + DC[d];
                if (isValid(nr, nc, ctx) && !visited.contains(nr + "," + nc)) {
                    visited.add(nr + "," + nc);
                    List<int[]> nextPath = new ArrayList<>(currPath);
                    nextPath.add(new int[] { nr, nc });
                    queue.add(nextPath);
                }
            }
        }
        return ind;
    }

    private List<int[]> generateRandomValidPath(MazeContext ctx) {
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[ctx.rows][ctx.cols];
        Map<String, int[]> parentMap = new HashMap<>();

        int[] start = { ctx.startRow, ctx.startCol };
        stack.push(start);
        visited[start[0]][start[1]] = true;
        parentMap.put(key(start), null);

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            if (curr[0] == ctx.endRow && curr[1] == ctx.endCol)
                return reconstructPath(parentMap, curr);

            List<Integer> directions = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(directions);

            for (int dir : directions) {
                int nr = curr[0] + DR[dir];
                int nc = curr[1] + DC[dir];

                if (isValid(nr, nc, ctx) && !visited[nr][nc]) {
                    visited[nr][nc] = true;
                    parentMap.put(key(new int[] { nr, nc }), curr);
                    stack.push(new int[] { nr, nc });
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
        for (int i = 1; i < path.size() - 1; i++)
            sum += grid[path.get(i)[0]][path.get(i)[1]];
        return sum;
    }

    private List<int[]> reconstructPath(Map<String, int[]> parentMap, int[] end) {
        List<int[]> path = new ArrayList<>();
        int[] curr = end;
        while (curr != null) {
            path.add(curr);
            curr = parentMap.get(key(curr));
        }
        Collections.reverse(path);
        return path;
    }

    private String key(int[] p) {
        return p[0] + "," + p[1];
    }
}