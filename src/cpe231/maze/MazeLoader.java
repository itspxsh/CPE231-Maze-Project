package cpe231.maze;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MazeLoader {

    // ค่าคงที่สำหรับบอกประเภทช่อง
    public static final int WALL = -1;  // กำแพง
    public static final int START = 0;  // จุดเริ่มต้น (เราให้เวลาเป็น 0 ไปก่อน)
    public static final int GOAL = 0;   // จุดสิ้นสุด

    // ตัวแปรเก็บพิกัดเริ่มต้นและสิ้นสุด (เพื่อให้ Algorithm เรียกใช้ได้ง่ายๆ)
    public static int startRow, startCol;
    public static int endRow, endCol;

    /**
     * ฟังก์ชันสำหรับอ่านไฟล์ Maze และแปลงเป็น 2D int array
     * @param filePath ที่อยู่ของไฟล์ (เช่น "data/m33_35.txt")
     * @return ตารางเขาวงกต (int[][]) ที่เก็บค่าเวลาเดิน (Time Cost)
     */
    public static int[][] loadMaze(String filePath) throws IOException {
        List<int[]> mazeList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int rowCount = 0;

        // Pattern สำหรับจับกลุ่มข้อมูล:
        // 1. "ตัวเลข" เช่น "10", "5"
        // 2. ตัวอักษรเดี่ยวๆ เช่น #, S, G
        Pattern pattern = Pattern.compile("\"(\\d+)\"|([#SG])");

        while ((line = reader.readLine()) != null) {
            // ข้ามบรรทัดว่าง (ถ้ามี)
            if (line.trim().isEmpty()) continue;

            List<Integer> rowValues = new ArrayList<>();
            Matcher matcher = pattern.matcher(line);
            int colCount = 0;

            while (matcher.find()) {
                if (matcher.group(1) != null) {
                    // กรณีเจอตัวเลขในฟันหนู เช่น "10" -> ดึงเลข 10 ออกมา
                    rowValues.add(Integer.parseInt(matcher.group(1)));
                } else if (matcher.group(2) != null) {
                    // กรณีเจอตัวอักษร #, S, G
                    String symbol = matcher.group(2);
                    switch (symbol) {
                        case "#":
                            rowValues.add(WALL);
                            break;
                        case "S":
                            rowValues.add(START);
                            startRow = rowCount;
                            startCol = colCount;
                            System.out.println("Start Point found at: " + startRow + "," + startCol);
                            break;
                        case "G":
                            rowValues.add(GOAL);
                            endRow = rowCount;
                            endCol = colCount;
                            System.out.println("Goal Point found at: " + endRow + "," + endCol);
                            break;
                    }
                }
                colCount++;
            }

            // แปลง List<Integer> เป็น int[] และเก็บลง mazeList
            if (!rowValues.isEmpty()) {
                int[] rowArray = rowValues.stream().mapToInt(i -> i).toArray();
                mazeList.add(rowArray);
                rowCount++;
            }
        }
        reader.close();

        // แปลง List เป็น Array 2 มิติส่งกลับไป
        return mazeList.toArray(new int[0][]);
    }
}