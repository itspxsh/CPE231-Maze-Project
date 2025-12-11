package cpe231.maze;

// MazeLoader.java (โค้ดที่แก้ไขแล้ว)

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class MazeLoader {
    public static final int WALL = -1;

    // เมธอดนี้จะคืนค่า MazeInfo แทน
    public static MazeInfo loadMaze(String filePath) throws IOException {
        List<int[]> mazeList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        int rowCount = 0;
        Pattern pattern = Pattern.compile("\"(\\d+)\"|([#SG])");

        // ประกาศตัวแปร Local สำหรับเก็บพิกัด Start/End
        int startR = -1, startC = -1;
        int endR = -1, endC = -1;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            List<Integer> rowValues = new ArrayList<>();
            Matcher matcher = pattern.matcher(line);
            int colCount = 0;
            while (matcher.find()) {
                if (matcher.group(1) != null) {
                    rowValues.add(Integer.parseInt(matcher.group(1)));
                } else if (matcher.group(2) != null) {
                    String symbol = matcher.group(2);
                    if (symbol.equals("#")) rowValues.add(WALL);
                    else if (symbol.equals("S")) {
                        rowValues.add(0); startR = rowCount; startC = colCount; // ✅ เก็บค่าลง Local Variable
                    } else if (symbol.equals("G")) {
                        rowValues.add(0); endR = rowCount; endC = colCount;     // ✅ เก็บค่าลง Local Variable
                    }
                }
                colCount++;
            }
            if (!rowValues.isEmpty()) {
                mazeList.add(rowValues.stream().mapToInt(i -> i).toArray());
                rowCount++;
            }
        }
        reader.close();
        
        int[][] mazeArray = mazeList.toArray(new int[0][]);
        
        // คืนค่า Maze Array พร้อมพิกัด Start/End ในรูปแบบ MazeInfo
        return new MazeInfo(mazeArray, new Coordinate(startR, startC), new Coordinate(endR, endC));
    }
}