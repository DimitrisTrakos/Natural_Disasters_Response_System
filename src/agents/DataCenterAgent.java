package agents;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import utils.SyncOutput;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.core.behaviours.CyclicBehaviour;

import java.util.*;
import java.util.stream.Collectors;

public class DataCenterAgent extends Agent {

    private List<FireReport> fireReports = new ArrayList<>();
    private List<int[]> homeLocations = new ArrayList<>();
    private int firefighterX = -1;
    private int firefighterY = -1;

    @Override
    protected void setup() {
        System.out.println("🖥️ " + getLocalName() + " initialized");

        // Register with DF
        registerWithDF();

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent();
                    
                    if (content.startsWith("POSITION:")) {
                        updateFirefighterPosition(content);
                    } 
                    else if (content.startsWith("EXTINGUISHED:")) {
                        removeExtinguishedFire(content);
                    }
                    else if (content.startsWith("HOME_DANGER:")) {
                        processHomeDanger(content);
                    }
                    else {
                        processFireReport(content);
                    }
                    sendNextTarget();
                } else {
                    block();
                }
            }
        });
    }

    private void registerWithDF() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("data-center");
        sd.setName("Fire-Control");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private void updateFirefighterPosition(String content) {
        String[] parts = content.replace("POSITION:", "").split(",");
        firefighterX = Integer.parseInt(parts[0]);
        firefighterY = Integer.parseInt(parts[1]);
        SyncOutput.println("🖥️  Firefighter position updated to (" + firefighterX + "," + firefighterY + ")");
    }

    private void processHomeDanger(String content) {
        String[] lines = content.replace("HOME_DANGER:", "").split("\n");
        String[] homeCoords = lines[0].split(",");
        int homeX = Integer.parseInt(homeCoords[0].trim());
        int homeY = Integer.parseInt(homeCoords[1].trim());
    
        if (homeLocations.stream().noneMatch(h -> h[0] == homeX && h[1] == homeY)) {
            homeLocations.add(new int[]{homeX, homeY});
        }
    
        for (int i = 1; i < lines.length; i++) {
            try {
                String line = lines[i].replace("Fire at (", "").replace(")", "").trim();
                String[] coordPart = line.split(" ")[0].split(",");
                int x = Integer.parseInt(coordPart[0].trim());
                int y = Integer.parseInt(coordPart[1].trim());
    
                boolean droneReported = fireReports.stream()
                    .anyMatch(f -> f.x == x && f.y == y && f.priority == 3);
                
                if (droneReported) {
                    SyncOutput.println("🖥️  Upgrading priority for fire at (" + x + "," + y + 
                                     ") from drone report to home priority");
                    fireReports.removeIf(f -> f.x == x && f.y == y);
                }
    
                boolean isHouseFire = line.toUpperCase().contains("INSIDE HOME");
                int priority = isHouseFire ? 1 : 2;
                addFireWithPriority(x, y, priority);
                
            } catch (Exception e) {
                System.err.println("❌ Failed to parse line: " + lines[i]);
            }
        }
    }

    private void addFireWithPriority(int x, int y, int priorityLevel) {
        // 1 = House fire, 2 = Near home, 3 = Regular
        fireReports.removeIf(f -> f.x == x && f.y == y);
        fireReports.add(new FireReport(x, y, priorityLevel));
        System.out.printf("🖥️  %s fire added at (%d,%d)%n",
            priorityLevel == 1 ? "HOUSE" : (priorityLevel == 2 ? "NEAR-HOME" : "NO-NEAR-HOME") , x, y);
    }

    private void processFireReport(String content) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 3) {
                try {
                    int x = Integer.parseInt(parts[0].trim());
                    int y = Integer.parseInt(parts[1].trim());
                    
                    // Check if this was already reported by a homeowner
                    boolean homePriority = fireReports.stream()
                        .anyMatch(f -> f.x == x && f.y == y && f.priority <= 2);
                    
                    // Only add as regular fire if not already reported by homeowner
                    if (!homePriority) {
                        boolean nearHome = homeLocations.stream()
                            .anyMatch(h -> Math.abs(h[0]-x) <= 1 && Math.abs(h[1]-y) <= 1);
                        
                        int priority = nearHome ? 2 : 3;
                        addFireWithPriority(x, y, priority);
                    } else {
                        SyncOutput.println("🖥️  Duplicate fire at (" + x + "," + y + 
                                        ") already reported by homeowner");
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid fire report: " + line);
                }
            }
        }
    }

    private void removeExtinguishedFire(String content) {
        String[] parts = content.replace("EXTINGUISHED:", "").split(",");
        int x = Integer.parseInt(parts[0].trim());
        int y = Integer.parseInt(parts[1].trim());
        
        fireReports.removeIf(f -> f.x == x && f.y == y);
        SyncOutput.println("🖥️  Fire removed at (" + x + "," + y + ")");
        checkForReturnCommand();
    }

    private void sendNextTarget() {
        if (fireReports.isEmpty() || firefighterX == -1) {
            SyncOutput.println("🖥️  No active fires");
            return;
        }

        FireReport nextFire = fireReports.stream()
            .sorted(Comparator.comparingInt(FireReport::getPriority)
                .thenComparing(f -> manhattanDistance(f.x, f.y, firefighterX, firefighterY)))
            .findFirst()
            .orElse(null);

        if (nextFire != null) {
            String priorityType = switch(nextFire.priority) {
                case 1 -> "HOUSE FIRE (HIGHEST PRIORITY)";
                case 2 -> "Fire near home (MEDIUM PRIORITY)";
                default -> "Regular fire (LOW PRIORITY)";
            };
            
            System.out.printf("\n🖥️  Selected target: (%d,%d) | %s | Distance: %d%n",
                nextFire.x, nextFire.y, priorityType, 
                manhattanDistance(nextFire.x, nextFire.y, firefighterX, firefighterY));

            sendTargetToFirefighter(nextFire.x, nextFire.y);
        }
    }

    private void sendTargetToFirefighter(int targetX, int targetY) {
        try {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.setContent("TARGET:" + targetX + "," + targetY);
            
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("firefighter");
            template.addServices(sd);

            for (DFAgentDescription desc : DFService.search(this, template)) {
                msg.addReceiver(desc.getName());
            }
            send(msg);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    private int manhattanDistance(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2);
    }
    private void checkForReturnCommand() {
        if (fireReports.isEmpty()) {
            SyncOutput.println("🖥️  No more fires - telling firefighter to return home");
            
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
                SyncOutput.println("🖥️  Sent return command to firefighter");
                
            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
            System.out.println("🖥️  " + getLocalName() + " shutting down");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }

    static class FireReport {
        int x, y;
        int priority; 
        
        FireReport(int x, int y, int priority) {
            this.x = x;
            this.y = y;
            this.priority = priority;
        }
        
        int getPriority() {
            return priority;
        }
    }
}