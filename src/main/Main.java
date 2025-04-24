package main;
import mapGrid.MapGrid;
import utils.FireUtils;
import utils.ForestUtils;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        int width = 20;
        int height = 20;

        MapGrid map = new MapGrid(width, height);

        ForestUtils.generateForest(map, 100);

        ForestUtils.generateForestClusters(map, 4, 30, 3);

        FireUtils.igniteRandomForestCell(map, new java.util.Random());

        for (int t = 0; t < 15; t++) {
            System.out.println("Time step: " + t);
            map.printMap();
            map.spreadFire();
            Thread.sleep(1000);
            System.out.println();
        }
    }
}