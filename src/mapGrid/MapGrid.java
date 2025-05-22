package mapGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MapGrid {
    private final int width;
    private final int height;
    private final GridCell[][] grid;
    private final Random random = new Random();
    private List<int[]> houseLocations = new ArrayList<>();

    public MapGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new GridCell[height][width];
        initializeGrid();
    }

    private void initializeGrid() {


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = new GridCell();
            }
        }

        
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public GridCell getCell(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[y][x];
        }
        return null;
    }
  
    public void addHouse(int x, int y) {
        GridCell cell = getCell(x, y);
        if (cell != null) {
            cell.isHouse = true;
            houseLocations.add(new int[]{x, y});
        }
    }
    public List<int[]> getHouseLocations() {
        return new ArrayList<>(houseLocations);
    }
    public void spreadFire() {
        boolean[][] newFires = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                GridCell cell = getCell(x, y);
                if (cell.isOnFire) {
                    int[][] directions = {
                            { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }
                    };

                    for (int[] dir : directions) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        GridCell neighbor = getCell(nx, ny);

                        if (neighbor != null && neighbor.isForest && !neighbor.isOnFire) {
                            if (random.nextDouble() > 0.2) {
                                newFires[ny][nx] = true;
                            }
                        }
                    }
                }
            }
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (newFires[y][x]) {
                    grid[y][x].isOnFire = true;
                }
            }
        }
    }

    public synchronized void printMap() {
        System.out.println("_________________________________\n");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                GridCell cell = getCell(x, y);
                
                if(cell.hasAgent && cell.agentType.equals("firefighter") && cell.fireFighterExtinguishFire ) 
                System.out.printf("💧🚒 ");
                    
                    else if (cell.hasAgent) {
                        switch (cell.agentType) {
                            case "drone":
                                System.out.printf("🚁 ");
                                break;
                            case "firefighter":
                                System.out.printf("🚒 ");
                                break;
                            case "medic":
                                System.out.printf("🚑 ");
                                break;
                            default:
                                System.out.printf("❓ ");
                                break;
                        }
                    } else if (cell.isOnFire) {
                        System.out.printf("🔥 ");
                    } 
                    else if (cell.isHouse) {
                        System.out.printf("🏠 ");
                    } else if (cell.isForest) {
                        System.out.printf("🌲 ");
                    } else {
                        System.out.printf("◻️ ");
                    }
                }
            System.out.println();
        }
        System.out.println("_________________________________\n");
    }
    public boolean inBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }
}
