package utils;
import mapGrid.MapGrid;
import mapGrid.GridCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FireUtils {
    public static void igniteRandomForestCell(MapGrid map, Random rand, 
                                            int droneX, int droneY,
                                            List<int[]> firefighterPositions,
                                            List<int[]> houseLocations) {
        
        List<int[]> houseCells = new ArrayList<>();
        List<int[]> nearHouseCells = new ArrayList<>();
        List<int[]> regularForestCells = new ArrayList<>();

        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                if ((x == droneX && y == droneY) || isFirefighterPosition(x, y, firefighterPositions)) {
                    continue;
                }

                GridCell cell = map.getCell(x, y);
                if (cell != null && cell.isForest && !cell.isOnFire) {
                    boolean isHouse = cell.isHouse;
                    boolean nearHouse = isNearHouse(x, y, houseLocations);

                    if (isHouse) {
                        houseCells.add(new int[]{x, y});
                    } else if (nearHouse) {
                        nearHouseCells.add(new int[]{x, y});
                    } else {
                        regularForestCells.add(new int[]{x, y});
                    }
                }
            }
        }

        // Weighted random selection (50% house, 20% near house, 30% regular)
        double probability = rand.nextDouble();
        int[] fireLocation = null;

        if (probability < 0.5 && !houseCells.isEmpty()) {
            fireLocation = houseCells.get(rand.nextInt(houseCells.size()));
        } 
        else if (probability < 0.7 && !nearHouseCells.isEmpty()) {
            fireLocation = nearHouseCells.get(rand.nextInt(nearHouseCells.size()));
        }
        else if (!regularForestCells.isEmpty()) {
            fireLocation = regularForestCells.get(rand.nextInt(regularForestCells.size()));
        }

        if (fireLocation != null) {
            map.getCell(fireLocation[0], fireLocation[1]).isOnFire = true;
        } else {
            System.out.println("⚠ No valid locations to start fire");
        }
    }

    private static boolean isFirefighterPosition(int x, int y, List<int[]> firefighterPositions) {
        for (int[] pos : firefighterPositions) {
            if (pos[0] == x && pos[1] == y) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNearHouse(int x, int y, List<int[]> houseLocations) {
        for (int[] house : houseLocations) {
            if (Math.abs(house[0] - x) <= 1 && Math.abs(house[1] - y) <= 1) {
                return true;
            }
        }
        return false;
    }
}