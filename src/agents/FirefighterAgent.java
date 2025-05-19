package agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import java.util.ArrayList;
import java.util.List;
import mapGrid.MapGrid;
import mapGrid.GridCell;
import utils.AStarPathfinder;

public class FirefighterAgent extends Agent {
    private MapGrid map;
    private int x, y;
    private List<int[]> fireList = new ArrayList<>();
    private List<int[]> pathToFire = new ArrayList<>();
    private boolean pauseAfterFire = false;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " launched.");

        // DF registration
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("firefighter");
        sd.setName("Fire-Responder");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        // Read map from arguments
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof MapGrid) {
            this.map = (MapGrid) args[0];
        } else {
            System.out.println("No map provided.");
            doDelete();
            return;
        }

        this.x = 0;
        this.y = map.getHeight() - 1; // Start at bottom-left corner
        System.out.println(getLocalName() + " positioned at (" + x + "," + y + ")");
        map.getCell(x, y).hasAgent = true;
        map.getCell(x, y).agentType = "firefighter";

        // Behavior to handle fire reports and movement
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                if (pauseAfterFire) {
                    pauseAfterFire = false;
                    System.out.println(getLocalName() + " is recovering after extinguishing fire...");
                    map.printMap();
                    return;
                }

                // Receive fire alerts
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    System.out.println(getLocalName() + " received fire alert:\n" + msg.getContent());

                    for (String line : msg.getContent().split("\n")) {
                        if (line.startsWith("Fire detected at:")) {
                            String[] coords = line.replaceAll("[^0-9,]", "").split(",");
                            int fx = Integer.parseInt(coords[0]);
                            int fy = Integer.parseInt(coords[1]);

                            boolean alreadyReported = fireList.stream()
                                    .anyMatch(f -> f[0] == fx && f[1] == fy);

                            if (!alreadyReported) {
                                fireList.add(new int[]{fx, fy});
                            }
                        }
                    }
                }

                // Move along path if we have one
                if (!pathToFire.isEmpty()) {
                    map.getCell(x, y).hasAgent = false;
                    map.getCell(x, y).agentType = "";

                    int[] next = pathToFire.remove(0);
                    x = next[0];
                    y = next[1];
                    GridCell current = map.getCell(x, y);
                    current.hasAgent = true;
                    current.agentType = "firefighter";

                    System.out.println(getLocalName() + " moved to (" + x + "," + y + ")");

                    if (current.isOnFire) {
                        pauseAfterFire = true;
                        System.out.println(getLocalName() + " extinguished fire at (" + x + "," + y + ")");
                        current.isOnFire = false;
                        current.fireFighterExtinguishFire= true;

                    }
                }

                // Plan new path to nearest fire
                if (pathToFire.isEmpty() && !fireList.isEmpty()) {
                    int[] nearestFire = null;
                    int shortestDistance = Integer.MAX_VALUE;

                    for (int[] firePos : fireList) {
                        int distance = Math.abs(firePos[0] - x) + Math.abs(firePos[1] - y); // Manhattan Distance
                        if (distance < shortestDistance) {
                            shortestDistance = distance;
                            nearestFire = firePos;
                        }
                    }

                    if (nearestFire != null) {
                        pathToFire = AStarPathfinder.findPath(map, x, y, nearestFire[0], nearestFire[1]);
                        fireList.remove(nearestFire);

                        if (pathToFire.isEmpty()) {
                            System.out.println(getLocalName() + ": No path to fire at (" + nearestFire[0] + "," + nearestFire[1] + ")");
                        } else {
                            System.out.println(getLocalName() + ": Path to fire (" + nearestFire[0] + "," + nearestFire[1] + ") calculated.");
                        }
                    }
                }

                // Print updated map
                map.printMap();
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}