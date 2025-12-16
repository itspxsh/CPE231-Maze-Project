package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class GAScaled implements MazeSolver {

    // EXPERIMENT C: SEARCH VOLUME (SCALED)
    private static final int POPULATION_SIZE = 500;
    private static final int MAX_GENERATIONS = 1000;
    
    private static final double CROSSOVER_RATE = 0.85;
    private static final double MUTATION_RATE = 0.20;
    private static final int ELITISM_COUNT = 20;

    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    private static class Individual implements Comparable<Individual> {
        List<int[]> path; int cost; double fitness;
        public Individual(List<int[]> path, int cost) {
            this.path = new ArrayList<>(path);
            this.cost = cost;
            this.fitness = 1.0 / (cost + 1); 
        }
        public int compareTo(Individual other) { return Double.compare(other.fitness, this.fitness); }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        List<Individual> population = initializePopulation(context);
        if (population.isEmpty()) return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);

        Individual bestSolution = population.get(0);
        long nodesExpanded = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            Collections.sort(population);
            if (population.get(0).fitness > bestSolution.fitness) bestSolution = population.get(0);
            if (isSolution(bestSolution, context)) break;

            List<Individual> newPop = new ArrayList<>();
            for (int i = 0; i < ELITISM_COUNT; i++) if (i < population.size()) newPop.add(population.get(i));

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
        return new AlgorithmResult(isSolution(bestSolution, context) ? "Success" : "Failed", bestSolution.path, bestSolution.cost, durationNs, nodesExpanded);
    }
    
    // --- Helper Methods ---
    private List<Individual> initializePopulation(MazeContext ctx) {
        List<Individual> pop = new ArrayList<>();
        for(int i=0; i<POPULATION_SIZE; i++) {
            List<int[]> path = bfsInit(ctx);
            if(path != null) pop.add(new Individual(path, calculateCost(path, ctx)));
        }
        return pop;
    }
    private List<int[]> bfsInit(MazeContext ctx) { return bfsInitPart(new int[]{ctx.startRow, ctx.startCol}, ctx); }
    private List<int[]> bfsInitPart(int[] start, MazeContext ctx) {
        Queue<List<int[]>> q = new LinkedList<>();
        List<int[]> init = new ArrayList<>(); init.add(start); q.add(init);
        Set<String> vis = new HashSet<>(); vis.add(key(start));
        int steps=0, max=(ctx.rows*ctx.cols)*2;
        while(!q.isEmpty() && steps<max) {
            List<int[]> p = q.poll(); steps++;
            int[] c = p.get(p.size()-1);
            if(c[0]==ctx.endRow && c[1]==ctx.endCol) return p;
            List<Integer> d = Arrays.asList(0,1,2,3); Collections.shuffle(d);
            for(int dir : d) {
                int nr=c[0]+DR[dir], nc=c[1]+DC[dir];
                if(isValid(nr,nc,ctx) && !vis.contains(key(new int[]{nr,nc}))) {
                    vis.add(key(new int[]{nr,nc}));
                    List<int[]> np = new ArrayList<>(p); np.add(new int[]{nr,nc});
                    q.add(np);
                    if(Math.random()<0.5) break;
                }
            }
        }
        return null;
    }
    private Individual crossover(Individual p1, Individual p2, MazeContext ctx) { return p1; }
    private Individual mutate(Individual ind, MazeContext ctx) {
         if (ind.path.size() < 2) return ind;
        int idx = 1 + (int)(Math.random() * (ind.path.size() - 2));
        List<int[]> tail = bfsInitPart(ind.path.get(idx), ctx);
        if (tail == null) return ind;
        List<int[]> newPath = new ArrayList<>(ind.path.subList(0, idx));
        newPath.addAll(tail);
        return new Individual(newPath, calculateCost(newPath, ctx));
    }
    private Individual selectParent(List<Individual> pop) {
        Individual best = null;
        for(int i=0; i<5; i++) {
            Individual rnd = pop.get((int)(Math.random()*pop.size()));
            if(best==null || rnd.fitness > best.fitness) best=rnd;
        }
        return best;
    }
    private boolean isValid(int r, int c, MazeContext ctx) { return ctx.isValid(r, c); }
    private int calculateCost(List<int[]> path, MazeContext ctx) {
        if(path==null || path.size()<=1) return 0;
        int sum=0; for(int i=1; i<path.size()-1; i++) sum+=ctx.getCost(path.get(i)[0], path.get(i)[1]);
        return sum;
    }
    private boolean isSolution(Individual ind, MazeContext ctx) { return !ind.path.isEmpty() && ind.path.get(ind.path.size()-1)[0]==ctx.endRow && ind.path.get(ind.path.size()-1)[1]==ctx.endCol; }
    private String key(int[] p) { return p[0]+","+p[1]; }
}