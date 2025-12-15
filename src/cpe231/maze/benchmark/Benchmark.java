package cpe231.maze.benchmark;

import cpe231.maze.algorithms.*;
import cpe231.maze.core.*;
import cpe231.maze.io.MazeLoader;
import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class Benchmark {
    public static void runBenchmarkSuite() {
        // Run in a separate thread so it doesn't freeze the GUI
        new Thread(() -> {
            System.out.println("\n=== BENCHMARK STARTED ===");
            File folder = new File("data");
            File[] files = folder.listFiles((d, name) -> name.endsWith(".txt"));
            
            if (files == null) return;
            
            // Natural Sort
            Arrays.sort(files, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return extractInt(f1.getName()) - extractInt(f2.getName());
                }
                int extractInt(String s) {
                    String num = s.replaceAll("\\D", "");
                    return num.isEmpty() ? 0 : Integer.parseInt(num);
                }
            });

            MazeSolver[] solvers = {
                new AStarSolver(),
                new DijkstraSolver(),
                new GeneticSolverPure(),   // Added back!
                new GeneticSolverAdaptive()
            };

            JFrame f = new JFrame("Benchmark Results");
            JTextArea area = new JTextArea();
            area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
            f.add(new JScrollPane(area));
            f.setSize(800, 600);
            f.setVisible(true);

            String header = String.format("%-15s | %-15s | %-10s | %-12s | %-8s\n", "Map", "Algorithm", "Time(ms)", "Nodes", "Cost");
            area.append(header);
            area.append("-".repeat(70) + "\n");

            for (File file : files) {
                try {
                    MazeLoader.loadMaze(file.getPath());
                    MazeContext ctx = new MazeContext(MazeLoader.maze, MazeLoader.startRow, MazeLoader.startCol, MazeLoader.endRow, MazeLoader.endCol);
                    
                    for (MazeSolver solver : solvers) {
                        AlgorithmResult res = solver.solve(ctx);
                        String name = solver.getClass().getSimpleName().replace("Solver", "");
                        if (name.length() > 15) name = name.substring(0, 15);
                        
                        String line = String.format("%-15s | %-15s | %-10.2f | %-12d | %-8d\n",
                            file.getName(), name, res.durationNs()/1_000_000.0, res.nodesExpanded(), res.cost());
                        
                        SwingUtilities.invokeLater(() -> area.append(line));
                    }
                    SwingUtilities.invokeLater(() -> area.append("\n"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}