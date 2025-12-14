package cpe231.maze;

import java.io.File;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String mazeFile = "data/m100_100.txt"; 
        
        File f = new File(mazeFile);
        if (!f.exists()) {
            System.err.println("Error: File not found at " + f.getAbsolutePath());
            System.err.println("Please create a folder 'data' and put a text file 'm100_100.txt' inside.");
            return;
        }

        try {
            System.out.println(">>> Loading Maze: " + mazeFile + " <<<");
            MazeContext maze = MazeLoader.loadMaze(mazeFile);
            System.out.println("Grid Size: " + maze.rows + "x" + maze.cols);
            System.out.println();
            
            // ใส่ครบ 4 ตัวเทพเลยครับ
            MazeSolver[] solvers = {
                new DijkstraSolver(),               // Baseline 1 (Optimal)
                new AStarSolver(),                  // Baseline 2 (Optimal & Fast)
                new GeneticSolverPure(),            // ของคุณ (Pure GA)
                new GeneticSolverUltimate()         // ของคุณแบบอัปเกรด (Hybrid/Memetic)
            };
            
            AlgorithmResult optimalRes = null;
            
            System.out.println("=".repeat(95));
            System.out.printf("%-35s | %-10s | %-12s | %-18s%n", 
                "Algorithm", "Cost", "Time(ms)", "Gap from Optimal");
            System.out.println("=".repeat(95));

            for (MazeSolver solver : solvers) {
                AlgorithmResult res = solver.solve(maze);
                
                // Set Optimal Baseline
                if (optimalRes == null && (res.algoName.contains("Dijkstra") || res.algoName.contains("A*"))) {
                    optimalRes = res;
                }
                
                double gap = 0;
                double gapPercent = 0;
                String gapStr = "-";

                if (optimalRes != null && res.totalCost != -1) {
                    gap = res.totalCost - optimalRes.totalCost;
                    if (optimalRes.totalCost > 0) {
                        gapPercent = (gap / optimalRes.totalCost * 100);
                    }
                    
                    if (gap == 0) gapStr = "OPTIMAL ★";
                    else gapStr = String.format("+%d (+%.2f%%)", (int)gap, gapPercent);
                }
                
                double timeMs = res.executionTimeNs / 1_000_000.0;
                String costStr = (res.totalCost == -1) ? "FAILED" : String.valueOf(res.totalCost);
                
                System.out.printf("%-35s | %-10s | %10.2f ms | %-18s%n", 
                    res.algoName, costStr, timeMs, gapStr);
            }
            System.out.println("=".repeat(95));
            
        } catch (IOException e) {
            System.err.println("Error loading maze: " + e.getMessage());
        }
    }
}