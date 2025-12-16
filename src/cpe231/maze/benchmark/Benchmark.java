package cpe231.maze.benchmark;

import cpe231.maze.algorithms.*;
import cpe231.maze.core.*;
import cpe231.maze.io.MazeLoader;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Benchmark {
    
    private static JDialog benchmarkDialog = null;
    private static JTable resultTable;
    private static DefaultTableModel tableModel;
    private static JProgressBar progressBar;
    private static JButton exportButton;
    
    // Number of times to run each algorithm for averaging
    private static final int ITERATIONS = 10;
    
    private static final Color PRIMARY_BG = new Color(240, 242, 245);
    private static final Color ACCENT_COLOR = new Color(37, 99, 235);
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129);
    private static final Color ERROR_COLOR = new Color(239, 68, 68);

    public static void main(String[] args) {
        Benchmark.runBenchmarkSuite();
    }
    
    public static void runBenchmarkSuite() {
        if (benchmarkDialog == null) {
            createBenchmarkDialog();
        }
        
        benchmarkDialog.setVisible(true);
        runBenchmarkInBackground();
    }
    
    private static void createBenchmarkDialog() {
        benchmarkDialog = new JDialog((Frame)null, "Benchmark Results (Average of " + ITERATIONS + " Runs)", false);
        benchmarkDialog.setLayout(new BorderLayout());
        benchmarkDialog.setSize(1100, 700);
        benchmarkDialog.setLocationRelativeTo(null);
        benchmarkDialog.getContentPane().setBackground(PRIMARY_BG);
        
        // Enhanced Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(31, 41, 55));
        headerPanel.setBorder(new EmptyBorder(20, 24, 20, 24));
        
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 4));
        titlePanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Algorithm Benchmark Results");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("Average performance over " + ITERATIONS + " runs per map");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(156, 163, 175));
        
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        
        benchmarkDialog.add(headerPanel, BorderLayout.NORTH);
        
        // Enhanced Table setup
        String[] columns = {"Map", "Algorithm", "Status", "Avg Time (ms)", "Avg Cost", "Avg Nodes", "Avg Path"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resultTable.setRowHeight(32);
        resultTable.setShowGrid(false);
        resultTable.setIntercellSpacing(new Dimension(0, 0));
        resultTable.setSelectionBackground(new Color(37, 99, 235, 30));
        resultTable.setSelectionForeground(Color.BLACK);
        resultTable.setAutoCreateRowSorter(true);
        
        // Styled table header
        JTableHeader header = resultTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(249, 250, 251));
        header.setForeground(new Color(31, 41, 55));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(229, 231, 235)));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        
        // Column widths
        resultTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        resultTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        resultTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        resultTable.getColumnModel().getColumn(6).setPreferredWidth(110);
        
        // Enhanced status column renderer
        resultTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                setHorizontalAlignment(SwingConstants.CENTER);
                
                if (value != null) {
                    String status = value.toString();
                    if (status.contains("SUCCESS")) {
                        setText(status);
                        setForeground(SUCCESS_COLOR);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setText(status);
                        setForeground(ERROR_COLOR);
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                
                // Alternating row colors
                if (!isSelected) {
                    if (row % 2 == 0) {
                        setBackground(Color.WHITE);
                    } else {
                        setBackground(new Color(249, 250, 251));
                    }
                }
                return c;
            }
        });
        
        // Enhanced cell renderer for other columns
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    if (row % 2 == 0) {
                        setBackground(Color.WHITE);
                    } else {
                        setBackground(new Color(249, 250, 251));
                    }
                }
                
                setForeground(new Color(31, 41, 55));
                
                // Center align numeric columns
                if (column >= 3) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                    setFont(new Font("Segoe UI", Font.PLAIN, 12));
                }
                
                return c;
            }
        };
        
        for (int i = 0; i < 7; i++) {
            if (i != 2) { // Skip status column
                resultTable.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
            }
        }
        
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(12, 16, 0, 16));
        scrollPane.getViewport().setBackground(Color.WHITE);
        benchmarkDialog.add(scrollPane, BorderLayout.CENTER);
        
        // Enhanced Bottom panel
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(new EmptyBorder(12, 16, 16, 16));
        
        // Progress section
        JPanel progressSection = new JPanel(new BorderLayout());
        progressSection.setBackground(Color.WHITE);
        
        // Styled progress bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
        progressBar.setPreferredSize(new Dimension(0, 28));
        progressBar.setForeground(ACCENT_COLOR);
        progressBar.setBackground(new Color(229, 231, 235));
        progressBar.setBorderPainted(false);
        
        JPanel progressWrapper = new JPanel(new BorderLayout());
        progressWrapper.setBackground(Color.WHITE);
        progressWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 2),
            BorderFactory.createEmptyBorder(3, 3, 3, 3)
        ));
        progressWrapper.add(progressBar);
        
        progressSection.add(progressWrapper, BorderLayout.CENTER);
        bottomPanel.add(progressSection, BorderLayout.NORTH);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBackground(Color.WHITE);
        
        exportButton = createStyledButton("↓ Export CSV", SUCCESS_COLOR);
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportToCSV());
        
        JButton clearButton = createStyledButton("✕ Clear", new Color(107, 114, 128));
        clearButton.addActionListener(e -> {
            tableModel.setRowCount(0);
            exportButton.setEnabled(false);
            progressBar.setValue(0);
            progressBar.setString("Ready to benchmark");
        });
        
        JButton closeButton = createStyledButton("Close", new Color(55, 65, 81));
        closeButton.addActionListener(e -> benchmarkDialog.setVisible(false));
        
        buttonPanel.add(exportButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(closeButton);
        
        bottomPanel.add(buttonPanel, BorderLayout.CENTER);
        benchmarkDialog.add(bottomPanel, BorderLayout.SOUTH);
    }
    
    private static JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(140, 34));
        button.setMinimumSize(new Dimension(140, 34));
        button.setMaximumSize(new Dimension(140, 34));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            Color originalColor = color;
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setBackground(color.brighter());
                }
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
        
        return button;
    }
    
    private static void runBenchmarkInBackground() {
        new Thread(() -> {
            try {
                System.out.println("\n=== BENCHMARK STARTED (Averaging " + ITERATIONS + " runs) ===");
                
                File folder = new File("data");
                File[] files = folder.listFiles((d, name) -> name.endsWith(".txt"));
                
                if (files == null || files.length == 0) {
                    JOptionPane.showMessageDialog(benchmarkDialog, 
                        "No maze files found in 'data' folder", 
                        "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                Arrays.sort(files, (f1, f2) -> {
                    int n1 = extractNumber(f1.getName());
                    int n2 = extractNumber(f2.getName());
                    return Integer.compare(n1, n2);
                });

                MazeSolver[] solvers = {
                    new AStarSolver(),
                    new DijkstraSolver(),
                    // new GeneticSolverPureV1(),
                    // new GeneticSolver(),
                    // new GeneticSolverV10(), // Added your new Adaptive solver here
                    new GeneticSolverAdaptive()
                };

                // Total steps = Files * Solvers (we process 10 runs internally as one step)
                int totalSteps = files.length * solvers.length;
                int stepCount = 0;
                
                SwingUtilities.invokeLater(() -> {
                    progressBar.setMaximum(totalSteps);
                    progressBar.setValue(0);
                    progressBar.setString("Starting average benchmark...");
                    tableModel.setRowCount(0);
                });

                for (File file : files) {
                    try {
                        MazeLoader.loadMaze(file.getPath());
                        
                        System.out.println("Benchmarking: " + file.getName());
                        
                        for (MazeSolver solver : solvers) {
                            
                            // --- Averaging Logic Variables ---
                            double totalTime = 0;
                            long totalCost = 0;
                            long totalNodes = 0;
                            long totalPathLen = 0;
                            int successfulRuns = 0;
                            
                            String algoName = solver.getClass().getSimpleName()
                                    .replace("Solver", "")
                                    .replace("Pure", "");

                            // Run 10 times
                            for (int i = 0; i < ITERATIONS; i++) {
                                // IMPORTANT: Create a fresh Context for every run to ensure no shared state
                                MazeContext ctx = new MazeContext(
                                    MazeLoader.maze, 
                                    MazeLoader.startRow, MazeLoader.startCol, 
                                    MazeLoader.endRow, MazeLoader.endCol
                                );
                                
                                try {
                                    AlgorithmResult result = solver.solve(ctx);
                                    if (result.isSuccess()) {
                                        totalTime += result.getDurationMs();
                                        totalCost += result.cost();
                                        totalNodes += result.nodesExpanded();
                                        totalPathLen += result.path().size();
                                        successfulRuns++;
                                    }
                                } catch (Exception e) {
                                    // Siently fail individual runs or log if needed
                                }
                            }
                            
                            // Calculate Averages
                            final Object[] rowData;
                            if (successfulRuns > 0) {
                                double avgTime = totalTime / successfulRuns;
                                double avgCost = (double) totalCost / successfulRuns;
                                double avgNodes = (double) totalNodes / successfulRuns;
                                double avgPath = (double) totalPathLen / successfulRuns;
                                
                                rowData = new Object[]{
                                    file.getName(),
                                    algoName,
                                    "SUCCESS (" + successfulRuns + "/" + ITERATIONS + ")",
                                    String.format("%.2f", avgTime),
                                    String.format("%.1f", avgCost),
                                    String.format("%,.0f", avgNodes),
                                    String.format("%.1f", avgPath)
                                };
                            } else {
                                rowData = new Object[]{
                                    file.getName(),
                                    algoName,
                                    "FAILED",
                                    "-", "-", "-", "-"
                                };
                            }

                            final int currentStep = ++stepCount;
                            SwingUtilities.invokeLater(() -> {
                                tableModel.addRow(rowData);
                                progressBar.setValue(currentStep);
                                progressBar.setString(String.format("Processed %d/%d (Map: %s)", 
                                    currentStep, totalSteps, file.getName()));
                            });
                            
                            // Tiny delay to keep UI responsive
                            Thread.sleep(20); 
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error loading " + file.getName() + ": " + e.getMessage());
                    }
                }
                
                SwingUtilities.invokeLater(() -> {
                    progressBar.setString("✓ Average Benchmark Complete");
                    exportButton.setEnabled(true);
                    
                    JOptionPane.showMessageDialog(benchmarkDialog, 
                        "Benchmark Suite Completed!\nResults are averaged over " + ITERATIONS + " runs.", 
                        "Done", 
                        JOptionPane.INFORMATION_MESSAGE);
                });
                
                System.out.println("=== BENCHMARK COMPLETE ===\n");
                
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(benchmarkDialog, 
                        "Critical Error: " + e.getMessage(), 
                        "Error", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
    
    private static void exportToCSV() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Averaged Benchmark Results");
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setSelectedFile(new File("benchmark_avg_results_" + timestamp + ".csv"));
        
        int userSelection = fileChooser.showSaveDialog(benchmarkDialog);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            
            try (PrintWriter writer = new PrintWriter(fileToSave)) {
                // CSV Header
                writer.println("Map,Algorithm,Status,Avg_Time_ms,Avg_Cost,Avg_Nodes_Expanded,Avg_Path_Length");
                
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        Object value = tableModel.getValueAt(i, j);
                        // Clean up strings for CSV (remove formatting commas/status text)
                        String strValue = value != null ? value.toString() : "";
                        strValue = strValue.replace(",", ""); // Remove thousand separators
                        
                        // Keep success status clean in CSV
                        if (j == 2 && strValue.contains("SUCCESS")) {
                            strValue = "SUCCESS"; 
                        }
                        
                        writer.print(strValue);
                        if (j < tableModel.getColumnCount() - 1) {
                            writer.print(",");
                        }
                    }
                    writer.println();
                }
                
                JOptionPane.showMessageDialog(benchmarkDialog, 
                    "Export Successful!\n" + fileToSave.getAbsolutePath(), 
                    "Export Complete", 
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(benchmarkDialog, 
                    "Error saving file: " + e.getMessage(), 
                    "Export Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private static int extractNumber(String s) {
        String num = s.replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }
}