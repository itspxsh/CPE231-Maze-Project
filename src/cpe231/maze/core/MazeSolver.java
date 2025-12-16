package cpe231.maze.core;

/**
 * Strategy pattern interface for maze-solving algorithms.
 * 
 * Design Pattern: Strategy Pattern
 * Benefits:
 * - Easy to add new algorithms without modifying existing code (Open/Closed Principle)
 * - Algorithms are interchangeable at runtime
 * - Each algorithm is isolated in its own class (Single Responsibility)
 * 
 * Usage Example:
 * <pre>
 * MazeSolver solver = new AStarSolver();
 * AlgorithmResult result = solver.solve(context);
 * </pre>
 */
public interface MazeSolver {
    
    /**
     * Solves the maze problem defined by the context.
     * 
     * @param context Immutable maze problem instance
     * @return Result containing path, cost, and performance metrics
     */
    AlgorithmResult solve(MazeContext context);
    
    /**
     * Returns a human-readable name for this algorithm.
     * Used for display and logging purposes.
     */
    default String getName() {
        return this.getClass().getSimpleName()
            .replace("Solver", "")
            .replaceAll("([A-Z])", " $1")
            .trim();
    }
}