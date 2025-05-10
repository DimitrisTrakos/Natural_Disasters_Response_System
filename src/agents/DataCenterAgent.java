package agents;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;

public class DataCenterAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " launched.");

        addBehaviour(new jade.core.behaviours.CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("DataCenter received fire data:" + msg.getContent());
                    System.out.println(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }
}
