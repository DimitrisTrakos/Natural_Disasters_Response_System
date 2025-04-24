package utils;
import mapGrid.MapGrid;
import mapGrid.GridCell;

import java.util.Random;

public class FireUtils {
    public static void igniteRandomForestCell(MapGrid map, Random rand) {
        int width = map.getWidth();
        int height = map.getHeight();

        while (true) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            GridCell cell = map.getCell(x, y);

            if (cell != null && cell.isForest && !cell.isOnFire) {
                cell.isOnFire = true;
                System.out.println("🔥 Fire started at: (" + x + ", " + y + ")");
                break;
            }
        }
    }
}