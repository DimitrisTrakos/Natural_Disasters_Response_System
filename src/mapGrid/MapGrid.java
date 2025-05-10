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
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = new GridCell();
            }
        }
    }

    public int getWidth() { return width; }

    public int getHeight() { return height; }

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
                        {-1, 0}, {1, 0}, {0, -1}, {0, 1}
                    };

                    for (int[] dir : directions) {
                        int nx = x + dir[0];
                        int ny = y + dir[1];
                        GridCell neighbor = getCell(nx, ny);

                        if (neighbor != null && neighbor.isForest && !neighbor.isOnFire) {
                            if (random.nextDouble() < 0.4) {
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

    public void printMap() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                GridCell cell = getCell(x, y);
    
                if (cell.hasAgent) {
                    switch (cell.agentType) {
                        case "drone":
                            System.out.print("🚁");
                            break;
                        case "firefighter":
                            System.out.print("🚒");
                            break;
                        case "medic":
                            System.out.print("🚑");
                            break;
                        default:
                            System.out.print("❓ ");
                            break;
                    }
                } else if (cell.isOnFire) {
                    System.out.print("🔥 ");
                } else if (cell.isForest) {
                    System.out.print("🌲 ");
                } else {
                    System.out.print("⬜ ");
                }
            }
            System.out.println();
        }
    }
}
