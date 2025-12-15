package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class GeneticSolverAdaptive implements MazeSolver {

    // --- Base GA Parameters ---
    private static final int BASE_POPULATION = 100;
    private static final int BASE_GENERATIONS = 200;
    private static final double ELITE_PERCENTAGE = 0.1;
    private static final double MUTATION_RATE = 0.25; // Higher mutation prevents sticking
    private static final double HEURISTIC_PROBABILITY = 0.7; // 70% chance to initialize smartly
    private static final int TOURNAMENT_SIZE = 6;

    private enum Direction { NORTH, SOUTH, WEST, EAST }

    // Helper class for Map Points
    private record Point(int r, int c) {}

    // Individual Chromosome
    private class Individual implements Comparable<Individual> {
        List<Direction> chromosome;
        double fitness;
        int cost;
        int stepsToGoal; // How close did it get?
        List<int[]> cachedPath; 
        boolean reachedGoal;

        Individual(List<Direction> chromosome) {
            this.chromosome = chromosome;
            this.fitness = 0.0;
            this.reachedGoal = false;
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness); // Descending order
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        
        // 1. Setup Context
        int[][] grid = context.getGridDirect();
        Point start = new Point(context.startRow, context.startCol);
        Point goal = new Point(context.endRow, context.endCol);
        int mazeSize = context.rows * context.cols;

        // 2. Adaptive Parameters (CRITICAL FOR LARGE MAPS)
        // Ensure population is large enough for big spaces
        int currentPopSize = Math.max(BASE_POPULATION, (int)(Math.sqrt(mazeSize) * 5)); 
        // Ensure chromosome is long enough to actually traverse the maze
        int maxSteps = Math.max((context.rows + context.cols) * 5, mazeSize); 
        // Run longer for harder mazes
        int maxGenerations = Math.max(BASE_GENERATIONS, mazeSize / 5); 

        Random rand = new Random();
        List<Individual> population = new ArrayList<>();

        // 3. Initialize Population
        for (int i = 0; i < currentPopSize; i++) {
            if (rand.nextDouble() < HEURISTIC_PROBABILITY) {
                population.add(initHeuristic(start, goal, grid, context.rows, context.cols, maxSteps, rand));
            } else {
                population.add(initRandom(maxSteps, rand));
            }
        }

        Individual bestEver = null;
        long nodesExpanded = 0;

        // 4. Evolution Loop
        for (int gen = 0; gen < maxGenerations; gen++) {
            
            // Evaluate
            for (Individual ind : population) {
                evaluate(ind, start, goal, grid, context.rows, context.cols);
                nodesExpanded++;
            }
            
            Collections.sort(population);
            Individual genBest = population.get(0);

            // Update Best Ever
            if (bestEver == null || genBest.fitness > bestEver.fitness) {
                bestEver = cloneIndividual(genBest);
            }

            // Early Exit: Goal reached with stable path
            if (bestEver.reachedGoal && gen > maxGenerations * 0.5) {
                break; 
            }

            // Create Next Generation
            List<Individual> nextGen = new ArrayList<>();
            
            // Elitism: Keep top X%
            int eliteCount = (int) (currentPopSize * ELITE_PERCENTAGE);
            for (int i = 0; i < eliteCount; i++) {
                nextGen.add(cloneIndividual(population.get(i)));
            }

            // Breeding
            while (nextGen.size() < currentPopSize) {
                Individual p1 = tournamentSelect(population, rand);
                Individual p2 = tournamentSelect(population, rand);
                
                List<Direction> childGenes = crossover(p1.chromosome, p2.chromosome, rand);
                
                if (rand.nextDouble() < MUTATION_RATE) {
                    mutate(childGenes, maxSteps, rand);
                }
                
                nextGen.add(new Individual(childGenes));
            }
            population = nextGen;
        }

        // 5. Result
        long duration = System.nanoTime() - startTime;
        
        if (bestEver != null && bestEver.cachedPath != null) {
            int finalCost = bestEver.reachedGoal ? bestEver.cost : -1;
            return new AlgorithmResult(
                bestEver.reachedGoal ? "Success" : "Timeout", 
                bestEver.cachedPath, 
                finalCost, 
                duration, 
                nodesExpanded
            );
        }

        return new AlgorithmResult("Failed", new ArrayList<>(), -1, duration, nodesExpanded);
    }

    // --- Helpers ---

    private void evaluate(Individual ind, Point start, Point goal, int[][] grid, int rows, int cols) {
        List<int[]> path = new ArrayList<>();
        path.add(new int[]{start.r, start.c});
        
        int r = start.r;
        int c = start.c;
        int cost = 0;
        int penalties = 0;
        boolean reached = false;

        for (Direction dir : ind.chromosome) {
            int nr = r, nc = c;
            switch(dir) {
                case NORTH -> nr--; case SOUTH -> nr++;
                case WEST -> nc--;  case EAST -> nc++;
            }
            
            // Check validity
            if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] != -1) {
                // Avoid loops (simple heuristic)
                boolean loop = false; 
                // Loop checking is expensive, only check last few steps for speed
                int lookback = Math.min(path.size(), 10);
                for(int i=1; i<=lookback; i++) {
                    int[] prev = path.get(path.size()-i);
                    if(prev[0]==nr && prev[1]==nc) { loop=true; break; }
                }

                if(!loop) {
                    r = nr; c = nc;
                    path.add(new int[]{r, c});
                    cost += grid[r][c];
                } else {
                    penalties += 5; // Penalty for dithering
                }
                
                if (r == goal.r && c == goal.c) {
                    reached = true;
                    break;
                }
            } else {
                penalties += 1; // Wall hit penalty
            }
        }
        
        ind.cachedPath = path;
        ind.cost = cost;
        ind.reachedGoal = reached;
        
        int distToGoal = Math.abs(r - goal.r) + Math.abs(c - goal.c);
        ind.stepsToGoal = distToGoal;

        // FITNESS FUNCTION
        if (reached) {
            // Reward low cost and short paths
            // Base 1,000,000 to ensure goal-reaching is always better than not reaching
            ind.fitness = 1_000_000.0 - (cost * 2.0) - (path.size() * 1.0);
        } else {
            // Reward getting close to the goal
            // 10,000 max for incomplete paths
            ind.fitness = 10_000.0 - (distToGoal * 100.0) - penalties;
        }
    }

    private Individual initHeuristic(Point start, Point goal, int[][] grid, int rows, int cols, int maxSteps, Random rand) {
        List<Direction> genes = new ArrayList<>();
        int r = start.r, c = start.c;
        Direction[] dirs = Direction.values();
        
        for (int i = 0; i < maxSteps; i++) {
            if (r == goal.r && c == goal.c) break;
            
            // 65% chance to move closer to goal
            if (rand.nextDouble() < 0.65) {
                Direction best = dirs[rand.nextInt(4)];
                int minDist = Integer.MAX_VALUE;
                
                for(Direction d : dirs) {
                    int nr = r, nc = c;
                    switch(d) { case NORTH->nr--; case SOUTH->nr++; case WEST->nc--; case EAST->nc++; }
                    if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] != -1) {
                        int dist = Math.abs(nr - goal.r) + Math.abs(nc - goal.c);
                        if (dist < minDist) { minDist = dist; best = d; }
                    }
                }
                genes.add(best);
                // Simulate step
                int nr = r, nc = c;
                switch(best) { case NORTH->nr--; case SOUTH->nr++; case WEST->nc--; case EAST->nc++; }
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] != -1) { r = nr; c = nc; }
            } else {
                genes.add(dirs[rand.nextInt(4)]);
            }
        }
        return new Individual(genes);
    }

    private Individual initRandom(int maxSteps, Random rand) {
        List<Direction> genes = new ArrayList<>();
        Direction[] dirs = Direction.values();
        for (int i = 0; i < maxSteps; i++) genes.add(dirs[rand.nextInt(4)]);
        return new Individual(genes);
    }

    private Individual tournamentSelect(List<Individual> pop, Random rand) {
        Individual best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual ind = pop.get(rand.nextInt(pop.size()));
            if (best == null || ind.fitness > best.fitness) best = ind;
        }
        return best;
    }

    private List<Direction> crossover(List<Direction> p1, List<Direction> p2, Random rand) {
        List<Direction> child = new ArrayList<>();
        int cut = rand.nextInt(Math.min(p1.size(), p2.size()));
        child.addAll(p1.subList(0, cut));
        child.addAll(p2.subList(cut, p2.size()));
        return child;
    }

    private void mutate(List<Direction> genes, int maxSteps, Random rand) {
        int idx = rand.nextInt(genes.size());
        genes.set(idx, Direction.values()[rand.nextInt(4)]); 
        
        // Grow or Shrink
        if (rand.nextDouble() < 0.1 && genes.size() < maxSteps) {
            genes.add(Direction.values()[rand.nextInt(4)]);
        }
    }
    
    private Individual cloneIndividual(Individual org) {
        Individual copy = new Individual(new ArrayList<>(org.chromosome));
        copy.fitness = org.fitness;
        copy.cost = org.cost;
        copy.reachedGoal = org.reachedGoal;
        copy.cachedPath = (org.cachedPath != null) ? new ArrayList<>(org.cachedPath) : null;
        return copy;
    }
}