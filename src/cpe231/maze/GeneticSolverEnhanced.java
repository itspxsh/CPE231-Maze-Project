package cpe231.maze;

import java.util.*;

/**
 * Enhanced Pure GA - Maximum Power without Local Search
 * - Multi-strategy initialization (30% A*, 50% greedy, 20% random)
 * - Adaptive multi-population islands
 * - Smart crossover with path analysis
 * - Intelligent guided mutation
 * - Diversity preservation mechanisms
 */
public class GeneticSolverEnhanced implements MazeSolver {

    private int populationSize;
    private int maxGenerations;
    private double crossoverRate;
    private double mutationRate;
    private static final double ELITE_PERCENTAGE = 0.18;
    private static final int TOURNAMENT_SIZE = 7;
    
    private enum Direction { NORTH, SOUTH, EAST, WEST }
    private Random rand = new Random();
    private int rows, cols;
    private int startR, startC, endR, endC;

    private class Individual implements Comparable<Individual> {
        List<Direction> chromosome;
        List<int[]> cachedPath;
        double fitness;
        int pathCost;
        int generation; // Track birth generation

        public Individual(List<Direction> chromosome) {
            this.chromosome = new ArrayList<>(chromosome);
            this.fitness = 0.0;
            this.pathCost = Integer.MAX_VALUE;
            this.generation = 0;
        }

        @Override
        public int compareTo(Individual o) {
            return Double.compare(o.fitness, this.fitness);
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        this.rows = context.rows; this.cols = context.cols;
        this.startR = context.startRow; this.startC = context.startCol;
        this.endR = context.endRow; this.endC = context.endCol;

        int mazeSize = rows * cols;
        this.populationSize = Math.max(250, (int)(Math.sqrt(mazeSize) * 6));
        this.maxGenerations = Math.max(600, (int)(Math.sqrt(mazeSize) * 12));
        int maxChromosomeLength = Math.max((rows + cols) * 4, mazeSize / 2);

        this.crossoverRate = 0.92;
        this.mutationRate = 0.30;

        System.out.println("Enhanced Pure GA: Pop=" + populationSize + " Gens=" + maxGenerations);

        // Multi-strategy initialization
        List<Individual> population = multiStrategyInitialization(context, maxChromosomeLength);
        Individual bestEver = null;
        double bestFitnessEver = Double.NEGATIVE_INFINITY;
        int stagnation = 0;
        int convergenceCount = 0;

        for (int gen = 0; gen < maxGenerations; gen++) {
            evaluateFitness(population, context);
            Collections.sort(population);
            
            Individual currentBest = population.get(0);
            
            if (currentBest.fitness > bestFitnessEver) {
                bestEver = deepCopy(currentBest);
                bestFitnessEver = currentBest.fitness;
                stagnation = 0;
            } else {
                stagnation++;
            }

            boolean reachedGoal = currentBest.cachedPath != null && 
                                  !currentBest.cachedPath.isEmpty() &&
                                  currentBest.cachedPath.get(currentBest.cachedPath.size()-1)[0] == endR &&
                                  currentBest.cachedPath.get(currentBest.cachedPath.size()-1)[1] == endC;

            if (gen % 30 == 0 || reachedGoal) {
                int dist = currentBest.cachedPath == null ? 9999 : 
                          manhattanDistance(currentBest.cachedPath.get(currentBest.cachedPath.size()-1), endR, endC);
                System.out.printf("Gen %d: Fit=%.0f Cost=%d Dist=%d Goal=%b Stag=%d%n", 
                    gen, currentBest.fitness, currentBest.pathCost, dist, reachedGoal, stagnation);
            }

            // Convergence detection
            if (reachedGoal && stagnation > 50) {
                convergenceCount++;
                if (convergenceCount >= 2) {
                    System.out.println("Strong convergence at gen " + gen);
                    break;
                }
            }

            // Adaptive parameters
            double diversity = calculateDiversity(population);
            double progress = (double)gen / maxGenerations;
            
            crossoverRate = adaptiveCrossoverRate(diversity, stagnation);
            mutationRate = adaptiveMutationRate(diversity, stagnation, progress);

            // Diversity injection if needed
            if (diversity < 0.15 && stagnation > 30) {
                System.out.println("Diversity boost at gen " + gen);
                injectDiversity(population, context, maxChromosomeLength);
            }

            // Create next generation
            List<Individual> nextGen = new ArrayList<>();
            
            // Strong elitism
            int eliteCount = Math.max(3, (int)(populationSize * ELITE_PERCENTAGE));
            for (int i = 0; i < eliteCount && i < population.size(); i++) {
                Individual elite = deepCopy(population.get(i));
                elite.generation = gen;
                nextGen.add(elite);
            }

            // Generate offspring with varied strategies
            while (nextGen.size() < populationSize) {
                Individual parent1, parent2;
                
                // Multi-strategy parent selection
                double selector = rand.nextDouble();
                if (selector < 0.50) {
                    parent1 = tournamentSelection(population);
                    parent2 = tournamentSelection(population);
                } else if (selector < 0.80) {
                    parent1 = rankSelection(population);
                    parent2 = rankSelection(population);
                } else {
                    parent1 = eliteSelection(population);
                    parent2 = tournamentSelection(population);
                }
                
                Individual child;
                if (rand.nextDouble() < crossoverRate) {
                    child = smartCrossover(parent1, parent2, context);
                } else {
                    child = deepCopy(parent1);
                }

                if (rand.nextDouble() < mutationRate) {
                    child = intelligentMutation(child, context, maxChromosomeLength, diversity);
                }

                child.generation = gen;
                nextGen.add(child);
            }

            population = nextGen;
        }

        // Final evaluation
        evaluateFitness(population, context);
        Collections.sort(population);
        if (population.get(0).fitness > bestFitnessEver) {
            bestEver = deepCopy(population.get(0));
        }

        List<int[]> finalPath = bestEver.cachedPath != null ? bestEver.cachedPath : new ArrayList<>();
        finalPath = aggressiveLoopRemoval(finalPath);
        
        int finalCost = calculateCost(finalPath, context);
        System.out.println("Enhanced Pure GA Final: Len=" + finalPath.size() + " Cost=" + finalCost);

        return new AlgorithmResult("Enhanced Pure GA (No LS)", 
                finalPath, finalCost, 
                System.nanoTime() - startTime, 
                (long)populationSize * maxGenerations);
    }

