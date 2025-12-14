package cpe231.maze;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;

public class MazePanel extends JPanel {
    private final MazeInfo info;
    private final List<Coordinate> fullPath;
    private final Set<Coordinate> pathSet; 
    
    // ตัวแปรสำหรับ Animation
    private List<Coordinate> animatedPath = new ArrayList<>();
    private Timer timer;
    private int stepIndex = 0;

    // 🎨 Color Palette (Modern Dark Mode)
    private static final Color PATH_COLOR = new Color(0, 255, 255);      // Cyan #00FFFF
    private static final Color START_COLOR = new Color(0, 255, 0);       // Green #00FF00
    private static final Color GOAL_COLOR = new Color(255, 0, 0);        // Red #FF0000
    private static final Color ANIMATION_HEAD_COLOR = new Color(255, 0, 255); // Magenta #FF00FF
    private static final Color WALL_COLOR = Color.BLACK;                 // Black #000000
    private static final Color WALKABLE_COLOR = Color.WHITE;             // White #FFFFFF
    private static final Color GRID_COLOR = new Color(200, 200, 200);   // Light Gray

    public MazePanel(MazeInfo info, AlgorithmResult result) {
        this.info = info;
        this.fullPath = convertRawPathToCoordinateList(result.path);
        this.pathSet = new HashSet<>(this.fullPath);
        
        setPreferredSize(new Dimension(300, 300));
        setMinimumSize(new Dimension(100, 100));
        setBackground(WALKABLE_COLOR);
    }

    private List<Coordinate> convertRawPathToCoordinateList(List<int[]> rawPath) {
        List<Coordinate> list = new ArrayList<>();
        if (rawPath != null) {
            for (int[] p : rawPath) {
                if (p.length >= 2) {
                    list.add(new Coordinate(p[0], p[1]));
                }
            }
        }
        return list;
    }
    
    // 🆕 แก้ไข startAnimation ให้รับค่า calculatedDelay
    public void startAnimation(int calculatedDelay) {
        if (fullPath.isEmpty() || timer != null) return;

        stepIndex = 0;
        animatedPath.clear();

        // 🆕 ใช้ calculatedDelay ที่ส่งมาแทนค่าคงที่
        timer = new Timer(calculatedDelay, e -> {
            if (stepIndex < fullPath.size()) {
                animatedPath.add(fullPath.get(stepIndex));
                stepIndex++;
                repaint();
            } else {
                timer.stop();
                timer = null;
                repaint();
            }
        });
        timer.start();
    }
    
    public void stopAnimation() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    public int getPathSize() {
        return fullPath.size();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // เปิด Anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int rows = info.maze().length;
        int cols = info.maze()[0].length;
        
        int currentWidth = getWidth();
        double currentCellSize = (double)currentWidth / cols;
        int cs = (int) currentCellSize;

        if (cs < 1) return; 

        int preferredHeight = (int)(currentCellSize * rows);
        setPreferredSize(new Dimension(currentWidth, preferredHeight));

        // 1. วาด Maze Grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cost = info.maze()[r][c];
                
                if (cost == MazeLoader.WALL) {
                    g2d.setColor(WALL_COLOR);
                } else {
                    g2d.setColor(WALKABLE_COLOR);
                }
                g2d.fillRect(c * cs, r * cs, cs, cs); 

                g2d.setColor(GRID_COLOR);
                g2d.drawRect(c * cs, r * cs, cs, cs); 
            }
        }
        
        // 2. วาด Path
        List<Coordinate> path = (timer != null) ? animatedPath : fullPath;
        
        for (Coordinate coord : path) {
            if (coord.equals(info.start()) || coord.equals(info.end())) continue; 
            
            g2d.setColor(PATH_COLOR); 
            g2d.fillRect(coord.c() * cs, coord.r() * cs, cs, cs);
            
            g2d.setColor(GRID_COLOR);
            g2d.drawRect(coord.c() * cs, coord.r() * cs, cs, cs);
        }

        // 3. วาด Start (S) - สีเขียว
        g2d.setColor(START_COLOR); 
        g2d.fillRect(info.start().c() * cs, info.start().r() * cs, cs, cs);
        g2d.setColor(GRID_COLOR);
        g2d.drawRect(info.start().c() * cs, info.start().r() * cs, cs, cs);

        // 4. วาด Goal (G) - สีแดง
        g2d.setColor(GOAL_COLOR); 
        g2d.fillRect(info.end().c() * cs, info.end().r() * cs, cs, cs);
        g2d.setColor(GRID_COLOR);
        g2d.drawRect(info.end().c() * cs, info.end().r() * cs, cs, cs);
        
        // 5. วาดหัว Animation
        if (timer != null && !animatedPath.isEmpty()) {
            Coordinate head = animatedPath.get(animatedPath.size() - 1);
            if (!head.equals(info.start())) {
                g2d.setColor(ANIMATION_HEAD_COLOR);
                int ovalSize = (int)(cs * 0.6);
                int offset = (cs - ovalSize) / 2;
                g2d.fillOval(head.c() * cs + offset, head.r() * cs + offset, ovalSize, ovalSize);
            }
        }
    }
}