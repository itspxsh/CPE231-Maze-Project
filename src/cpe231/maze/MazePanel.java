package cpe231.maze;

import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.*;

public class MazePanel extends JPanel {
    private final MazeInfo info;
    private final Set<Coordinate> pathSet; 

    public MazePanel(MazeInfo info, AlgorithmResult result) {
        this.info = info;
        
        this.pathSet = convertRawPathToSet(result.path); 
        
        // กำหนด PreferredSize เป็นขนาดเริ่มต้นที่เหมาะสม
        setPreferredSize(new Dimension(300, 300));
        setMinimumSize(new Dimension(100, 100)); 
    }

    private Set<Coordinate> convertRawPathToSet(List<int[]> rawPath) {
        Set<Coordinate> set = new HashSet<>();
        if (rawPath != null) {
            for (int[] p : rawPath) {
                if (p.length >= 2) {
                    set.add(new Coordinate(p[0], p[1]));
                }
            }
        }
        return set;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        int rows = info.maze().length;
        int cols = info.maze()[0].length;
        
        int currentWidth = getWidth();
        
        // คำนวณขนาดเซลล์ (cs) โดยใช้ความกว้างของ Panel เท่านั้น
        double currentCellSize = (double)currentWidth / cols;
        int cs = (int) currentCellSize;

        if (cs < 1) return; 

        // ปรับ PreferredSize เพื่อช่วยให้ Layout Manager จัดการได้ดีขึ้น
        int preferredHeight = (int)(currentCellSize * rows);
        setPreferredSize(new Dimension(currentWidth, preferredHeight));

        // 🛑 ลบ: การคำนวณ Font Size สำหรับ Cost ไม่จำเป็นแล้ว
        // int costFontSize = Math.max(8, cs / 3);

        // 1. วาด Maze Grid
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int cost = info.maze()[r][c];
                
                // 1.1 วาดพื้นหลัง (สีตาม Cost)
                if (cost == MazeLoader.WALL) {
                    g2d.setColor(Color.BLACK); // กำแพง
                } else {
                    // 🛑 แก้ไข: ปรับสีให้เป็นเขียวอ่อน (Cost 1) ไปเขียวเข้ม (Cost 10)
                    // HSB: Hue = 0.35f (เขียว), Saturation (สีเข้มขึ้นเมื่อ Cost เพิ่ม), Brightness (สีเข้มขึ้นเมื่อ Cost เพิ่ม)
                    // Cost 1: Sat=0.1, Bright=0.8 (อ่อน)
                    // Cost 10: Sat=0.9, Bright=0.4 (เข้ม)
                    float saturation = 0.1f + (float) cost / 10.0f * 0.8f; 
                    float brightness = 0.8f - (float) cost / 10.0f * 0.4f; 
                    g2d.setColor(Color.getHSBColor(0.35f, saturation, brightness));
                }
                g2d.fillRect(c * cs, r * cs, cs, cs); 

                // 🛑 ลบ: 1.2 วาด Cost Text ออกไป
                
                // 1.3 วาด Grid Lines
                g2d.setColor(Color.GRAY);
                g2d.drawRect(c * cs, r * cs, cs, cs); 
            }
        }
        
        // 2. วาด Path (เส้นทางที่เลือก)
        for (Coordinate coord : pathSet) {
            // โหนดที่เป็น S หรือ G จะถูกวาดทับใน Step 3
            if (coord.equals(info.start()) || coord.equals(info.end())) continue; 
            
            g2d.setColor(Color.RED.darker()); 
            g2d.fillRect(coord.c() * cs, coord.r() * cs, cs, cs);
            
            // วาดเส้น Grid ทับ
            g2d.setColor(Color.GRAY);
            g2d.drawRect(coord.c() * cs, coord.r() * cs, cs, cs);
        }

        // 3. วาด Start (S) และ Goal (G) เป็นสีเหลืองเต็มช่อง
        
        // Start (S)
        g2d.setColor(Color.YELLOW); 
        g2d.fillRect(info.start().c() * cs, info.start().r() * cs, cs, cs);
        
        // วาดเส้น Grid ทับ Start
        g2d.setColor(Color.GRAY);
        g2d.drawRect(info.start().c() * cs, info.start().r() * cs, cs, cs);

        // Goal (G)
        g2d.setColor(Color.YELLOW); 
        g2d.fillRect(info.end().c() * cs, info.end().r() * cs, cs, cs);
        
        // วาดเส้น Grid ทับ Goal
        g2d.setColor(Color.GRAY);
        g2d.drawRect(info.end().c() * cs, info.end().r() * cs, cs, cs);
    }
}