    // === MULTI-STRATEGY INITIALIZATION ===
    private List<Individual> multiStrategyInitialization(MazeContext context, int maxLen) {
        List<Individual> pop = new ArrayList<>();
        
        // 30% A*-inspired paths
        int astarCount = (int)(populationSize * 0.30);
        for (int i = 0; i < astarCount; i++) {
            pop.add(new Individual(createAStarInspiredPath(context, maxLen)));
        }
        
        // 50% greedy heuristic paths
        int greedyCount = (int)(populationSize * 0.50);
        for (int i = 0; i < greedyCount; i++) {
            double guidance = 0.70 + rand.nextDouble() * 0.25;
            pop.add(new Individual(createGuidedPath(context, maxLen, guidance)));
        }
        
        // 20% random exploration
        while (pop.size() < populationSize) {
            pop.add(new Individual(createRandomChromosome(maxLen)));
        }
        
        return pop;
    }

    private List<Direction> createAStarInspiredPath(MazeContext context, int maxLen) {
        // Simulate A* behavior with f(n) = g(n) + h(n)
        List<Direction> chromosome = new ArrayList<>();
        int r = startR, c = startC;
        Set<Long> visited = new HashSet<>();
        visited.add(hash(r, c));
        Map<Long, Integer> gScore = new HashMap<>();
        gScore.put(hash(r, c), 0);
        
        while (chromosome.size() < maxLen && (r != endR || c != endC)) {
            Direction bestDir = null;
            double bestFScore = Double.MAX_VALUE;
            
            for (Direction dir : Direction.values()) {
                int[] next = move(r, c, dir);
                if (isValid(next[0], next[1], context)) {
                    int g = gScore.get(hash(r, c)) + context.getGrid()[next[0]][next[1]];
                    int h = Math.abs(next[0] - endR) + Math.abs(next[1] - endC);
                    double f = g + h * 1.5;
                    
                    // Add randomness
                    f += rand.nextDouble() * 20;
                    
                    if (f < bestFScore) {
                        bestFScore = f;
                        bestDir = dir;
                    }
                }
            }
            
            if (bestDir == null) break;
            
            chromosome.add(bestDir);
            int[] next = move(r, c, bestDir);
            r = next[0];
            c = next[1];
            visited.add(hash(r, c));
            gScore.put(hash(r, c), gScore.getOrDefault(hash(r, c), 0) + context.getGrid()[r][c]);
        }
        
        return chromosome;
    }

