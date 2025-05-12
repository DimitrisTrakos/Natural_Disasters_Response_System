package agents;

import jade.core.Agent;
<<<<<<< HEAD
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
=======
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.*;
import jade.core.behaviours.CyclicBehaviour;
>>>>>>> main

public class DataCenterAgent extends Agent {

    @Override
    protected void setup() {
        System.out.println(getLocalName() + " launched.");

<<<<<<< HEAD
        // Register this agent with the Directory Facilitator (DF)
=======
        // Register this agent with the DF
>>>>>>> main
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
<<<<<<< HEAD
        sd.setType("data-center"); // Service type other agents will search for
        sd.setName("Fire-Data-Service"); // A friendly name for the service
=======
        sd.setType("data-center");
        sd.setName("Fire-Data-Service");
>>>>>>> main
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
            System.out.println(getLocalName() + " registered with DF.");
        } catch (FIPAException e) {
            System.err.println(getLocalName() + " failed to register with DF.");
            e.printStackTrace();
        }

<<<<<<< HEAD
        addBehaviour(new jade.core.behaviours.CyclicBehaviour() {
=======
        addBehaviour(new CyclicBehaviour() {
>>>>>>> main
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
<<<<<<< HEAD
                    System.out.println("DataCenter received fire data: " + msg.getContent());
=======
                    String content = msg.getContent();
                    System.out.println("DataCenter received fire data:\n" + content);

                    // Forward message to all firefighter agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("firefighter");
                    template.addServices(sd);

                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        for (DFAgentDescription desc : result) {
                            AID receiver = desc.getName();
                            ACLMessage forward = new ACLMessage(ACLMessage.INFORM);
                            forward.addReceiver(receiver);
                            forward.setContent(content);
                            send(forward);
                            System.out.println(getLocalName() + " forwarded fire data to: " + receiver.getLocalName());
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }
>>>>>>> main
                } else {
                    block();
                }
            }
        });
    }

    @Override
    protected void takeDown() {
<<<<<<< HEAD
        // Unregister from DF when the agent is killed
=======
>>>>>>> main
        try {
            DFService.deregister(this);
            System.out.println(getLocalName() + " deregistered from DF.");
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
