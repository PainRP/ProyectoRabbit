package com.universidad.proyecto._24_5462.TransaccionesConsumer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.universidad.proyecto.model.Transaccion;

public class Main {
    private static final String POST_URL = "https://7e0d9ogwzd.execute-api.us-east-1.amazonaws.com/default/guardarTransacciones";

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); 

        HttpClient httpClient = HttpClient.newHttpClient(); 
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); 

        String[] bancos = {"BANRURAL", "BAC", "BI", "GYT"};
        Set<String> transaccionesProcesadas = new HashSet<>(); 
        
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            System.out.println(" [*] Consumer iniciado. Esperando mensajes. Presiona Ctrl+C para salir.");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String mensajeJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                String colaOrigen = delivery.getEnvelope().getRoutingKey();

                Integer prioridadNum = delivery.getProperties().getPriority();
                String prioridadTexto = (prioridadNum != null && prioridadNum == 10) ? "Alta" : "Normal";

                try {
                    Transaccion tx = mapper.readValue(mensajeJson, Transaccion.class);
                    String idTx = tx.getIdTransaccion();

                    if(transaccionesProcesadas.contains(idTx)) { 
                    	channel.basicPublish("", "cola_duplicados", null, mensajeJson.getBytes(StandardCharsets.UTF_8));
                        
                        
                        System.out.println("id Transacción: " + idTx + " | estado:[Duplicada] | prioridad:[" + prioridadTexto + "] | cola destino: cola_duplicados");
                        
                        channel.basicAck(deliveryTag, false); 
                    } else {
                    	System.out.println("\n [x] Procesando transacción " + tx.getIdTransaccion() + " de la cola " + colaOrigen);
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(POST_URL))
                                .header("Content-Type", "application/json")  
                                .POST(HttpRequest.BodyPublishers.ofString(mensajeJson))
                                .build();

                        boolean exito = false;
                        int intentos = 0;
                        int maxIntentos = 2;

                        while (intentos < maxIntentos && !exito) {
                            intentos++;
                            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                            if (response.statusCode() == 200 || response.statusCode() == 201) { 
                                exito = true;
                                transaccionesProcesadas.add(idTx);
                                
                                
                                System.out.println("id Transacción: " + idTx + " | estado:[Procesada] | prioridad:[" + prioridadTexto + "] | cola destino: API POST");
                                
                                channel.basicAck(deliveryTag, false);
                            } else {
                            	System.err.println("     [!] Error API (Intento " + intentos + "): HTTP " + response.statusCode());
                                if (intentos < maxIntentos) {
                                	System.out.println("     [!] Reintentando en 2 segundos...");
                                    Thread.sleep(2000); 
                                }
                            }
                        }

                        
                        if (!exito) {
                        	channel.basicPublish("", "cola_errores", null, mensajeJson.getBytes(StandardCharsets.UTF_8));
                            
                            
                            System.out.println("id Transacción: " + idTx + " | estado:[Error] | prioridad:[" + prioridadTexto + "] | cola destino: cola_errores");
                            
                            channel.basicAck(deliveryTag, false); 
                        }
                    }
                    
                } catch (Exception e) {
                	channel.basicPublish("", "cola_errores", null, mensajeJson.getBytes(StandardCharsets.UTF_8));
                	System.err.println("     [X] Error procesando mensaje: " + e.getMessage());
                    channel.basicAck(deliveryTag, false); 
                }
            };

          
            Map<String, Object> queueArgs = new HashMap<>();
            queueArgs.put("x-max-priority", 10);

            for (String banco : bancos) {
                channel.queueDeclare(banco, true, false, false, queueArgs); 
                channel.basicConsume(banco, false, deliverCallback, consumerTag -> { }); 
            }

        } catch (Exception e) {
            System.err.println("Error crítico en la conexión: " + e.getMessage());
        }
    }
}