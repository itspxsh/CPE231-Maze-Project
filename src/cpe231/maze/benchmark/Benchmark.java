package cpe231.maze.benchmark;

import cpe231.maze.algorithms.*;
import cpe231.maze.core.*;
import cpe231.maze.io.MazeLoader;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class Benchmark {
    
    private static JDialog benchmarkDialog = null;
    private static JTable resultTable;
    private static DefaultTableModel tableModel;
    private static JProgressBar progressBar;
    private static JButton exportButton;
    
    public static void runBenchmarkSuite() {
        if (benchmarkDialog == null) {
            createBenchmarkDialog();
        }
        benchmarkDialog.setVisible(true);
        runBenchmarkInBackground();
    }
    
    private static void createBenchmarkDialog() {
        benchmarkDialog = new JDialog((Frame)null, "Scientific Benchmark Results", false);
        benchmarkDialog.setLayout(new BorderLayout());
        
        tableModel = new DefaultTableModel(new String[]{"Map", "Algorithm", "Status", "Time(ms)", "Cost", "Nodes", "Len"}, 0);
        resultTable = new JTable(tableModel);
        
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        
        exportButton = new JButton("Export CSV");
        exportButton.setEnabled(false);
        exportButton.addActionListener(e -> exportResults());
        
        benchmarkDialog.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(exportButton, BorderLayout.SOUTH);
        
        benchmarkDialog.add(bottomPanel, BorderLayout.SOUTH);
        benchmarkDialog.setSize(900, 600);
        benchmarkDialog.setLocationRelativeTo(null);
    }
    
    private static void runBenchmarkInBackground() {
        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                File folder = new File("maps");
                File[] mapFiles = folder.listFiles((dir, name) -> name.endsWith(".txt"));
                
                if (mapFiles == null) return null;
                Arrays.sort(mapFiles, Comparator.comparingInt(f -> extractNumber(f.getName())));

                // ---------------------------------------------------------
                // DEFINING THE EXPERIMENTAL SUITE
                // ---------------------------------------------------------
                Map<String, MazeSolver> algorithms = new LinkedHashMap<>();
                algorithms.put("A* (Ref)", new AStarSolver());
                algorithms.put("GA-Baseline", new GABaseline());
                algorithms.put("GA-Adaptive", new GAAdaptive());
                algorithms.put("GA-Diversity", new GADiversity());
                algorithms.put("GA-Scaled", new GAScaled()); 
                // ---------------------------------------------------------

                progressBar.setMaximum(mapFiles.length * algorithms.size());
                int progress = 0;

                for (File mapFile : mapFiles) {
                    MazeContext context = MazeLoader.loadMaze(mapFile.getPath());
                    
                    for (Map.Entry<String, MazeSolver> entry : algorithms.entrySet()) {
                        String algoName = entry.getKey();
                        MazeSolver solver = entry.getValue();
                        
                        try {
                            AlgorithmResult result = solver.solve(context);
                            publish(new Object[]{
                                mapFile.getName(),
                                algoName,
                                result.success ? "✓" : "✗",
                                String.format("%.2f", result.timeTaken),
                                result.cost,
                                result.nodesExpanded,
                                result.path.size()
                            });
                        } catch (Exception e) {
                            publish(new Object[]{mapFile.getName(), algoName, "Error", 0, 0, 0, 0});
                        }
                        
                        progress++;
                        progressBar.setValue(progress);
                    }
                    publish(new Object[]{"", "", "", "", "", "", ""}); // Spacer
                }
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) tableModel.addRow(row);
            }

            @Override
            protected void done() {
                exportButton.setEnabled(true);
                JOptionPane.showMessageDialog(benchmarkDialog, "Benchmark Completed!");
            }
        }.execute();
    }
    
    private static void exportResults() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Benchmark Results");
        if (fileChooser.showSaveDialog(benchmarkDialog) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".csv");
            }
            try (PrintWriter writer = new PrintWriter(fileToSave)) {
                writer.println("Map,Algorithm,Status,Time_ms,Cost,Nodes_Expanded,Path_Length");
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) {
                        Object value = tableModel.getValueAt(i, j);
                        writer.print(value != null ? value.toString() : "");
                        if (j < tableModel.getColumnCount() - 1) writer.print(",");
                    }
                    writer.println();
                }
                JOptionPane.showMessageDialog(benchmarkDialog, "Saved to " + fileToSave.getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(benchmarkDialog, "Error saving: " + e.getMessage());
            }
        }
    }
    
    private static int extractNumber(String s) {
        String num = s.replaceAll("\\D", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }
}