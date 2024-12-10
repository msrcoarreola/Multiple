package examples.yellowPages;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.OneShotBehaviour; //agente usado
import org.json.JSONObject;
import org.json.JSONArray;

import java.text.DecimalFormat;

import static java.lang.Math.pow;

public class BuscaServicio extends Agent {

    protected void setup() {
        System.out.println("Agente " + getLocalName() + " iniciado.");
        doWait(2000);
        addBehaviour(new ClasificacionYRegresionBehaviour());
    }

    private class ClasificacionYRegresionBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            // Definir el conjunto de datos
            DataSet dataSet = new DataSet(
                    new double[]{41.9, 43.4, 43.9, 44.5, 47.3, 47.5, 47.9, 50.2, 52.8, 53.2, 56.7, 57.0, 63.5, 65.3,
                            71.1, 77.0, 77.8}, // x o x1 Para regresiones simples y polinomiales SALES INDEPENDIENTE exogena

                    new double[]{251.3, 251.3, 248.3, 267.5, 273.0, 276.5, 270.3, 274.9, 285.0, 290.0, 297.0, 302.5,
                            304.5, 309.3, 321.7, 330.7, 349.0} // y ADVERTISING DEPENDIENTE endogena
            );

            double inicioX1 = 40, finX1 = 58;

            // Buscar agentes de clasificación
            AID classificationAgent = searchAgent("classification-service");
            if (classificationAgent == null) {
                System.out.println("No se encontró un agente de clasificación.");
                return;
            }

            // Enviar solicitud de clasificación
            ACLMessage classMsg = new ACLMessage(ACLMessage.REQUEST);
            classMsg.addReceiver(classificationAgent);
            classMsg.setContent(serializeVariablesToJson(dataSet));
            classMsg.setConversationId("classification-analysis");
            send(classMsg);

