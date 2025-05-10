package agents;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class DataCenterAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " launched.");

        // Register this agent with the Directory Facilitator (DF)
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType("data-center"); // Service type other agents will search for
        sd.setName("Fire-Data-Service"); // A friendly name for the service
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " registered with DF.");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " failed to register with DF.");
            e.printStackTrace();
        }

        addBehaviour(new jade.core.behaviours.CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("DataCenter received fire data: " + msg.getContent());
                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
        // Unregister from DF when the agent is killed
        try {
            DFService.deregister(this);
            System.out.println(getLocalName() + " deregistered from DF.");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
