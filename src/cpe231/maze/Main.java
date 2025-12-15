package cpe231.maze;

import cpe231.maze.ui.VisualizationApp;

public class Main {
    public static void main(String[] args) {
        // Ensure GUI runs on Event Dispatch Thread
        javax.swing.SwingUtilities.invokeLater(() -> {
            VisualizationApp app = new VisualizationApp();
            app.runDemo(); // No arguments needed now
        });
    }
}