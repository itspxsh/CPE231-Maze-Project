# ⚡ Quick Reference Guide

## 🔥 Super Fast Start (Copy-Paste These)

### Option 1: Visualization Demo (GUI)
```bash
# Compile
javac -d bin src/cpe231/maze/**/*.java src/cpe231/maze/*.java

# Run
java -cp bin cpe231.maze.Main
```

### Option 2: Benchmark All Mazes
```bash
# Compile (if not already done)
javac -d bin src/cpe231/maze/**/*.java src/cpe231/maze/*.java

# Run benchmark
java -cp bin cpe231.maze.Main
# Then change Main.java: RunMode mode = RunMode.BENCHMARK;
```

---

## 📝 Step-by-Step Setup

### 1. Project Structure Check
```
CPE231-Maze-Project/
├── src/cpe231/maze/Main.java       ✓ Should exist
├── data/m33_35.txt                 ✓ Should exist
└── bin/                            ← Will be created by javac
```

### 2. Compile
```bash
# Make sure you're in project root (where src/ and data/ are)
pwd  # Should show: /path/to/CPE231-Maze-Project

# Compile all files
javac -d bin src/cpe231/maze/**/*.java src/cpe231/maze/*.java

# Check compilation
ls bin/cpe231/maze/  # Should show Main.class and folders
```

### 3. Run
```bash
# Option A: Run from project root (recommended)
java -cp bin cpe231.maze.Main

# Option B: Run from bin directory
cd bin
java cpe231.maze.Main
cd ..
```

---

## 🎛️ Switching Modes

### Edit Main.java

Open `src/cpe231/maze/Main.java` and find this line:

```java
RunMode mode = RunMode.VISUALIZATION;  // ← Change here
```

**Available Modes:**
```java
VISUALIZATION      // GUI with animations
BENCHMARK          // Full benchmark suite
QUICK_TEST         // Fast test on 15x15
CUSTOM_BENCHMARK   // Custom file and runs
```

**Example:**
```java
// For benchmark:
RunMode mode = RunMode.BENCHMARK;

// For custom test:
RunMode mode = RunMode.CUSTOM_BENCHMARK;
// Also edit:
String customFile = "data/m100_100.txt";
int customRuns = 100;
```

After editing, recompile and run again.

---

## 🔧 Common Tasks

### Test Different Maze Files

```java
// In Main.java, find:
String demoFile = "data/m33_35.txt";  // ← Change this

// Available files:
"data/m15_15.txt"    // Small maze
"data/m33_35.txt"    // Medium maze
"data/m50_50.txt"    // Large maze
"data/m100_100.txt"  // Huge maze
```

### Change Algorithm Set

```java
// In VisualizationApp.java or Benchmark.java, find:
MazeSolver[] solvers = {
    new AStarSolver(),
    new DijkstraSolver(),
    new GeneticSolverPure(),
    new GeneticSolverAdaptive()
};

// Comment out any you don't want:
MazeSolver[] solvers = {
    new AStarSolver(),
    new DijkstraSolver(),
    // new GeneticSolverPure(),  // Commented out
};
```

### Adjust GA Parameters

```java
// In GeneticSolverPure.java:
private static final int POPULATION_SIZE = 100;  // ← Change
private static final int MAX_GENERATIONS = 200;  // ← Change
private static final double MUTATION_RATE = 0.05; // ← Change
```

### Change Benchmark Runs

```java
// In Benchmark.java:
private static final int DEFAULT_RUNS = 50;  // ← Change
```

---

## 🎯 For Your Presentation/Video

### Best Visualization Setup

```java
// Main.java
RunMode mode = RunMode.VISUALIZATION;
String demoFile = "data/m33_35.txt";  // Good size for presentation
```

**Then:**
1. Run the program
2. Wait for GUI to load
3. Click "▶ Start All Animations"
4. Screen record the synchronized animations
5. Explain the differences in runtime and path quality

### Best Benchmark for Report

```java
// Main.java
RunMode mode = RunMode.BENCHMARK;
```

**Then:**
1. Run and let it complete (takes ~5-10 minutes)
2. Copy the output tables
3. Paste into your report
4. Analyze the gap percentages

---

## 🐛 Troubleshooting

### Problem: Compilation Error

