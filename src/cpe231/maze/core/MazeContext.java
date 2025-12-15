package cpe231.maze.core;

/**
 * Immutable context object representing a maze problem instance.
 * Follows Data Transfer Object pattern for clean separation of concerns.
 * 
 * Design Principles:
 * - Immutability: Thread-safe, prevents accidental modification
 * - Encapsulation: Defensive copying of mutable grid
 * - Single Responsibility: Only holds maze state, no logic
 */
public final class MazeContext {
    private final int[][] grid;
    public final int rows;
    public final int cols;
    public final int startRow, startCol;
    public final int endRow, endCol;

    /**
     * Creates a new maze context with defensive copying.
     * 
     * @param grid 2D maze array where -1 = wall, >= 0 = traversal cost
     * @param startRow Starting row coordinate
     * @param startCol Starting column coordinate
     * @param endRow Goal row coordinate
     * @param endCol Goal column coordinate
     */
    public MazeContext(int[][] grid, int startRow, int startCol, int endRow, int endCol) {
        // Defensive copy to ensure immutability
        this.grid = new int[grid.length][];
        for (int i = 0; i < grid.length; i++) {
            this.grid[i] = grid[i].clone();
        }
        
        this.rows = grid.length;
        this.cols = grid[0].length;
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
    }

    /**
     * Returns a defensive copy of the grid to prevent external modification.
     */
    public int[][] getGrid() {
        int[][] copy = new int[rows][];
        for (int i = 0; i < rows; i++) {
            copy[i] = grid[i].clone();
        }
        return copy;
    }
    
    /**
     * Direct access for performance-critical algorithms.
     * WARNING: Callers must NOT modify the returned array.
     */
    public int[][] getGridDirect() {
        return grid;
    }
    
    /**
     * Converts 2D coordinates to 1D index for optimized algorithms.
     */
    public int getStartIndex() { 
        return startRow * cols + startCol; 
    }
    
    public int getEndIndex() { 
        return endRow * cols + endCol; 
    }
    
    /**
     * Validates if a position is within bounds and not a wall.
     */
    public boolean isValid(int row, int col) {
        return row >= 0 && row < rows && 
               col >= 0 && col < cols && 
               grid[row][col] != -1;
    }
    
    /**
     * Gets the cost of traversing a specific cell.
     * Returns -1 if wall or out of bounds.
     */
    public int getCost(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return -1;
        }
        return grid[row][col];
    }
    
    @Override
    public String toString() {
        return String.format("MazeContext[%dx%d, start=(%d,%d), end=(%d,%d)]",
            rows, cols, startRow, startCol, endRow, endCol);
    }
}
