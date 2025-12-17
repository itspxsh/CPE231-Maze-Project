# 🧩 CPE231 Maze Pathfinding Project

**Course:** CPE231 Algorithms
**Project Type:** Algorithm Analysis & Comparison (Heuristic Search vs. Evolutionary Computation)
**Language:** Java (Swing GUI)

---

## 📖 Project Overview

This project is a comparative study of pathfinding algorithms implemented in Java. It solves maze traversal problems using both deterministic graph search algorithms (Dijkstra, A*) and stochastic evolutionary algorithms (Genetic Algorithm).

The application features a **GUI Visualization Tool** that allows users to observe, compare, and benchmark these algorithms in real time on various maze datasets.

---

## 🚀 Features

* **Multi-Algorithm Comparison**
  Run A*, Dijkstra, and Genetic Algorithms simultaneously.

* **Synchronized Visualization (Race Mode)**
  All algorithms start and animate their pathfinding steps at the exact same time for direct visual comparison.

* **Performance Metrics**
  Real-time tracking of:

  * **Path Cost:** Total weight of the traversed path
  * **Execution Time:** Time taken to find the solution
  * **Nodes Expanded:** Efficiency of the search process

* **Benchmark Suite**
  A dedicated module to run headless performance tests across all datasets.

* **Dynamic Configuration**
  Adjustable animation speed and **Skip Animation** mode for instant results.

---

## 🧠 Algorithms Implemented

### 1. Dijkstra's Algorithm

* **Type:** Uninformed Search (Greedy)
* **Behavior:** Guarantees the absolute shortest path. Used as the **baseline** for accuracy.
* **Mechanism:** Explores all neighbors uniformly based on current path cost *(g(n))*.

---

### 2. A* Search (A-Star)

* **Type:** Informed Search
* **Heuristic:** Manhattan Distance
  (h(n) = |x_1 - x_2| + |y_1 - y_2|)
* **Behavior:** The most efficient deterministic solver in this project.
* **Evaluation Function:**
  (f(n) = g(n) + h(n))

---

### 3. Pure Genetic Algorithm (GA)

* **Type:** Evolutionary Computation (Stochastic)
* **Philosophy:** *Survival of the fittest*

**Key Components:**

* **Encoding:** Paths represented as chromosomes
* **Selection:** Tournament selection to pick parents
* **Crossover:** Combining path segments from two parents
* **Mutation:** **Pure Random Walk**
  No heuristics or BFS repair are used in the mutation phase, strictly adhering to *Pure GA* requirements.

**Goal:** To demonstrate how evolutionary pressure alone can solve complex pathfinding problems without graph traversal knowledge.

---

### 4. Hybrid Genetic Algorithm (Experimental)

* **Type:** Hybrid Evolutionary Algorithm
* **Improvement:** Incorporates heuristic-guided initialization or local search operators to improve convergence speed compared to the Pure GA.

---

## 📂 Project Structure

The project follows a modular architecture separating logic, UI, and data.

```text
CPE231-MAZE-PROJECT/
├── src/cpe231/maze/
│   ├── algorithms/     # A*, Dijkstra, PureGA, HybridGA implementations
│   ├── core/           # Interfaces (MazeSolver) and shared data (AlgorithmResult)
│   ├── io/             # File parsing and maze loading logic
│   ├── ui/             # Swing-based GUI (VisualizationApp, MazePanel)
│   └── benchmark/      # Headless performance testing suite
├── data/               # Input maze test cases (e.g., m15_15.txt, m100_100.txt)
├── output/             # Generated and example benchmark outputs
│   └── results/        # Example benchmark CSV result files
├── docs/               # Project documentation (PDF report)
└── bin/                # Compiled bytecode
```

---

## 🛠️ Getting Started

### Prerequisites

* Java Development Kit (JDK) 8 or higher

---

### Running the Application

1. **Clone the repository**

```bash
git clone https://github.com/itspxsh/CPE231-Maze-Project.git
```

2. **Compile the source code**

```bash
javac -d bin -sourcepath src src/cpe231/maze/*.java
```

3. **Run the GUI**

```bash
java -cp bin cpe231.maze.Main
```

---

## 🧾 Input File Format (`.txt`)

Maze files in the `data/` directory follow this structure:

* **Line 1:** `Rows Cols` (dimensions)
* **Grid:** Tab- or space-separated integers

  * `-1` : Wall / Obstacle
  * `1–9`: Floor cost (traversal weight)
* **Last 2 Lines:**

  * Start `(r, c)`
  * End `(r, c)`

---

## 📊 Benchmark Results

* **Optimal Path:** Dijkstra and A* always find the optimal path cost
* **Speed:** A* is consistently the fastest due to the Manhattan heuristic
* **Evolutionary Efficiency:**
  The Pure GA successfully converges on a valid path but may not always find the global optimum compared to deterministic methods, highlighting the trade-off between **exploration** (GA) and **exploitation** (A*).

---

## 👨‍💻 Authors

* **Kamonnat Seetakai** — 67070501001
* **Benyapha Rattanakhunodom** — 67070501030
* **Pawarisa Thongchua** — 67070501032
* **Rapheephitcha Warasinkullphat** — 67070501036

Computer Engineering, KMUTT
*CPE231 Algorithms Course Project*
