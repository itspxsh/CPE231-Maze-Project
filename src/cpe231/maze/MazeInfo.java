package cpe231.maze;

// ใช้ Record เพื่อรวบรวม Maze, Start, End
public record MazeInfo(int[][] maze, Coordinate start, Coordinate end) {
}