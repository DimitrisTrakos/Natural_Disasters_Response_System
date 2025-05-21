package agents;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.core.behaviours.CyclicBehaviour;

import java.util.*;

public class DataCenterAgent extends Agent {

    private List<FireReport> fireReports = new ArrayList<>();
    private int firefighterX = -1;
    private int firefighterY = -1;

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " launched.");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("data-center");
        sd.setName("Fire-Data-Service");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " registered with DF.");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " failed to register with DF.");
            e.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    System.out.println(getLocalName() + " received: " + content);

                    if (content.startsWith("POSITION:")) {
                        updateFirefighterPosition(content);
                        sendNextTarget();
                    } else if (content.startsWith("EXTINGUISHED:")) {
                        removeExtinguishedFire(content);
                    } else {
                        processFireReport(content);
                        sendNextTarget();
                    }
                } else {
                    block();
                }
            }
        });
    }

    private void updateFirefighterPosition(String content) {
        String[] parts = content.replace("POSITION:", "").trim().split(",");
        firefighterX = Integer.parseInt(parts[0]);
        firefighterY = Integer.parseInt(parts[1]);
        System.out.println("Updated firefighter position: (" + firefighterX + "," + firefighterY + ")");
    }

    private void processFireReport(String content) {
        System.out.println("\n📊 DATACENTER: Processing fire report...");
        String[] lines = content.split("\n");
        int newFiresAdded = 0;
        int duplicateFires = 0;
    
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length == 3) {
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    boolean isHouse = Boolean.parseBoolean(parts[2].trim());
    
                    boolean fireExists = fireReports.stream()
                        .anyMatch(f -> f.x == x && f.y == y);
    
                    if (!fireExists) {
                        fireReports.add(new FireReport(x, y, isHouse));
                        newFiresAdded++;
                        System.out.printf("➕ NEW FIRE: (%d,%d) | Type: %s%n",
                            x, y, isHouse ? "House Fire 🏠🔥" : "Regular Fire 🔥");
                    } else {
                        duplicateFires++;
                        System.out.printf("♻ DUPLICATE: Fire at (%d,%d) already registered%n", x, y);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("⚠ Invalid fire report format: " + line);
                }
            } else {
                System.err.println("⚠ Malformed fire report: " + line);
            }
        }
    
        System.out.printf("📝 Report Summary: %d new fires | %d duplicates ignored%n", 
            newFiresAdded, duplicateFires);
    
        if (newFiresAdded > 0) {
            sendNextTarget();
        }
    }

    private void removeExtinguishedFire(String content) {
        String[] parts = content.replace("EXTINGUISHED:", "").trim().split(",");
        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());

        fireReports.removeIf(f -> f.x == x && f.y == y);
        System.out.println("Removed extinguished fire at: (" + x + "," + y + ")");
        checkForReturnCommand();
    }

    private void sendNextTarget() {
        if (fireReports.isEmpty()) {
            System.out.println("📊 No active fires in database");
            return;
        }
    
        FireReport nextFire = fireReports.stream()
            .min(Comparator.comparing((FireReport f) -> !f.isHouse)
                .thenComparing(f -> manhattanDistance(f.x, f.y, firefighterX, firefighterY)))
            .orElse(null);
    
        if (nextFire != null) {
            System.out.printf("\n🎯 SELECTED TARGET: (%d,%d) | Priority: %s | Distance: %d%n",
                nextFire.x, nextFire.y,
                nextFire.isHouse ? "HIGH (House Fire)" : "Normal",
                manhattanDistance(nextFire.x, nextFire.y, firefighterX, firefighterY));
    
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("TARGET:" + nextFire.x + "," + nextFire.y);
            
            try {
                // Create the template directly here instead of calling a separate method
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("firefighter");
                template.addServices(sd);
                
                DFAgentDescription[] result = DFService.search(this, template);
                for (DFAgentDescription desc : result) {
                    msg.addReceiver(desc.getName());
                }
                send(msg);
                System.out.println("📤 Sent target to firefighter");
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    private int manhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }

    private void checkForReturnCommand() {
        if (fireReports.isEmpty()) {
            System.out.println("📭 No more fires - telling firefighter to return home");
            
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("RETURN_HOME");
            
            try {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("firefighter");
                template.addServices(sd);
                
                DFAgentDescription[] result = DFService.search(this, template);
                for (DFAgentDescription desc : result) {
                    msg.addReceiver(desc.getName());
                }
                send(msg);
                System.out.println("📤 Sent return command to firefighter");
                
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
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

    static class FireReport {
        int x, y;
        boolean isHouse;
        long timestamp = System.currentTimeMillis();

        FireReport(int x, int y, boolean isHouse) {
            this.x = x;
            this.y = y;
            this.isHouse = isHouse;
        }
    }
}