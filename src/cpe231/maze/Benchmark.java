package cpe231.maze;

import java.io.File;

public class Benchmark {

    // รายชื่อไฟล์ที่จะทดสอบ
    private static final String[] TEST_FILES = {
        "m15_15.txt", "m24_20.txt", "m30_30.txt", "m33_35.txt",
        "m40_40.txt", "m40_45.txt", "m45_45.txt", "m50_50.txt",
        "m60_60.txt", "m70_60.txt", "m80_50.txt", "m100_90.txt", "m100_100.txt"
    };

    private static final int RUNS = 200; // จำนวนรอบรันเพื่อหาค่าเฉลี่ย

    public static void runAll() {
        System.out.println("Benchmarking A* Algorithm (" + RUNS + " runs average)");
        System.out.println(); 

        for (String fileName : TEST_FILES) {
            runSingleFile("data/" + fileName);
        }
        
        System.out.println();
        System.out.println("Benchmark Completed.");
    }

    private static void runSingleFile(String filePath) {
        try {
            File f = new File(filePath);
            if (!f.exists()) {
                System.out.println("File not found: " + filePath);
                return;
            }
            
            // โหลด Maze และตั้งค่า Start/End ใน MazeLoader
            int[][] maze = MazeLoader.loadMaze(filePath);
            
            // Warmup (รันเล่นๆ ให้ Java ตื่นตัว)
            for (int i = 0; i < 10; i++) AStar.solve(maze);

            long totalTime = 0;
            AlgorithmResult result = null;

            // Benchmark Loop (จับเวลาจริง)
            for (int i = 0; i < RUNS; i++) {
                long start = System.nanoTime();
                result = AStar.solve(maze); 
                long end = System.nanoTime();
                totalTime += (end - start);
            }

            double avgTimeMs = (totalTime / (double) RUNS) / 1_000_000.0;

            // ดึงค่าผลลัพธ์
            int cost = result.totalCost; 
            long nodes = result.nodesExpanded;

            // Print แบบ simple text
            System.out.printf("File: %-15s  Time: %8.4f ms   Cost: %-6d   Nodes: %d%n", 
                              f.getName(), avgTimeMs, cost, nodes);

        } catch (Exception e) {
            System.out.println("Error processing " + filePath + ": " + e.getMessage());
        }
    }
}