```bash
# Error: package cpe231.maze does not exist

# Solution: Make sure you're in the RIGHT directory
pwd  # Should show project root, not inside src/

# Compile from project root:
javac -d bin src/cpe231/maze/**/*.java src/cpe231/maze/*.java
```

### Problem: FileNotFoundException

```bash
# Error: data/m33_35.txt (No such file or directory)

# Solution: Run from project root
pwd  # Must be in CPE231-Maze-Project/
ls data/  # Should show .txt files

# Run from correct location:
java -cp bin cpe231.maze.Main
```

### Problem: GUI Doesn't Appear

```bash
# Check Java version
java -version  # Should be 17 or higher

# Check if GUI libraries are available
java -cp bin cpe231.maze.Main
# If no error about AWT/Swing, it should work
```

### Problem: "OutOfMemoryError" with Large Mazes

```bash
# Increase heap size:
java -Xmx2G -cp bin cpe231.maze.Main

# Or reduce GA population/generations in code
```

---

## 📊 Expected Output Examples

### Visualization Mode
```
╔════════════════════════════════════════════════════════════════╗
║   CPE231 MAZE PATHFINDING PROJECT                              ║
║   Comparing: A*, Dijkstra, Genetic Algorithms                  ║
╚════════════════════════════════════════════════════════════════╝

Mode: Visualization Demo
File: data/m33_35.txt

Starting GUI... (this may take a moment for large mazes)

[GUI Window Opens]
```

### Benchmark Mode
```
╔════════════════════════════════════════════════════════════════╗
║   PATHFINDING ALGORITHM BENCHMARK SUITE                        ║
║   Runs per maze: 50 (avg)                                      ║
╚════════════════════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📁 File: m33_35.txt
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Maze Size: 33x35 (1,155 total cells)

   ┌─────────────────────────────┬──────────┬────────────┬──────────────┬─────────────┐
   │ Algorithm                   │   Cost   │  Time (ms) │ Nodes Exp.   │ Gap         │
   ├─────────────────────────────┼──────────┼────────────┼──────────────┼─────────────┤
   │ A* (Manhattan)              │      234 │     0.4521 │          678 │ OPTIMAL ★   │
   │ Dijkstra (Optimized)        │      234 │     0.3812 │          892 │ OPTIMAL ★   │
   │ Genetic Algorithm (Pure)    │      245 │    12.3456 │       20,000 │ +11 (4.7%)  │
   │ Genetic Algorithm (Adaptive)│      236 │    23.5678 │       25,000 │ +2 (0.9%)   │
   └─────────────────────────────┴──────────┴────────────┴──────────────┴─────────────┘
```

---

## 💡 Pro Tips

### For Faster Development
```bash
# Create an alias for compilation
alias compile-maze='javac -d bin src/cpe231/maze/**/*.java src/cpe231/maze/*.java'
alias run-maze='java -cp bin cpe231.maze.Main'

# Then just use:
compile-maze && run-maze
```

### For Report Writing
```bash
# Run benchmark and save output
java -cp bin cpe231.maze.Main > results.txt

# Open in editor
cat results.txt
```

### For Video Recording
1. Set mode to `VISUALIZATION`
2. Choose a medium-sized maze (33×35 or 40×40)
3. Start recording
4. Run the program
5. Click "Start All Animations"
6. Explain algorithm behaviors as they animate
7. Pause and compare final costs

---

## 🎓 For Your Academic Defense

### Key Points to Mention

1. **Architecture:**
   - "We used the Strategy pattern for algorithm interchangeability"
   - "MVC separation ensures UI doesn't contain business logic"
   - "Immutable DTOs prevent accidental state modifications"

2. **Performance:**
   - "Dijkstra is faster than A* due to lower per-node overhead"
   - "Custom heap implementation eliminates object allocation costs"
   - "Cache-friendly data structures (flattened arrays) improve locality"

3. **Genetic Algorithm:**
   - "Pure GA uses only evolutionary operators (selection, crossover, mutation)"
   - "Adaptive GA is a memetic/Lamarckian hybrid with local search"
   - "Tournament selection with size 5 balances diversity and convergence"

4. **Testing:**
   - "Comprehensive test suite: 13 mazes from 15×15 to 100×100"
   - "Statistical averaging over 50 runs eliminates variance"
   - "Warm-up phase eliminates JIT compilation effects"

---

**That's it! You're ready to go! 🚀**

Need help? Check the full README.md for detailed explanations.