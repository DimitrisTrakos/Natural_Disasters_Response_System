package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import mapGrid.MapGrid;
import mapGrid.GridCell;

public class DroneAgent extends Agent {

    private MapGrid map;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " launched.");

        // Retrieve the map object passed from the main method
        Object[] args = getArguments();
        if (args != null && args.length > 0 && args[0] instanceof MapGrid) {
            this.map = (MapGrid) args[0];
            System.out.println(getLocalName() + ": Map received.");
        } else {
            System.out.println(getLocalName() + ": No map received.");
            doDelete();
            return;
        }

        // Add behaviour to scan the grid and report fire locations
        addBehaviour(new CyclicBehaviour() {
            private int step = 0;

            @Override
            public void action() {
                // Every few cycles, scan the map for fires
                if (step % 5 == 0) { // every 5th step
                    StringBuilder fireReport = new StringBuilder();
                    for (int y = 0; y < map.getHeight(); y++) {
                        for (int x = 0; x < map.getWidth(); x++) {
                            GridCell cell = map.getCell(x, y);
                            if (cell != null && cell.isOnFire) { // Check if the cell is on fire
                                fireReport.append("Fire detected at: (" + x + "," + y + ")\n");
                            }
                        }
                    }

                    // If fires are detected, send a message to the DataCenterAgent
                    if (fireReport.length() > 0) {
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(new jade.core.AID("DataCenter", jade.core.AID.ISLOCALNAME)); // Sending to DataCenter
                        msg.setContent(fireReport.toString()); // Include the fire locations in the message
                        send(msg);
                        System.out.println(getLocalName() + " sent fire report to DataCenter.");
                    } else {
                        System.out.println(getLocalName() + ": No fires detected.");
                    }
                }
                step++;
                block(1000); // wait 1 second before the next check
            }
        });
    }
}
