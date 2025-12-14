package cpe231.maze;

import java.util.*;

// Class นี้ผมปรับปรุงจาก HybridGA ให้ Implement MazeSolver ถูกต้องครับ
public class GeneticSolverUltimate implements MazeSolver {

    // --- Parameters (จูนให้สมดุลระหว่างความเร็วกับความแม่น) ---
    private static final int POPULATION_SIZE = 150;     // เพิ่มประชากรเพื่อให้มีความหลากหลาย
    private static final int MAX_GENERATIONS = 500;     // จำนวนรุ่น
    private static final double CROSSOVER_RATE = 0.85;
    private static final double MUTATION_RATE = 0.25;   // โอกาสกลายพันธุ์
    private static final int ELITISM_COUNT = 5;         // เก็บตัวท็อปไว้
    
    // ทิศทาง (Up, Down, Left, Right)
    private static final int[] DR = {-1, 1, 0, 0};
    private static final int[] DC = {0, 0, -1, 1};

    // Inner class สำหรับเก็บข้อมูลแต่ละเส้นทาง
    private static class Individual implements Comparable<Individual> {
        List<int[]> path;
        int cost;
        double fitness;

        public Individual(List<int[]> path, int cost) {
            this.path = new ArrayList<>(path);
            this.cost = cost;
            // Fitness: ยิ่ง cost น้อย ยิ่งค่า fitness มาก
            this.fitness = 1_000_000.0 / (cost + 1.0);
        }

        @Override
        public int compareTo(Individual other) {
            return Double.compare(other.fitness, this.fitness); // Descending order
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext mazeCtx) {
        long startTime = System.nanoTime();
        
        // 1. ดึงข้อมูลจาก MazeContext (แก้ปัญหา MazeLoader.startRow ที่ Error)
        int[][] maze = mazeCtx.getGrid();
        int rows = mazeCtx.rows;
        int cols = mazeCtx.cols;
        int startR = mazeCtx.startRow;
        int startC = mazeCtx.startCol;
        int endR = mazeCtx.endRow;
        int endC = mazeCtx.endCol;

        // 2. Initial Population (ผสม Heuristic Walk กับ Random)
        List<Individual> population = initializePopulation(maze, rows, cols, startR, startC, endR, endC);

        if (population.isEmpty()) {
            return new AlgorithmResult("Genetic Algorithm (Ultimate)", new ArrayList<>(), -1, System.nanoTime() - startTime, 0);
        }

        Individual bestSolution = population.get(0);
        long nodesExpandedEstimator = 0; // เอาไว้นับคร่าวๆ

        // --- Main Evolution Loop ---
        for (int generation = 0; generation < MAX_GENERATIONS; generation++) {
            Collections.sort(population);
            
            // Update Best Solution
            if (population.get(0).fitness > bestSolution.fitness) {
                bestSolution = population.get(0);
                // Tip: ทุกครั้งที่เจอ Best ใหม่ ลอง Optimize ทันที
                bestSolution = optimizeLocalSearch(bestSolution, maze);
            }

            // ถ้าเจอคำตอบที่ดีมากแล้ว (เช่น Cost = Manhattan Distance หรือใกล้เคียง) อาจจะ break ได้ (แต่รันให้ครบตามโจทย์)

            List<Individual> nextGen = new ArrayList<>();

            // A. Elitism: เก็บตัวที่ดีที่สุดข้ามไปรุ่นถัดไปเลย
            for (int i = 0; i < ELITISM_COUNT && i < population.size(); i++) {
                nextGen.add(population.get(i));
            }

            // B. Reproduction
            while (nextGen.size() < POPULATION_SIZE) {
                Individual p1 = selectParent(population);
                Individual p2 = selectParent(population);

                Individual child;
                // Crossover
                if (Math.random() < CROSSOVER_RATE) {
                    child = crossover(p1, p2, maze);
                } else {
                    child = p1;
                }

                // Mutation
                if (Math.random() < MUTATION_RATE) {
                    child = mutate(child, maze, rows, cols);
                }

                // Optimization ย่อย (Smoothing)
                child = simplifyPath(child, maze);
                
                nextGen.add(child);
                nodesExpandedEstimator++;
            }
            population = nextGen;
        }

        // Final Optimization: รีดยางครั้งสุดท้ายก่อนส่ง
        bestSolution = optimizeLocalSearch(bestSolution, maze);
        
        long duration = System.nanoTime() - startTime;
        return new AlgorithmResult("Genetic Algorithm (Ultimate)", bestSolution.path, bestSolution.cost, duration, nodesExpandedEstimator);
    }

