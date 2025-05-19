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
    private int x, y;
    private Stack<int[]> pathToFire = new Stack<>();
    private boolean isExtinguishing = false;
    private boolean skipNextStep = false;  
    @Override
    protected void setup() {
        System.out.println("🚒 " + getLocalName() + " launched.");

        // DF Registration
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("firefighter");
        sd.setName("Fire-Responder");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println("📝 " + getLocalName() + " registered with DF");
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof MapGrid) {
            map = (MapGrid) args[0];
        } else {
            System.err.println("❌ " + getLocalName() + ": No map provided - terminating");
            doDelete();
            return;
        }

        x = 0;
        y = map.getHeight() - 1;
        updateMapPosition();
        System.out.println("📍 Initial position: (" + x + "," + y + ")");

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                if (skipNextStep) {
                    System.out.println("⏸️ Recovering after extinguishing fire...");
                    skipNextStep = false;
                    return;
                }

                if (isExtinguishing) {
                    isExtinguishing = false;
                    skipNextStep = true;  
                    return;
                }

                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null && msg.getContent().startsWith("TARGET:")) {
                    handleNewTarget(msg.getContent());
                }

                if (!pathToFire.isEmpty()) {
                    moveAlongPath();
                } else {
                    sendPositionUpdate();
                }
            }
        });
    }

    private void updateMapPosition() {
        GridCell cell = map.getCell(x, y);
        cell.hasAgent = true;
        cell.agentType = "firefighter";
    }

    private void handleNewTarget(String targetMsg) {
        String[] parts = targetMsg.replace("TARGET:", "").split(",");
        int targetX = Integer.parseInt(parts[0]);
        int targetY = Integer.parseInt(parts[1]);
        
        System.out.printf("\n🎯 NEW TARGET: (%d,%d) | Current: (%d,%d)%n", 
            targetX, targetY, x, y);
        
        List<int[]> path = AStarPathfinder.findPath(map, x, y, targetX, targetY);
        if (!path.isEmpty()) {
            pathToFire.clear();
            for (int i = path.size()-1; i >= 0; i--) {
                pathToFire.push(path.get(i));
            }
            System.out.printf("🛣️  Path calculated (%d steps): %s%n", 
                path.size(), formatPath(path));
        } else {
            System.err.println("⚠ ERROR: No valid path to target!");
        }
    }

    private String formatPath(List<int[]> path) {
        StringBuilder sb = new StringBuilder();
        for (int[] coord : path) {
            sb.append("(").append(coord[0]).append(",").append(coord[1]).append(") ");
        }
        return sb.toString().trim();
    }

    private void moveAlongPath() {
        map.getCell(x, y).hasAgent = false;
        map.getCell(x, y).agentType = "";

        int[] next = pathToFire.pop();
        System.out.printf("\n🚒 MOVING: (%d,%d) → (%d,%d)%n", 
            x, y, next[0], next[1]);
        x = next[0];
        y = next[1];
        updateMapPosition();

        GridCell current = map.getCell(x, y);
        if (current.isOnFire) {
            extinguishFire();
        } else if (pathToFire.isEmpty()) {
            System.out.println("✅ Reached target position");
            sendExtinguishedUpdate(x, y);
        }
    }

    private void extinguishFire() {
        isExtinguishing = true;
        GridCell cell = map.getCell(x, y);
        cell.isOnFire = false;
        cell.fireFighterExtinguishFire = true;
        System.out.printf("\n🧯 EXTINGUISHED FIRE at (%d,%d)%n", x, y);
        sendExtinguishedUpdate(x, y);
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
            System.out.println("🛑 " + getLocalName() + " shutting down");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}