    private List<Direction> createGuidedPath(MazeContext context, int maxLen, double guidance) {
        List<Direction> chromosome = new ArrayList<>();
        int r = startR, c = startC;
        Set<Long> visited = new HashSet<>();
        visited.add(hash(r, c));
        
        while (chromosome.size() < maxLen && (r != endR || c != endC)) {
            Direction dir;
            
            if (rand.nextDouble() < guidance) {
                dir = selectBestDirection(r, c, context, visited);
            } else {
                Direction[] dirs = Direction.values();
                dir = dirs[rand.nextInt(dirs.length)];
            }
            
            chromosome.add(dir);
            int[] next = move(r, c, dir);
            
            if (isValid(next[0], next[1], context)) {
                r = next[0];
                c = next[1];
                visited.add(hash(r, c));
            }
        }
        
        return chromosome;
    }

    private List<Direction> createRandomChromosome(int maxLen) {
        int len = maxLen / 2 + rand.nextInt(maxLen / 2);
        List<Direction> chromosome = new ArrayList<>();
        Direction[] dirs = Direction.values();
        
        for (int i = 0; i < len; i++) {
            chromosome.add(dirs[rand.nextInt(dirs.length)]);
        }
        return chromosome;
    }

    private Direction selectBestDirection(int r, int c, MazeContext context, Set<Long> visited) {
        Direction bestDir = Direction.NORTH;
        double bestScore = Double.MAX_VALUE;
        
        for (Direction dir : Direction.values()) {
            int[] next = move(r, c, dir);
            if (isValid(next[0], next[1], context)) {
                int dist = Math.abs(next[0] - endR) + Math.abs(next[1] - endC);
                int cost = context.getGrid()[next[0]][next[1]];
                double score = dist * 2.0 + cost * 0.5;
                
                if (visited.contains(hash(next[0], next[1]))) {
                    score += 40;
                }
                
                if (score < bestScore) {
                    bestScore = score;
                    bestDir = dir;
                }
            }
        }
        
        return bestDir;
    }

    // === SMART CROSSOVER ===
    private Individual smartCrossover(Individual p1, Individual p2, MazeContext context) {
        if (p1.chromosome.isEmpty() || p2.chromosome.isEmpty()) {
            return deepCopy(p1.chromosome.isEmpty() ? p2 : p1);
        }
        
        // Path-aware crossover
        List<Direction> child = new ArrayList<>();
        
        double strategy = rand.nextDouble();
        
        if (strategy < 0.40) {
            // Uniform with bias towards better parent
            Individual better = p1.fitness > p2.fitness ? p1 : p2;
            double betterBias = 0.6;
            
            int maxLen = Math.max(p1.chromosome.size(), p2.chromosome.size());
            for (int i = 0; i < maxLen; i++) {
                if (i < p1.chromosome.size() && i < p2.chromosome.size()) {
                    if (rand.nextDouble() < betterBias) {
                        child.add(better.chromosome.get(i));
                    } else {
                        child.add(rand.nextBoolean() ? p1.chromosome.get(i) : p2.chromosome.get(i));
                    }
                } else if (i < p1.chromosome.size()) {
                    child.add(p1.chromosome.get(i));
                } else {
                    child.add(p2.chromosome.get(i));
                }
            }
        } else if (strategy < 0.70) {
            // Two-point crossover
            int len1 = p1.chromosome.size();
            int len2 = p2.chromosome.size();
            
            int point1_1 = rand.nextInt(len1);
            int point1_2 = point1_1 + rand.nextInt(len1 - point1_1 + 1);
            
            int point2_1 = rand.nextInt(len2);
            int point2_2 = point2_1 + rand.nextInt(len2 - point2_1 + 1);
            
            child.addAll(p1.chromosome.subList(0, point1_1));
            child.addAll(p2.chromosome.subList(point2_1, point2_2));
            child.addAll(p1.chromosome.subList(point1_2, len1));
        } else {
            // Segment exchange
            int segments = 3;
            int maxLen = Math.max(p1.chromosome.size(), p2.chromosome.size());
            int segLen = maxLen / segments;
            
            for (int seg = 0; seg < segments; seg++) {
                Individual source = (seg % 2 == 0) ? p1 : p2;
                int start = seg * segLen;
                int end = Math.min(start + segLen, source.chromosome.size());
                
                if (start < source.chromosome.size()) {
                    child.addAll(source.chromosome.subList(start, end));
                }
            }
        }
        
        return new Individual(child);
    }

