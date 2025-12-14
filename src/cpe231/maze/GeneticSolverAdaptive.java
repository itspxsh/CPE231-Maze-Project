package cpe231.maze;

import java.util.*;

public class GeneticSolverAdaptive implements MazeSolver {

    // --- Adaptive Parameters ---
    private static final int POPULATION_SIZE = 50;
    private static final int MAX_GENERATIONS = 500;
    
    // Adaptive Mutation vars
    private double currentMutationRate = 0.1;
    private static final int STAGNATION_LIMIT = 15;

    // Directions
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    // --- Static Inner Class (Must access only static methods) ---
    private static class Individual implements Comparable<Individual> {
        List<int[]> path;
        int cost;
        double fitness;

        public Individual(List<int[]> path, int[][] maze) {
            this.path = new ArrayList<>(path);
            // FIX: เรียก method static ได้แล้ว
            this.cost = calculateCost(path, maze);
            // Fitness สูตรเดิม
            this.fitness = Math.pow(100000.0 / (cost + 1), 2);
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness);
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        int[][] maze = context.getGrid();
        
        List<Individual> population = initializeSmartPopulation(context);
        
        if (population.isEmpty()) return new AlgorithmResult("Adaptive GA (Optimal)", new ArrayList<>(), -1, 0, 0);

        Individual bestSolution = population.get(0);
        int stagnationCount = 0;

        for (int gen = 0; gen < MAX_GENERATIONS; gen++) {
            Collections.sort(population);

            // Adaptive Logic
            if (population.get(0).cost < bestSolution.cost) {
                bestSolution = population.get(0);
                stagnationCount = 0;
                currentMutationRate = 0.1; 
                bestSolution = optimizePath(bestSolution, maze);
            } else {
                stagnationCount++;
                if (stagnationCount > STAGNATION_LIMIT) {
                    currentMutationRate = Math.min(0.6, currentMutationRate + 0.05);
                }
            }

            List<Individual> nextGen = new ArrayList<>();
            
            // Elitism
            nextGen.add(optimizePath(population.get(0), maze)); 
            nextGen.add(population.get(1));

            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                Individual child;
                if (Math.random() < 0.9) { 
                    child = crossover(p1, p2, maze);
                } else {
                    child = p1;
                }

                if (Math.random() < currentMutationRate) {
                    child = mutate(child, maze, context.rows, context.cols);
                }
                
                child = optimizePath(child, maze);
                nextGen.add(child);
            }
            population = nextGen;
        }

        bestSolution = optimizePath(bestSolution, maze);

