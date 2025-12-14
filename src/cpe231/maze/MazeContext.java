package cpe231.maze;

public class MazeContext {
    private final int[][] grid;
    public final int rows;
    public final int cols;
    public final int startRow, startCol;
    public final int endRow, endCol;

    public MazeContext(int[][] grid, int startRow, int startCol, int endRow, int endCol) {
        this.grid = grid;
        this.rows = grid.length;
        this.cols = grid[0].length;
        this.startRow = startRow;
        this.startCol = startCol;
        this.endRow = endRow;
        this.endCol = endCol;
    }

    public int[][] getGrid() {
        return grid;
    }
    
    // Helper เพื่อหา Index 1D
    public int getStartIndex() { return startRow * cols + startCol; }
    public int getEndIndex() { return endRow * cols + endCol; }
}