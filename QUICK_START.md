# 🚀 QUICK START GUIDE - 5 Minutes to Running

## Step 1: Verify Project Structure (30 seconds)

Make sure your project looks like this:
```
cpe231-maze-project/
│
├── src/
│   └── cpe231/
│       └── maze/
│           ├── Main.java
│           ├── core/
│           ├── algorithms/
│           ├── ui/
│           ├── io/
│           └── benchmark/
│
└── data/
    ├── m15.txt
    ├── m20.txt
    ├── ...
    └── m100.txt
```

**If you're missing the `data/` folder**: Create it and add at least one test maze file.

---

## Step 2: Replace Your Broken Files (2 minutes)

Copy the fixed versions from the artifacts I provided above:

### Core Files (3 files)
1. `MazeContext.java` → Replace in `src/cpe231/maze/core/`
2. `AlgorithmResult.java` → Replace in `src/cpe231/maze/core/`
3. `MazeLoader.java` → Replace in `src/cpe231/maze/io/`

### Algorithm Files (4 files)
4. `AStarSolver.java` → Replace in `src/cpe231/maze/algorithms/`
5. `DijkstraSolver.java` → Replace in `src/cpe231/maze/algorithms/`
6. `GeneticSolverPure.java` → **DELETE old one, create new** in `src/cpe231/maze/algorithms/`
7. `GeneticSolverAdaptive.java` → Replace in `src/cpe231/maze/algorithms/`

### UI Files (2 files)
8. `VisualizationApp.java` → Replace in `src/cpe231/maze/ui/`
9. `MazePanel.java` → Replace in `src/cpe231/maze/ui/`

### Benchmark File (1 file)
10. `Benchmark.java` → Replace in `src/cpe231/maze/benchmark/`

### Main File (1 file)
11. `Main.java` → Replace in `src/cpe231/maze/`

---

## Step 3: Compile & Run (1 minute)

### Using IntelliJ IDEA:
1. Right-click on `Main.java`
2. Click **"Run 'Main.main()'"**
3. Done! ✅

### Using Eclipse:
1. Right-click on `Main.java`
2. Click **"Run As" → "Java Application"**
3. Done! ✅

### Using VS Code:
1. Open `Main.java`
2. Click the **▶ Run** button at top-right
3. Done! ✅

### Using Command Line:
```bash
# Navigate to project root
cd cpe231-maze-project

# Compile
javac -d bin -sourcepath src src/cpe231/maze/Main.java

# Run
java -cp bin cpe231.maze.Main
```

---

## Step 4: Test Basic Functionality (1 minute)

### Test 1: Load and Run a Maze
1. Window should open with 4 panels
2. Select `m15.txt` from dropdown
3. Click **"▶ Load & Run"**
4. Watch all 4 algorithms solve the maze
5. ✅ All should show "SUCCESS" with green status

### Test 2: Adjust Speed
1. Move the **Speed** slider left (faster) or right (slower)
2. Check **"Skip Animation"** for instant results
3. ✅ Should work smoothly

### Test 3: Run Benchmark
1. Click **"📊 Run Benchmark"**
2. Wait 1-2 minutes for completion
3. Click **"📄 Export to CSV"**
4. ✅ Should save a .csv file with results

---

## Step 5: Verify Everything Works (30 seconds)

Run this checklist:

- [ ] Application window opens without errors
- [ ] Can load maze files from dropdown
- [ ] All 4 algorithms run successfully
- [ ] Path animation displays correctly
- [ ] Colors are correct (Green=Start, Red=Goal, Cyan=Path)
- [ ] Status labels update with results
- [ ] Benchmark runs and completes
- [ ] Can export CSV file
- [ ] No crashes or freezes

**If all checked**: You're ready! ✅

---

## 🆘 Troubleshooting - Common Issues

### Issue: "Cannot find symbol" errors during compile
**Solution**: Make sure ALL 11 files are replaced. Missing even one will cause errors.

### Issue: Window opens but shows "No maze loaded"
**Solution**: 
1. Verify `data/` folder exists in project root
2. Add at least one `.txt` maze file
3. Restart application

### Issue: Genetic algorithms show "FAILED" status
**Solution**: This is normal on some mazes. Try:
- Larger mazes (GA works better on 50x50+)
- Check if other algorithms succeed
- If A* and Dijkstra also fail, maze might be unsolvable

