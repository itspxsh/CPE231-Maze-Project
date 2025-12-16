package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class GeneticSolverPureV5 implements MazeSolver {

    // HIGH DIVERSITY PARAMETERS
    private static final int POPULATION_SIZE = 200; 
    private static final int MAX_GENERATIONS = 1000; 
    private static final double CROSSOVER_RATE = 0.7; // Lower crossover
    private static final double MUTATION_RATE = 0.4; // 40% Mutation rate (Very High)
    private static final int ELITISM_COUNT = 2; // Only keep the absolute top 2

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private static class Individual implements Comparable<Individual> {
        List<int[]> path; int cost; double fitness;
        public Individual(List<int[]> path, int cost) {
            this.path = new ArrayList<>(path);
            this.cost = cost;
            this.fitness = 1.0 / (cost + 1); 
        }
        public int compareTo(Individual o) { return Double.compare(o.fitness, this.fitness); }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        List<Individual> population = initializePopulation(context);
        if (population.isEmpty()) return new AlgorithmResult("Failed", new ArrayList<>(), -1, 0, 0);

        Individual bestSolution = population.get(0);
        long nodesExpanded = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            Collections.sort(population);
            if (population.get(0).fitness > bestSolution.fitness) bestSolution = population.get(0);

            List<Individual> nextGen = new ArrayList<>();
            // Very Low Elitism
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) nextGen.add(population.get(i));

            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);
                Individual child;
                if (Math.random() < CROSSOVER_RATE) child = twoPointCrossover(p1, p2, context);
                else child = p1;

                if (Math.random() < MUTATION_RATE) child = mutate(child, context);
                nextGen.add(child);
                nodesExpanded++;
            }
            population = nextGen;
        }
        return new AlgorithmResult("GeneticPureV5", bestSolution.path, bestSolution.cost, System.nanoTime() - startTime, nodesExpanded);
    }

    // --- Helpers Copied from V1 ---
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
            List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(dirs); 
            for (int dir : dirs) {
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
    private Individual twoPointCrossover(Individual p1, Individual p2, MazeContext ctx) {
        Set<String> p1Map = new HashSet<>();
        List<int[]> intersections = new ArrayList<>();
        for(int[] p : p1.path) p1Map.add(key(p));
        for(int i=1; i<p2.path.size()-1; i++) {
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
                return new Individual(newPath, calculateCost(newPath, ctx));
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
    private Individual selectParent(List<Individual> pop) {
        Individual best = null;
        for (int i = 0; i < 3; i++) { // Smaller tournament for diversity
            Individual ind = pop.get((int) (Math.random() * pop.size()));
            if (best == null || ind.fitness > best.fitness) best = ind;
        }
        return best;
    }
    private boolean isValid(int r, int c, MazeContext ctx) { return r >= 0 && r < ctx.rows && c >= 0 && c < ctx.cols && ctx.getGridDirect()[r][c] != -1; }
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