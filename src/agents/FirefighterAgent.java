package agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.util.List;
import java.util.Stack;

import mapGrid.MapGrid;
import mapGrid.GridCell;
import utils.AStarPathfinder;

public class FirefighterAgent extends Agent {

    private MapGrid map;
    private int x, y;  // Current position
    private  int startX = 0;  // Starting X position (always 0)
    private  int startY;      // Starting Y position (bottom of map)
    private Stack<int[]> pathToFire = new Stack<>();
    private boolean isExtinguishing = false;
    private boolean skipNextStep = false;
    private boolean returningHome = false;

    @Override
    protected void setup() {
        System.out.println("🚒 Firefighter " + getLocalName() + " initializing...");

        // Initialize starting position
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof MapGrid) {
            map = (MapGrid) args[0];
            startY = map.getHeight() - 1;  
        } else {
            System.err.println("❌ Error: No map provided!");
            doDelete();
            return;
        }

        // Set initial position
        x = startX;
        y = startY;
        updateMapPosition();
        System.out.println("📍 Starting at position (" + startX + "," + startY + ")");

        // Register with DF
        registerWithDF();

        // Main behavior
        addBehaviour(new TickerBehaviour(this, 1000) {  // Check every second
            @Override
            protected void onTick() {
                // Handle pause after extinguishing
                if (skipNextStep) {
                    skipNextStep = false;
                    return;
                }

                // Process messages
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null) {
                    if (msg.getContent().startsWith("TARGET:")) {
                        handleTargetMessage(msg.getContent());
                    } else if (msg.getContent().equals("RETURN_HOME")) {
                        returnToStartPosition();
                    }
                }

                // Movement logic
                if (!pathToFire.isEmpty()) {
                    moveToNextPosition();
                } else if (!returningHome) {
                    sendPositionUpdate();
                }
            }
        });
    }

    private void registerWithDF() {
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
    }

    private void handleTargetMessage(String content) {
        String[] coords = content.replace("TARGET:", "").split(",");
        int targetX = Integer.parseInt(coords[0]);
        int targetY = Integer.parseInt(coords[1]);
        
        System.out.println("\n🎯 New target received: (" + targetX + "," + targetY + ")");
        calculatePathTo(targetX, targetY);
        returningHome = false;
    }

    private void returnToStartPosition() {
        if (x == startX && y == startY) {
            System.out.println("ℹ Already at starting position");
            return;
        }
        
        System.out.println("\n🏠 ORDERED TO RETURN HOME");
        returningHome = true;
        calculatePathTo(startX, startY);
    }

    private void calculatePathTo(int targetX, int targetY) {
        List<int[]> path = AStarPathfinder.findPath(map, x, y, targetX, targetY);
        
        if (path.isEmpty()) {
            System.err.println("⚠ No valid path to (" + targetX + "," + targetY + ")");
            return;
        }
        
        pathToFire.clear();
        // Reverse to use as stack
        for (int i = path.size()-1; i >= 0; i--) {
            pathToFire.push(path.get(i));
        }
        System.out.println("🛣️  Path calculated (" + path.size() + " steps)");
    }

    private void moveToNextPosition() {
        int[] next = pathToFire.pop();
        
        // Clear current position
        map.getCell(x, y).hasAgent = false;
        map.getCell(x, y).agentType = "";
        
        // Update position
        x = next[0];
        y = next[1];
        updateMapPosition();
        System.out.println("➡ Moved to (" + x + "," + y + ")");

        // Handle arrival
        if (pathToFire.isEmpty()) {
            if (returningHome) {
                System.out.println("✅ Reached starting position");
                returningHome = false;
            } else {
                handleArrivalAtTarget();
            }
        }
    }

    private void handleArrivalAtTarget() {
        GridCell cell = map.getCell(x, y);
        if (cell.isOnFire) {
            extinguishFire();
        } else {
            System.out.println("ℹ Reached target position (no fire found)");
            sendExtinguishedUpdate(x, y);
        }
    }

    private void extinguishFire() {
        isExtinguishing = true;
        skipNextStep = true;  // Pause after extinguishing
        
        GridCell cell = map.getCell(x, y);
        cell.isOnFire = false;
        cell.fireFighterExtinguishFire = true;
        
        System.out.println("🧯 Fire extinguished at (" + x + "," + y + ")");
        sendExtinguishedUpdate(x, y);
    }

    private void updateMapPosition() {
        GridCell cell = map.getCell(x, y);
        cell.hasAgent = true;
        cell.agentType = "firefighter";
    }

    private void sendPositionUpdate() {
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("POSITION:" + x + "," + y);
            
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("data-center");
            template.addServices(sd);

            for (DFAgentDescription desc : DFService.search(this, template)) {
                msg.addReceiver(desc.getName());
            }
            send(msg);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void sendExtinguishedUpdate(int x, int y) {
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("EXTINGUISHED:" + x + "," + y);
            
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("data-center");
            template.addServices(sd);

            for (DFAgentDescription desc : DFService.search(this, template)) {
                msg.addReceiver(desc.getName());
            }
            send(msg);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("🛑 Firefighter " + getLocalName() + " shutting down");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}