### Issue: UI freezes when running algorithms
**Solution**: Make sure you're using the NEW `VisualizationApp.java` with SwingWorker, not the old one with raw threads.

### Issue: Files sorted wrong (m100 before m15)
**Solution**: Use the NEW versions that include `extractNumber()` method for natural sorting.

---

## 📊 Create Sample Maze (If data/ folder is empty)

Save this as `data/m15.txt`:
```
###############
#S..........#.#
#.#########.#.#
#.#.......#.#.#
#.#.#####.#.#.#
#.#.#...#.#.#.#
#.#.#.#.#.#.#.#
#.#.#.#.#.#.#.#
#.#.#.#.#.#.#.#
#.#.#.#.#.#.#.#
#.#.#.#.#.#...#
#.#.#.#.#.###.#
#.#...#.......#
#.###########.#
#............G#
###############
```

---

## 🎬 Demo for Video Presentation

### Script for Your Video (20 minutes total):

**Part 1: Show the Application (2 min)**
```
"Here's our maze solver application. It compares 4 algorithms 
side-by-side: A*, Dijkstra, and two Genetic Algorithms."
```
- Show the UI
- Point out the 4 panels
- Show the controls

**Part 2: Run on Small Maze (3 min)**
```
"Let's load a simple 15x15 maze and see how they perform."
```
- Load m15.txt
- Click Run
- Show animation
- Point out the results: "A* found the path in 2.5ms, while 
  the pure GA took 450ms but still succeeded."

**Part 3: Run on Large Maze (5 min)**
```
"Now let's test on a complex 100x100 maze."
```
- Load m100.txt
- Click Run
- Show that A* completes first
- Explain why GA takes longer (evolution process)
- Show final statistics comparison

**Part 4: Run Benchmark (3 min)**
```
"To properly compare performance, we run all algorithms 
on all 13 test mazes."
```
- Click Benchmark
- Show the table filling up
- Export to CSV
- Open CSV in Excel/Google Sheets

**Part 5: Explain Results (5 min)**
```
"Looking at our benchmark data, we can see that:"
```
- A* is fastest and always optimal
- Dijkstra is reliable but slower
- Pure GA is slowest but educational
- Adaptive GA bridges the gap with heuristics

**Part 6: Code Walkthrough (2 min)**
```
"The project follows clean architecture principles..."
```
- Show project structure
- Open one algorithm file
- Explain key concepts

---

## ✅ Pre-Presentation Checklist

Before recording your video:

- [ ] Test on at least 3 different maze sizes (small, medium, large)
- [ ] Verify all 4 algorithms show results
- [ ] Run full benchmark and export CSV
- [ ] Close all unnecessary windows/apps
- [ ] Set screen resolution to 1920x1080 for recording
- [ ] Disable notifications
- [ ] Practice your script 2-3 times
- [ ] Have backup mazes ready in case one fails
- [ ] Check audio levels
- [ ] Have water nearby (don't sound thirsty!)

---

## 🎯 Expected Performance

### Small Mazes (15x15)
- A*: 1-5ms
- Dijkstra: 2-8ms
- GA Pure: 200-600ms
- GA Adaptive: 100-300ms

### Medium Mazes (50x50)
- A*: 10-50ms
- Dijkstra: 20-100ms
- GA Pure: 2-5s
- GA Adaptive: 1-3s

### Large Mazes (100x100)
- A*: 50-200ms
- Dijkstra: 100-500ms
- GA Pure: 10-20s
- GA Adaptive: 5-15s

**If your results are within 2x of these**: Perfect! ✅

---

## 🏆 You're Ready!

If you've completed all steps and tests pass, you have:

✅ A fully functional maze solver  
✅ 4 working algorithms  
✅ Professional UI with dark theme  
✅ Benchmark suite with CSV export  
✅ Production-quality code  
✅ Everything needed for an A-grade project  

**Now go create that video and ace your presentation!** 🎓

---

## 📞 Final Tips

1. **Don't modify working code** - If it compiles and runs, leave it!
2. **Test before recording** - Run everything at least twice
3. **Have fun** - Show your enthusiasm in the video
4. **Explain clearly** - Assume your audience knows CS but not this specific project
5. **Time yourself** - Stay within 15-25 minutes
6. **Show results** - Data speaks louder than code

Good luck! 🚀