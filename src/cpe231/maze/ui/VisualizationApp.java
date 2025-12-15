package cpe231.maze.ui;

import cpe231.maze.algorithms.*;
import cpe231.maze.core.*;
import cpe231.maze.io.MazeLoader;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class VisualizationApp extends JFrame {

    private MazePanel[] panels;
    private JLabel[] statusLabels;
    private MazeContext currentContext;
    public static volatile int delay = 20; 
    private JComboBox<String> fileSelector;
    private JCheckBox skipAnimationCheck;
    private boolean isRunning = false;

    public VisualizationApp() {
        setTitle("CPE231 Maze Project - Algorithm Comparison");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- TOP PANEL ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        File dataDir = new File("data");
        String[] files = dataDir.list((dir, name) -> name.endsWith(".txt"));
        
        // FIX: Natural Sort (m15 before m100)
        if (files != null) {
            Arrays.sort(files, new Comparator<String>() {
                public int compare(String s1, String s2) {
                    return extractInt(s1) - extractInt(s2);
                }
                int extractInt(String s) {
                    String num = s.replaceAll("\\D", "");
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }
            });
        }
        
        fileSelector = new JComboBox<>(files != null ? files : new String[]{"No files"});
        JButton btnLoad = new JButton("Load & Run");
        btnLoad.addActionListener(e -> startDemo());

        JSlider speedSlider = new JSlider(0, 100, 20);
        speedSlider.setInverted(true);
        speedSlider.addChangeListener(e -> delay = speedSlider.getValue());
        
        skipAnimationCheck = new JCheckBox("Skip Anim");
        skipAnimationCheck.addActionListener(e -> {
            if(skipAnimationCheck.isSelected()) delay = 0;
            else delay = speedSlider.getValue();
        });

        JButton btnBenchmark = new JButton("📊 Run Benchmark");
        btnBenchmark.addActionListener(e -> cpe231.maze.benchmark.Benchmark.runBenchmarkSuite());

        controlPanel.add(new JLabel("Map:"));
        controlPanel.add(fileSelector);
        controlPanel.add(btnLoad);
        controlPanel.add(new JLabel("Speed:"));
        controlPanel.add(speedSlider);
        controlPanel.add(skipAnimationCheck);
        controlPanel.add(btnBenchmark);
        add(controlPanel, BorderLayout.NORTH);

        // --- GRID PANEL ---
        JPanel gridPanel = new JPanel(new GridLayout(1, 4, 10, 0)); 
        gridPanel.setBackground(new Color(45, 45, 45)); // Dark Gap
        panels = new MazePanel[4];
        statusLabels = new JLabel[4];
        String[] headers = {"A* (Manhattan)", "Dijkstra", "Genetic (Pure)", "Genetic (Adaptive)"};
        
        for (int i = 0; i < 4; i++) {
            JPanel container = new JPanel(new BorderLayout());
            container.setBackground(new Color(30, 30, 30));
            container.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), 
                headers[i], 
                0, 0, 
                new Font("SansSerif", Font.BOLD, 12), 
                Color.CYAN));
            
            panels[i] = new MazePanel();
            statusLabels[i] = new JLabel("<html>Waiting...</html>");
            statusLabels[i].setForeground(Color.LIGHT_GRAY);
            statusLabels[i].setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            
            container.add(panels[i], BorderLayout.CENTER);
            container.add(statusLabels[i], BorderLayout.SOUTH);
            gridPanel.add(container);
        }
        add(gridPanel, BorderLayout.CENTER);

        setSize(1400, 800);
        setLocationRelativeTo(null);
    }

    public void runDemo() {
        setVisible(true);
        if (fileSelector.getItemCount() > 0) {
            loadMap((String) fileSelector.getSelectedItem());
        }
    }

    private void loadMap(String filename) {
        if (filename == null) return;
        try {
            MazeLoader.loadMaze("data/" + filename);
            currentContext = new MazeContext(MazeLoader.maze, MazeLoader.startRow, MazeLoader.startCol, MazeLoader.endRow, MazeLoader.endCol);
            for (MazePanel p : panels) p.setMaze(currentContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startDemo() {
        if (isRunning) return;
        isRunning = true;
        
        loadMap((String) fileSelector.getSelectedItem());
        
        MazeSolver[] solvers = {
            new AStarSolver(),
            new DijkstraSolver(),
            new GeneticSolverPure(),
            new GeneticSolverAdaptive()
        };

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            final MazeSolver solver = solvers[i];
            
            new Thread(() -> {
                try {
                    // Slight stagger to prevent UI freeze
                    Thread.sleep(idx * 50); 
                    
                    AlgorithmResult result = solver.solve(currentContext);
                    
                    if (!skipAnimationCheck.isSelected()) {
                        List<int[]> animatedPath = new ArrayList<>();
                        for (int[] step : result.path()) {
                            animatedPath.add(step);
                            panels[idx].setPath(new ArrayList<>(animatedPath));
                            try { Thread.sleep(Math.max(1, delay)); } catch (InterruptedException ignored) {}
                        }
                    } 
                    
                    panels[idx].setPath(result.path());
                    
                    String color = result.cost() != -1 ? "#00FF00" : "#FF0000";
                    String stat = String.format("<html>Status: <font color='%s'>%s</font><br>Cost: %d<br>Time: %.2f ms<br>Nodes: %d</html>",
                        color, result.cost() != -1 ? "SUCCESS" : "FAILED", 
                        result.cost(), result.durationNs() / 1_000_000.0, result.nodesExpanded());
                    
                    SwingUtilities.invokeLater(() -> statusLabels[idx].setText(stat));
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (Exception e){}
            isRunning = false;
        }).start();
    }
}