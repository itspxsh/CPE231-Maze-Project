package cpe231.maze.core;

/**
 * Immutable context object representing a maze problem instance.
 * 
 * IMPROVEMENTS:
 * - Added validation for maze integrity
 * - Better encapsulation with truly immutable grid
 * - Added utility methods for algorithm implementations
 * - Fixed defensive copying performance issues
 */
public final class MazeContext {
    private final int[][] grid;
    public final int rows;
    public final int cols;
    public final int startRow, startCol;
    public final int endRow, endCol;

    /**
     * Creates a new maze context with validation and defensive copying.
     * 
     * @throws IllegalArgumentException if maze is invalid
     */
    public MazeContext(int[][] grid, int startRow, int startCol, int endRow, int endCol) {
        // Input validation
        if (grid == null || grid.length == 0) {
            throw new IllegalArgumentException("Grid cannot be null or empty");
        }
        if (grid[0] == null || grid[0].length == 0) {
            throw new IllegalArgumentException("Grid rows cannot be empty");
        }
        
        this.rows = grid.length;
        this.cols = grid[0].length;
        
        // Validate start/end positions
        if (startRow < 0 || startRow >= rows || startCol < 0 || startCol >= cols) {
            throw new IllegalArgumentException("Start position out of bounds");
        }
        if (endRow < 0 || endRow >= rows || endCol < 0 || endCol >= cols) {
            throw new IllegalArgumentException("End position out of bounds");
        }
        if (grid[startRow][startCol] == -1) {
            throw new IllegalArgumentException("Start position is on a wall");
        }
        if (grid[endRow][endCol] == -1) {
            throw new IllegalArgumentException("End position is on a wall");
        }
        
        // Defensive copy with validation
        this.grid = new int[rows][];
        for (int i = 0; i < rows; i++) {
            if (grid[i].length != cols) {
                throw new IllegalArgumentException("Inconsistent row length at row " + i);
            }
            this.grid[i] = grid[i].clone();
        }
        
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
    }

    /**
     * Returns defensive copy of grid (use sparingly - expensive operation)
     */
    public int[][] getGrid() {
        int[][] copy = new int[rows][];
        for (int i = 0; i < rows; i++) {
            copy[i] = grid[i].clone();
        }
        return copy;
    }
    
    /**
     * Direct read-only access for performance.
     * CALLERS MUST NOT MODIFY THE RETURNED ARRAY!
     */
    public int[][] getGridDirect() {
        return grid;
    }
    
    /**
     * Safe accessor for individual cells
     */
    public int getCell(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return -1; // Out of bounds = wall
        }
        return grid[row][col];
    }
    
    public int getStartIndex() { 
        return startRow * cols + startCol; 
    }
    
    public int getEndIndex() { 
        return endRow * cols + endCol; 
    }
    
    /**
     * Validates if a position is within bounds and not a wall
     */
    public boolean isValid(int row, int col) {
        return row >= 0 && row < rows && 
               col >= 0 && col < cols && 
               grid[row][col] != -1;
    }
    
    /**
     * Gets cost of traversing a cell (-1 if invalid)
     */
    public int getCost(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return -1;
        }
        return grid[row][col];
    }
    
    /**
     * Manhattan distance from any position to goal
     */
    public int manhattanToGoal(int row, int col) {
        return Math.abs(row - endRow) + Math.abs(col - endCol);
    }
    
    /**
     * Calculates minimum possible cost (for heuristic scaling)
     */
    public int getMinCellCost() {
        int min = Integer.MAX_VALUE;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c] > 0 && grid[r][c] < min) {
                    min = grid[r][c];
                }
            }
        }
        return min == Integer.MAX_VALUE ? 1 : min;
    }
    
    @Override
    public String toString() {
        return String.format("MazeContext[%dx%d, start=(%d,%d), end=(%d,%d)]",
            rows, cols, startRow, startCol, endRow, endCol);
    }
}