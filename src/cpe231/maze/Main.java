package cpe231.maze;

import java.io.IOException;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        
        // --- ส่วนที่ 1: รัน Benchmark เพื่อวัดผล (สำหรับรายงาน) ---
        // Benchmark.runAll(); 

        // --- ส่วนที่ 2: รันโชว์เส้นทาง (Visualization) สำหรับวิดีโอ/โจทย์ข้อ 3 ---
        // เลือกไฟล์ที่อยากโชว์ (แนะนำ m33_35.txt หรือ m100_100.txt)
        String demoFile = "data/m100_100.txt"; 
        runDemo(demoFile);
    }

    public static void runDemo(String filePath) {
        System.out.println("\n>>> DEMO MODE: " + filePath + " <<<");
        try {
            int[][] maze = MazeLoader.loadMaze(filePath);
            
            // 1. Run A* (เพื่อใช้เป็น Baseline หรือคำตอบที่ดีที่สุด)
            System.out.println("\n-----------------------------------");
            AlgorithmResult aStarRes = AStar.solve(maze);
            printSummary(aStarRes);
            
            // 2. Run Dijkstra (เปิดคอมเมนต์เมื่อมีไฟล์ Dijkstra.java)
            System.out.println("\n-----------------------------------");
            AlgorithmResult dijkRes = Dijkstra.solve(maze);
            printSummary(dijkRes);
            
            // 3. Run GA (พระเอกของเรา)
            System.out.println("\n-----------------------------------");
            AlgorithmResult gaRes = GeneticAlgo.solve(maze);
            printSummary(gaRes);

            // --- ส่วนที่เพิ่ม: เปรียบเทียบผลลัพธ์ (Gap Calculation) ---
            System.out.println("\n===================================");
            System.out.println("       🏆 FINAL VERDICT 🏆       ");
            System.out.println("===================================");
            
            if (aStarRes.totalCost != -1 && gaRes.totalCost != -1) {
                System.out.println("Best Cost (A* / Optimal): " + aStarRes.totalCost);
                System.out.println("Your Cost (GA): " + gaRes.totalCost);

                // คำนวณส่วนต่าง
                double gap = gaRes.totalCost - aStarRes.totalCost;
                System.out.println("Gap from Optimal: " + gap); 

                // ตัดสินผลลัพธ์
                if (gap == 0) {
                    System.out.println(">>> Status: Perfect Solution! (Optimal) 🌟");
                } else if (gap <= 20) {
                    System.out.println(">>> Status: Near Optimal Solution (Excellent!) ✅");
                } else {
                    System.out.println(">>> Status: Good Solution (Can be improved) ⚠️");
                }
            } else {
                System.out.println("Error: One of the algorithms failed to find a path.");
            }

            // เลือกวาดเส้นทางของ GA เพื่อโชว์ใน Video
            // drawMazeWithPath(maze, gaRes.path); 

        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // โจทย์ข้อ 2.3 และ 3.2: แสดงค่าผลรวม และ ลำดับตำแหน่ง
    private static void printSummary(AlgorithmResult res) {
        System.out.println("Algorithm: " + res.algoName);
        if (res.totalCost != -1) {
            System.out.println("✅ Status: Found Path");
            System.out.println("💰 Total Cost: " + res.totalCost);
            System.out.println("⏱ Runtime: " + String.format("%.4f", res.executionTimeNs / 1_000_000.0) + " ms");
            System.out.println("👣 Steps: " + res.path.size());
            // System.out.println("📍 Path: " + pathToString(res.path)); // ปริ้นท์พิกัดถ้ารกให้ปิด
        } else {
            System.out.println("❌ Status: Path Not Found");
        }
    }

    // โจทย์ข้อ 3.1: แสดงเขาวงกตและเส้นทางที่เลือก (ใช้ * แทนเส้นทาง)
    private static void drawMazeWithPath(int[][] maze, List<int[]> path) {
        System.out.println("\n[ Visual Map ]");
        Set<String> pathSet = new HashSet<>();
        if (path != null) {
            for (int[] p : path) pathSet.add(p[0] + "," + p[1]);
        }

        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[0].length; j++) {
                if (i == MazeLoader.startRow && j == MazeLoader.startCol) System.out.print("S  ");
                else if (i == MazeLoader.endRow && j == MazeLoader.endCol) System.out.print("G  ");
                else if (maze[i][j] == -1) System.out.print("## ");
                else if (pathSet.contains(i + "," + j)) System.out.print("** "); // ทางเดินที่เป็นคำตอบ
                else System.out.printf("%-2d ", maze[i][j]); // ทางเดินปกติ
            }
            System.out.println();
        }
    }
}