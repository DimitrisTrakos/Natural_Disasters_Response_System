package agents;

import java.util.Random;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import mapGrid.MapGrid;
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

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " launched.");

        // DF Registration
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("drone");
        sd.setName("Fire-Scanner");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " registered with DF.");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " DF registration failed.");
            e.printStackTrace();
        }

        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof MapGrid) {
            this.map = (MapGrid) args[0];
            System.out.println(getLocalName() + ": Map received.");
        } else {
            System.out.println(getLocalName() + ": No map received.");
            doDelete();
            return;
        }

        this.x = 0;
        this.y = 0;
        System.out.println(getLocalName() + " positioned at (" + x + "," + y + ")");
        updateMap();

        addBehaviour(new CyclicBehaviour() {
            private int step = 0;

            @Override
            public void action() {
                moveZigZag();
                updateMap();

                if (step % 5 == 0) {
                    StringBuilder fireReport = new StringBuilder();

                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0)
                                continue;

                            int neighborX = x + dx;
                            int neighborY = y + dy;

                            if (neighborX >= 0 && neighborX < map.getWidth() &&
                                    neighborY >= 0 && neighborY < map.getHeight()) {

                                GridCell neighborCell = map.getCell(neighborX, neighborY);
                                if (neighborCell != null && neighborCell.isOnFire) {
                                    fireReport.append("Fire detected at: (" + neighborX + "," + neighborY + ")\n");
                                }
                            }
                        }
                    }

                    if (fireReport.length() > 0) {
                        if (dataCenterAID == null) {
                            // Search for the DataCenter agent only once or if null
                            DFAgentDescription template = new DFAgentDescription();
                            ServiceDescription sd = new ServiceDescription();
                            sd.setType("data-center");
                            template.addServices(sd);
                            try {
                                DFAgentDescription[] result = DFService.search(myAgent, template);
                                if (result.length > 0) {
                                    dataCenterAID = result[0].getName();
                                    System.out.println(getLocalName() + " found DataCenter agent: "
                                            + dataCenterAID.getLocalName());
                                } else {
                                    System.out.println(getLocalName() + ": No DataCenter found.");
                                }
                            } catch (FIPAException e) {
                                e.printStackTrace();
                            }
                        }

                        if (dataCenterAID != null) {
                            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                            msg.addReceiver(dataCenterAID);
                            msg.setContent(fireReport.toString());
                            send(msg);
                            System.out.println(getLocalName() + " sent fire report to " + dataCenterAID.getLocalName());
                        }
                    } else {
                        System.out.println(getLocalName() + ": No fires detected.");
                    }
                }
                step++;
                block(1000);
            }

            private boolean scanningDown = true; // true = top to bottom, false = bottom to top

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
                                // Bottom reached: start scanning up
                                scanningDown = false;
                                movingRight = false;
                                return;
                            }
                        } else {
                            if (y - 1 >= 0) {
                                newY = y - 1;
                                movingRight = false;
                            } else {
                                // Top reached: start scanning down
                                scanningDown = true;
                                movingRight = false;
                                return;
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
                                // Bottom reached: start scanning up
                                scanningDown = false;
                                movingRight = true;
                                return;
                            }
                        } else {
                            if (y - 1 >= 0) {
                                newY = y - 1;
                                movingRight = true;
                            } else {
                                // Top reached: start scanning down
                                scanningDown = true;
                                movingRight = true;
                                return;
                            }
                        }
                    }
                }
            
                clearOldPosition();  // this will respect firefighter presence (handled separately)
                x = newX;
                y = newY;
                System.out.println(getLocalName() + " moved to (" + x + "," + y + ")");
            }
        });
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println(getLocalName() + " deregistered from DF.");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void updateMap() {
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
}
