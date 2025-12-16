Here is a professional, academic-grade **README.md** tailored for your GitHub repository. It highlights the technical depth of your work, the algorithms used, and the project structure, which is perfect for a professor's review.

You can copy and paste this directly into your `README.md` file.

---

#CPE231 Maze Pathfinding Project 🧩**Course:** CPE231 Algorithms

**Project Type:** Algorithm Analysis & Comparison (Heuristic Search vs. Evolutionary Computation)

**Language:** Java (Swing GUI)

##📖 Project OverviewThis project is a comparative study of pathfinding algorithms implemented in Java. It solves maze traversal problems using both deterministic graph search algorithms (Dijkstra, A*) and stochastic evolutionary algorithms (Genetic Algorithm).

The application features a **GUI Visualization Tool** that allows users to observe, compare, and benchmark these algorithms in real-time on various maze datasets.

##🚀 Features* **Multi-Algorithm Comparison:** Run A*, Dijkstra, and Genetic Algorithms simultaneously.
* **Synchronized Visualization:** A "Race Mode" visualization where all algorithms start and animate their pathfinding steps at the exact same time for direct visual comparison.
* **Performance Metrics:** Real-time tracking of:
* **Path Cost:** Total weight of the traversed path.
* **Execution Time:** Time taken to find the solution.
* **Nodes Expanded:** Efficiency of the search process.


* **Benchmark Suite:** A dedicated module to run headless performance tests across all datasets.
* **Dynamic Configuration:** Adjustable animation speed and "Skip Animation" mode for instant results.

##🧠 Algorithms Implemented###1. Dijkstra's Algorithm* **Type:** Uninformed Search (Greedy).
* **Behavior:** Guarantees the absolute shortest path. Used as the **baseline** for accuracy.
* **Mechanism:** Explores all neighbors uniformly based on current path cost (g(n)).

###2. A* Search (A-Star)* **Type:** Informed Search.
* **Heuristic:** Manhattan Distance (h(n) = |x_1 - x_2| + |y_1 - y_2|).
* **Behavior:** The most efficient deterministic solver in this project. Balances actual cost and estimated cost (f(n) = g(n) + h(n)).

###3. Pure Genetic Algorithm (GA)* **Type:** Evolutionary Computation (Stochastic).
* **Philosophy:** "Survival of the fittest."
* **Key Components:**
* **Encoding:** Paths represented as chromosomes.
* **Selection:** Tournament selection to pick parents.
* **Crossover:** Combining path segments from two parents.
* **Mutation:** **Pure Random Walk**. No heuristics or BFS repair are used in the mutation phase, strictly adhering to "Pure GA" requirements.


* **Goal:** To demonstrate how evolutionary pressure alone can solve complex pathfinding problems without graph traversal knowledge.

###4. Hybrid Genetic Algorithm (Experimental)* **Type:** Hybrid Evolutionary.
* **Improvement:** Incorporates heuristic-guided initialization or local search operators to improve convergence speed compared to the Pure GA.

---

##📂 Project StructureThe project follows a modular architecture separating logic, UI, and data.

```
CPE231-MAZE-PROJECT/
├── src/cpe231/maze/
│   ├── algorithms/    # Implementation of A*, Dijkstra, PureGA, HybridGA
│   ├── core/          # Interfaces (MazeSolver) and Shared Data (AlgorithmResult)
│   ├── io/            # File parsing and maze loading logic
│   ├── ui/            # Swing-based GUI (VisualizationApp, MazePanel)
│   └── benchmark/     # Headless performance testing suite
├── data/              # Test cases (e.g., m15_15.txt, m100_100.txt)
└── bin/               # Compiled bytecode

```

##🛠️ Getting Started###Prerequisites* Java Development Kit (JDK) 8 or higher.

###Running the Application1. **Clone the repository:**
```bash
git clone https://github.com/itspxsh/CPE231-Maze-Project.git

```


2. **Compile the source code:**
```bash
javac -d bin -sourcepath src src/cpe231/maze/*.java

```


3. **Run the GUI:**
```bash
java -cp bin cpe231.maze.Main

```



###Input File Format (.txt)The maze files in the `data/` directory follow this structure:

* **Line 1:** `Rows Cols` (Dimensions)
* **Grid:** Tab/Space separated integers.
* `-1`: Wall/Obstacle
* `1-9`: Floor cost (Traversal weight)


* **Last 2 Lines:** Start `(r, c)` and End `(r, c)` coordinates.

##📊 Benchmark Results* **Optimal Path:** Dijkstra and A* always find the optimal cost.
* **Speed:** A* is consistently the fastest due to the Manhattan heuristic.
* **Evolutionary Efficiency:** The Pure GA successfully converges on a valid path but may not always find the global optimum compared to deterministic methods, highlighting the trade-off between exploration (GA) and exploitation (A*).

---

###👨‍💻 Author**[Your Name]** ID: [Your Student ID]

Computer Engineering, KMUTT

*CPE231 Algorithms Course Project*