    // --- 1. Initialization Logic ---
    private List<Individual> initializePopulation(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec) {
        List<Individual> pop = new ArrayList<>();
        int attempts = 0;
        
        while (pop.size() < POPULATION_SIZE && attempts < POPULATION_SIZE * 2) {
            attempts++;
            // 70% ใช้ Heuristic (เดินเข้าหา goal), 30% Random เดินมั่ว (เพื่อกระจายความเสี่ยง)
            boolean smart = Math.random() < 0.7;
            List<int[]> path = generatePath(maze, rows, cols, sr, sc, er, ec, smart);
            if (path != null) {
                pop.add(new Individual(path, calculateCost(path, maze)));
            }
        }
        
        // ถ้าหาทางไม่ได้เลย ให้ลองสร้างแบบ Greedy สุดๆ 1 ตัว
        if (pop.isEmpty()) {
            List<int[]> path = generatePath(maze, rows, cols, sr, sc, er, ec, true);
            if (path != null) pop.add(new Individual(path, calculateCost(path, maze)));
        }
        
        return pop;
    }

    private List<int[]> generatePath(int[][] maze, int rows, int cols, int sr, int sc, int er, int ec, boolean smart) {
        Stack<int[]> stack = new Stack<>();
        boolean[][] visited = new boolean[rows][cols];
        Map<String, int[]> parentMap = new HashMap<>();
        
        stack.push(new int[]{sr, sc});
        visited[sr][sc] = true;
        parentMap.put(sr + "," + sc, null);

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            if (curr[0] == er && curr[1] == ec) return reconstructPath(parentMap, er, ec);

            List<int[]> neighbors = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                int nr = curr[0] + DR[i];
                int nc = curr[1] + DC[i];
                if (isValid(nr, nc, rows, cols, maze) && !visited[nr][nc]) {
                    neighbors.add(new int[]{nr, nc});
                }
            }

            if (smart) {
                // เรียงลำดับ: เลือกตัวที่ Score ดีสุด (Distance ใกล้ Goal + Cost ต่ำ) ไว้ท้ายสุดของ List (เพื่อให้ Stack pop ออกมาก่อน)
                neighbors.sort((a, b) -> {
                    double scoreA = (Math.abs(a[0]-er) + Math.abs(a[1]-ec)) * 1.5 + maze[a[0]][a[1]];
                    double scoreB = (Math.abs(b[0]-er) + Math.abs(b[1]-ec)) * 1.5 + maze[b[0]][b[1]];
                    return Double.compare(scoreB, scoreA); // Reverse logic for Stack
                });
            } else {
                Collections.shuffle(neighbors);
            }

