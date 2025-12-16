package cpe231.maze.ui;

import cpe231.maze.core.MazeContext;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class MazePanel extends JPanel {
    private MazeContext ctx;
    private List<int[]> path;
    
    // CACHE: Stores the drawn walls/floor so we don't redraw them every frame
    private BufferedImage cachedBackground;
    
    // Professional color palette
    private static final Color BG_COLOR = new Color(245, 245, 250);
    private static final Color WALL_COLOR = new Color(52, 73, 94);
    private static final Color FLOOR_COLOR = new Color(236, 240, 241);
    private static final Color GRID_COLOR = new Color(220, 225, 230);
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
        this.cachedBackground = null; // Invalidate cache when map changes
        repaint();
    }

    public void setPath(List<int[]> path) {
        if (path != null) {
            this.path = new ArrayList<>(path); // Copy to prevent concurrent modification
        } else {
            this.path = null;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (ctx == null) {
            drawPlaceholder((Graphics2D)g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. Calculate Sizes
        int cellW = getWidth() / ctx.cols;
        int cellH = getHeight() / ctx.rows;
        int cellSize = Math.min(cellW, cellH);
        if (cellSize < 1) cellSize = 1;

        int mapWidth = ctx.cols * cellSize;
        int mapHeight = ctx.rows * cellSize;
        int xOffset = (getWidth() - mapWidth) / 2;
        int yOffset = (getHeight() - mapHeight) / 2;

        // 2. Draw Background (Using Cache)
        if (cachedBackground == null || cachedBackground.getWidth() != getWidth() || cachedBackground.getHeight() != getHeight()) {
            renderMazeToCache(cellSize, xOffset, yOffset);
        }
        g2.drawImage(cachedBackground, 0, 0, null);

        // 3. Draw Path (Dynamic Layer)
        if (path != null && !path.isEmpty()) {
            g2.setColor(PATH_COLOR);
            float strokeWidth = Math.max(2f, cellSize * 0.4f);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            
            if (path.size() > 1) {
                int[] xPoints = new int[path.size()];
                int[] yPoints = new int[path.size()];
                for (int i = 0; i < path.size(); i++) {
                    int[] p = path.get(i);
                    xPoints[i] = xOffset + p[1] * cellSize + cellSize/2;
                    yPoints[i] = yOffset + p[0] * cellSize + cellSize/2;
                }
                g2.drawPolyline(xPoints, yPoints, path.size());
            } else {
                int[] p = path.get(0);
                int x = xOffset + p[1] * cellSize + cellSize/2;
                int y = yOffset + p[0] * cellSize + cellSize/2;
                g2.fillOval((int)(x-strokeWidth), (int)(y-strokeWidth), (int)(strokeWidth*2), (int)(strokeWidth*2));
            }
        }

        // 4. Draw Markers (Always Top)
        drawMarker(g2, ctx.startRow, ctx.startCol, START_COLOR, cellSize, xOffset, yOffset);
        drawMarker(g2, ctx.endRow, ctx.endCol, END_COLOR, cellSize, xOffset, yOffset);
    }

    private void renderMazeToCache(int cellSize, int xOffset, int yOffset) {
        cachedBackground = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = cachedBackground.createGraphics();
        
        g2.setColor(BG_COLOR);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int[][] grid = ctx.getGridDirect();
        for (int r = 0; r < ctx.rows; r++) {
            for (int c = 0; c < ctx.cols; c++) {
                int x = xOffset + c * cellSize;
                int y = yOffset + r * cellSize;
                
                if (grid[r][c] == -1) {
                    g2.setColor(WALL_COLOR);
                    g2.fillRect(x, y, cellSize, cellSize);
                } else {
                    g2.setColor(FLOOR_COLOR);
                    g2.fillRect(x, y, cellSize, cellSize);
                    if (cellSize > 4) {
                        g2.setColor(GRID_COLOR);
                        g2.drawRect(x, y, cellSize, cellSize);
                    }
                }
            }
        }
        g2.dispose();
    }

    private void drawMarker(Graphics2D g2, int r, int c, Color color, int size, int xOff, int yOff) {
        int x = xOff + c * size;
        int y = yOff + r * size;
        int margin = Math.max(1, size / 5);
        g2.setColor(color);
        g2.fillRoundRect(x + margin, y + margin, size - 2*margin, size - 2*margin, 4, 4);
    }

    private void drawPlaceholder(Graphics2D g2) {
        g2.setColor(new Color(149, 165, 166));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        FontMetrics fm = g2.getFontMetrics();
        String msg = "No maze loaded";
        int x = (getWidth() - fm.stringWidth(msg)) / 2;
        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(msg, x, y);
    }
}