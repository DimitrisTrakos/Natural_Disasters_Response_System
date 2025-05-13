package mapGrid;

import java.util.Random;

public class MapGrid {
    private final int width;
    private final int height;
    private final GridCell[][] grid;
    private final Random random = new Random();

    public MapGrid(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new GridCell[height][width];
        initializeGrid();
    }

    private void initializeGrid() {

        int totalCells = width * height;
        int houseCount = (int) (totalCells * 0.05); // 5% of the map

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = new GridCell();
            }
        }

        // Randomly place houses
        for (int i = 0; i < houseCount; i++) {
            int x, y;
            do {
                x = random.nextInt(width);
                y = random.nextInt(height);
            } while (grid[y][x].isHouse); // Ensure unique positions
            grid[y][x].isHouse = true;
            grid[y][x].isForest = false; // Optional: override forest if needed
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
                            if (random.nextDouble() < 0.2) {
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

                if (cell.hasAgent) {
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
                } else if (cell.isHouse) {
                    System.out.printf("🏠 ");
                } else if (cell.isForest) {
                    System.out.printf("🌲 ");
                } else {
                    System.out.printf("▫️ ");
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
