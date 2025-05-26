package agents;

import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import agents.DataCenterAgent.FireReport;
import mapGrid.MapGrid;
import mapGrid.GridCell;
import utils.AStarPathfinder;
import utils.SyncOutput;

public class FirefighterAgent extends Agent {

    private MapGrid map;
    private int x, y; 
    private  int startX;  
    private  int startY;     
    private Stack<int[]> pathToFire = new Stack<>();
    private boolean isExtinguishing = false;
    private boolean skipNextStep = false;
    private boolean returningHome = false;
    private String id;

    @Override
    protected void setup() {
        SyncOutput.println("🚒 Firefighter " + getLocalName() + " initializing...");

        Object[] args = getArguments();
        if (args != null && args.length > 2 && args[0] instanceof MapGrid) {
            this.map = (MapGrid) args[0];
            this.x = (int) args[1]; 
            this.y = (int) args[2]; 
            this.startX = x;         
            this.startY = y;
            this.id = getLocalName();
        
        } else {
            System.err.println("❌ Error: No map provided!");
            doDelete();
            return;
        }

        x = startX;
        y = startY;
        updateMapPosition();
        SyncOutput.println("🚒 Starting at position (" + startX + "," + startY + ")");

        registerWithDF();


        addBehaviour(new TickerBehaviour(this, 1000) { 
            @Override
            protected void onTick() {
                if (skipNextStep) {
                    skipNextStep = false;
                    return;
                }

                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                if (msg != null) {
                    if (msg.getContent().startsWith("TARGET:")) {
                        handleTargetMessage(msg.getContent());
                    } else if (msg.getContent().equals("RETURN_HOME")) {
                        returnToStartPosition();
                    }
                }

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
        
        SyncOutput.println("\n🚒 New target received: (" + targetX + "," + targetY + ")");
        calculatePathTo(targetX, targetY);
        returningHome = false;
    }

    private void returnToStartPosition() {
        if (x == startX && y == startY) {
            SyncOutput.println("🚒 Already at starting position");
            return;
        }
        
        SyncOutput.println("\n🚒 ORDERED TO RETURN HOME");
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
        for (int i = path.size()-1; i >= 0; i--) {
            pathToFire.push(path.get(i));
        }
        SyncOutput.println("🚒 Path calculated (" + path.size() + " steps)");
    }

    private void moveToNextPosition() {
        // Clear current position first
        GridCell currentCell = map.getCell(x, y);
        if (currentCell != null) {
            currentCell.hasAgent = false;
            currentCell.agentType = "";
        }
        
        int[] next = pathToFire.pop();
        int nextX = next[0];
        int nextY = next[1];
        
        
        GridCell nextCell = map.getCell(nextX, nextY);
        if (nextCell != null && nextCell.isOnFire) {
            x = nextX;
            y = nextY;
            updateMapPosition();
            extinguishFire();
            pathToFire.clear();
            return;
        }
        
        x = nextX;
        y = nextY;
        updateMapPosition();
        SyncOutput.println("🚒 Moved to (" + x + "," + y + ")");
        
        map.printMap();
        
        if (pathToFire.isEmpty()) {
            if (returningHome) {
                SyncOutput.println("🚒 Reached starting position");
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
            SyncOutput.println("ℹ Reached target position (no fire found)");
            sendExtinguishedUpdate(x, y);
        }
    }

    private void extinguishFire() {
        isExtinguishing = true;
        skipNextStep = true;  
        
        GridCell cell = map.getCell(x, y);
        cell.isOnFire = false;
        cell.fireFighterExtinguishFire = true;
        
        SyncOutput.println("🚒 Fire extinguished at (" + x + "," + y + ")");
        sendExtinguishedUpdate(x, y);
    }

    private void updateMapPosition() {
        GridCell cell = map.getCell(x, y);
        if (cell != null) {
            cell.hasAgent = true;
            cell.agentType = "firefighter";
        }
    }

    private void sendPositionUpdate() {
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("POSITION:" + id + "," + x + "," + y);
            
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
            msg.setContent("EXTINGUISHED:" + x + "," + y + "," + id);

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
            System.out.println("🚒  Firefighter " + getLocalName() + " shutting down");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}