package cpe231.maze;
import java.io.IOException;

public class Read {
    public static void main(String[] args) {
        // เรียกบรรทัดเดียว จบงาน
        Benchmark.runAll();
    }

    public static void test(String[] args) {
        // ตรวจสอบว่าใส่ Argument มาไหม
        if (args.length == 0) {
            System.out.println("Usage: java -cp bin Main data/m33_35.txt");
            return;
        }

        String mazeFile = args[0];
        System.out.println("Loading maze from: " + mazeFile);

        try {
            // 1. เรียกใช้ MazeLoader อ่านไฟล์
            int[][] maze = MazeLoader.loadMaze(mazeFile);

            // 2. แสดงผลลัพธ์เบื้องต้นเพื่อเช็คความถูกต้อง
            System.out.println("Maze Loaded Successfully!");
            System.out.println("Dimensions: " + maze.length + " rows x " + maze[0].length + " cols");
            System.out.println("Start Position: (" + MazeLoader.startRow + ", " + MazeLoader.startCol + ")");
            System.out.println("Goal Position: (" + MazeLoader.endRow + ", " + MazeLoader.endCol + ")");

            // ลองปริ้นท์เขาวงกตออกมาดู (แสดงเป็นตัวเลข)
            // หมายเหตุ: -1 คือกำแพง
            printMaze(maze);

            // --- พื้นที่สำหรับเรียก Algorithm ของเพื่อน ---
            // GeneticAlgorithm.run(maze);
            // Dijkstra.run(maze);
            // ---------------------------------------

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    // ฟังก์ชันช่วยปริ้นท์ Maze ออกมาดูเล่นๆ
    public static void printMaze(int[][] maze) {
        for (int[] row : maze) {
            for (int val : row) {
                if (val == -1) {
                    System.out.printf("%4s", "#"); // จองพื้นที่ 4 ช่องให้เครื่องหมาย #
                } else {
                    System.out.printf("%4d", val); // จองพื้นที่ 4 ช่องให้ตัวเลข
                }
            }
            System.out.println();
        }
    }
}