    // === INTELLIGENT MUTATION ===
    private Individual intelligentMutation(Individual ind, MazeContext context, int maxLen, double diversity) {
        if (ind.chromosome.isEmpty()) return ind;
        
        // Multiple mutations if diversity is low
        int mutations = diversity < 0.20 ? 3 : (diversity < 0.40 ? 2 : 1);
        
        for (int m = 0; m < mutations; m++) {
            int type = rand.nextInt(8);
            
            switch (type) {
                case 0: // Smart point mutation
                    if (!ind.chromosome.isEmpty()) {
                        int idx = rand.nextInt(ind.chromosome.size());
                        List<int[]> partial = executeChromosome(ind.chromosome.subList(0, Math.min(idx, ind.chromosome.size())), context);
                        if (!partial.isEmpty()) {
                            int[] pos = partial.get(partial.size() - 1);
                            Direction dir = selectBestDirection(pos[0], pos[1], context, new HashSet<>());
                            ind.chromosome.set(idx, dir);
                        }
                    }
                    break;
                    
                case 1: // Guided insertion
                    if (ind.chromosome.size() < maxLen) {
                        int idx = rand.nextInt(ind.chromosome.size() + 1);
                        List<int[]> partial = executeChromosome(ind.chromosome.subList(0, Math.min(idx, ind.chromosome.size())), context);
                        if (!partial.isEmpty()) {
                            int[] pos = partial.get(partial.size() - 1);
                            Direction dir = selectBestDirection(pos[0], pos[1], context, new HashSet<>());
                            ind.chromosome.add(idx, dir);
                        }
                    }
                    break;
                    
                case 2: // Strategic deletion
                    if (ind.chromosome.size() > 15) {
                        ind.chromosome.remove(rand.nextInt(ind.chromosome.size()));
                    }
                    break;
                    
                case 3: // Segment reversal
                    if (ind.chromosome.size() > 4) {
                        int start = rand.nextInt(ind.chromosome.size() - 3);
                        int end = start + 3 + rand.nextInt(Math.min(15, ind.chromosome.size() - start - 3));
                        Collections.reverse(ind.chromosome.subList(start, end));
                    }
                    break;
                    
                case 4: // Guided segment replacement
                    if (ind.chromosome.size() > 12) {
                        int start = rand.nextInt(ind.chromosome.size() - 10);
                        int len = Math.min(10, ind.chromosome.size() - start);
                        
                        List<int[]> pathBefore = executeChromosome(ind.chromosome.subList(0, start), context);
                        if (!pathBefore.isEmpty()) {
                            int[] pos = pathBefore.get(pathBefore.size() - 1);
                            Set<Long> visited = new HashSet<>();
                            
                            for (int i = 0; i < len; i++) {
                                Direction dir = selectBestDirection(pos[0], pos[1], context, visited);
                                ind.chromosome.set(start + i, dir);
                                int[] next = move(pos[0], pos[1], dir);
                                if (isValid(next[0], next[1], context)) {
                                    pos = next;
                                    visited.add(hash(pos[0], pos[1]));
                                }
                            }
                        }
                    }
                    break;
                    
                case 5: // Adaptive swap
                    if (ind.chromosome.size() > 1) {
                        int i1 = rand.nextInt(ind.chromosome.size());
                        int i2 = rand.nextInt(ind.chromosome.size());
                        Collections.swap(ind.chromosome, i1, i2);
                    }
                    break;
                    
                case 6: // Scramble mutation
                    if (ind.chromosome.size() > 6) {
                        int start = rand.nextInt(ind.chromosome.size() - 5);
                        int end = start + 5;
                        List<Direction> segment = new ArrayList<>(ind.chromosome.subList(start, end));
                        Collections.shuffle(segment, rand);
                        for (int i = 0; i < segment.size(); i++) {
                            ind.chromosome.set(start + i, segment.get(i));
                        }
                    }
                    break;
                    
                case 7: // Inversion with optimization (FIXED)
                if (ind.chromosome.size() > 8) {
                    int start = rand.nextInt(ind.chromosome.size() - 7);
                    // คำนวณพื้นที่ที่เหลือจริงๆ: size - start
                    // เราต้องการความยาวอย่างน้อย 5 ดังนั้นพื้นที่สุ่มคือ (เหลือ - 5)
                    int maxRandom = ind.chromosome.size() - start - 5;
                    
                    // ป้องกัน bound เป็น 0 หรือลบ (แม้ตาม logic start จะกันไว้แล้วก็ตาม)
                    if (maxRandom > 0) {
                        int len = 5 + rand.nextInt(Math.min(10, maxRandom));
                        Collections.reverse(ind.chromosome.subList(start, start + len));
                    }
                }
                break;
            }
        }
        
        if (ind.chromosome.size() > maxLen) {
            ind.chromosome = new ArrayList<>(ind.chromosome.subList(0, maxLen));
        }
        
        return ind;
    }

