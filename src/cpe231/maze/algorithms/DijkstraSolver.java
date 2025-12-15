package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

public class DijkstraSolver implements MazeSolver {

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long startTime = System.nanoTime();
        
        int rows = context.rows;
        int cols = context.cols;
        int startIdx = context.getStartIndex();
        int endIdx = context.getEndIndex();
        int[][] grid = context.getGridDirect();
        int numNodes = rows * cols;

        // Validations
        if (grid == null || grid.length == 0) {
            return new AlgorithmResult("Error", new ArrayList<>(), -1, 0, 0);
        }

        // Data Structures
        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.cost));
        int[] dist = new int[numNodes];
        int[] parent = new int[numNodes];
        
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        // Initialization
        dist[startIdx] = 0;
        pq.add(new Node(startIdx, 0));
        
        long nodesExpanded = 0;
        
        // Directions: Up, Down, Left, Right
        int[] dr = {-1, 1, 0, 0};
        int[] dc = {0, 0, -1, 1};

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int u = current.id;
            
            // Lazy Deletion: If we found a shorter way to 'u' already, skip this old entry
            if (current.cost > dist[u]) continue;
            
            nodesExpanded++;

            // Goal Check
            if (u == endIdx) {
                return new AlgorithmResult(
                    "Success", 
                    reconstructPath(parent, u, cols), 
                    dist[u], 
                    System.nanoTime() - startTime, 
                    nodesExpanded
                );
            }

            int r = u / cols;
            int c = u % cols;

            // Explore Neighbors
            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i];
                int nc = c + dc[i];
                
                // Boundary and Wall Check
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && grid[nr][nc] != -1) {
                    int v = nr * cols + nc;
                    int weight = grid[nr][nc]; // Dijkstra uses actual weights (1, 2, 5, etc.)
                    int newDist = dist[u] + weight;
                    
                    if (newDist < dist[v]) {
                        dist[v] = newDist;
                        parent[v] = u;
                        pq.add(new Node(v, newDist));
                    }
                }
            }
        }

        // Path not found
        return new AlgorithmResult("Failed", new ArrayList<>(), -1, System.nanoTime() - startTime, nodesExpanded);
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

    // Helper Record for PriorityQueue
    record Node(int id, int cost) {}
}