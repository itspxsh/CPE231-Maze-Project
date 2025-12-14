package cpe231.maze;

import java.util.*;

public class AStarSolver implements MazeSolver {

    private class Node implements Comparable<Node> {
        int index, g, f;
        public Node(int index, int g, int f) {
            this.index = index; this.g = g; this.f = f;
        }
        public int compareTo(Node o) { return Integer.compare(this.f, o.f); }
    }

    @Override
    public AlgorithmResult solve(MazeContext context) {
        long start = System.nanoTime();
        int rows = context.rows, cols = context.cols;
        int startIdx = context.getStartIndex();
        int endIdx = context.getEndIndex();
        int[][] maze = context.getGrid();

        PriorityQueue<Node> open = new PriorityQueue<>();
        int[] gScore = new int[rows * cols];
        int[] parent = new int[rows * cols];
        Arrays.fill(gScore, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        gScore[startIdx] = 0;
        open.add(new Node(startIdx, 0, manhattan(context.startRow, context.startCol, context.endRow, context.endCol)));

        long nodesExp = 0;

        while (!open.isEmpty()) {
            Node current = open.poll();
            nodesExp++;

            if (current.g > gScore[current.index]) continue;
            if (current.index == endIdx) {
                return new AlgorithmResult("A* Search", reconstruct(parent, cols, endIdx), current.g, System.nanoTime() - start, nodesExp);
            }

            int r = current.index / cols, c = current.index % cols;
            int[] dr = {-1, 1, 0, 0}, dc = {0, 0, -1, 1};

            for (int i = 0; i < 4; i++) {
                int nr = r + dr[i], nc = c + dc[i];
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && maze[nr][nc] != -1) {
                    int nIdx = nr * cols + nc;
                    int newG = current.g + maze[nr][nc];
                    if (newG < gScore[nIdx]) {
                        gScore[nIdx] = newG;
                        parent[nIdx] = current.index;
                        open.add(new Node(nIdx, newG, newG + manhattan(nr, nc, context.endRow, context.endCol)));
                    }
                }
            }
        }
        return new AlgorithmResult("A* Search", new ArrayList<>(), -1, System.nanoTime() - start, nodesExp);
    }

    private int manhattan(int r, int c, int er, int ec) { return Math.abs(r - er) + Math.abs(c - ec); }
    
    private List<int[]> reconstruct(int[] parent, int cols, int curr) {
        List<int[]> path = new ArrayList<>();
        while (curr != -1) { path.add(new int[]{curr / cols, curr % cols}); curr = parent[curr]; }
        Collections.reverse(path);
        return path;
    }
}