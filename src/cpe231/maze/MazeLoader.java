package cpe231.maze;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class MazeLoader {
    public static final int WALL = -1;

    public static MazeContext loadMaze(String filePath) throws IOException {
        List<int[]> mazeList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(filePath));
        String line;
        
        int tempStartR = 0, tempStartC = 0, tempEndR = 0, tempEndC = 0;
        int rowCount = 0;
        
        // Regex จับตัวเลขใน "" หรือ #, S, G
        Pattern pattern = Pattern.compile("\"(\\d+)\"|([#SG])");

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
                    if (symbol.equals("#")) {
                        rowValues.add(WALL);
                    } else if (symbol.equals("S")) {
                        rowValues.add(0); 
                        tempStartR = rowCount; tempStartC = colCount;
                    } else if (symbol.equals("G")) {
                        rowValues.add(0); 
                        tempEndR = rowCount; tempEndC = colCount;
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
        
        int[][] grid = mazeList.toArray(new int[0][]);
        return new MazeContext(grid, tempStartR, tempStartC, tempEndR, tempEndC);
    }
}