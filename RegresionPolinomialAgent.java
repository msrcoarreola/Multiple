package examples.yellowPages;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.json.JSONObject;
import org.json.JSONArray;

public class RegresionPolinomialAgent extends Agent {

    //(X^TX)^-1 X^T Y
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");

        // Registrar el servicio de regresión polinomial
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        // Establece el tipo del servicio
        sd.setType("polynomial-regression-service");
        // Asigna un nombre al servicio
        sd.setName("polynomial-regression-service");
        dfd.addServices(sd);
        try {
            // registra el agente
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new ReceiveDatasetBehaviour());
    }

    private class ReceiveDatasetBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            // método que intenta recibir un mensaje.
            ACLMessage msg = receive();
            if (msg != null && msg.getConversationId().equals("regression-analysis")) {
                JSONObject json = new JSONObject(msg.getContent());
                JSONArray exogenaArray = json.getJSONArray("x1");
                JSONArray endogenaArray = json.getJSONArray("y");

                double[] exogena = parseJsonArray(exogenaArray);
                double[] endogena = parseJsonArray(endogenaArray);

                double[][] regresion = generarSumatorias(exogena, 2);
                double[][] regresionT = generarTranspuesta(regresion);
                regresion = multiplicarMatrices(regresionT, regresion);
                regresion = obtenerInversa(regresion);
                regresion = multiplicarMatrices(regresion, regresionT);
                double[] coeficientes = multiplicarMatrizVector(regresion, endogena);

                JSONObject regressionResult = new JSONObject();
                regressionResult.put("Coeficientes", coeficientes);

                String mensaje = regressionResult.toString();
                // Enviar los resultados al agente solicitante
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(mensaje);
                send(reply);
                System.out.println("Coeficientes de regresion polinomial: \n");
            } else {
                block();
            }
        }
    }

    // convierte un JSONArray (arreglo en formato JSON) a un arreglo de double en Java
    public double[] parseJsonArray(JSONArray jsonArray) {
        double[] array = new double[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            array[i] = jsonArray.getDouble(i);
        }
        return array;
    }

    public double[][] generarSumatorias(double[] exogena, int exponente) {
        double[][] matriz = new double[exogena.length][exponente + 1];

        for (int i = 0; i < exogena.length; i++) {
            for (int j = 0; j <= exponente; j++) {
                matriz[i][j] = Math.pow(exogena[i], j);
            }
        }
        return matriz;
    }

    public double[][] generarTranspuesta(double[][] exogena) {
        double[][] matriz = new double[exogena[0].length][exogena.length];

        for (int i = 0; i < exogena.length; i++) {
            for (int j = 0; j < exogena[0].length; j++) {
                matriz[j][i] = exogena[i][j];
            }
        }
        return matriz;
    }

    public static double[][] multiplicarMatrices(double[][] exogena, double[][] exogenaT) {
        double[][] matriz = new double[exogena.length][exogenaT[0].length];

        for (int i = 0; i < exogena.length; i++) {
            for (int j = 0; j < exogenaT[0].length; j++) {
                double sum = 0.0;
                for (int k = 0; k < exogena[0].length; k++) {
                    sum += exogena[i][k] * exogenaT[k][j];
                }
                matriz[i][j] = sum;
            }
        }
        return matriz;
    }

    public static double[] multiplicarMatrizVector(double[][] exogena, double[] endogena) {
        double[] vector = new double[exogena.length];

        for (int i = 0; i < exogena.length; i++) {
            double suma = 0.0;
            for (int j = 0; j < exogena[0].length; j++) {
                suma += exogena[i][j] * endogena[j];
            }
            vector[i] = suma;
        }
        return vector;
    }

    public static double[][] obtenerInversa(double[][] matriz) {
        int n = matriz.length;
        double[][] augmentada = new double[n][2 * n];

        // Crear la matriz aumentada [matriz | identidad]
        for (int i = 0; i < n; i++) {
            System.arraycopy(matriz[i], 0, augmentada[i], 0, n);
            augmentada[i][i + n] = 1.0;
        }

        // Aplicar Gauss-Jordan
        for (int i = 0; i < n; i++) {
            // Escalar la fila actual al pivote
            double pivot = augmentada[i][i];
            for (int j = 0; j < 2 * n; j++) {
                augmentada[i][j] /= pivot;
            }

            // Eliminación hacia abajo y hacia arriba
            for (int k = 0; k < n; k++) {
                if (k != i) {
                    double factor = augmentada[k][i];
                    for (int j = 0; j < 2 * n; j++) {
                        augmentada[k][j] -= factor * augmentada[i][j];
                    }
                }
            }
        }

        // Extraer la matriz inversa
        double[][] inversa = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(augmentada[i], n, inversa[i], 0, n);
        }

        return inversa;
    }
}