        long duration = System.nanoTime() - startTime;
        return new AlgorithmResult("Genetic Algorithm (Adaptive)", bestSolution.path, bestSolution.cost, duration, POPULATION_SIZE * MAX_GENERATIONS);
    }

    // --- Helpers (Changed all to static to fix error) ---

    private static List<Individual> initializeSmartPopulation(MazeContext ctx) {
        List<Individual> pop = new ArrayList<>();
        List<int[]> aStarPath = generateNoisyAStar(ctx, 0.0);
        if (aStarPath != null) pop.add(new Individual(aStarPath, ctx.getGrid()));

        while(pop.size() < POPULATION_SIZE) {
            double noise = 1.5 + Math.random() * 3.5; 
            List<int[]> path = generateNoisyAStar(ctx, noise);
            if (path != null) pop.add(new Individual(path, ctx.getGrid()));
        }
        return pop;
    }

    private static List<int[]> generateNoisyAStar(MazeContext ctx, double noiseFactor) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(ctx.startRow, ctx.startCol, 0, 0));
        
        int[][] dist = new int[ctx.rows][ctx.cols];
        for(int[] row : dist) Arrays.fill(row, Integer.MAX_VALUE);
        dist[ctx.startRow][ctx.startCol] = 0;
        
        Map<String, int[]> parent = new HashMap<>();
        parent.put(ctx.startRow+","+ctx.startCol, null);

        while(!pq.isEmpty()) {
            Node curr = pq.poll();
            if(curr.r == ctx.endRow && curr.c == ctx.endCol) return reconstructPath(parent, ctx.endRow, ctx.endCol);
            
            if(curr.g > dist[curr.r][curr.c]) continue;

            for(int i=0; i<4; i++) {
                int nr = curr.r + DR[i], nc = curr.c + DC[i];
                if(isValid(nr, nc, ctx.rows, ctx.cols, ctx.getGrid())) {
                    int newG = curr.g + ctx.getGrid()[nr][nc];
                    if(newG < dist[nr][nc]) {
                        dist[nr][nc] = newG;
                        parent.put(nr+","+nc, new int[]{curr.r, curr.c});
                        int h = Math.abs(nr - ctx.endRow) + Math.abs(nc - ctx.endCol);
                        double randomH = h * (1.0 + (Math.random() - 0.5) * noiseFactor); 
                        pq.add(new Node(nr, nc, newG, newG + (int)randomH));
                    }
                }
            }
        }
        return null;
    }

    private static Individual optimizePath(Individual ind, int[][] maze) {
        List<int[]> path = new ArrayList<>(ind.path);
        boolean improved = true;
        while(improved) {
            improved = false;
            if(path.size() < 3) break;
            
            List<int[]> newPath = new ArrayList<>();
            newPath.add(path.get(0));
            int currIdx = 0;
            
            while(currIdx < path.size() - 1) {
                int bestJump = currIdx + 1;
                int maxLook = Math.min(path.size()-1, currIdx + 20);
                for(int look = maxLook; look > currIdx + 1; look--) {
                    int[] c = path.get(currIdx);
                    int[] t = path.get(look);
                    if (Math.abs(c[0]-t[0]) + Math.abs(c[1]-t[1]) <= 1) {
                        bestJump = look;
                        improved = true;
                        break;
                    }
                }
                newPath.add(path.get(bestJump));
                currIdx = bestJump;
            }
            path = newPath;
        }
        return new Individual(path, maze);
    }

    private static Individual selectParent(List<Individual> pop) {
        Individual best = null;
        for(int i=0; i<3; i++) { 
            Individual ind = pop.get((int)(Math.random()*pop.size()));
            if(best==null || ind.fitness > best.fitness) best = ind;
        }
        return best;
    }

    private static Individual crossover(Individual p1, Individual p2, int[][] maze) {
        Set<String> set1 = new HashSet<>();
        for(int[] p : p1.path) set1.add(p[0]+","+p[1]);
        List<int[]> common = new ArrayList<>();
        for(int i=1; i<p2.path.size()-1; i++) {
            if(set1.contains(p2.path.get(i)[0]+","+p2.path.get(i)[1])) common.add(p2.path.get(i));
        }
        if(common.isEmpty()) return p1;
        int[] cut = common.get((int)(Math.random()*common.size()));
        
        List<int[]> child = new ArrayList<>();
        for(int[] p : p1.path) { child.add(p); if(p[0]==cut[0] && p[1]==cut[1]) break; }
        boolean f = false;
        for(int[] p : p2.path) { if(p[0]==cut[0] && p[1]==cut[1]) f=true; if(f && (p[0]!=cut[0] || p[1]!=cut[1])) child.add(p); }
        return new Individual(child, maze);
    }

    private static Individual mutate(Individual ind, int[][] maze, int rows, int cols) {
        List<int[]> path = ind.path;
        if(path.size() < 5) return ind;
        int idx1 = (int)(Math.random()*(path.size()-3));
        int idx2 = idx1 + 2 + (int)(Math.random()*Math.min(15, path.size()-idx1-2));
        
        List<int[]> shortPath = findLocalAStar(path.get(idx1), path.get(idx2), maze, rows, cols);
        if(shortPath != null) {
            List<int[]> newPath = new ArrayList<>();
            for(int i=0; i<=idx1; i++) newPath.add(path.get(i));
            for(int i=1; i<shortPath.size()-1; i++) newPath.add(shortPath.get(i));
            for(int i=idx2; i<path.size(); i++) newPath.add(path.get(i));
            return new Individual(newPath, maze);
        }
        return ind;
    }
    
    private static List<int[]> findLocalAStar(int[] s, int[] e, int[][] maze, int rows, int cols) {
        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(s[0], s[1], 0, 0));
        Map<String, int[]> par = new HashMap<>(); par.put(s[0]+","+s[1], null);
        Map<String, Integer> gs = new HashMap<>(); gs.put(s[0]+","+s[1], 0);
        int limit = 100; 

        while(!pq.isEmpty() && limit-- > 0) {
            Node c = pq.poll();
            if(c.r == e[0] && c.c == e[1]) return reconstructPath(par, e[0], e[1]);
            for(int i=0; i<4; i++) {
                int nr=c.r+DR[i], nc=c.c+DC[i];
                if(isValid(nr,nc,rows,cols,maze)) {
                    int ng = c.g + maze[nr][nc];
                    if(ng < gs.getOrDefault(nr+","+nc, Integer.MAX_VALUE)) {
                        gs.put(nr+","+nc, ng);
                        par.put(nr+","+nc, new int[]{c.r,c.c});
                        pq.add(new Node(nr,nc,ng, ng + Math.abs(nr-e[0])+Math.abs(nc-e[1])));
                    }
                }
            }
        }
        return null;
    }

    // --- Static Helpers (Fixed) ---
    private static boolean isValid(int r, int c, int rows, int cols, int[][] maze) {
        return r>=0 && r<rows && c>=0 && c<cols && maze[r][c]!=-1;
    }
    
    // FIX: ใส่ static เรียบร้อย
    private static int calculateCost(List<int[]> path, int[][] maze) {
        int s=0; for(int[] p:path) s+=maze[p[0]][p[1]]; return s;
    }
    
    private static List<int[]> reconstructPath(Map<String, int[]> pm, int er, int ec) {
        List<int[]> p = new ArrayList<>(); int[] c = {er, ec};
        while(c!=null) { p.add(c); c=pm.get(c[0]+","+c[1]); }
        Collections.reverse(p); return p;
    }
    
    private static class Node implements Comparable<Node> {
        int r, c, g, f;
        public Node(int r, int c, int g, int f) { this.r=r; this.c=c; this.g=g; this.f=f; }
        public int compareTo(Node o) { return Integer.compare(this.f, o.f); }
    }
}