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
<<<<<<< HEAD
=======
import jade.wrapper.ContainerController;
>>>>>>> main

public class Main {

    public static void main(String[] args) throws InterruptedException {
        int width = 7;
        int height = 7;

        MapGrid map = new MapGrid(width, height);

        ForestUtils.generateForest(map, 100);

        ForestUtils.generateForestClusters(map, 4, 30, 3);

        FireUtils.igniteRandomForestCell(map, new java.util.Random());
        
        launchJadeAgents(map);

        for (int t = 0; t < 15; t++) {
            System.out.println("Time step: " + t);
            map.printMap();
            map.spreadFire();
            Thread.sleep(1000);
            System.out.println();
        }
        
    }

    private static void launchJadeAgents(MapGrid map) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
<<<<<<< HEAD
        p.setParameter(Profile.LOCAL_PORT, "2000");  // change from 1099
        AgentContainer container = rt.createMainContainer(p);
    
        try {
            container.createNewAgent("DataCenter", "agents.DataCenterAgent", null).start();
            container.createNewAgent("Drone", "agents.DroneAgent", new Object[]{map}).start();
=======
        p.setParameter(Profile.LOCAL_PORT, "2002");
        // change from 1099
        AgentContainer container = rt.createMainContainer(p);

        try {
            container.createNewAgent("DataCenter", "agents.DataCenterAgent", null).start();
            container.createNewAgent("Drone", "agents.DroneAgent", new Object[]{map}).start();
            container.createNewAgent("Firefighter", "agents.FirefighterAgent", new Object[]{map}).start();

>>>>>>> main
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
    
<<<<<<< HEAD
}
=======
}
>>>>>>> main
