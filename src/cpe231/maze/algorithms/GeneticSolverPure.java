package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class GeneticSolverPure implements MazeSolver {

    // --- GA Parameters ---
    private static final int POPULATION_SIZE = 50;   
    private static final int MAX_GENERATIONS = 100;  
    private static final double CROSSOVER_RATE = 0.8; 
    private static final double MUTATION_RATE = 0.2;  
    private static final int ELITISM_COUNT = 5; 

    // Directions
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private class Individual implements Comparable<Individual> {
        List<int[]> path;
        int cost;
        double fitness;

        public Individual(List<int[]> path, int[][] grid) {
            this.path = new ArrayList<>(path);
            this.cost = calculateCost(this.path, grid);
            // Fitness: Minimize Cost
            this.fitness = 1.0 / (this.cost + 1);
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness); 
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        
        int rows = context.rows;
        int cols = context.cols;
        int[][] grid = context.getGridDirect();

        // 1. Initialize Population
        List<Individual> population = initializePopulation(context, POPULATION_SIZE);

        if (population.isEmpty()) {
            return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);
        }

        Individual bestSolution = population.get(0);
        long nodesExpanded = POPULATION_SIZE; 

        // Main Loop
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            
            Collections.sort(population);

            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
            }

            List<Individual> nextGen = new ArrayList<>();
            // Elitism
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);
                Individual child;
                
                if (Math.random() < CROSSOVER_RATE) {
                    child = crossover(p1, p2, grid);
                } else {
                    child = p1;
                }

                if (Math.random() < MUTATION_RATE) {
                    child = mutate(child, grid, rows, cols);
                }

                nextGen.add(child);
                nodesExpanded++;
            }
            population = nextGen;
        }

        long duration = System.nanoTime() - startTime;
        
        // Return using the NEW Record format
        return new AlgorithmResult(
            "Success", 
            bestSolution.path, 
            bestSolution.cost, 
            duration, 
            nodesExpanded
        );
    }

    // --- GA Helper Methods ---

    private List<Individual> initializePopulation(MazeContext ctx, int size) {
        List<Individual> pop = new ArrayList<>();
        int attempts = 0;
        int maxAttempts = size * 10; 

        while (pop.size() < size && attempts < maxAttempts) {
            List<int[]> path = generateRandomValidPath(ctx);
            if (path != null && !path.isEmpty()) {
                pop.add(new Individual(path, ctx.getGridDirect()));
            }
            attempts++;
        }
        return pop;
    }

    private List<int[]> generateRandomValidPath(MazeContext ctx) {
        Stack<int[]> stack = new Stack<>();
        int startR = ctx.startRow, startC = ctx.startCol;
        stack.push(new int[]{startR, startC});
        
        Set<Integer> visited = new HashSet<>();
        visited.add(startR * ctx.cols + startC);
        Map<Integer, int[]> parentMap = new HashMap<>();

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            int r = curr[0];
            int c = curr[1];

            if (r == ctx.endRow && c == ctx.endCol) {
                return reconstructPath(parentMap, ctx, curr);
            }

            List<Integer> dirs = new ArrayList<>(Arrays.asList(0, 1, 2, 3));
            Collections.shuffle(dirs);

            for (int d : dirs) {
                int nr = r + DR[d];
                int nc = c + DC[d];
                int flatIdx = nr * ctx.cols + nc;

                if (isValid(nr, nc, ctx) && !visited.contains(flatIdx)) {
                    visited.add(flatIdx);
                    parentMap.put(flatIdx, curr);
                    stack.push(new int[]{nr, nc});
                }
            }
        }
        return null;
    }

    private Individual selectParent(List<Individual> pop) {
        int tournamentSize = 4;
        Individual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Individual rand = pop.get((int) (Math.random() * pop.size()));
            if (best == null || rand.fitness > best.fitness) best = rand;
        }
        return best;
    }

    private Individual crossover(Individual p1, Individual p2, int[][] grid) {
        Set<String> p1Coords = new HashSet<>();
        for (int[] pos : p1.path) p1Coords.add(pos[0] + "," + pos[1]);

        List<int[]> intersections = new ArrayList<>();
        for (int i = 1; i < p2.path.size() - 1; i++) {
            int[] pos = p2.path.get(i);
            if (p1Coords.contains(pos[0] + "," + pos[1])) {
                intersections.add(pos);
            }
        }

        if (intersections.isEmpty()) return p1;

        int[] cutPoint = intersections.get((int)(Math.random() * intersections.size()));
        List<int[]> newPath = new ArrayList<>();
        
        for (int[] pos : p1.path) {
            newPath.add(pos);
            if (pos[0] == cutPoint[0] && pos[1] == cutPoint[1]) break;
        }
        
        boolean foundCut = false;
        for (int[] pos : p2.path) {
            if (pos[0] == cutPoint[0] && pos[1] == cutPoint[1]) foundCut = true;
            if (foundCut && (pos[0] != cutPoint[0] || pos[1] != cutPoint[1])) {
                newPath.add(pos);
            }
        }
        return new Individual(newPath, grid);
    }

    private Individual mutate(Individual ind, int[][] grid, int rows, int cols) {
        if (ind.path.size() < 5) return ind;

        int idx1 = (int) (Math.random() * (ind.path.size() - 2));
        int idx2 = (int) (Math.random() * (ind.path.size() - idx1 - 1)) + idx1 + 1;

        int[] startNode = ind.path.get(idx1);
        int[] endNode = ind.path.get(idx2);

        List<int[]> shortcut = findLocalPath(startNode, endNode, grid, rows, cols);

        if (shortcut != null) {
            List<int[]> newPath = new ArrayList<>();
            for (int i = 0; i <= idx1; i++) newPath.add(ind.path.get(i));
            for (int i = 1; i < shortcut.size() - 1; i++) newPath.add(shortcut.get(i));
            for (int i = idx2; i < ind.path.size(); i++) newPath.add(ind.path.get(i));
            return new Individual(newPath, grid);
        }
        return ind;
    }

    private List<int[]> findLocalPath(int[] start, int[] end, int[][] grid, int rows, int cols) {
        Queue<List<int[]>> q = new LinkedList<>();
        List<int[]> initial = new ArrayList<>();
        initial.add(start);
        q.add(initial);
        Set<String> visited = new HashSet<>();
        visited.add(start[0] + "," + start[1]);
        int limit = 200; 

        while (!q.isEmpty() && limit-- > 0) {
            List<int[]> path = q.poll();
            int[] last = path.get(path.size() - 1);

            if (last[0] == end[0] && last[1] == end[1]) return path;

            List<Integer> dirs = Arrays.asList(0, 1, 2, 3);
            Collections.shuffle(dirs);

            for (int d : dirs) {
                int nr = last[0] + DR[d];
                int nc = last[1] + DC[d];
                String key = nr + "," + nc;

                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] != -1 && !visited.contains(key)) {
                    visited.add(key);
                    List<int[]> next = new ArrayList<>(path);
                    next.add(new int[]{nr, nc});
                    q.add(next);
                }
            }
        }
        return null;
    }

    private boolean isValid(int r, int c, MazeContext ctx) {
        return r >= 0 && r < ctx.rows && c >= 0 && c < ctx.cols && ctx.getGridDirect()[r][c] != -1;
    }

    private int calculateCost(List<int[]> path, int[][] grid) {
        int sum = 0;
        for (int[] p : path) sum += grid[p[0]][p[1]];
        return sum;
    }

    private List<int[]> reconstructPath(Map<Integer, int[]> parentMap, MazeContext ctx, int[] end) {
        List<int[]> path = new ArrayList<>();
        int[] curr = end;
        while (curr != null) {
            path.add(curr);
            int idx = curr[0] * ctx.cols + curr[1];
            if (curr[0] == ctx.startRow && curr[1] == ctx.startCol) break;
            curr = parentMap.get(idx);
        }
        Collections.reverse(path);
        return path;
    }
}