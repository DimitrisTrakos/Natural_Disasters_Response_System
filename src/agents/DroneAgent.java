package agents;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import mapGrid.MapGrid;
import utils.SyncOutput;
import mapGrid.GridCell;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
public class DroneAgent extends Agent {

    private MapGrid map;
    private int x, y;
    private boolean movingRight = true;
    private jade.core.AID dataCenterAID;
    private int scanCounter = 0;
    private Set<String> reportedFires = new HashSet<>();

    @Override
    protected void setup() {
        SyncOutput.println("🚁 " + getLocalName() + " launched and initializing...");

        registerWithDF();

        initializePosition();
        
        addBehaviour(new DroneScanningBehaviour());
    }

    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("drone");
        sd.setName("Fire-Scanner");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            SyncOutput.println("📝 " + getLocalName() + " successfully registered with DF");
        } catch (FIPAException e) {
            System.err.println("❌ " + getLocalName() + " DF registration failed");
            e.printStackTrace();
        }
    }

    private void initializePosition() {
        Object[] args = getArguments();
        if (args != null && args.length > 2 && args[0] instanceof MapGrid) {
            this.map = (MapGrid) args[0];
            this.x = (int) args[1]; 
            this.y = (int) args[2];
            updateMapPosition();
            SyncOutput.println("🚁 " + getLocalName() + " initialized at position (" + x + "," + y + ")");
        } else {
            SyncOutput.println("❌ " + getLocalName() + ": No valid map received - terminating");
            doDelete();
        }
    }

    private class DroneScanningBehaviour extends CyclicBehaviour {
        private boolean scanningDown = true;

        @Override
        public void action() {
            moveZigZag();
            
            if (scanCounter % 2 == 0) {
                scanForFires();
            }
            scanCounter++;
            
            block(1000); 
        }

        private void scanForFires() {
            StringBuilder fireReport = new StringBuilder();
            int newFiresDetected = 0;
            int duplicateFires = 0;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;

                    int scanX = x + dx;
                    int scanY = y + dy;

                    if (isValidPosition(scanX, scanY)) {
                        GridCell cell = map.getCell(scanX, scanY);
                        
                        if (cell != null && cell.isOnFire) {
                            String fireKey = scanX + "," + scanY;
                            
                            if (!reportedFires.contains(fireKey)) {
                                // New fire detected
                                fireReport.append(scanX).append(",")
                                          .append(scanY).append(",")
                                          .append(cell.isHouse ? "true" : "false")
                                          .append("\n");
                                reportedFires.add(fireKey);
                                newFiresDetected++;
                                SyncOutput.println("🚁 NEW FIRE at (" + scanX + "," + scanY + 
                                                 ") - " + (cell.isHouse ? "House Fire!" : "Regular Fire"));
                            } else {
                                duplicateFires++;
                                SyncOutput.println("🚁 Already reported fire at (" + scanX + "," + scanY + ")");
                            }
                        }
                    }
                }
            }

            if (newFiresDetected > 0) {
                sendFireReport(fireReport.toString(), newFiresDetected);
                SyncOutput.println("🚁 Fire Report: " + newFiresDetected + " new | " + 
                                 duplicateFires + " duplicates");
            } else if (duplicateFires > 0) {
                SyncOutput.println("🚁 Scan complete - " + duplicateFires + " known fires in vicinity");
            } else {
                SyncOutput.println("🚁 Scan complete - No fires detected");
            }
        }

        private void sendFireReport(String report, int fireCount) {
            if (dataCenterAID == null) {
                findDataCenter();
            }

            if (dataCenterAID != null) {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.addReceiver(dataCenterAID);
                msg.setContent(report);
                send(msg);
                SyncOutput.println("🚁 Sent report with " + fireCount + " fires to DataCenter");
            } else {
                SyncOutput.println("⚠ Couldn't send fire report - DataCenter unavailable");
            }
        }

        private void findDataCenter() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("data-center");
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                if (result.length > 0) {
                    dataCenterAID = result[0].getName();
                    System.out.println("🚁 Connected to DataCenter: " + dataCenterAID.getLocalName());
                }
            } catch (FIPAException e) {
                System.err.println("❌ Error searching for DataCenter");
                e.printStackTrace();
            }
        }

        private void moveZigZag() {
            int newX = x;
            int newY = y;

            if (movingRight) {
                if (x + 1 < map.getWidth()) {
                    newX = x + 1;
                } else {
                    if (scanningDown) {
                        if (y + 1 < map.getHeight()) {
                            newY = y + 1;
                            movingRight = false;
                        } else {
                            scanningDown = false;
                            movingRight = false;
                        }
                    } else {
                        if (y - 1 >= 0) {
                            newY = y - 1;
                            movingRight = false;
                        } else {
                            scanningDown = true;
                            movingRight = false;
                        }
                    }
                }
            } else {
                if (x - 1 >= 0) {
                    newX = x - 1;
                } else {
                    if (scanningDown) {
                        if (y + 1 < map.getHeight()) {
                            newY = y + 1;
                            movingRight = true;
                        } else {
                            scanningDown = false;
                            movingRight = true;
                        }
                    } else {
                        if (y - 1 >= 0) {
                            newY = y - 1;
                            movingRight = true;
                        } else {
                            scanningDown = true;
                            movingRight = true;
                        }
                    }
                }
            }

            clearOldPosition();
            x = newX;
            y = newY;
            updateMapPosition();
            try{
                Thread.sleep(10);
                SyncOutput.println("🚁 Moved to (" + x + "," + y + ")");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isValidPosition(int x, int y) {
        return x >= 0 && x < map.getWidth() && y >= 0 && y < map.getHeight();
    }

    private void updateMapPosition() {
        GridCell cell = map.getCell(x, y);
        if (!"firefighter".equals(cell.agentType)) {
            cell.hasAgent = true;
            cell.agentType = "drone";
        }
        map.printMap();
    }

    private void clearOldPosition() {
        GridCell oldCell = map.getCell(x, y);
        if (oldCell != null && !"firefighter".equals(oldCell.agentType)) {
            oldCell.hasAgent = false;
            oldCell.agentType = "";
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("🛑 " + getLocalName() + " shutting down and deregistering from DF");
        } catch (FIPAException e) {
            System.err.println("❌ Error during shutdown");
            e.printStackTrace();
        }
    }
}