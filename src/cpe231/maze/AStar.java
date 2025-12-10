package cpe231.maze;

import java.util.*;

public class AStar {
    private static class Node implements Comparable<Node> {
        int index, g, f, h;
        public Node(int index, int g, int h) {
            this.index = index; this.g = g; this.h = h; this.f = g + h;
        }
        public int compareTo(Node o) {
            return (this.f == o.f) ? Integer.compare(this.h, o.h) : Integer.compare(this.f, o.f);
        }
    }

    public static AlgorithmResult solve(int[][] maze) {
        long start = System.nanoTime();
        long nodesExp = 0;
        int rows = maze.length, cols = maze[0].length;
        int startIdx = MazeLoader.startRow * cols + MazeLoader.startCol;
        int endIdx = MazeLoader.endRow * cols + MazeLoader.endCol;

        PriorityQueue<Node> open = new PriorityQueue<>();
        int[] gScore = new int[rows * cols];
        int[] parent = new int[rows * cols];
        Arrays.fill(gScore, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        gScore[startIdx] = 0;
        open.add(new Node(startIdx, 0, manhattan(MazeLoader.startRow, MazeLoader.startCol, MazeLoader.endRow, MazeLoader.endCol)));

        while (!open.isEmpty()) {
            Node current = open.poll();
            nodesExp++;
            if (current.g > gScore[current.index]) continue;
            if (current.index == endIdx) {
                return new AlgorithmResult("A* Search", reconstruct(parent, rows, cols, endIdx), current.g, System.nanoTime() - start, nodesExp);
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
                        open.add(new Node(nIdx, newG, manhattan(nr, nc, MazeLoader.endRow, MazeLoader.endCol)));
                    }
                }
            }
        }
        return new AlgorithmResult("A* Search", new ArrayList<>(), -1, System.nanoTime() - start, nodesExp);
    }

    private static int manhattan(int r, int c, int er, int ec) { return Math.abs(r - er) + Math.abs(c - ec); }
    private static List<int[]> reconstruct(int[] parent, int rows, int cols, int curr) {
        List<int[]> path = new ArrayList<>();
        while (curr != -1) { path.add(new int[]{curr / cols, curr % cols}); curr = parent[curr]; }
        Collections.reverse(path);
        return path;
    }
}