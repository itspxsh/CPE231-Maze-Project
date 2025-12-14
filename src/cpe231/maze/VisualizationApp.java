package cpe231.maze;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class VisualizationApp {

    // 🎨 Color Palette (Modern Dark Mode)
    private static final Color DARK_BG = new Color(40, 44, 52);          // #282c34
    private static final Color PANEL_BG = new Color(33, 37, 43);         // #21252B
    private static final Color TEXT_COLOR = new Color(171, 178, 191);    // #ABB2BF
    private static final Color ACCENT_CYAN = new Color(86, 182, 194);    // #56B6C2
    private static final Color ACCENT_GREEN = new Color(152, 195, 121);  // #98C379
    private static final Color ACCENT_RED = new Color(224, 108, 117);    // #E06C75
    private static final Color BORDER_COLOR = new Color(60, 63, 70);     // เส้นขอบเข้ม
    
    // 🆕 ค่าคงที่สำหรับคำนวณความเร็ว
    private static final int BASE_DELAY_MS = 10;          // Delay พื้นฐาน (10 ms)
    private static final int MAX_ANIMATION_DELAY_MS = 200; // Delay สูงสุด (200 ms)
    
    private static List<MazePanel> mazePanels;
    
    // 🆕 ตัวแปรสำหรับเก็บค่า Delay ที่คำนวณได้
    private static int aStarDelay, dijkstraDelay, gaDelay;

    public static void runDemo(String filePath) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. โหลด Maze
                MazeInfo info = MazeLoader.loadMaze(filePath);
                
                // 2. รัน Algorithms
                AlgorithmResult aStarRes = AlgorithmAdapter.solve("A* Manhattan", info);
                AlgorithmResult dijkRes = AlgorithmAdapter.solve("Dijkstra (Optimized)", info);
                AlgorithmResult gaRes = AlgorithmAdapter.solve("Genetic Algorithm", info);
                
                // 🆕 3. คำนวณค่า Delay สำหรับ Animation (Inverse Speed Scaling)
                int[] delays = calculateSpeedDelays(aStarRes, dijkRes, gaRes);
                aStarDelay = delays[0];
                dijkstraDelay = delays[1];
                gaDelay = delays[2];
                
                // 4. สร้าง Frame หลัก
                JFrame frame = new JFrame("Pathfinding Comparison: " + filePath);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                frame.getContentPane().setBackground(DARK_BG);
                
                JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
                contentPanel.setBackground(DARK_BG);
                contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
                
                // 5. สร้าง MazePanel ทั้งสาม (ไม่ส่ง Delay เข้า Constructor แล้ว)
                mazePanels = new ArrayList<>();
                
                MazePanel aStarPanel = new MazePanel(info, aStarRes);
                MazePanel dijkstraPanel = new MazePanel(info, dijkRes);
                MazePanel gaPanel = new MazePanel(info, gaRes);
                
                mazePanels.add(aStarPanel);
                mazePanels.add(dijkstraPanel);
                mazePanels.add(gaPanel);
                
                // 6. จัด Layout สำหรับ Maze Panels
                JPanel mainPanel = new JPanel(new GridLayout(1, 3, 15, 15));
                mainPanel.setBackground(DARK_BG);
                
                // 7. สร้าง Panel ของแต่ละ Algorithm (ส่งค่า Delay เพื่อแสดงใน Summary)
                mainPanel.add(createAlgorithmPanel(aStarPanel, aStarRes, ACCENT_CYAN, aStarDelay));
                mainPanel.add(createAlgorithmPanel(dijkstraPanel, dijkRes, ACCENT_GREEN, dijkstraDelay));
                mainPanel.add(createAlgorithmPanel(gaPanel, gaRes, ACCENT_RED, gaDelay));

                // 8. สร้าง Master Button Control Panel
                JPanel controlPanel = createMasterControlPanel();
                
                // 9. จัดวางส่วนประกอบหลัก
                contentPanel.add(mainPanel, BorderLayout.CENTER);
                contentPanel.add(controlPanel, BorderLayout.SOUTH);

                frame.add(contentPanel);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

            } catch (IOException e) {
                showErrorDialog("Error reading maze file: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                showErrorDialog("Error running algorithms: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // 🆕 เมธอดคำนวณ Animation Delay (Direct Speed Scaling)
    /**
     * คำนวณ Animation Delay ตามสูตร:
     * D_algo = D_base × (Runtime_algo / T_min)
     * 
     * หมายเหตุ: Algorithm ที่รันเร็วที่สุด (T_min) จะได้ Delay น้อยที่สุด (D_base) → Animation เร็วที่สุด
     *          Algorithm ที่รันช้ากว่าจะได้ Delay มากขึ้น → Animation ช้าลง
     */
    private static int[] calculateSpeedDelays(AlgorithmResult res1, AlgorithmResult res2, AlgorithmResult res3) {
        // 1. ดึง Runtime (Ns) ออกมา (ใช้ Long.MAX_VALUE ถ้า Path ไม่พบ)
        long runtime1 = (res1.executionTimeNs > 0) ? res1.executionTimeNs : Long.MAX_VALUE;
        long runtime2 = (res2.executionTimeNs > 0) ? res2.executionTimeNs : Long.MAX_VALUE;
        long runtime3 = (res3.executionTimeNs > 0) ? res3.executionTimeNs : Long.MAX_VALUE;

        // 2. หา Algorithm ที่รันเร็วที่สุด (Runtime ต่ำสุด = T_min)
        long minRuntime = Math.min(runtime1, Math.min(runtime2, runtime3));
        
        // ป้องกันกรณี minRuntime = Long.MAX_VALUE (ทุก Algorithm ไม่พบ Path)
        if (minRuntime == Long.MAX_VALUE) {
            minRuntime = 1;
        }

        // 3. คำนวณ Delay ของแต่ละ Algorithm
        // สูตร: D_algo = D_base × (Runtime_algo / T_min)
        // Algorithm ที่เร็วที่สุดจะได้ Delay = D_base
        // Algorithm ที่ช้ากว่าจะได้ Delay มากขึ้นตามสัดส่วน
        
        int delay1 = (runtime1 == Long.MAX_VALUE) ? MAX_ANIMATION_DELAY_MS : 
                     (int) (BASE_DELAY_MS * runtime1 / minRuntime);
        int delay2 = (runtime2 == Long.MAX_VALUE) ? MAX_ANIMATION_DELAY_MS : 
                     (int) (BASE_DELAY_MS * runtime2 / minRuntime);
        int delay3 = (runtime3 == Long.MAX_VALUE) ? MAX_ANIMATION_DELAY_MS : 
                     (int) (BASE_DELAY_MS * runtime3 / minRuntime);
        
        // 4. จำกัดค่า Delay ให้อยู่ในช่วงที่เหมาะสม
        delay1 = Math.max(BASE_DELAY_MS, Math.min(MAX_ANIMATION_DELAY_MS, delay1));
        delay2 = Math.max(BASE_DELAY_MS, Math.min(MAX_ANIMATION_DELAY_MS, delay2));
        delay3 = Math.max(BASE_DELAY_MS, Math.min(MAX_ANIMATION_DELAY_MS, delay3));

        return new int[]{delay1, delay2, delay3};
    }

    private static JPanel createMasterControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JButton startAllButton = new JButton("▶  Start All Animations");
        startAllButton.setFont(new Font("Arial", Font.BOLD, 16));
        startAllButton.setForeground(Color.WHITE);
        startAllButton.setBackground(ACCENT_GREEN);
        startAllButton.setFocusPainted(false);
        startAllButton.setBorderPainted(false);
        startAllButton.setPreferredSize(new Dimension(250, 45));
        startAllButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        startAllButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                startAllButton.setBackground(ACCENT_GREEN.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                startAllButton.setBackground(ACCENT_GREEN);
            }
        });
        
        // 🆕 ส่งค่า Delay ที่คำนวณได้เข้าไปใน startAnimation()
        startAllButton.addActionListener(e -> {
            int[] delays = {aStarDelay, dijkstraDelay, gaDelay};
            
            for (int i = 0; i < mazePanels.size(); i++) {
                MazePanel mp = mazePanels.get(i);
                mp.stopAnimation();
                if (mp.getPathSize() > 0) {
                    mp.startAnimation(delays[i]); // 🆕 ส่ง Delay เข้าไป
                }
            }
        });
        
        panel.add(startAllButton);
        return panel;
    }

    private static JPanel createAlgorithmPanel(MazePanel mazeViz, AlgorithmResult result, Color accentColor, int calculatedDelay) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        
        // TitledBorder
        TitledBorder titleBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(accentColor, 2),
            result.algoName
        );
        titleBorder.setTitleFont(new Font("Arial", Font.BOLD, 20));
        titleBorder.setTitleColor(accentColor);
        titleBorder.setTitleJustification(TitledBorder.CENTER);
        panel.setBorder(BorderFactory.createCompoundBorder(
            titleBorder,
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        // 🆕 Summary Text (เพิ่มแถวสำหรับแสดง Adjusted Animation Speed)
        JTextArea summary = new JTextArea(6, 25);
        summary.setEditable(false);
        summary.setFont(new Font("Consolas", Font.PLAIN, 14));
        summary.setBackground(DARK_BG);
        summary.setForeground(TEXT_COLOR);
        summary.setCaretColor(TEXT_COLOR);
        summary.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        
        if (result.totalCost != -1) {
            // 🆕 เพิ่มบรรทัด "Anim Speed" ที่แสดงค่า Delay ที่คำนวณได้
            summary.setText(String.format(
                "Algorithm: %s\n" +
                "Cost (Optimal): %d\n" +
                "Nodes Exp.:     %d\n" +
                "Runtime (ms):   %.4f\n" +
                "Path Steps:     %d\n" +
                "Anim Speed:     %d ms/step",
                result.algoName,
                result.totalCost,
                result.nodesExpanded,
                result.executionTimeNs / 1_000_000.0,
                result.path.size(),
                calculatedDelay // 🆕 แสดงค่า Delay ที่คำนวณได้
            ));
        } else {
            summary.setText("Algorithm: " + result.algoName + "\n\n❌ Path Not Found.");
        }
        
        // จัด Layout
        GridBagConstraints gbc = new GridBagConstraints();
        
        // MazePanel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(mazeViz, gbc);

        // Summary
        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 0, 0);
        panel.add(summary, gbc);
        
        return panel;
    }
    
    private static void showErrorDialog(String message) {
        JOptionPane optionPane = new JOptionPane(
            message,
            JOptionPane.ERROR_MESSAGE
        );
        
        JDialog dialog = optionPane.createDialog("Error");
        dialog.getContentPane().setBackground(DARK_BG);
        dialog.setVisible(true);
    }
}