package cpe231.maze;

import cpe231.maze.ui.VisualizationApp;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main entry point for CPE231 Maze Solver Project
 * 
 * @author Your Name
 * @version 1.0
 */
public class Main {
    public static void main(String[] args) {
        // Set system look and feel for better appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }
        
        // Launch GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            try {
                System.out.println("====================================");
                System.out.println("  CPE231 Maze Pathfinding Project");
                System.out.println("  Algorithm Comparison Tool");
                System.out.println("====================================");
                System.out.println();
                System.out.println("Starting application...");
                
                VisualizationApp app = new VisualizationApp();
                app.runDemo();
                
                System.out.println("Application launched successfully");
                System.out.println("- Load a maze file from the dropdown");
                System.out.println("- Click 'Load & Run' to start comparison");
                System.out.println("- Use 'Run Benchmark' for full analysis");
                System.out.println();
                
            } catch (Exception e) {
                System.err.println("Failed to start application:");
                e.printStackTrace();
                System.exit(1);
            }
        });
    }
}