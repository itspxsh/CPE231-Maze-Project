package cpe231.maze.algorithms;
import cpe231.maze.core.*;
import java.util.*;

public class DijkstraSolver implements MazeSolver {
    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        int rows = context.rows, cols = context.cols;
        int start = context.startRow * cols + context.startCol;
        int end = context.endRow * cols + context.endCol;
        int[][] grid = context.getGridDirect();

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        int[] dist = new int[rows * cols];
        int[] parent = new int[rows * cols];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        dist[start] = 0;
        pq.add(new int[]{start, 0});
        
        long expanded = 0;
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        while (!pq.isEmpty()) {
            int[] curr = pq.poll();
            int u = curr[0];
            
            // CHANGE: Subtract goal tile cost to match the 1085 definition
            if (u == end) {
                int finalCost = dist[end] - grid[context.endRow][context.endCol];
                return new AlgorithmResult("Success", getPath(parent, end, cols), finalCost, System.nanoTime()-startTime, expanded);
            }
            
            if (curr[1] > dist[u]) continue;
            
            expanded++;
            for(int i=0; i<4; i++) {
                int nr = u/cols+dr[i], nc = u%cols+dc[i];
                if(nr>=0 && nr<rows && nc>=0 && nc<cols && grid[nr][nc]!=-1) {
                    int v = nr*cols+nc;
                    int newDist = dist[u] + grid[nr][nc];
                    if(newDist < dist[v]) {
                        dist[v] = newDist;
                        parent[v] = u;
                        pq.add(new int[]{v, newDist});
                    }
                }
            }
        }
        return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime()-startTime, expanded);
    }
    private List<int[]> getPath(int[] p, int end, int cols) {
        List<int[]> path = new ArrayList<>();
        for(int c=end; c!=-1; c=p[c]) path.add(new int[]{c/cols, c%cols});
        Collections.reverse(path);
        return path;
    }
}