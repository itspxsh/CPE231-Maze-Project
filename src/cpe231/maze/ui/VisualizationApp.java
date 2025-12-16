package cpe231.maze.ui;

import cpe231.maze.algorithms.*;
import cpe231.maze.core.*;
import cpe231.maze.io.MazeLoader;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class VisualizationApp extends JFrame {

    private MazePanel[] panels;
    private JLabel[] statusLabels;
    private MazeContext currentContext;
    private volatile int delay = 20;
    private JComboBox<String> fileSelector;
    private JCheckBox skipAnimationCheck;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private JButton btnLoad;
    private JButton btnCancel;

    // Enhanced color palette
    private static final Color PRIMARY_BG = new Color(240, 242, 245);
    private static final Color CONTROL_BG = new Color(255, 255, 255);
    private static final Color ACCENT_COLOR = new Color(37, 99, 235);
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129);
    private static final Color ERROR_COLOR = new Color(239, 68, 68);
    private static final Color HOVER_COLOR = new Color(59, 130, 246);

    public VisualizationApp() {
        setTitle("CPE231 Maze Pathfinding - Algorithm Comparison");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(PRIMARY_BG);

        // Control Panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.NORTH);

        // Visualization Grid
        JPanel gridPanel = createVisualizationGrid();
        add(gridPanel, BorderLayout.CENTER);

        // Footer with info
        JPanel footerPanel = createFooter();
        add(footerPanel, BorderLayout.SOUTH);

        setSize(1500, 850);
        setLocationRelativeTo(null);
    }

    private JPanel createControlPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(CONTROL_BG);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(229, 231, 235)),
            new EmptyBorder(16, 20, 16, 20)
        ));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        leftPanel.setBackground(CONTROL_BG);

        // Map selection with enhanced styling
        JLabel lblMap = new JLabel("Maze File:");
        lblMap.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblMap.setForeground(new Color(31, 41, 55));
        
        File dataDir = new File("data");
        String[] files = dataDir.list((dir, name) -> name.endsWith(".txt"));
        
        if (files != null) {
            Arrays.sort(files, (s1, s2) -> {
                int n1 = extractNumber(s1);
                int n2 = extractNumber(s2);
                return Integer.compare(n1, n2);
            });
        }
        
        fileSelector = new JComboBox<>(files != null ? files : new String[]{"No files"});
        fileSelector.setPreferredSize(new Dimension(180, 36));
        fileSelector.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        fileSelector.setBackground(Color.WHITE);
        
        // Enhanced control buttons
        btnLoad = createStyledButton("▶  Load & Run", ACCENT_COLOR);
        btnLoad.addActionListener(e -> startDemo());
        
        btnCancel = createStyledButton("■  Cancel", ERROR_COLOR);
        btnCancel.setEnabled(false);
        btnCancel.addActionListener(e -> cancelDemo());

        leftPanel.add(lblMap);
        leftPanel.add(fileSelector);
        leftPanel.add(btnLoad);
        leftPanel.add(btnCancel);

        // Speed controls with better styling
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        rightPanel.setBackground(CONTROL_BG);

        JLabel lblSpeed = new JLabel("Animation Speed:");
        lblSpeed.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSpeed.setForeground(new Color(31, 41, 55));
        
        JSlider speedSlider = new JSlider(0, 100, 20);
        speedSlider.setInverted(true);
        speedSlider.setPreferredSize(new Dimension(120, 36));
        speedSlider.setBackground(CONTROL_BG);
        speedSlider.setOpaque(true);
        speedSlider.addChangeListener(e -> {
            if (!skipAnimationCheck.isSelected()) {
                delay = speedSlider.getValue();
            }
        });
        
        skipAnimationCheck = new JCheckBox("Skip Animation");
        skipAnimationCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        skipAnimationCheck.setForeground(new Color(55, 65, 81));
        skipAnimationCheck.setBackground(CONTROL_BG);
        skipAnimationCheck.setFocusPainted(false);
        skipAnimationCheck.addActionListener(e -> {
            delay = skipAnimationCheck.isSelected() ? 0 : speedSlider.getValue();
        });

        JButton btnBenchmark = createStyledButton("≡  Run Benchmark", new Color(139, 92, 246));
        btnBenchmark.addActionListener(e -> runBenchmark());

        rightPanel.add(lblSpeed);
        rightPanel.add(speedSlider);
        rightPanel.add(skipAnimationCheck);
        rightPanel.add(btnBenchmark);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        return mainPanel;
    }

    private JButton createStyledButton(String text, Color baseColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(baseColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(150, 36));
        button.setMinimumSize(new Dimension(150, 36));
        button.setMaximumSize(new Dimension(150, 36));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalColor = baseColor;
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(baseColor.brighter());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
        
        return button;
    }

    private JPanel createVisualizationGrid() {
        JPanel gridPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        gridPanel.setBackground(PRIMARY_BG);
        gridPanel.setBorder(new EmptyBorder(16, 16, 8, 16));
        
        panels = new MazePanel[4];
        statusLabels = new JLabel[4];
        
        String[] headers = {"A* (Manhattan)", "Dijkstra", "Genetic Algorithm", "Memetic Algorithm"};
        String[] descriptions = {"Optimal path with heuristic", "Guaranteed shortest path", "Evolutionary optimization", "Hybrid local search"};
        Color[] headerColors = {
            new Color(37, 99, 235),    // Blue
            new Color(139, 92, 246),   // Purple
            new Color(234, 179, 8),    // Yellow
            new Color(249, 115, 22)    // Orange
        };
        
        for (int i = 0; i < 4; i++) {
            JPanel container = new JPanel(new BorderLayout());
            container.setBackground(Color.WHITE);
            container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));
            
            // Enhanced header with gradient-like appearance
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(headerColors[i]);
            headerPanel.setBorder(new EmptyBorder(14, 12, 14, 12));
            
            JLabel headerTitle = new JLabel(headers[i]);
            headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
            headerTitle.setForeground(Color.WHITE);
            
            JLabel headerDesc = new JLabel(descriptions[i]);
            headerDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            headerDesc.setForeground(new Color(255, 255, 255, 200));
            
            JPanel headerTextPanel = new JPanel(new GridLayout(2, 1, 0, 2));
            headerTextPanel.setOpaque(false);
            headerTextPanel.add(headerTitle);
            headerTextPanel.add(headerDesc);
            
            headerPanel.add(headerTextPanel, BorderLayout.WEST);
            
            panels[i] = new MazePanel();
            
            // Enhanced status panel with metrics
            statusLabels[i] = new JLabel("<html><div style='text-align:center; padding:8px;'>" +
                "<span style='color:#6B7280; font-size:12px;'>Ready to solve</span></div></html>");
            statusLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));
            statusLabels[i].setHorizontalAlignment(SwingConstants.CENTER);
            statusLabels[i].setOpaque(true);
            statusLabels[i].setBackground(new Color(249, 250, 251));
            statusLabels[i].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(229, 231, 235)),
                new EmptyBorder(12, 10, 12, 10)
            ));
            
            container.add(headerPanel, BorderLayout.NORTH);
            container.add(panels[i], BorderLayout.CENTER);
            container.add(statusLabels[i], BorderLayout.SOUTH);
            gridPanel.add(container);
        }
        
        return gridPanel;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(31, 41, 55));
        footer.setBorder(new EmptyBorder(12, 20, 12, 20));
        
        JLabel footerText = new JLabel("CPE231 Maze Solver • Algorithm Performance Comparison Tool");
        footerText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footerText.setForeground(new Color(156, 163, 175));
        
        JLabel versionText = new JLabel("Version 1.0");
        versionText.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        versionText.setForeground(new Color(107, 114, 128));
        
        footer.add(footerText, BorderLayout.WEST);
        footer.add(versionText, BorderLayout.EAST);
        
        return footer;
    }

    public void runDemo() {
        setVisible(true);
        if (fileSelector.getItemCount() > 0) {
            loadMap((String) fileSelector.getSelectedItem());
        }
    }

    private void loadMap(String filename) {
        if (filename == null || filename.equals("No files")) return;
        try {
            MazeLoader.loadMaze("data/" + filename);
            currentContext = new MazeContext(
                MazeLoader.maze, 
                MazeLoader.startRow, MazeLoader.startCol, 
                MazeLoader.endRow, MazeLoader.endCol
            );
            for (MazePanel p : panels) {
                p.setMaze(currentContext);
                p.setPath(null);
                p.repaint();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to load maze: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startDemo() {
        if (isRunning.get()) return;
        loadMap((String) fileSelector.getSelectedItem());
        if (currentContext == null) return;
        
        isRunning.set(true);
        btnLoad.setEnabled(false);
        btnCancel.setEnabled(true);
        fileSelector.setEnabled(false);
        
        MazeSolver[] solvers = {
            new AStarSolver(),
            new DijkstraSolver(),
            new GeneticSolverPureV1(),
            new HybridGeneticSolver()
        };

        for (int i = 0; i < 4; i++) {
            statusLabels[i].setText("<html><div style='text-align:center; padding:8px;'>" +
                "<span style='color:#F59E0B; font-weight:bold; font-size:13px;'>● Processing...</span><br>" +
                "<span style='color:#9CA3AF; font-size:11px;'>Computing optimal path</span></div></html>");
            panels[i].setPath(null);
            new AlgorithmWorker(solvers[i], i).execute();
        }
    }

    private void cancelDemo() {
        isRunning.set(false);
        btnLoad.setEnabled(true);
        btnCancel.setEnabled(false);
        fileSelector.setEnabled(true);
    }
    
    private void runBenchmark() {
        new Thread(cpe231.maze.benchmark.Benchmark::runBenchmarkSuite).start();
    }

    private class AlgorithmWorker extends SwingWorker<AlgorithmResult, List<int[]>> {
        private final MazeSolver solver;
        private final int panelIndex;
        
        AlgorithmWorker(MazeSolver solver, int panelIndex) {
            this.solver = solver;
            this.panelIndex = panelIndex;
        }
        
        @Override
        protected AlgorithmResult doInBackground() throws Exception {
            Thread.sleep(panelIndex * 100); 
            AlgorithmResult result = solver.solve(currentContext);
            
            if (!skipAnimationCheck.isSelected() && result.path() != null) {
                List<int[]> animatedPath = new ArrayList<>();
                for (int[] step : result.path()) {
                    if (!isRunning.get()) break;
                    animatedPath.add(step);
                    publish(new ArrayList<>(animatedPath)); 
                    Thread.sleep(Math.max(1, delay));
                }
            }
            return result;
        }
        
        @Override
        protected void process(List<List<int[]>> chunks) {
            if (isRunning.get() && !chunks.isEmpty()) {
                panels[panelIndex].setPath(chunks.get(chunks.size() - 1));
            }
        }
        
        @Override
        protected void done() {
            try {
                AlgorithmResult result = get();
                panels[panelIndex].setPath(result.path());
                
                String statusColor = result.isSuccess() ? "#10B981" : "#EF4444";
                String statusIcon = result.isSuccess() ? "✓" : "✗";
                String statusText = result.isSuccess() ? "SUCCESS" : "FAILED";
                
                statusLabels[panelIndex].setText(String.format(
                    "<html><div style='text-align:center; padding:8px;'>" +
                    "<div style='color:%s; font-weight:bold; font-size:14px; margin-bottom:6px;'>%s %s</div>" +
                    "<div style='background:#F9FAFB; padding:8px; border-radius:4px; margin:4px 0;'>" +
                    "<table style='width:100%%; font-size:11px;'>" +
                    "<tr><td style='color:#6B7280; text-align:left;'>Path Cost:</td><td style='color:#111827; text-align:right; font-weight:bold;'>%d</td></tr>" +
                    "<tr><td style='color:#6B7280; text-align:left;'>Execution:</td><td style='color:#111827; text-align:right; font-weight:bold;'>%.2f ms</td></tr>" +
                    "<tr><td style='color:#6B7280; text-align:left;'>Explored:</td><td style='color:#111827; text-align:right; font-weight:bold;'>%,d nodes</td></tr>" +
                    "</table></div></div></html>",
                    statusColor, statusIcon, statusText, result.cost(), result.getDurationMs(), result.nodesExpanded()));
            } catch (Exception e) {
                statusLabels[panelIndex].setText(
                    "<html><div style='text-align:center; color:#EF4444; font-weight:bold; padding:8px;'>✗ ERROR</div></html>");
                e.printStackTrace();
            } finally {
                checkAllComplete();
            }
        }
    }

    private synchronized void checkAllComplete() {
        javax.swing.Timer timer = new javax.swing.Timer(500, e -> {
            btnLoad.setEnabled(true);
            btnCancel.setEnabled(false);
            fileSelector.setEnabled(true);
            isRunning.set(false);
        });
        timer.setRepeats(false);
        timer.start();
    }

    private int extractNumber(String s) {
        String num = s.replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }
}