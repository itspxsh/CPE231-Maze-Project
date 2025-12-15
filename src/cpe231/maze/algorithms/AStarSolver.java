// src/cpe231/maze/algorithms/AStarSolver.java
package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class AStarSolver implements MazeSolver {
    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        int rows = context.rows;
        int cols = context.cols;
        int startIdx = context.getStartIndex();
        int endIdx = context.getEndIndex();
        int[][] grid = context.getGridDirect();

        // Safety: If maze not loaded
        if (grid == null || grid.length == 0) return new AlgorithmResult("Error", new ArrayList<>(), -1, 0, 0);

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.f));
        int[] dist = new int[rows * cols];
        int[] parent = new int[rows * cols];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        dist[startIdx] = 0;
        pq.add(new Node(startIdx, 0 + heuristic(context, startIdx)));
        
        long nodesExpanded = 0;

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int u = current.id;
            nodesExpanded++;

            if (u == endIdx) {
                return new AlgorithmResult(
                    "Success", 
                    reconstructPath(parent, u, cols), 
                    dist[u], 
                    System.nanoTime() - startTime, 
                    nodesExpanded
                );
            }

            if (current.f > dist[u] + heuristic(context, u)) continue;

            int r = u / cols;
            int c = u % cols;
            
            // Neighbors: Up, Down, Left, Right
            int[] dr = {-1, 1, 0, 0};
            int[] dc = {0, 0, -1, 1};

            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];
                
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] != -1) {
                    int v = nr * cols + nc;
                    int weight = grid[nr][nc]; // Use cell cost (1 or higher)
                    if (dist[u] + weight < dist[v]) {
                        dist[v] = dist[u] + weight;
                        parent[v] = u;
                        pq.add(new Node(v, dist[v] + heuristic(context, v)));
                    }
                }
            }
        }
        
        return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, nodesExpanded);
    }

    private int heuristic(MazeContext ctx, int idx) {
        int r = idx / ctx.cols;
        int c = idx % ctx.cols;
        return Math.abs(r - ctx.endRow) + Math.abs(c - ctx.endCol);
    }

    private List<int[]> reconstructPath(int[] parent, int curr, int cols) {
        List<int[]> path = new ArrayList<>();
        while (curr != -1) {
            path.add(new int[]{curr / cols, curr % cols});
            curr = parent[curr];
        }
        Collections.reverse(path);
        return path;
    }

    record Node(int id, int f) {}
}