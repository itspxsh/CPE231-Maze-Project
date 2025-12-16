package cpe231.maze.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MazeLoader {
    public static int[][] maze;
    public static int startRow = -1, startCol = -1;
    public static int endRow = -1, endCol = -1;

    public static void loadMaze(String filePath) throws IOException {
        System.out.println("📂 Loading maze from: " + filePath);
        
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Remove trailing whitespace but KEEP quotes/structure
                if (!line.trim().isEmpty() && !line.startsWith("[")) {
                    lines.add(line.replaceAll("\\s+$", ""));
                }
            }
        }

        if (lines.isEmpty()) throw new IOException("File is empty: " + filePath);

        int rows = lines.size();
        
        // Dynamic parsing to find true width
        List<int[]> parsedRows = new ArrayList<>();
        int maxCols = 0;
        
        // Reset markers
        startRow = -1; startCol = -1;
        endRow = -1; endCol = -1;

        for (int r = 0; r < rows; r++) {
            String line = lines.get(r);
            List<Integer> rowValues = new ArrayList<>();
            
            int i = 0;
            while (i < line.length()) {
                char c = line.charAt(i);
                
                if (c == '"') {
                    // Quoted value (e.g., "10")
                    int closeQuote = line.indexOf('"', i + 1);
                    if (closeQuote != -1) {
                        String numStr = line.substring(i + 1, closeQuote);
                        try {
                            rowValues.add(Integer.parseInt(numStr));
                        } catch (NumberFormatException e) {
                            rowValues.add(1);
                        }
                        i = closeQuote + 1;
                        continue;
                    }
                }
                
                // Single Character processing
                if (c == '#') {
                    rowValues.add(-1);
                } else if (c == 'S' || c == 's') {
                    startRow = r;
                    startCol = rowValues.size();
                    rowValues.add(1);
                    System.out.println("   📍 Start found at: " + r + "," + startCol);
                } else if (c == 'G' || c == 'g' || c == 'E' || c == 'e') {
                    endRow = r;
                    endCol = rowValues.size();
                    rowValues.add(1);
                    System.out.println("   🏁 Goal found at: " + r + "," + endCol);
                } else if (Character.isDigit(c)) {
                    rowValues.add(c - '0');
                } else if (c != '"') {
                    // Walkable default (skip random quotes if malformed)
                    rowValues.add(1);
                }
                i++;
            }
            
            // Convert List to array
            int[] rowArr = new int[rowValues.size()];
            for(int k=0; k<rowValues.size(); k++) rowArr[k] = rowValues.get(k);
            parsedRows.add(rowArr);
            maxCols = Math.max(maxCols, rowArr.length);
        }

        // Build final grid
        maze = new int[rows][maxCols];
        for(int r=0; r<rows; r++) {
            int[] pRow = parsedRows.get(r);
            for(int c=0; c<maxCols; c++) {
                if (c < pRow.length) maze[r][c] = pRow[c];
                else maze[r][c] = -1; // Fill jagged edges with walls
            }
        }
        
        System.out.println("   📐 Grid Size: " + rows + "x" + maxCols);

        if (startRow == -1 || endRow == -1) 
            throw new IOException("Maze missing 'S' or 'G' markers.");
    }
}