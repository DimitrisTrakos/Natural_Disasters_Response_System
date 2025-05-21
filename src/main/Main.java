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

    public static void main(String[] args) throws InterruptedException {
        int width = 10;
        int height = 10;
        int numberOfHouses = 3;
        
        MapGrid map = new MapGrid(width, height);
        int droneStartX = 0, droneStartY = 0;        
        int firefighterStartX = 0, firefighterStartY = height - 1; 

        ForestUtils.generateForest(map, 100);

        ForestUtils.generateForestClusters(map, 4, 30, 3);
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

        launchJadeAgents(map,droneStartX, droneStartY, firefighterStartX, firefighterStartY);

        
        map.printMap();
        map.spreadFire();
        System.out.println();
        

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
