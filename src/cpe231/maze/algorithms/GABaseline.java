package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class GABaseline implements MazeSolver {

    // CONTROL GROUP (BASELINE)
    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATIONS = 300;
    private static final double CROSSOVER_RATE = 0.85;
    private static final double MUTATION_RATE = 0.15;
    private static final int ELITISM_COUNT = 5;

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
        
        List<Individual> population = initializePopulation(context);
        if (population.isEmpty()) {
            return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);
        }

        Individual bestSolution = population.get(0);
        long nodesExpanded = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            Collections.sort(population);
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
            }

            if (isSolution(bestSolution, context)) break;

            List<Individual> newPop = new ArrayList<>();
            for (int i = 0; i < ELITISM_COUNT; i++) {
                if (i < population.size()) newPop.add(population.get(i));
            }

            while (newPop.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);
                Individual child = p1;

                if (Math.random() < CROSSOVER_RATE) child = crossover(p1, p2, context);
                if (Math.random() < MUTATION_RATE) child = mutate(child, context);
                newPop.add(child);
            }
            population = newPop;
            nodesExpanded += population.size();
        }

        long durationNs = System.nanoTime() - startTime;
        return new AlgorithmResult(
            isSolution(bestSolution, context) ? "Success" : "Failed",
            bestSolution.path,
            bestSolution.cost,
            durationNs, // Passing long (nanoseconds) as expected by Record
            nodesExpanded
        );
    }

    // --- HELPER METHODS ---

    private List<Individual> initializePopulation(MazeContext ctx) {
        List<Individual> pop = new ArrayList<>();
        for(int i=0; i<POPULATION_SIZE; i++) {
            List<int[]> path = bfsInit(ctx);
            if(path != null && !path.isEmpty()) pop.add(new Individual(path, calculateCost(path, ctx)));
        }
        return pop;
    }

    private List<int[]> bfsInit(MazeContext ctx) {
        // Fix: Construct start array from integer fields
        return bfsInitPart(new int[]{ctx.startRow, ctx.startCol}, ctx);
    }

    private List<int[]> bfsInitPart(int[] start, MazeContext ctx) {
        Queue<List<int[]>> queue = new LinkedList<>();
        List<int[]> initial = new ArrayList<>();
        initial.add(start);
        queue.add(initial);
        Set<String> visited = new HashSet<>();
        visited.add(key(start));
        
        int steps = 0;
        int maxSteps = (ctx.rows * ctx.cols) * 2; 

        while (!queue.isEmpty() && steps < maxSteps) {
            List<int[]> path = queue.poll();
            int[] curr = path.get(path.size() - 1);
            steps++;

            // Fix: Compare against endRow/endCol fields
            if (curr[0] == ctx.endRow && curr[1] == ctx.endCol) return path;

            List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(dirs);

            for (int d : dirs) {
                int nr = curr[0] + DR[d];
                int nc = curr[1] + DC[d];
                if (isValid(nr, nc, ctx) && !visited.contains(key(new int[]{nr, nc}))) {
                    visited.add(key(new int[]{nr, nc}));
                    List<int[]> newPath = new ArrayList<>(path);
                    newPath.add(new int[]{nr, nc});
                    queue.add(newPath);
                    if(Math.random() < 0.5) break; 
                }
            }
        }
        return null;
    }

    private Individual crossover(Individual p1, Individual p2, MazeContext ctx) {
        Set<String> p1Set = new HashSet<>();
        for (int[] p : p1.path) p1Set.add(key(p));
        
        List<int[]> common = new ArrayList<>();
        for (int[] p : p2.path) {
            if (p1Set.contains(key(p))) common.add(p);
        }
        
        if (common.isEmpty()) return p1;
        int[] splicePoint = common.get((int)(Math.random() * common.size()));
        
        List<int[]> newPath = new ArrayList<>();
        for(int[] p : p1.path) {
            newPath.add(p);
            if (key(p).equals(key(splicePoint))) break;
        }
        
        boolean appending = false;
        for(int[] p : p2.path) {
            if (key(p).equals(key(splicePoint))) appending = true;
            if (appending && !key(p).equals(key(splicePoint))) newPath.add(p);
        }
        
        return new Individual(newPath, calculateCost(newPath, ctx));
    }

    private Individual mutate(Individual ind, MazeContext ctx) {
        if (ind.path.size() < 2) return ind;
        int idx = 1 + (int)(Math.random() * (ind.path.size() - 2));
        int[] startNode = ind.path.get(idx);
        
        List<int[]> tail = bfsInitPart(startNode, ctx);
        if (tail == null) return ind;
        
        List<int[]> newPath = new ArrayList<>(ind.path.subList(0, idx));
        newPath.addAll(tail);
        return new Individual(newPath, calculateCost(newPath, ctx));
    }

    private Individual selectParent(List<Individual> pop) {
        int tournamentSize = 3;
        Individual best = null;
        for(int i=0; i<tournamentSize; i++) {
            Individual rnd = pop.get((int)(Math.random() * pop.size()));
            if (best == null || rnd.fitness > best.fitness) best = rnd;
        }
        return best;
    }

    private boolean isValid(int r, int c, MazeContext ctx) {
        // Fix: Use getCost to be safe, or check bounds manually
        return ctx.isValid(r, c);
    }

    private int calculateCost(List<int[]> path, MazeContext ctx) {
        if (path == null || path.size() <= 1) return 0;
        int sum = 0;
        for (int i = 1; i < path.size() - 1; i++) {
            // Fix: Use getCost() instead of direct grid access
            sum += ctx.getCost(path.get(i)[0], path.get(i)[1]);
        }
        return sum;
    }

    private boolean isSolution(Individual ind, MazeContext ctx) {
        if (ind.path.isEmpty()) return false;
        int[] last = ind.path.get(ind.path.size() - 1);
        // Fix: Compare against fields
        return last[0] == ctx.endRow && last[1] == ctx.endCol;
    }

    private String key(int[] p) { return p[0] + "," + p[1]; }
}