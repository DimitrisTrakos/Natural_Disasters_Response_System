package main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import mapGrid.MapGrid;
import utils.ForestUtils;
import utils.FireUtils;
import utils.HouseUtils;
import utils.SyncOutput;
import jade.wrapper.ContainerController;

public class Main {
    private static final int DEFAULT_WIDTH = 12;
    private static final int DEFAULT_HEIGHT = 12;
    private static final int DEFAULT_HOUSES = 5;
    private static final int DEFAULT_NUM_TREES = 100;
    private static final int DEFAULT_CLUSTERS = 4;
    private static final int DEFAULT_CLUSTER_SIZE = 30;
    private static final int DEFAULT_CLUSTER_RADIUS = 3;

    private static int parseArg(String[] args, int index, int defaultValue) {
        if (args.length > index) {
            try {
                return Integer.parseInt(args[index]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid argument at index " + index + ", using default: " + defaultValue);
            }
        }
        return defaultValue;
    }

    public static void main(String[] args) throws InterruptedException {
        try {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            PrintStream fileOut = new PrintStream(new FileOutputStream("output_" + timestamp + ".txt"));

            System.setOut(fileOut); // Redirect standard output to the file
            System.setErr(fileOut); // (Optional) Redirect error output too
        } catch (FileNotFoundException e) {
            System.err.println("❌ Could not redirect output: " + e.getMessage());
        }

        int width = parseArg(args, 0, DEFAULT_WIDTH);
        int height = parseArg(args, 1, DEFAULT_HEIGHT);
        int numberOfHouses = parseArg(args, 2, DEFAULT_HOUSES);
        int numTrees = parseArg(args, 3, DEFAULT_NUM_TREES);
        int clusters = parseArg(args, 4, DEFAULT_CLUSTERS);
        int clusterSize = parseArg(args, 5, DEFAULT_CLUSTER_SIZE);
        int clusterRadius = parseArg(args, 6, DEFAULT_CLUSTER_RADIUS);

        MapGrid map = new MapGrid(width, height);
        int droneStartX = 0, droneStartY = 0;
        List<int[]> firefighterPositions = new ArrayList<>();
        firefighterPositions.add(new int[] { 0, height - 1 });
        firefighterPositions.add(new int[] { height - 1, 0 });

        ForestUtils.generateForest(map, numTrees);

        ForestUtils.generateForestClusters(map, clusters, clusterSize, clusterRadius);
        HouseUtils.generateHouses(map, numberOfHouses);
        List<int[]> houseLocations = map.getHouseLocations();

        map.setSpreadProbability(0.1); // 60% chance of spread
        map.igniteRandomFires(2); // Start with 5 random fires
        map.startFireSpreading(10000); // Spread fire every 1000ms (1 sec)

        map.getCell(droneStartX, droneStartY).isForest = false;
        map.getCell(droneStartX, droneStartY).isHouse = false;
        for (int[] pos : firefighterPositions) {
            System.out.println("🚒 Firefighter starting position: (" + pos[0] + "," + pos[1] + ")");
            map.getCell(pos[0], pos[1]).isForest = false;
        }

        FireUtils.igniteRandomForestCell(map, new Random(),
                droneStartX, droneStartY,
                firefighterPositions,
                houseLocations);

        map.printMap();
        map.spreadFire();
        System.out.println();
        launchJadeAgents(map, droneStartX, droneStartY, firefighterPositions);

    }

    private static void launchJadeAgents(MapGrid map, int droneX, int droneY, List<int[]> firefighterPositions) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.LOCAL_PORT, "2002");
        // change from 1099
        AgentContainer container = rt.createMainContainer(p);

        try {
            container.createNewAgent("DataCenter", "agents.DataCenterAgent", null).start();
            container.createNewAgent("Drone", "agents.DroneAgent", new Object[] { map, droneX, droneY }).start();

            for (int i = 0; i < firefighterPositions.size(); i++) {
                int[] pos = firefighterPositions.get(i);
                String agentName = "Firefighter" + (i + 1);
                Object[] args = new Object[] { map, pos[0], pos[1] };
                container.createNewAgent(agentName, "agents.FirefighterAgent", args).start();
            }

            List<int[]> houseLocations = map.getHouseLocations();
            for (int i = 0; i < houseLocations.size(); i++) {
                int[] coords = houseLocations.get(i);
                String agentName = "Homeowner" + (i + 1);
                Object[] args = new Object[] { map, coords[0], coords[1] };
                container.createNewAgent(agentName, "agents.HomeOwnerAgent", args).start();
                SyncOutput.println("👤 Created " + agentName + " for house at (" + coords[0] + "," + coords[1] + ")");
            }

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

}
