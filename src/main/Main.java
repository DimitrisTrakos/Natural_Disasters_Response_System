package main;

import java.util.List;
import java.util.Random;

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
        int width = parseArg(args, 0, DEFAULT_WIDTH);
        int height = parseArg(args, 1, DEFAULT_HEIGHT);
        int numberOfHouses = parseArg(args, 2, DEFAULT_HOUSES);
        int numTrees = parseArg(args, 3, DEFAULT_NUM_TREES);
        int clusters = parseArg(args, 4, DEFAULT_CLUSTERS);
        int clusterSize = parseArg(args, 5, DEFAULT_CLUSTER_SIZE);
        int clusterRadius = parseArg(args, 6, DEFAULT_CLUSTER_RADIUS);
        
        MapGrid map = new MapGrid(width, height);
        int droneStartX = 0, droneStartY = 0;        
        int firefighterStartX = 0, firefighterStartY = height - 1; 

        ForestUtils.generateForest(map, numTrees);

        ForestUtils.generateForestClusters(map, clusters, clusterSize, clusterRadius);
        HouseUtils.generateHouses(map, numberOfHouses);
        List<int[]> houseLocations = map.getHouseLocations();

        map.getCell(droneStartX, droneStartY).isForest = false;
        map.getCell(droneStartX, droneStartY).isHouse = false;
        map.getCell(firefighterStartX, firefighterStartY).isForest = false;
        map.getCell(firefighterStartX, firefighterStartY).isHouse = false;
        FireUtils.igniteRandomForestCell(map, new Random(),
                droneStartX, droneStartY,
                firefighterStartX, firefighterStartY,
                houseLocations);


        
        map.printMap();
        map.spreadFire();
        System.out.println();
        launchJadeAgents(map,droneStartX, droneStartY, firefighterStartX, firefighterStartY);

        

    }

    private static void launchJadeAgents(MapGrid map,int droneX, int droneY, int firefighterX, int firefighterY) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.LOCAL_PORT, "2002");
        // change from 1099
        AgentContainer container = rt.createMainContainer(p);

        try {
            container.createNewAgent("DataCenter", "agents.DataCenterAgent", null).start();
            container.createNewAgent("Drone", "agents.DroneAgent", new Object[]{map, droneX, droneY}).start();
            container.createNewAgent("Firefighter", "agents.FirefighterAgent", new Object[]{map, firefighterX, firefighterY}).start();

            List<int[]> houseLocations = map.getHouseLocations();
            for (int i = 0; i < houseLocations.size(); i++) {
                int[] coords = houseLocations.get(i);
                String agentName = "Homeowner" + (i + 1);
                Object[] args = new Object[]{map, coords[0], coords[1]};
                container.createNewAgent(agentName, "agents.HomeOwnerAgent", args).start();
                System.out.println("👤 Created " + agentName + " for house at (" + coords[0] + "," + coords[1] + ")");
            }

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

}
