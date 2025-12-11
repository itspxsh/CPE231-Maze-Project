// VisualizationApp.java (แก้ไขให้มั่นใจ)

package cpe231.maze;

import java.awt.*;
import java.io.IOException;
import javax.swing.*;

public class VisualizationApp {

    public static void runDemo(String filePath) {
        // *** Critical Fix: ต้องรันใน Event Dispatch Thread (EDT) ***
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. โหลด Maze
                MazeInfo info = MazeLoader.loadMaze(filePath);
                
                // 2. รัน Algorithms (เรียกผ่าน Adapter)
                AlgorithmResult aStarRes = AlgorithmAdapter.solve("A* Manhattan", info);
                AlgorithmResult dijkRes = AlgorithmAdapter.solve("Dijkstra (Optimized)", info);
                AlgorithmResult gaRes = AlgorithmAdapter.solve("Genetic Algorithm", info);
                
                // 3. สร้าง Frame หลัก
                JFrame frame = new JFrame("Pathfinding Comparison: " + filePath);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
                // 4. จัด Layout 1 แถว 3 คอลัมน์
                JPanel mainPanel = new JPanel(new GridLayout(1, 3, 15, 15)); 
                
                // 5. สร้าง Panel ของแต่ละ Algorithm
                mainPanel.add(createAlgorithmPanel(info, aStarRes));
                mainPanel.add(createAlgorithmPanel(info, dijkRes));
                mainPanel.add(createAlgorithmPanel(info, gaRes));

                frame.add(mainPanel);
                frame.pack(); 
                frame.setLocationRelativeTo(null); 
                frame.setVisible(true);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Error reading maze file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "Error running algorithms: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }

    private static JPanel createAlgorithmPanel(MazeInfo info, AlgorithmResult result) {
        // 🛑 แก้ไข: เปลี่ยนไปใช้ GridBagLayout แทน BoxLayout
        JPanel panel = new JPanel(new GridBagLayout()); 
        
        // ** ตรวจสอบ: Path ใน result ต้องเป็น List<int[]> ชั่วคราว **
        
        // 5.1 เพิ่ม Maze Visualization (ภาพ)
        MazePanel mazeViz = new MazePanel(info, result);
        
        // 5.2 เพิ่ม Summary Text (ผลลัพธ์ตัวเลข)
        JTextArea summary = new JTextArea(4, 25); 
        summary.setEditable(false);
        summary.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        // (โค้ดส่วนนี้เหมือนเดิม) ...
        if (result.totalCost != -1) {
             summary.setText(
                "Algorithm: " + result.algoName + "\n" +
                "Cost (Optimal): " + result.totalCost + "\n" +
                "Nodes Exp.:   " + result.nodesExpanded + "\n" +
                "Runtime (ms): " + String.format("%.4f", result.executionTimeNs / 1_000_000.0) + "\n" +
                "Steps : " + result.path.size()
            );
        } else {
             summary.setText("Algorithm: " + result.algoName + "\nPath Not Found.");
        }
        
        // จัด Border ให้สวยงาม
        panel.setBorder(BorderFactory.createTitledBorder(result.algoName));
        
        // 🛑 NEW: ใช้ GridBagConstraints ในการจัดวาง
        GridBagConstraints gbc = new GridBagConstraints();
        
        // 1. เพิ่ม MazePanel (ให้ใช้พื้นที่ที่เหลือทั้งหมด)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0; // ใช้ความกว้างเต็ม
        gbc.weighty = 1.0; // ใช้ความสูงเต็ม
        gbc.fill = GridBagConstraints.BOTH; // ขยายทั้งความกว้างและความสูง
        panel.add(mazeViz, gbc);

        // 2. เพิ่ม Summary (ให้อยู่ด้านล่างและไม่ขยายความสูง)
        gbc.gridy = 1;
        gbc.weighty = 0; // ไม่ขยายความสูง
        gbc.fill = GridBagConstraints.HORIZONTAL; // ขยายแค่ความกว้าง
        gbc.insets = new Insets(10, 5, 5, 5); // เพิ่มระยะห่าง
        panel.add(summary, gbc);
        
        return panel;
    }
}