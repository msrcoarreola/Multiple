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

public class RegresionMultipleAgent extends Agent {

    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");

        // Registrar el servicio de regresión múltiple
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        // Establece el tipo del servicio
        sd.setType("multiple-regression-service");
        // Asigna un nombre al servicio
        sd.setName("multiple-regression-service");
        dfd.addServices(sd);
        try {
            // registra el agente
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new ReceiveDatasetBehaviour());
    }

    // convierte un JSONArray (arreglo en formato JSON) a un arreglo de double en
    // Java
    public double[] parseJsonArray(JSONArray jsonArray) {
        double[] array = new double[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            array[i] = jsonArray.getDouble(i);
        }
        return array;
    }

    private String performMultipleRegression(double[] x1, double[] x2, double[] y) {
        discreteMathematics discretas = new discreteMathematics();
        linearAlgebra algebra = new linearAlgebra();

        double sumX1 = discretas.sumatoria(x1);
        double sumX2 = discretas.sumatoria(x2);
        double sumY = discretas.sumatoria(y);
        double sumX1Squared = discretas.sumatoriaExponencial(x1);
        double sumX2Squared = discretas.sumatoriaExponencial(x2);
        double sumX1X2 = discretas.sumatoriadeProductos(x1, x2);
        double sumX1Y = discretas.sumatoriadeProductos(x1, y);
        double sumX2Y = discretas.sumatoriadeProductos(x2, y);

        double[][] matrix = {
                { x1.length, sumX1, sumX2 },
                { sumX1, sumX1Squared, sumX1X2 },
                { sumX2, sumX1X2, sumX2Squared }
        };

        double[][] inverseMatrix = algebra.calcularInversa(matrix);

        double[] b = { sumY, sumX1Y, sumX2Y };
        double[] coeficientes = algebra.productoMatrizA(inverseMatrix, b);

        // Crear un JSON con los resultados de la regresión
        JSONObject regressionResult = new JSONObject();
        regressionResult.put("Coeficientes", coeficientes);

        return regressionResult.toString();
    }

    public class linearAlgebra {
        public double[][] calcularInversa(double[][] matriz) {
            double det = matriz[0][0] * (matriz[1][1] * matriz[2][2] - matriz[1][2] * matriz[2][1])
                    - matriz[0][1] * (matriz[1][0] * matriz[2][2] - matriz[1][2] * matriz[2][0])
                    + matriz[0][2] * (matriz[1][0] * matriz[2][1] - matriz[1][1] * matriz[2][0]);

            double[][] inversa = new double[3][3];

            inversa[0][0] = (matriz[1][1] * matriz[2][2] - matriz[1][2] * matriz[2][1]) / det;
            inversa[0][1] = (matriz[0][2] * matriz[2][1] - matriz[0][1] * matriz[2][2]) / det;
            inversa[0][2] = (matriz[0][1] * matriz[1][2] - matriz[0][2] * matriz[1][1]) / det;
            inversa[1][0] = (matriz[1][2] * matriz[2][0] - matriz[1][0] * matriz[2][2]) / det;
            inversa[1][1] = (matriz[0][0] * matriz[2][2] - matriz[0][2] * matriz[2][0]) / det;
            inversa[1][2] = (matriz[0][2] * matriz[1][0] - matriz[0][0] * matriz[1][2]) / det;
            inversa[2][0] = (matriz[1][0] * matriz[2][1] - matriz[1][1] * matriz[2][0]) / det;
            inversa[2][1] = (matriz[0][1] * matriz[2][0] - matriz[0][0] * matriz[2][1]) / det;
            inversa[2][2] = (matriz[0][0] * matriz[1][1] - matriz[0][1] * matriz[1][0]) / det;

            return inversa;
        }

        public double[] productoMatrizA(double[][] matriz, double[] a) {
            double[] resultado = new double[matriz.length];
            for (int i = 0; i < matriz.length; i++) {
                double suma = 0.0;
                for (int j = 0; j < matriz[0].length; j++) {
                    suma += matriz[i][j] * a[j];
                }
                resultado[i] = suma;
            }
            return resultado;
        }
    }

    public class discreteMathematics {
        public double sumatoria(double[] a) {
            double r = 0;
            for (double v : a) {
                r += v;
            }
            return r;
        }

        public double sumatoriadeProductos(double[] a, double[] b) {
            double r = 0;
            for (int i = 0; i < a.length; i++) {
                r += a[i] * b[i];
            }
            return r;
        }

        public double sumatoriaExponencial(double[] a) {
            double r = 0;
            for (double num : a) {
                r += Math.pow(num, 2);
            }
            return r;
        }

    }

    private class ReceiveDatasetBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // método que intenta recibir un mensaje.
            ACLMessage msg = receive();
            // Asegura que efectivamente se ha recibido un mensaje
            // Y que es parte de una conversación de análisis de regresión
            if (msg != null && msg.getConversationId().equals("regression-analysis")) {
                JSONObject json = new JSONObject(msg.getContent());
                JSONArray x1Array = json.getJSONArray("x1");
                JSONArray x2Array = json.getJSONArray("x2");
                JSONArray yArray = json.getJSONArray("y");

                double[] x1Values = parseJsonArray(x1Array);
                double[] x2Values = parseJsonArray(x2Array);
                double[] yValues = parseJsonArray(yArray);

                // Realizar regresión y obtener los resultados
                String regressionResult = performMultipleRegression(x1Values, x2Values, yValues);

                // Enviar los resultados al agente solicitante
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(regressionResult);
                send(reply);
                System.out.println("Coeficientes de regresion multiple: \n");
            } else {
                block();
            }
        }
    }
}
