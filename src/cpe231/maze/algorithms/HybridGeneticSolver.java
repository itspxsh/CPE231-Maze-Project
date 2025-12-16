package cpe231.maze.algorithms;

import cpe231.maze.core.*;
import java.util.*;

/**
 * HYBRID (MEMETIC) GENETIC ALGORITHM
 * - Heuristic seeding (generation 0 only)
 * - Goal-enforced fitness
 * - Elite preservation of valid paths
 */
public class HybridGeneticSolver implements MazeSolver {

    private static final int POP_SIZE = 160;
    private static final int GENERATIONS = 300;
    private static final double MUT_RATE = 0.18;
    private static final int ELITE = 12;

    private static final int[] DR = {-1, 0, 1, 0};
    private static final int[] DC = {0, 1, 0, -1};

    private final Random rnd = new Random();

    private class Individual implements Comparable<Individual> {
        List<Integer> g;
        double f;
        boolean ok;

        Individual(List<Integer> g) {
            this.g = new ArrayList<>(g);
        }

        @Override
        public int compareTo(Individual o) {
            return Double.compare(o.f, this.f);
        }
    }

    @Override
    public AlgorithmResult solve(MazeContext ctx) {
        long start = System.nanoTime();
        long nodes = 0;

        List<Individual> pop = seed(ctx);
        Individual best = null;

        for (int gen = 0; gen < GENERATIONS; gen++) {
            for (Individual i : pop) {
                eval(i, ctx);
                nodes += i.g.size();
                if (i.ok && (best == null || i.f > best.f))
                    best = i;
            }

            Collections.sort(pop);
            List<Individual> next = new ArrayList<>();

            int kept = 0;
            for (Individual i : pop) {
                if (i.ok) {
                    next.add(i);
                    if (++kept == ELITE) break;
                }
            }

            while (next.size() < POP_SIZE) {
                Individual p1 = pick(pop);
                Individual p2 = pick(pop);
                Individual c = cross(p1, p2);
                mutate(c, ctx);
                next.add(c);
            }
            pop = next;
        }

        if (best == null)
            return new AlgorithmResult("Failed", new ArrayList<>(), -1,
                    System.nanoTime() - start, nodes);

        List<int[]> path = decode(best, ctx);
        int cost = pathCost(path, ctx);

        return new AlgorithmResult("Success", path, cost,
                System.nanoTime() - start, nodes);
    }

    /* ================= CORE ================= */

    private List<Individual> seed(MazeContext ctx) {
        List<Individual> p = new ArrayList<>();
        for (int i = 0; i < POP_SIZE; i++)
            p.add(i < POP_SIZE / 3 ? heuristic(ctx) : random(ctx));
        return p;
    }

    private Individual heuristic(MazeContext ctx) {
        List<Integer> g = new ArrayList<>();
        int r = ctx.startRow, c = ctx.startCol;

        for (int i = 0; i < ctx.rows * ctx.cols; i++) {
            if (r == ctx.endRow && c == ctx.endCol) break;
            int best = -1, bd = Integer.MAX_VALUE;
            for (int d = 0; d < 4; d++) {
                int nr = r + DR[d], nc = c + DC[d];
                if (ctx.isValid(nr, nc)) {
                    int dist = Math.abs(nr - ctx.endRow)
                             + Math.abs(nc - ctx.endCol);
                    if (dist < bd) {
                        bd = dist;
                        best = d;
                    }
                }
            }
            if (best == -1) break;
            g.add(best);
            r += DR[best];
            c += DC[best];
        }
        return new Individual(g);
    }

    private Individual random(MazeContext ctx) {
        int len = ctx.rows + ctx.cols + rnd.nextInt(30);
        List<Integer> g = new ArrayList<>();
        for (int i = 0; i < len; i++)
            g.add(rnd.nextInt(4));
        return new Individual(g);
    }

    private void eval(Individual i, MazeContext ctx) {
        int r = ctx.startRow, c = ctx.startCol;
        int cost = 0;
        boolean ok = false;

        for (int d : i.g) {
            int nr = r + DR[d], nc = c + DC[d];
            if (!ctx.isValid(nr, nc)) break;
            r = nr;
            c = nc;
            cost += ctx.getCost(r, c);
            if (r == ctx.endRow && c == ctx.endCol) {
                ok = true;
                break;
            }
        }

        i.ok = ok;
        int dist = Math.abs(r - ctx.endRow)
                 + Math.abs(c - ctx.endCol);

        if (!ok) {
            i.f = -1_000_000 - dist * 1000;
        } else {
            i.f = 1_000_000 - cost * 1000 - i.g.size();
        }
    }

    private Individual pick(List<Individual> p) {
        Individual b = null;
        for (int i = 0; i < 5; i++) {
            Individual c = p.get(rnd.nextInt(p.size()));
            if (b == null || c.f > b.f) b = c;
        }
        return b;
    }

    private Individual cross(Individual a, Individual b) {
        int m = Math.min(a.g.size(), b.g.size());
        if (m == 0) return new Individual(a.g);
        int s = rnd.nextInt(m);
        List<Integer> g = new ArrayList<>(a.g.subList(0, s));
        g.addAll(b.g.subList(s, b.g.size()));
        return new Individual(g);
    }

    private void mutate(Individual i, MazeContext ctx) {
        for (int x = 0; x < i.g.size(); x++)
            if (rnd.nextDouble() < MUT_RATE)
                i.g.set(x, rnd.nextInt(4));

        if (i.g.size() > ctx.rows * ctx.cols)
            i.g = i.g.subList(0, ctx.rows * ctx.cols);
    }

    /* ================= PATH ================= */

    private List<int[]> decode(Individual i, MazeContext ctx) {
        List<int[]> p = new ArrayList<>();
        int r = ctx.startRow, c = ctx.startCol;
        p.add(new int[]{r, c});

        for (int d : i.g) {
            int nr = r + DR[d], nc = c + DC[d];
            if (!ctx.isValid(nr, nc)) break;
            r = nr;
            c = nc;
            p.add(new int[]{r, c});
            if (r == ctx.endRow && c == ctx.endCol) break;
        }
        return p;
    }

    private int pathCost(List<int[]> p, MazeContext ctx) {
        int c = 0;
        for (int i = 1; i < p.size(); i++)
            c += ctx.getCost(p.get(i)[0], p.get(i)[1]);
        return c;
    }
}
