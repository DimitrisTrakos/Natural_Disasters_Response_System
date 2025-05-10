package main;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import mapGrid.MapGrid;
import utils.ForestUtils;
import utils.FireUtils;

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
        launchJadeAgents(map);
    }

    private static void launchJadeAgents(MapGrid map) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        AgentContainer container = rt.createMainContainer(p);

        try {
            container.createNewAgent("DataCenter", "agents.DataCenterAgent", null).start();
            container.createNewAgent("Drone", "agents.DroneAgent", new Object[]{map}).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
