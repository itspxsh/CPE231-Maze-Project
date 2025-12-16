package cpe231.maze.ui;

import cpe231.maze.core.MazeContext;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MazePanel extends JPanel {
    private MazeContext ctx;
    private List<int[]> path;
    
    // Professional color palette
    private static final Color BG_COLOR = new Color(245, 245, 250);
    private static final Color WALL_COLOR = new Color(52, 73, 94);
    private static final Color FLOOR_COLOR = new Color(236, 240, 241);
    private static final Color GRID_COLOR = new Color(189, 195, 199);
    private static final Color PATH_COLOR = new Color(46, 204, 113);
    private static final Color START_COLOR = new Color(52, 152, 219);
    private static final Color END_COLOR = new Color(231, 76, 60);

    public MazePanel() {
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1));
    }

    public void setMaze(MazeContext ctx) {
        this.ctx = ctx;
        this.path = null;
        repaint();
    }

    public void setPath(List<int[]> path) {
        this.path = path;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (ctx == null) {
            g2.setColor(new Color(149, 165, 166));
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            FontMetrics fm = g2.getFontMetrics();
            String msg = "No maze loaded";
            int x = (getWidth() - fm.stringWidth(msg)) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(msg, x, y);
            return;
        }

        int rows = ctx.rows;
        int cols = ctx.cols;
        int cellW = getWidth() / cols;
        int cellH = getHeight() / rows;
        int cellSize = Math.min(cellW, cellH);
        
        if (cellSize < 1) cellSize = 1;

        int xOffset = (getWidth() - (cols * cellSize)) / 2;
        int yOffset = (getHeight() - (rows * cellSize)) / 2;

        int[][] grid = ctx.getGridDirect();

        // Draw cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = xOffset + c * cellSize;
                int y = yOffset + r * cellSize;
                
                int val = grid[r][c];
                
                if (val == -1) {
                    // Wall
                    g2.setColor(WALL_COLOR);
                    g2.fillRect(x, y, cellSize, cellSize);
                } else {
                    // Floor
                    g2.setColor(FLOOR_COLOR);
                    g2.fillRect(x, y, cellSize, cellSize);
                    
                    // Grid lines (only if cells are large enough)
                    if (cellSize > 8) {
                        g2.setColor(GRID_COLOR);
                        g2.drawRect(x, y, cellSize, cellSize);
                    }
                }
            }
        }

        // Draw path BEFORE start/end markers so they appear on top
        if (path != null && !path.isEmpty()) {
            g2.setColor(PATH_COLOR);
            g2.setStroke(new BasicStroke(Math.max(2, cellSize / 3), 
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            for (int i = 0; i < path.size() - 1; i++) {
                int[] p1 = path.get(i);
                int[] p2 = path.get(i+1);
                
                int x1 = xOffset + p1[1] * cellSize + cellSize/2;
                int y1 = yOffset + p1[0] * cellSize + cellSize/2;
                int x2 = xOffset + p2[1] * cellSize + cellSize/2;
                int y2 = yOffset + p2[0] * cellSize + cellSize/2;
                
                g2.drawLine(x1, y1, x2, y2);
            }
        }

        // Draw Start marker
        int startX = xOffset + ctx.startCol * cellSize;
        int startY = yOffset + ctx.startRow * cellSize;
        g2.setColor(START_COLOR);
        g2.fillRoundRect(startX + 2, startY + 2, cellSize - 4, cellSize - 4, 4, 4);
        
        // Draw End marker
        int endX = xOffset + ctx.endCol * cellSize;
        int endY = yOffset + ctx.endRow * cellSize;
        g2.setColor(END_COLOR);
        g2.fillRoundRect(endX + 2, endY + 2, cellSize - 4, cellSize - 4, 4, 4);
    }
}