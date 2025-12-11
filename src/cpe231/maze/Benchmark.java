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
            // ... (ส่วนเช็คไฟล์)
            
            // 🛑 แก้ไข: ต้องรับ MazeInfo
            MazeInfo mazeInfo = MazeLoader.loadMaze(filePath);
            int[][] maze = mazeInfo.maze(); // ดึง Maze Array (int[][]) ออกมา

            // Warmup
            for (int i = 0; i < 10; i++) 
                // 🛑 ส่ง Maze Array (int[][]) และพิกัด 4 ค่า
                AStar.solve(maze, mazeInfo.start().r(), mazeInfo.start().c(), mazeInfo.end().r(), mazeInfo.end().c());

            long totalTime = 0;
            AlgorithmResult result = null;

            // Benchmark Loop (จับเวลาจริง)
            for (int i = 0; i < RUNS; i++) {
                long start = System.nanoTime();
                // 🛑 ส่ง Maze Array (int[][]) และพิกัด 4 ค่า
                result = AStar.solve(maze, mazeInfo.start().r(), mazeInfo.start().c(), mazeInfo.end().r(), mazeInfo.end().c()); 
                long end = System.nanoTime();
                totalTime += (end - start);
            }
            // ... (ส่วนล่างเหมือนเดิม)
        } catch (Exception e) {
            System.out.println("Error processing " + filePath + ": " + e.getMessage());
        }
    }
}