package agents;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import mapGrid.MapGrid;
import mapGrid.GridCell;

public class HomeOwnerAgent extends Agent {

    private MapGrid map;
    private int homeX, homeY;  
    private jade.core.AID dataCenterAID;
    private boolean fireReported = false;

    @Override
    protected void setup() {
        System.out.println("🏠 " + getLocalName() + " initialized");

        Object[] args = getArguments();
        if (args != null && args.length > 2 && args[0] instanceof MapGrid) {
            map = (MapGrid) args[0];
            homeX = (int) args[1];
            homeY = (int) args[2];
            System.out.println("📍 Home located at (" + homeX + "," + homeY + ")");
        } else {
            System.err.println("❌ Error: Missing home coordinates");
            doDelete();
            return;
        }

        // Register with DF
        registerWithDF();

        // Add monitoring behavior (checks every 2 seconds)
        addBehaviour(new TickerBehaviour(this, 2000) {
            @Override
            protected void onTick() {
                checkForNearbyFires();
            }
        });
    }

    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("homeowner");
        sd.setName("Home-Monitor");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void checkForNearbyFires() {
        boolean fireDetected = false;
        StringBuilder dangerReport = new StringBuilder();

        for (int dy = -1; dy <=0; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int checkX = homeX + dx;
                int checkY = homeY + dy;

                if (map.inBounds(checkX, checkY)) {
                    GridCell cell = map.getCell(checkX, checkY);
                    if (cell != null && cell.isOnFire) {
                        fireDetected = true;
                        dangerReport.append("Fire at (").append(checkX).append(",")
                                   .append(checkY).append(") - ")
                                   .append(dx == 0 && dy == 0 ? "INSIDE HOME!" : "Near home")
                                   .append("\n");
                    }
                }
            }
        }

        if (fireDetected && !fireReported) {
            sendFireAlert(dangerReport.toString());
            fireReported = true;
        } else if (!fireDetected) {
            fireReported = false;  
        }
    }

    private void sendFireAlert(String dangerInfo) {
        if (dataCenterAID == null) {
            findDataCenter();
        }

        if (dataCenterAID != null) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(dataCenterAID);
            msg.setContent("HOME_DANGER:" + homeX + "," + homeY + "\n" + dangerInfo);
            send(msg);
            System.out.println("👤 Sent fire alert to DataCenter:\n" + dangerInfo);
        }
    }

    private void findDataCenter() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("data-center");
        template.addServices(sd);

        try {
            DFAgentDescription[] result = DFService.search(this, template);
            if (result.length > 0) {
                dataCenterAID = result[0].getName();
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("👤 " + getLocalName() + " shutting down");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
