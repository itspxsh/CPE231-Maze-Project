package cpe231.maze.ui;

import cpe231.maze.core.MazeContext;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MazePanel extends JPanel {
    private MazeContext context;
    private List<int[]> path;

    public MazePanel() {
        this.path = new ArrayList<>();
        this.setBackground(new Color(30, 30, 30)); // Dark Background
    }

    public void setMaze(MazeContext context) {
        this.context = context;
        this.path.clear();
        repaint();
    }

    public void setPath(List<int[]> path) {
        this.path = (path != null) ? path : new ArrayList<>();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (context == null) return;

        int[][] grid = context.getGridDirect();
        int rows = context.rows;
        int cols = context.cols;

        int cellW = getWidth() / Math.max(1, cols);
        int cellH = getHeight() / Math.max(1, rows);
        int size = Math.min(cellW, cellH);
        
        int offsetX = (getWidth() - (cols * size)) / 2;
        int offsetY = (getHeight() - (rows * size)) / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = offsetX + c * size;
                int y = offsetY + r * size;

                if (grid[r][c] == -1) {
                    g.setColor(Color.BLACK); 
                    g.fillRect(x, y, size, size);
                } else {
                    g.setColor(Color.WHITE); 
                    g.fillRect(x, y, size, size);
                    g.setColor(new Color(220, 220, 220)); 
                    g.drawRect(x, y, size, size);
                }
            }
        }

        // FIX IS HERE: Removed the syntax error
        if (path != null && !path.isEmpty()) {
            g.setColor(new Color(0, 255, 255, 200)); 
            for (int[] p : path) {
                g.fillRect(offsetX + p[1] * size, offsetY + p[0] * size, size, size);
            }
            
            if (!path.isEmpty()) {
                int[] head = path.get(path.size() - 1);
                g.setColor(new Color(255, 0, 100));
                g.fillRect(offsetX + head[1] * size, offsetY + head[0] * size, size, size);
            }
        }

        g.setColor(new Color(0, 255, 0));
        g.fillRect(offsetX + context.startCol * size, offsetY + context.startRow * size, size, size);

        g.setColor(new Color(255, 0, 0));
        g.fillRect(offsetX + context.endCol * size, offsetY + context.endRow * size, size, size);
    }
}