package cpe231.maze;
public class AlgorithmAdapter {

    // เมธอดหลักที่เรียกจาก VisualizationApp
    public static AlgorithmResult solve(String algoName, MazeInfo info) {
        
        int[][] maze = info.maze();
        int startR = info.start().r();
        int startC = info.start().c();
        int endR = info.end().r();
        int endC = info.end().c();
        
        // ประกาศตัวแปร List<int[]> ชั่วคราว (เนื่องจาก Algorithm เดิมใช้ int[])
        AlgorithmResult tempResult;
        
        // 1. เรียกใช้ Algorithm เดิม (ส่ง Maze Array + พิกัด 4 ค่า)
        if ("A* Manhattan".equals(algoName)) {
            tempResult = AStar.solve(maze, startR, startC, endR, endC); 
            
        } else if ("Dijkstra (Optimized)".equals(algoName)) {
            tempResult = Dijkstra.solve(maze, startR, startC, endR, endC);
            
        } else if ("Genetic Algorithm".equals(algoName)) {
            tempResult = GeneticAlgo.solve(maze, startR, startC, endR, endC);
            
        } else {
            throw new IllegalArgumentException("Unknown algorithm: " + algoName);
        }
        
        // เราจะไม่แปลง Path ใน Adapter เพราะเราต้องการให้ MazePanel ใช้ List<Coordinate>
        // ดังนั้นต้องแก้ไข MazePanel ให้รับ List<int[]> ชั่วคราว และแปลงเอง
        // (ในกรณีนี้ เราจะส่ง tempResult ที่มี List<int[]> ให้ MazePanel ไปจัดการ)
        return tempResult;
    }
}