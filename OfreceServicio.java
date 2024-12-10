package examples.yellowPages;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Arrays;

public class OfreceServicio extends Agent {

    @Override
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");

        // Registrar el servicio de clasificación una sola vez
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("classification-service");
        sd.setName("classification-service");
        dfd.addServices(sd);
        try {
            // registra el agente
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Añadir comportamiento para recibir mensajes de otros agentes
        addBehaviour(new OfreceClasificacionBehaviour());
    }

    @Override
    // Desregistra al agente del DF y notifica que ya no ofrece sus servicios.
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println();
        System.out.println("Luke, el agente " + getAID().getName() + ", es tu padre...");
    }

    // El comportamiento cíclico permite al agente recibir mensajes continuamente y
    // determinar el tipo de regresión adecuado.
    private class OfreceClasificacionBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = receive();
            // recibe el mensaje y determina si no es nulo
            if (msg != null && msg.getConversationId().equals("classification-analysis")) {

                // Extrae el arreglo en formato JSON
                JSONObject json = new JSONObject(msg.getContent());
                JSONArray yArray = json.getJSONArray("y");
                double[] yValues = convertirJsonArray(yArray);

                double[] x1Values = null;
                double[] x2Values = null;

                // Verificación de regresión múltiple o polinómica
                // Y extrae los datos de los arreglos de x
                if (json.has("x1") && json.has("x2") && json.getJSONArray("x2").length() > 0) {
                    x1Values = convertirJsonArray(json.getJSONArray("x1"));
                    x2Values = convertirJsonArray(json.getJSONArray("x2"));
                } else if (json.has("x1")) {
                    x1Values = convertirJsonArray(json.getJSONArray("x1"));
                }

                // Determinar tipo de regresión
                String regressionType = clasificarRegresion(x1Values, x2Values, yValues);

                // Enviar tipo de análisis recomendado
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(regressionType);
                send(reply);
            } else {
                block(); // Block until new messages arrive
            }
        }

        // convierte un JSONArray (arreglo en formato JSON) a un arreglo de double en
        // Java
        private double[] convertirJsonArray(JSONArray jsonArray) {
            double[] array = new double[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                array[i] = jsonArray.getDouble(i);
            }
            return array;
        }
    }

    private String clasificarRegresion(double[] exogena1, double[] exogena2, double[] endogena) {
        if (exogena2 != null && exogena2.length > 0) {
            return "Multiple Linear Regression";
        } else if (exogena1 != null) {
            return "Polynomial Regression";
        }
        return "Regresion desconocida";
    }
}
