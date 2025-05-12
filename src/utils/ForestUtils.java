package utils;
import java.util.Random;
import mapGrid.GridCell;
import mapGrid.MapGrid;
public class ForestUtils {

    public static void generateForest(MapGrid map, int numTrees) {
        Random rand = new Random();
        int width = map.getWidth();
        int height = map.getHeight();

        for (int i = 0; i < numTrees; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            GridCell cell = map.getCell(x, y);

            if (cell != null && !cell.isForest) {
                cell.isForest = true;
            }
        }
    }

    // public static void generateForestClusters(MapGrid map, int numClusters, int treesPerCluster, int radius) {
    //     Random rand = new Random();
    //     int width = map.getWidth();
    //     int height = map.getHeight();

    //     for (int i = 0; i < numClusters; i++) {
    //         int centerX = rand.nextInt(width);
    //         int centerY = rand.nextInt(height);

    //         for (int j = 0; j < treesPerCluster; j++) {
    //             int dx = rand.nextInt(2 * radius + 1) - radius;
    //             int dy = rand.nextInt(2 * radius + 1) - radius;

    //             int x = centerX + dx;
    //             int y = centerY + dy;

    //             if (x >= 0 && x < width && y >= 0 && y < height) {
    //                 GridCell cell = map.getCell(x, y);
    //                 if (cell != null) {
    //                     cell.isForest = true;
    //                 }
    //             }
    //         }
    //     }
    // }
    public static void generateForestClusters(MapGrid map, int numClusters, int treesPerCluster, int radius) {
        if (radius <= 0) {
            System.out.println("Error: radius must be positive.");
            return; // Exit the method if radius is invalid
        }
        
        Random rand = new Random();
        int width = map.getWidth();
        int height = map.getHeight();
    
        for (int i = 0; i < numClusters; i++) {
            // Randomly choose a center point for the cluster
            int centerX = rand.nextInt(width);
            int centerY = rand.nextInt(height);
    
            // Generate the trees around the center
            for (int j = 0; j < treesPerCluster; j++) {
                // Randomly pick an offset (dx, dy) within a radius from the center
                int dx = rand.nextInt(2 * radius + 1) - radius;
                int dy = rand.nextInt(2 * radius + 1) - radius;
    
                int x = centerX + dx;
                int y = centerY + dy;
    
                // Ensure the x, y coordinates are within bounds
                if (x >= 0 && x < width && y >= 0 && y < height) {
                    GridCell cell = map.getCell(x, y);
                    if (cell != null) {
                        // Mark this cell as a forest cell
                        cell.isForest = true;
                    }
                }
            }
        }
    }

    
    public static boolean checkForDenseForest(MapGrid map, int x, int y, int radius) {
        int treesFound = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int nx = x + dx;
                int ny = y + dy;

                GridCell cell = map.getCell(nx, ny);
                if (cell != null && cell.isForest) {
                    treesFound++;
                }
            }
        }
        return treesFound > (Math.PI * radius * radius / 2);
    }
}