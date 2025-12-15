package cpe231.maze.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MazeLoader {
    // Make these PUBLIC static so VisualizationApp can access them
    public static int[][] maze;
    public static int startRow, startCol;
    public static int endRow, endCol;

    public static void loadMaze(String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }

        int rows = lines.size();
        int cols = lines.get(0).length();
        maze = new int[rows][cols];

        for (int i = 0; i < rows; i++) {
            String line = lines.get(i);
            for (int j = 0; j < cols; j++) {
                char c = line.charAt(j);
                if (c == '#') {
                    maze[i][j] = -1; // Wall
                } else {
                    // Cost based on character (default 1)
                    if (c >= '1' && c <= '9') {
                        maze[i][j] = c - '0';
                    } else {
                        maze[i][j] = 1; 
                    }

                    if (c == 'S') {
                        startRow = i;
                        startCol = j;
                        maze[i][j] = 1;
                    } else if (c == 'E') {
                        endRow = i;
                        endCol = j;
                        maze[i][j] = 1;
                    }
                }
            }
        }
    }
}