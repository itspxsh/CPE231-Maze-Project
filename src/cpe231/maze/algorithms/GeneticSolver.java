package cpe231.maze.algorithms;

import cpe231.maze.core.*;

import java.util.*;

public class GeneticSolver implements MazeSolver {

    // --- Parameter Settings ---

    private static final int POPULATION_SIZE = 500;

    private static final int MAX_GENERATIONS = 1000;

    // 6. Crossover rate (70%-100%)

    private static final double CROSSOVER_RATE = 0.85;

    // 7. Mutation rate (0% - 5%)

    private static final double MUTATION_RATE = 0.05;

    // 8. Elitism rate (1 minus crossover rate)

    private static final double ELITISM_RATE = 1.0 - CROSSOVER_RATE;

    private static final int[] DR = { -1, 1, 0, 0 };

    private static final int[] DC = { 0, 0, -1, 1 };

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

        long startTime = System.nanoTime();

        long nodesExpanded = 0;

        // 1. Initial population fn

        List<Individual> population = initializePopulation(context);

        if (population.isEmpty())
            return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);

        Individual bestSolution = population.get(0);

        // Termination Criteria: Loop until MAX_GENERATIONS

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {

            // Sort to prepare for elitism (ranking)

            Collections.sort(population);

            // Track best solution

            if (population.get(0).fitness > bestSolution.fitness) {

                bestSolution = population.get(0);

            }

            List<Individual> nextGen = new ArrayList<>();

            // 7. Select elitism fn

            nextGen.addAll(selectElites(population));

            // Fill the rest of the population

            while (nextGen.size() < POPULATION_SIZE) {

                // 4. Select parent fn

                Individual p1 = selectParent(population);

                Individual p2 = selectParent(population);

                Individual child;

                // 5. Crossover fn

                if (Math.random() < CROSSOVER_RATE) {

                    child = crossover(p1, p2, context);

                } else {

                    child = p1; // Clone if no crossover

                }

                // 6. Mutation fn

                if (Math.random() < MUTATION_RATE) {

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

    // --- 1. Initial population fn ---

    private List<Individual> initializePopulation(MazeContext ctx) {

        List<Individual> pop = new ArrayList<>();

        int attempts = 0;

        while (pop.size() < POPULATION_SIZE && attempts < POPULATION_SIZE * 20) {

            List<int[]> rawPath = generateRandomValidPath(ctx);

            if (rawPath != null) {

                // Calls Encode and Evaluate internally

                pop.add(encode(rawPath, ctx));

            }

            attempts++;

        }

        return pop;

    }

    // --- 2. Encode fn ---

    // Encodes a raw path into an Individual with calculated fitness

    private Individual encode(List<int[]> path, MazeContext ctx) {

        int cost = calculateCost(path, ctx);

        double fitness = evaluateFitness(cost);

        return new Individual(path, cost, fitness);

    }

    // --- 3. Evaluate fitness fn ---

    private double evaluateFitness(int cost) {

        // Simple inverse cost function

        return 1.0 / (cost + 1);

    }

    // --- 4. Select parent fn ---

    // Uses Tournament Selection

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

    // --- 5. Crossover fn ---

    // Two-point crossover (based on path intersections)

    private Individual crossover(Individual p1, Individual p2, MazeContext ctx) {

        Set<String> p1Map = new HashSet<>();

        List<int[]> intersections = new ArrayList<>();

        for (int[] p : p1.path)
            p1Map.add(key(p));

        for (int i = 1; i < p2.path.size() - 1; i++) {

            if (p1Map.contains(key(p2.path.get(i))))
                intersections.add(p2.path.get(i));

        }

        if (intersections.isEmpty())
            return p1;

        int[] cut = intersections.get((int) (Math.random() * intersections.size()));

        List<int[]> newPath = new ArrayList<>();

        // Take first part from P1

        for (int[] p : p1.path) {

            newPath.add(p);

            if (p[0] == cut[0] && p[1] == cut[1])
                break;

        }

        // Take second part from P2

        boolean recording = false;

        for (int[] p : p2.path) {

            if (p[0] == cut[0] && p[1] == cut[1])
                recording = true;

            if (recording && (p[0] != cut[0] || p[1] != cut[1]))
                newPath.add(p);

        }

        return encode(newPath, ctx); // Re-encode to calc new cost/fitness

    }

    // --- 6. Mutation fn ---

    // BFS Shortcut Mutation

    private Individual mutate(Individual ind, MazeContext ctx) {

        List<int[]> path = ind.path;

        if (path.size() < 5)
            return ind;

        int idx1 = (int) (Math.random() * (path.size() - 2));

        int idx2 = (int) (Math.random() * (path.size() - idx1 - 1)) + idx1 + 1;

        // Try to find a path between idx1 and idx2 that is different/shorter

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

            // If we reached the target point

            if (currPos[0] == path.get(idx2)[0] && currPos[1] == path.get(idx2)[1]) {

                List<int[]> newPath = new ArrayList<>();

                for (int i = 0; i <= idx1; i++)
                    newPath.add(path.get(i));

                for (int i = 1; i < currPath.size() - 1; i++)
                    newPath.add(currPath.get(i));

                for (int i = idx2; i < path.size(); i++)
                    newPath.add(path.get(i));

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

    // --- 7. Select elitism fn ---

    private List<Individual> selectElites(List<Individual> sortedPop) {

        List<Individual> elites = new ArrayList<>();

        int eliteCount = (int) (POPULATION_SIZE * ELITISM_RATE);

        for (int i = 0; i < eliteCount && i < sortedPop.size(); i++) {

            elites.add(sortedPop.get(i));

        }

        return elites;

    }

    // --- Helper Methods ---

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

        if (path == null || path.size() <= 1)
            return 0;

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