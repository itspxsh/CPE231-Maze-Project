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
import java.util.concurrent.atomic.AtomicInteger;

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

    // Synchronization barriers
    private AlgorithmResult[] resultsBuffer;
    private AtomicInteger completedCount;

    // Colors
    private static final Color PRIMARY_BG = new Color(240, 242, 245);
    private static final Color CONTROL_BG = new Color(255, 255, 255);
    private static final Color ACCENT_COLOR = new Color(37, 99, 235);
    private static final Color ERROR_COLOR = new Color(239, 68, 68);

    public VisualizationApp() {
        setTitle("CPE231 Maze Pathfinding - Algorithm Comparison");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(PRIMARY_BG);

        add(createControlPanel(), BorderLayout.NORTH);
        add(createVisualizationGrid(), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);

        setSize(1400, 800);
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

        JLabel lblMap = new JLabel("Maze File:");
        lblMap.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        File dataDir = new File("data");
        if (!dataDir.exists()) dataDir.mkdir();
        
        String[] files = dataDir.list((dir, name) -> name.endsWith(".txt"));
        
        if (files != null) {
            Arrays.sort(files, (s1, s2) -> {
                int n1 = extractNumber(s1);
                int n2 = extractNumber(s2);
                return Integer.compare(n1, n2);
            });
        }
        
        fileSelector = new JComboBox<>(files != null && files.length > 0 ? files : new String[]{"No files"});
        fileSelector.setPreferredSize(new Dimension(180, 36));
        fileSelector.setBackground(Color.WHITE);
        
        btnLoad = createStyledButton("▶  Load & Run", ACCENT_COLOR);
        btnLoad.addActionListener(e -> startDemo());
        
        btnCancel = createStyledButton("■  Cancel", ERROR_COLOR);
        btnCancel.setEnabled(false);
        btnCancel.addActionListener(e -> cancelDemo());

        leftPanel.add(lblMap);
        leftPanel.add(fileSelector);
        leftPanel.add(btnLoad);
        leftPanel.add(btnCancel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 5));
        rightPanel.setBackground(CONTROL_BG);

        JLabel lblSpeed = new JLabel("Animation Speed:");
        lblSpeed.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        JSlider speedSlider = new JSlider(0, 100, 20);
        speedSlider.setInverted(true); 
        speedSlider.setPreferredSize(new Dimension(120, 36));
        speedSlider.setBackground(CONTROL_BG);
        speedSlider.addChangeListener(e -> {
            if (!skipAnimationCheck.isSelected()) delay = speedSlider.getValue();
        });
        
        skipAnimationCheck = new JCheckBox("Skip Animation");
        skipAnimationCheck.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        skipAnimationCheck.setBackground(CONTROL_BG);
        skipAnimationCheck.setFocusPainted(false);
        skipAnimationCheck.addActionListener(e -> {
            delay = skipAnimationCheck.isSelected() ? 0 : speedSlider.getValue();
        });

        JButton btnBenchmark = createStyledButton("≡  Run Benchmark", new Color(139, 92, 246));
        btnBenchmark.addActionListener(e -> {
            try {
                Class.forName("cpe231.maze.benchmark.Benchmark")
                     .getMethod("runBenchmarkSuite")
                     .invoke(null);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Benchmark module not found.");
            }
        });

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
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalColor = baseColor;
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) button.setBackground(baseColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
        return button;
    }

    private JPanel createVisualizationGrid() {
        JPanel gridPanel = new JPanel(new GridLayout(1, 3, 16, 0));
        gridPanel.setBackground(PRIMARY_BG);
        gridPanel.setBorder(new EmptyBorder(16, 16, 8, 16));
        
        panels = new MazePanel[3];
        statusLabels = new JLabel[3];
        
        String[] headers = {"A* (Manhattan)", "Dijkstra", "Pure Genetic Algo"};
        String[] descriptions = {"Optimal path with heuristic", "Guaranteed shortest path", "Natural Selection Evolution"};
        Color[] headerColors = {
            new Color(37, 99, 235),
            new Color(139, 92, 246),
            new Color(234, 179, 8)
        };
        
        for (int i = 0; i < 3; i++) {
            JPanel container = new JPanel(new BorderLayout());
            container.setBackground(Color.WHITE);
            container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));
            
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
        footer.add(footerText, BorderLayout.WEST);
        return footer;
    }

    public void runDemo() {
        setVisible(true);
        if (fileSelector.getItemCount() > 0 && !fileSelector.getItemAt(0).equals("No files")) {
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
            JOptionPane.showMessageDialog(this, "Failed to load: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // PHASE 1: START PARALLEL COMPUTATIONS
    // =========================================================================
    private void startDemo() {
        if (isRunning.get()) return;
        loadMap((String) fileSelector.getSelectedItem());
        if (currentContext == null) return;
        
        isRunning.set(true);
        
        // Lock UI
        btnLoad.setEnabled(false);
        btnCancel.setEnabled(true);
        fileSelector.setEnabled(false);
        
        // Prepare Synchronization
        resultsBuffer = new AlgorithmResult[3];
        completedCount = new AtomicInteger(0);
        
        MazeSolver[] solvers = {
            new AStarSolver(),
            new DijkstraSolver(),
            new PureGASolver(),
        };

        // Launch 3 Silent Workers
        for (int i = 0; i < 3; i++) {
            statusLabels[i].setText("<html><div style='text-align:center; padding:8px;'>" +
                "<span style='color:#3B82F6; font-weight:bold; font-size:13px;'>● Calculating...</span><br>" +
                "<span style='color:#9CA3AF; font-size:11px;'>Please wait</span></div></html>");
            panels[i].setPath(null);
            
            // Start worker
            new ComputationWorker(solvers[i], i).execute();
        }
    }

    private void cancelDemo() {
        isRunning.set(false);
        btnLoad.setEnabled(true);
        btnCancel.setEnabled(false);
        fileSelector.setEnabled(true);
        // Reset labels
        for(JLabel l : statusLabels) l.setText("<html><div style='text-align:center; padding:8px;'><span style='color:#6B7280'>Cancelled</span></div></html>");
    }

    private int extractNumber(String s) {
        String num = s.replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }

    // =========================================================================
    // WORKER 1: PURE COMPUTATION (Silent, Background)
    // =========================================================================
    private class ComputationWorker extends SwingWorker<AlgorithmResult, Void> {
        private final MazeSolver solver;
        private final int index;

        ComputationWorker(MazeSolver solver, int index) {
            this.solver = solver;
            this.index = index;
        }

        @Override
        protected AlgorithmResult doInBackground() {
            // NOTE: calling solve() directly (not solveWithProgress) suppresses 
            // the GA evolution animation, keeping this phase strictly "Computing..."
            return solver.solve(currentContext);
        }

        @Override
        protected void done() {
            if (!isRunning.get()) return;
            try {
                // 1. Store Result
                resultsBuffer[index] = get();
                
                // 2. Check if all 3 are ready
                if (completedCount.incrementAndGet() == 3) {
                    startSynchronizedAnimation();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // =========================================================================
    // WORKER 2: SYNCHRONIZED ANIMATION (Single Driver for All Panels)
    // =========================================================================
    private void startSynchronizedAnimation() {
        if (!isRunning.get()) return;
        
        // Update Status to "Animating"
        for (int i = 0; i < 3; i++) {
            statusLabels[i].setText("<html><div style='text-align:center; padding:8px;'>" +
                "<span style='color:#8B5CF6; font-weight:bold; font-size:13px;'>▶ Animating</span><br>" +
                "<span style='color:#9CA3AF; font-size:11px;'>Visualizing Path</span></div></html>");
        }
        
        new SyncedAnimationWorker().execute();
    }

    private class SyncedAnimationWorker extends SwingWorker<Void, Integer> {
        
        @Override
        protected Void doInBackground() throws Exception {
            // Find longest path to know when to stop loop
            int maxSteps = 0;
            for (AlgorithmResult res : resultsBuffer) {
                if (res != null && res.path() != null) {
                    maxSteps = Math.max(maxSteps, res.path().size());
                }
            }

            // Quick skip if requested
            if (skipAnimationCheck.isSelected()) {
                publish(maxSteps); 
                return null;
            }

            // Small pause before action starts
            Thread.sleep(300);

            // Frame-by-frame loop
            for (int step = 0; step <= maxSteps; step++) {
                if (!isRunning.get()) break;
                
                publish(step);
                
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            }
            return null;
        }

        @Override
        protected void process(List<Integer> steps) {
            if (!isRunning.get()) return;
            int currentStep = steps.get(steps.size() - 1); // Get latest frame

            // Update all 3 panels to this frame
            for (int i = 0; i < 3; i++) {
                AlgorithmResult res = resultsBuffer[i];
                if (res != null && res.isSuccess()) {
                    List<int[]> fullPath = res.path();
                    int limit = Math.min(currentStep, fullPath.size());
                    // Create sublist for animation effect
                    panels[i].setPath(fullPath.subList(0, limit));
                }
            }
        }

        @Override
        protected void done() {
            // Finalize: Show stats and ensure full paths are drawn
            for (int i = 0; i < 3; i++) {
                AlgorithmResult result = resultsBuffer[i];
                if (result == null) continue;

                // Ensure full path is drawn
                if (result.path() != null) {
                    panels[i].setPath(result.path());
                }

                // Show Stats
                String statusColor = result.isSuccess() ? "#10B981" : "#EF4444";
                String statusIcon = result.isSuccess() ? "✓" : "✗";
                String statusText = result.isSuccess() ? "SUCCESS" : "FAILED";
                
                statusLabels[i].setText(String.format(
                    "<html><div style='text-align:center; padding:8px;'>" +
                    "<div style='color:%s; font-weight:bold; font-size:14px; margin-bottom:6px;'>%s %s</div>" +
                    "<div style='background:#F9FAFB; padding:8px; border-radius:4px; margin:4px 0;'>" +
                    "<table style='width:100%%; font-size:11px;'>" +
                    "<tr><td style='color:#6B7280; text-align:left;'>Path Cost:</td><td style='color:#111827; text-align:right; font-weight:bold;'>%d</td></tr>" +
                    "<tr><td style='color:#6B7280; text-align:left;'>Execution:</td><td style='color:#111827; text-align:right; font-weight:bold;'>%.2f ms</td></tr>" +
                    "<tr><td style='color:#6B7280; text-align:left;'>Explored:</td><td style='color:#111827; text-align:right; font-weight:bold;'>%,d nodes</td></tr>" +
                    "</table></div></div></html>",
                    statusColor, statusIcon, statusText, result.cost(), result.getDurationMs(), result.nodesExpanded()));
            }
            
            checkAllComplete();
        }
    }
    
    private synchronized void checkAllComplete() {
        btnLoad.setEnabled(true);
        btnCancel.setEnabled(false);
        fileSelector.setEnabled(true);
        isRunning.set(false);
    }
}