            // Recibir recomendación de análisis
            ACLMessage classReply = blockingReceive();
            if (classReply != null && classReply.getPerformative() == ACLMessage.INFORM) {
                String analysisType = classReply.getContent();

                // Determinar el tipo de servicio de regresión
                String regressionServiceType = "";
                switch (analysisType) {
                    case "Multiple Linear Regression":
                        regressionServiceType = "multiple-regression-service";
                        break;
                    case "Polynomial Regression":
                        regressionServiceType = "polynomial-regression-service";
                        break;
                    default:
                        System.out.println("Tipo de análisis desconocido.");
                        return;
                }

                // Buscar el agente de regresión adecuado y enviar los datos
                AID regressionAgent = searchAgent(regressionServiceType);
                if (regressionAgent != null) {
                    ACLMessage regressionRequest = new ACLMessage(ACLMessage.REQUEST);
                    regressionRequest.addReceiver(regressionAgent);
                    regressionRequest.setContent(serializeVariablesToJson(dataSet));
                    regressionRequest.setConversationId("regression-analysis");
                    send(regressionRequest);

                    // Recibir parámetros de regresión
                    ACLMessage regressionReply = blockingReceive();
                    if (regressionReply != null && regressionReply.getPerformative() == ACLMessage.INFORM) {

                        String jsonContent = regressionReply.getContent();
                        double[] coeficientes = extractCoeficientesFromJson(jsonContent);

                        switch (analysisType) {
                            case "Multiple Linear Regression":
                                MultipleLinearRegression regressionM = new MultipleLinearRegression();
                                regressionM.predicciones(coeficientes, dataSet.exogena1, dataSet.exogena2, dataSet.endogena);
                                break;
                            case "Polynomial Regression":
                                PolynomialRegression regressionP = new PolynomialRegression();
                                regressionP.generarPronostico(coeficientes, inicioX1, finX1);
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    System.out.println("No se encontró el agente de regresión adecuado.");
                }
            }


            System.out.println();
            System.out.println();
            System.out.println();


            //Algoritmo genetico
            AID agenteGenetico = searchAgent("servicio-algoritmo-genetico");
            if (agenteGenetico == null) {
                System.out.println("No se encontró un Agente genetico.");
                return;
            }

            ACLMessage geneticRequest = new ACLMessage(ACLMessage.REQUEST);
            geneticRequest.addReceiver(agenteGenetico);
            geneticRequest.setContent(serializeVariablesToJson(dataSet));
            geneticRequest.setConversationId("analisis-genetico");
            send(geneticRequest);

            // Recibir coeficientes optimizados
            ACLMessage geneticReply = blockingReceive();
            if (geneticReply != null && geneticReply.getPerformative() == ACLMessage.INFORM) {
                System.out.println("Parámetros de recibidos: " + geneticReply.getContent());
                String jsonContent = geneticReply.getContent();
                double[] coefOptimos = extractCoeficientesFromJson(jsonContent);
                coefOptimos = extractCoeficientesFromJson(geneticReply.getContent());

                // Realizar predicciones con coeficientes optimizados
                predicciones(coefOptimos, inicioX1, finX1);
            } else {
                System.out.println("No se recibió respuesta del agente de algoritmo genético.");
            }
        }


        private AID searchAgent(String serviceType) {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType(serviceType);
            template.addServices(sd);
            try {
                DFAgentDescription[] results = DFService.search(myAgent, template);
                if (results.length > 0)
                    return results[0].getName();
            } catch (FIPAException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public class DataSet {
        protected double[] exogena1;
        protected double[] exogena2;
        protected double[] endogena;

        public DataSet(double[] exogena1, double[] endogena) {
            this.exogena1 = exogena1;
            this.endogena = endogena;
        }

        public DataSet(double[] exogena1, double[] exogena2, double[] endogena) {
            this.exogena1 = exogena1;
            this.exogena2 = exogena2;
            this.endogena = endogena;
        }
    }

    public class MultipleLinearRegression {
        public void predicciones(double[] coefi, double[] exogena1, double[] exogena2, double[] endogena) {
            double x1, x2;
            int j = 0;
            DecimalFormat formato = new DecimalFormat("#.##");
            System.out.println("\nŷ = " + coefi[0] + " + " + coefi[1] + "(X1) + " + coefi[2] + "(X2)\n");
            x1 = exogena1[endogena.length - 1];
            x2 = exogena2[endogena.length - 1];

            System.out.println("#    X1    X2     Y");
            for (int i = 80; i <= 110; i += 5) {
                double res = (coefi[0] + coefi[1] * x1 + coefi[2] * x2);
                System.out.print(j + "  " + x1 + "  " + formato.format(x2) + "  " + formato.format(res));
                System.out.println("       ŷ = " + coefi[0] + " + " + coefi[1] + " ( " + formato.format(x1) + " ) + " + coefi[2] + " ( " + formato.format(x2) + " )");

                x1 = i;
                x2 += 0.20;
                j++;
            }
        }
    }

    public class PolynomialRegression {
        public void generarPronostico(double[] recta, double init, double end) {
            System.out.println("\nŷ = " + recta[0] + "  " + recta[1] + "  " + recta[2]);
            double numx = 0, res = 0;
            DecimalFormat formato = new DecimalFormat("#.##");
            System.out.println(" X     Y");
            for (double i = init; i < end; i++) {
                numx = i;
                res = recta[2] * pow(numx,2) + recta[1] * numx + recta[0];
                System.out.print(numx + "  " + formato.format(res));
                System.out.println("       y = " + recta[2] + "  +  " + recta[1] + " ( " + numx + " )" + "  +  " + recta[0] + " ( " + pow(numx, 2) + " )");
            }
        }
    }

    public void predicciones(double[] coef, double inicio, double fin) {
        double numx;
        double pronostico = 0;
        DecimalFormat formato = new DecimalFormat("#.##");
        System.out.println("   (X)     (Y)");
        for (double i = inicio; i < fin; i++) {
            numx = i;
            pronostico = coef[1] + coef[0] * numx;
            System.out.print("  " + numx + "    " + formato.format(pronostico));
            System.out.println("        ŷ = " + coef[1] + " + " + coef[0] + " ( " + numx + " ) ");
        }
    }

    private double[] extractCoeficientesFromJson(String jsonContent) {
        JSONObject jsonObject = new JSONObject(jsonContent);
        JSONArray coeficientesArray = jsonObject.getJSONArray("Coeficientes");
        double[] coeficientes = new double[coeficientesArray.length()];

        for (int i = 0; i < coeficientesArray.length(); i++) {
            coeficientes[i] = coeficientesArray.getDouble(i);
        }
        return coeficientes;
    }

    private String serializeVariablesToJson(DataSet data) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        if (data.exogena1 != null) {
            sb.append("\"x1\": [");
            for (int i = 0; i < data.exogena1.length; i++) {
                sb.append(data.exogena1[i]);
                if (i < data.exogena1.length - 1)
                    sb.append(", ");
            }
            sb.append("], ");
        }

        if (data.exogena2 != null) {
            sb.append("\"x2\": [");
            for (int i = 0; i < data.exogena2.length; i++) {
                sb.append(data.exogena2[i]);
                if (i < data.exogena2.length - 1)
                    sb.append(", ");
            }
            sb.append("], ");
        }

        if (data.endogena != null) {
            sb.append("\"y\": [");
            for (int i = 0; i < data.endogena.length; i++) {
                sb.append(data.endogena[i]);
                if (i < data.endogena.length - 1)
                    sb.append(", ");
            }
            sb.append("]");
        }
        sb.append("}");
        return sb.toString();
    }
}
