package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.MessageTemplate;
import java.util.ArrayList;
import mapGrid.MapGrid;
import mapGrid.GridCell;
import utils.AStarPathfinder;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FirefighterAgent extends Agent {
    private MapGrid map;
    private int x, y;
    private Queue<int[]> fireQueue = new LinkedList<>();
    private List<int[]> pathToFire = new ArrayList<>();
    private boolean moving = false;

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

        // Listen for fire reports
        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                ACLMessage msg = receive(mt);
                if (msg != null) {
                    System.out.println(getLocalName() + " received fire alert:\n" + msg.getContent());

                    for (String line : msg.getContent().split("\n")) {
                        if (line.startsWith("Fire detected at:")) {
                            String[] coords = line.replaceAll("[^0-9,]", "").split(",");
                            int fx = Integer.parseInt(coords[0]);
                            int fy = Integer.parseInt(coords[1]);
                            fireQueue.add(new int[]{fx, fy});
                        }
                    }
                }

                // Move along path if set
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
                        current.isOnFire = false;
                        System.out.println(getLocalName() + " extinguished fire at (" + x + "," + y + ")");
                    }
                }

                // Plan new path if needed
                if (pathToFire.isEmpty() && !fireQueue.isEmpty()) {
                    int[] firePos = fireQueue.poll();
                    pathToFire = AStarPathfinder.findPath(map, x, y, firePos[0], firePos[1]);
                    if (pathToFire.isEmpty()) {
                        System.out.println(getLocalName() + ": No path to fire at (" + firePos[0] + "," + firePos[1] + ")");
                    } else {
                        System.out.println(getLocalName() + ": Path to fire (" + firePos[0] + "," + firePos[1] + ")" + "calculated.");
                    }
                }

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