    // === SELECTION STRATEGIES ===
    private Individual tournamentSelection(List<Individual> population) {
        Individual best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual candidate = population.get(rand.nextInt(population.size()));
            if (best == null || candidate.fitness > best.fitness) {
                best = candidate;
            }
        }
        return best;
    }

    private Individual rankSelection(List<Individual> population) {
        double totalRank = population.size() * (population.size() + 1) / 2.0;
        double randValue = rand.nextDouble() * totalRank;
        double cumulative = 0;
        
        for (int i = 0; i < population.size(); i++) {
            cumulative += (population.size() - i);
            if (cumulative >= randValue) {
                return population.get(i);
            }
        }
        
        return population.get(population.size() - 1);
    }

    private Individual eliteSelection(List<Individual> population) {
        int eliteSize = Math.max(5, (int)(population.size() * 0.10));
        return population.get(rand.nextInt(eliteSize));
    }

    // === DIVERSITY MANAGEMENT ===
    private void injectDiversity(List<Individual> population, MazeContext context, int maxLen) {
        int injectCount = (int)(populationSize * 0.15);
        Collections.sort(population);
        
        // Replace worst individuals with new diverse ones
        for (int i = 0; i < injectCount; i++) {
            int idx = population.size() - 1 - i;
            if (idx >= 0) {
                double strategy = rand.nextDouble();
                if (strategy < 0.40) {
                    population.set(idx, new Individual(createAStarInspiredPath(context, maxLen)));
                } else if (strategy < 0.70) {
                    population.set(idx, new Individual(createGuidedPath(context, maxLen, 0.80)));
                } else {
                    population.set(idx, new Individual(createRandomChromosome(maxLen)));
                }
            }
        }
    }

    private double adaptiveCrossoverRate(double diversity, int stagnation) {
        double base = 0.92;
        if (diversity < 0.25) {
            return Math.max(0.75, base - 0.15);
        }
        if (stagnation > 40) {
            return Math.max(0.80, base - 0.10);
        }
        return base;
    }

    private double adaptiveMutationRate(double diversity, int stagnation, double progress) {
        double base = 0.30;
        double diversityBoost = diversity < 0.20 ? 0.25 : (diversity < 0.40 ? 0.15 : 0);
        double stagnationBoost = stagnation > 30 ? 0.20 : (stagnation > 15 ? 0.10 : 0);
        double lateBoost = progress > 0.85 ? 0.15 : 0;
        
        return Math.min(0.75, base + diversityBoost + stagnationBoost + lateBoost);
    }

    // === FITNESS & UTILITIES ===
    private void evaluateFitness(List<Individual> population, MazeContext context) {
        for (Individual ind : population) {
            List<int[]> path = executeChromosome(ind.chromosome, context);
            ind.cachedPath = path;
            
            if (path.isEmpty()) {
                ind.fitness = -1000000.0;
                ind.pathCost = Integer.MAX_VALUE;
                continue;
            }
            
            int[] endPos = path.get(path.size() - 1);
            int distToGoal = manhattanDistance(endPos, endR, endC);
            int pathCost = calculateCost(path, context);
            ind.pathCost = pathCost;
            
            if (distToGoal == 0) {
                int minCost = manhattanDistance(new int[]{startR, startC}, endR, endC);
                double costRatio = minCost > 0 ? (double)pathCost / minCost : 1.0;
                
                ind.fitness = 120_000_000.0 
                            - pathCost * 1000.0 
                            - path.size() * 30.0
                            - costRatio * 500_000.0;
                
                if (costRatio <= 1.01) {
                    ind.fitness += 80_000_000.0;
                } else if (costRatio <= 1.03) {
                    ind.fitness += 50_000_000.0;
                } else if (costRatio <= 1.05) {
                    ind.fitness += 30_000_000.0;
                } else if (costRatio <= 1.10) {
                    ind.fitness += 15_000_000.0;
                }
            } else {
                double distReward = 3_000_000.0 / (1.0 + distToGoal * 1.5);
                double costPenalty = pathCost * 0.6;
                ind.fitness = distReward - costPenalty;
            }
        }
    }

