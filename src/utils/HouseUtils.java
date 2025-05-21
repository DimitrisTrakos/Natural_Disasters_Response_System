package utils;
import java.util.Random;
import mapGrid.GridCell;
import mapGrid.MapGrid;

public class HouseUtils {
    
    public static void generateHouses(MapGrid map, int count) {
        Random rand = new Random();
        int housesPlaced = 0;
        
        while (housesPlaced < count) {
            int x = rand.nextInt(map.getWidth());
            int y = rand.nextInt(map.getHeight());
            
            GridCell cell = map.getCell(x, y);
            if (cell != null && !cell.isHouse) {
                map.addHouse(x, y);
                housesPlaced++;
                System.out.println("🏠 House placed at (" + x + "," + y + ")");
            }
        }
    }
}
