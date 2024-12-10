package examples.yellowPages;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour; //agente usado
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.Random;

public class AlgoritmoGeneticoAgent extends Agent {

    private double tasaMutacion = 0.01; // Definimos tasaMutacion

    @Override
    protected void setup() {

        System.out.println("Agente de Algoritmo Genético " + getLocalName() + " iniciado.");

        // Registrar el servicio de algoritmo genético
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("servicio-algoritmo-genetico");
        sd.setName("algoritmo-genetico");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new ReceiveDatasetBehaviour());
    }

    private double[] parseJsonArray(JSONArray jsonArray) {
        double[] array = new double[jsonArray.length()];
        for (int i = 0; i < jsonArray.length(); i++) {
            array[i] = jsonArray.getDouble(i);
        }
        return array;
    }

    static class Data {
        double[] x;
        double[] y;

        public Data(double[] x, double[] y) {
            this.x = x;
            this.y = y;
        }
    }

    private String generarPoblacionAleatoria(Data data) {
        int tamanioPoblacion = 100;
        int limiteGeneraciones = 100;
        double tasaCruce = 0.95;
        ArrayList<Individuo> poblacion;

        // Inicialización de la población
        poblacion = inicializarPoblacion(tamanioPoblacion);
        evaluarPoblacion(poblacion, data);

        int generacion = 0;

        // Bucle evolutivo
        while (generacion < limiteGeneraciones) {
            ArrayList<Individuo> nuevaPoblacion = new ArrayList<>();

            // Elitismo
            nuevaPoblacion.add(obtenerMasApto(poblacion));

            // Cruce y mutación
            while (nuevaPoblacion.size() < tamanioPoblacion) {
                Individuo padre1 = seleccionarPadre(poblacion);
                Individuo padre2 = seleccionarPadre(poblacion);

                if (Math.random() < tasaCruce) {
                    nuevaPoblacion.addAll(cruzar(padre1, padre2));
                } else {
                    nuevaPoblacion.add(new Individuo(padre1));
                }
            }

            mutarPoblacion(nuevaPoblacion);
            evaluarPoblacion(nuevaPoblacion, data);

            // Actualizar la población
            poblacion = nuevaPoblacion;
            generacion++;
        }

        // Mostrar el mejor resultado
        Individuo mejor = obtenerMasApto(poblacion);
        double b0 = mejor.obtenerBeta0();
        double b1 = mejor.obtenerBeta1();

        double[] coeficientes = {b0, b1};

        JSONObject resultado = new JSONObject();
        resultado.put("Coeficientes", coeficientes);

        return resultado.toString();
    }

    //Genera una población inicial con valores aleatorios para b0 y b1
    private ArrayList<Individuo> inicializarPoblacion(int tam) {
        ArrayList<Individuo> poblacionInicial = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < tam; i++) {
            double beta0 = rand.nextDouble() * 200 - 100; // Valores entre -100 y 100
            double beta1 = rand.nextDouble() * 200 - 100;
            poblacionInicial.add(new Individuo(beta0, beta1));
        }
        return poblacionInicial;
    }

    //Evalúa la aptitud de cada individuo en la población.(fitness)
    private void evaluarPoblacion(ArrayList<Individuo> poblacion, Data data) {
        for (Individuo individuo : poblacion) {
            individuo.evaluarAptitud(data);
        }
    }

    //Selecciona un padre usando el metodo de la ruleta
    private Individuo seleccionarPadre(ArrayList<Individuo> poblacion) {
        double totalAptitud = poblacion.stream().mapToDouble(Individuo::obtenerAptitud).sum();
        double umbral = Math.random() * totalAptitud;

        double acumulado = 0.0;
        for (Individuo individuo : poblacion) {
            acumulado += individuo.obtenerAptitud();
            if (acumulado >= umbral) {
                return individuo;
            }
        }
        return poblacion.get(new Random().nextInt(poblacion.size()));
    }

    // Crea descendencia intercambiando valores de los padres
    private ArrayList<Individuo> cruzar(Individuo padre1, Individuo padre2) {
        ArrayList<Individuo> descendencia = new ArrayList<>();
        Random rand = new Random();
        double beta0 = rand.nextBoolean() ? padre1.obtenerBeta0() : padre2.obtenerBeta0();
        double beta1 = rand.nextBoolean() ? padre1.obtenerBeta1() : padre2.obtenerBeta1();
        descendencia.add(new Individuo(beta0, beta1));
        return descendencia;
    }

    //Aplica mutación aleatoria a algunos individuos.
    private void mutarPoblacion(ArrayList<Individuo> poblacion) {
        Random rand = new Random();
        for (Individuo individuo : poblacion) {
            if (Math.random() < tasaMutacion) {
                individuo.mutar(rand);
            }
        }
    }

    //Encuentra al individuo con la mejor aptitud (r^2)
    private Individuo obtenerMasApto(ArrayList<Individuo> poblacion) {
        return poblacion.stream().max((i1, i2) -> Double.compare(i1.obtenerAptitud(), i2.obtenerAptitud())).orElse(null);
    }

    static class Individuo {
        private double beta0;
        private double beta1;
        private double aptitud;

        public Individuo(double beta0, double beta1) {
            this.beta0 = beta0;
            this.beta1 = beta1;
        }

        public Individuo(Individuo otro) {
            this.beta0 = otro.beta0;
            this.beta1 = otro.beta1;
        }

        public double obtenerBeta0() {
            return beta0;
        }

        public double obtenerBeta1() {
            return beta1;
        }

        public double obtenerAptitud() {
            return aptitud;
        }

        public void evaluarAptitud(Data data) {
            // Calcular el promedio de y manualmente
            double sumaY = 0;
            for (double valorY : data.y) {
                sumaY += valorY;
            }
            double meanY = sumaY / data.y.length;

            // Calcular SS Total y SS Residual manualmente
            double ssTotal = 0, ssResidual = 0;
            for (int i = 0; i < data.x.length; i++) {
                double prediction = beta0 + beta1 * data.x[i];
                ssTotal += (data.y[i] - meanY) * (data.y[i] - meanY);
                ssResidual += (data.y[i] - prediction) * (data.y[i] - prediction);
            }

            // Calcular el coeficiente R^2
            if (ssTotal != 0) {
                this.aptitud = 1 - (ssResidual / ssTotal);
            } else {
                this.aptitud = 0; // Evitar división por cero
            }
        }

        public void mutar(Random rand) {
            if (rand.nextBoolean()) {
                beta0 += rand.nextGaussian();
            } else {
                beta1 += rand.nextGaussian();
            }
        }
    }

    private class ReceiveDatasetBehaviour extends CyclicBehaviour {
        @Override
        public void action() {
            ACLMessage msg = myAgent.receive();
            if (msg != null && msg.getConversationId().equals("analisis-genetico")) {

                // Procesar datos recibidos
                JSONObject json = new JSONObject(msg.getContent());
                JSONArray xArray = json.getJSONArray("x1");
                JSONArray yArray = json.getJSONArray("y");

                double[] x = parseJsonArray(xArray);
                double[] y = parseJsonArray(yArray);

                Data data = new Data(x, y);

                // Realizar la optimización genética
                String resultado = generarPoblacionAleatoria(data);

                // Enviar los resultados al agente solicitante
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent(resultado);
                send(reply);
                System.out.println("Agente genetico");
                System.out.println("Coeficientes calculados enviados al agente solicitante.");
            } else {
                block();
            }
        }
    }
}