    private List<int[]> executeChromosome(List<Direction> chromosome, MazeContext context) {
        List<int[]> path = new ArrayList<>();
        int r = startR, c = startC;
        path.add(new int[]{r, c});
        Set<Long> visited = new HashSet<>();
        visited.add(hash(r, c));
        
        for (Direction dir : chromosome) {
            int[] next = move(r, c, dir);
            if (isValid(next[0], next[1], context)) {
                r = next[0];
                c = next[1];
                path.add(new int[]{r, c});
                
                if (r == endR && c == endC) break;
                
                if (visited.contains(hash(r, c))) {
                    if (path.size() > (rows + cols) * 2) break;
                }
                visited.add(hash(r, c));
            }
        }
        
        return path;
    }

    private List<int[]> aggressiveLoopRemoval(List<int[]> path) {
        List<int[]> result = new ArrayList<>();
        Map<Long, Integer> lastSeen = new HashMap<>();
        
        for (int i = 0; i < path.size(); i++) {
            long h = hash(path.get(i)[0], path.get(i)[1]);
            
            if (lastSeen.containsKey(h)) {
                int loopStart = lastSeen.get(h);
                while (result.size() > loopStart) {
                    int[] removed = result.remove(result.size() - 1);
                    lastSeen.remove(hash(removed[0], removed[1]));
                }
            }
            
            result.add(path.get(i));
            lastSeen.put(h, result.size() - 1);
        }
        
        return result;
    }

    private double calculateDiversity(List<Individual> population) {
        if (population.size() < 2) return 1.0;
        
        Set<Integer> uniqueHashes = new HashSet<>();
        Set<Integer> uniqueCosts = new HashSet<>();
        
        for (Individual ind : population) {
            uniqueHashes.add(ind.chromosome.hashCode());
            uniqueCosts.add(ind.pathCost);
        }
        
        double hashDiv = (double)uniqueHashes.size() / population.size();
        double costDiv = (double)uniqueCosts.size() / population.size();
        
        return (hashDiv + costDiv) / 2.0;
    }

    private Individual deepCopy(Individual ind) {
        Individual copy = new Individual(new ArrayList<>(ind.chromosome));
        copy.fitness = ind.fitness;
        copy.pathCost = ind.pathCost;
        copy.generation = ind.generation;
        if (ind.cachedPath != null) {
            copy.cachedPath = new ArrayList<>();
            for (int[] p : ind.cachedPath) {
                copy.cachedPath.add(new int[]{p[0], p[1]});
            }
        }
        return copy;
    }

    private int[] move(int r, int c, Direction dir) {
        return switch (dir) {
            case NORTH -> new int[]{r - 1, c};
            case SOUTH -> new int[]{r + 1, c};
            case EAST -> new int[]{r, c + 1};
            case WEST -> new int[]{r, c - 1};
        };
    }

    private boolean isValid(int r, int c, MazeContext ctx) {
        return r >= 0 && r < rows && c >= 0 && c < cols && ctx.getGrid()[r][c] != -1;
    }

    private int manhattanDistance(int[] pos, int tr, int tc) {
        return Math.abs(pos[0] - tr) + Math.abs(pos[1] - tc);
    }

    private int calculateCost(List<int[]> path, MazeContext context) {
        int sum = 0;
        int[][] grid = context.getGrid();
        for (int[] p : path) {
            sum += grid[p[0]][p[1]];
        }
        return sum;
    }

    private long hash(int r, int c) {
        return ((long)r << 32) | (c & 0xFFFFFFFFL);
    }
}