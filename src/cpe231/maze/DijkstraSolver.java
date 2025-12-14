package cpe231.maze;

import java.util.*;

public class DijkstraSolver implements MazeSolver {

    @Override
    public AlgorithmResult solve(MazeContext maze) {
        long start = System.nanoTime();
        
        int rows = maze.rows;
        int cols = maze.cols;
        int numNodes = rows * cols;
        int startIdx = maze.getStartIndex();
        int endIdx = maze.getEndIndex();
        int[][] grid = maze.getGrid();

        // 1. Flatten Grid
        int[] flatMaze = new int[numNodes];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(grid[i], 0, flatMaze, i * cols, cols);
        }

        int[] dist = new int[numNodes];
        int[] parent = new int[numNodes];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        // 2. Custom Heap
        int[] heapIndex = new int[numNodes * 4]; 
        int[] heapCost = new int[numNodes * 4];
        int heapSize = 0;

        dist[startIdx] = 0;
        heapIndex[0] = startIdx;
        heapCost[0] = 0;
        heapSize++;

        long nodesExp = 0;

        while (heapSize > 0) {
            int currIdx = heapIndex[0];
            int currCost = heapCost[0];
            
            // Heap Pop
            heapSize--;
            if (heapSize > 0) {
                int lastIndex = heapIndex[heapSize];
                int lastCost = heapCost[heapSize];
                int i = 0, half = heapSize >>> 1;
                while (i < half) {
                    int child = (i << 1) + 1;
                    int right = child + 1;
                    if (right < heapSize && heapCost[right] < heapCost[child]) child = right;
                    if (lastCost <= heapCost[child]) break;
                    heapIndex[i] = heapIndex[child]; heapCost[i] = heapCost[child]; i = child;
                }
                heapIndex[i] = lastIndex; heapCost[i] = lastCost;
            }

            nodesExp++;
            if (currCost > dist[currIdx]) continue;
            if (currIdx == endIdx) {
                return new AlgorithmResult("Dijkstra (Optimized)", reconstruct(parent, cols, endIdx), dist[endIdx], System.nanoTime() - start, nodesExp);
            }

            // Neighbors: Up, Down, Left, Right
            int[] neighbors = {currIdx - cols, currIdx + cols, currIdx - 1, currIdx + 1};
            for (int k = 0; k < 4; k++) {
                int nIdx = neighbors[k];
                boolean isValid = false;
                if (k == 0 && currIdx >= cols) isValid = true; 
                else if (k == 1 && currIdx < numNodes - cols) isValid = true;
                else if (k == 2 && currIdx % cols != 0) isValid = true;
                else if (k == 3 && (currIdx + 1) % cols != 0) isValid = true;
                
                if (isValid && flatMaze[nIdx] != -1) {
                    int newCost = currCost + flatMaze[nIdx];
                    if (newCost < dist[nIdx]) {
                        dist[nIdx] = newCost;
                        parent[nIdx] = currIdx;
                        // Heap Push
                        int i = heapSize++;
                        while (i > 0) {
                            int p = (i - 1) >>> 1;
                            if (heapCost[p] <= newCost) break;
                            heapIndex[i] = heapIndex[p]; heapCost[i] = heapCost[p]; i = p;
                        }
                        heapIndex[i] = nIdx; heapCost[i] = newCost;
                    }
                }
            }
        }
        return new AlgorithmResult("Dijkstra (Optimized)", new ArrayList<>(), -1, System.nanoTime() - start, nodesExp);
    }

    private List<int[]> reconstruct(int[] parent, int cols, int curr) {
        List<int[]> path = new ArrayList<>();
        while (curr != -1) {
            path.add(new int[]{curr / cols, curr % cols});
            curr = parent[curr];
        }
        Collections.reverse(path);
        return path;
    }
}