            for (int[] n : neighbors) {
                visited[n[0]][n[1]] = true;
                parentMap.put(n[0] + "," + n[1], curr);
                stack.push(n);
            }
        }
        return null;
    }

    // --- 2. Operators ---
    private Individual selectParent(List<Individual> pop) {
        // Tournament Selection
        int k = 5;
        Individual best = null;
        for(int i=0; i<k; i++) {
            Individual ind = pop.get((int)(Math.random()*pop.size()));
            if(best == null || ind.fitness > best.fitness) best = ind;
        }
        return best;
    }

    private Individual crossover(Individual p1, Individual p2, int[][] maze) {
        // หาจุดตัดที่ทั้งสองเส้นทางมีร่วมกัน (Common Points)
        Set<String> set1 = new HashSet<>();
        for(int[] p : p1.path) set1.add(p[0]+","+p[1]);
        
        List<int[]> common = new ArrayList<>();
        for(int i=1; i<p2.path.size()-1; i++) { // ไม่เอา start/end
            if(set1.contains(p2.path.get(i)[0]+","+p2.path.get(i)[1])) {
                common.add(p2.path.get(i));
            }
        }
        
        if(common.isEmpty()) return p1; // ไม่มีจุดร่วม ตัดต่อไม่ได้

        int[] cut = common.get((int)(Math.random()*common.size()));
        
        // สร้างลูก: Start->Cut (จาก p1) + Cut->End (จาก p2)
        List<int[]> childPath = new ArrayList<>();
        boolean cutFoundInP1 = false;
        
        for(int[] p : p1.path) {
            childPath.add(p);
            if(p[0]==cut[0] && p[1]==cut[1]) { cutFoundInP1 = true; break; }
        }
        
        boolean startAddingP2 = false;
        for(int[] p : p2.path) {
            if(p[0]==cut[0] && p[1]==cut[1]) startAddingP2 = true;
            if(startAddingP2 && (p[0]!=cut[0] || p[1]!=cut[1])) {
                childPath.add(p);
            }
        }
        
        return new Individual(childPath, calculateCost(childPath, maze));
    }

    private Individual mutate(Individual ind, int[][] maze, int rows, int cols) {
        // ลองหาทางลัดระหว่าง 2 จุดสุ่มในเส้นทาง
        if(ind.path.size() < 5) return ind;
        
        int idx1 = (int)(Math.random() * (ind.path.size() - 2));
        int idx2 = idx1 + 2 + (int)(Math.random() * Math.min(20, ind.path.size() - idx1 - 2)); // ไม่ไกลมาก
        
        if(idx2 >= ind.path.size()) idx2 = ind.path.size() - 1;

        // ลองเดินด้วย BFS สั้นๆ (Local Search)
        List<int[]> shortCut = findShortPathBFS(ind.path.get(idx1), ind.path.get(idx2), maze, rows, cols);
        
        if(shortCut != null) {
            // เช็คว่าทางใหม่ดีกว่าทางเก่าไหม
            int oldCost = 0;
            for(int i=idx1; i<=idx2; i++) oldCost += maze[ind.path.get(i)[0]][ind.path.get(i)[1]];
            int newCost = calculateCost(shortCut, maze);
            
            if(newCost < oldCost) {
                List<int[]> newPath = new ArrayList<>();
                for(int i=0; i<=idx1; i++) newPath.add(ind.path.get(i));
                for(int i=1; i<shortCut.size()-1; i++) newPath.add(shortCut.get(i)); // Add shortcut middle
                for(int i=idx2; i<ind.path.size(); i++) newPath.add(ind.path.get(i));
                return new Individual(newPath, calculateCost(newPath, maze));
            }
        }
        return ind;
    }

    // --- Optimization Helpers ---
    private Individual simplifyPath(Individual ind, int[][] maze) {
        // ตัด Loop: ถ้า A->...->A หรือ A->B (โดยที่ B คือเพื่อนบ้าน A แต่เราเดินอ้อม) ให้ตัดทิ้ง
        List<int[]> path = new ArrayList<>(ind.path);
        boolean changed = true;
        
        while(changed) {
            changed = false;
            if(path.size() < 3) break;
            List<int[]> optimized = new ArrayList<>();
            optimized.add(path.get(0));
            int curr = 0;
            
            while(curr < path.size() - 1) {
                int bestJump = curr + 1;
                // มองไปข้างหน้า ดูว่ามีจุดไหนที่อยู่ติดกับเราบ้าง (Is Neighbor)
                for(int look=path.size()-1; look > curr+1; look--) {
                    int[] t = path.get(look);
                    int[] c = path.get(curr);
                    if(Math.abs(t[0]-c[0]) + Math.abs(t[1]-c[1]) == 1) {
                        bestJump = look;
                        changed = true;
                        break;
                    }
                }
                optimized.add(path.get(bestJump));
                curr = bestJump;
            }
            path = optimized;
        }
        return new Individual(path, calculateCost(path, maze));
    }
    
    private Individual optimizeLocalSearch(Individual ind, int[][] maze) {
        // Advance: ลองหา Local A* ในช่วงที่คดเคี้ยว
        // (ในที่นี้ใช้ simplifyPath ก็ช่วยได้มากแล้วครับ เพื่อไม่ให้โค้ดบวมเกินไป)
        return simplifyPath(ind, maze);
    }

    // --- Common Helpers ---
    private List<int[]> findShortPathBFS(int[] s, int[] e, int[][] maze, int rows, int cols) {
        Queue<List<int[]>> q = new LinkedList<>();
        List<int[]> init = new ArrayList<>(); init.add(s);
        q.add(init);
        Set<String> visited = new HashSet<>(); visited.add(s[0]+","+s[1]);
        int limit = 100; // จำกัด node กันค้าง
        
        while(!q.isEmpty() && limit-- > 0) {
            List<int[]> p = q.poll();
            int[] c = p.get(p.size()-1);
            if(c[0]==e[0] && c[1]==e[1]) return p;
            
            for(int i=0; i<4; i++) {
                int nr = c[0]+DR[i], nc = c[1]+DC[i];
                if(isValid(nr, nc, rows, cols, maze) && !visited.contains(nr+","+nc)) {
                    visited.add(nr+","+nc);
                    List<int[]> np = new ArrayList<>(p); np.add(new int[]{nr, nc});
                    q.add(np);
                }
            }
        }
        return null;
    }

    private boolean isValid(int r, int c, int rows, int cols, int[][] maze) {
        return r >= 0 && r < rows && c >= 0 && c < cols && maze[r][c] != -1;
    }

    private int calculateCost(List<int[]> path, int[][] maze) {
        int sum = 0;
        for (int[] p : path) sum += maze[p[0]][p[1]];
        return sum;
    }

    private List<int[]> reconstructPath(Map<String, int[]> pm, int er, int ec) {
        List<int[]> p = new ArrayList<>();
        int[] c = {er, ec};
        while (c != null) {
            p.add(c);
            c = pm.get(c[0] + "," + c[1]);
        }
        Collections.reverse(p);
        return p;
    }
}