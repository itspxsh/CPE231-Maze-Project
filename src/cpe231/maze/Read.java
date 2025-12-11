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
            // 🛑 แก้ไข: ต้องรับ MazeInfo
            MazeInfo info = MazeLoader.loadMaze(mazeFile);
            int[][] maze = info.maze(); // ดึง Maze Array ออกมา

            // 2. แสดงผลลัพธ์เบื้องต้นเพื่อเช็คความถูกต้อง
            System.out.println("Maze Loaded Successfully!");
            // 🛑 แก้ไข: ใช้ info.maze().length แทน maze.length
            System.out.println("Dimensions: " + maze.length + " rows x " + maze[0].length + " cols");
            // 🛑 แก้ไข: ใช้ info.start().r() แทน MazeLoader.startRow
            System.out.println("Start Position: (" + info.start().r() + ", " + info.start().c() + ")");
            System.out.println("Goal Position: (" + info.end().r() + ", " + info.end().c() + ")");

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