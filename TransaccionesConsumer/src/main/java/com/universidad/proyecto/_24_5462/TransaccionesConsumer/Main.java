package com.universidad.proyecto._24_5462.TransaccionesConsumer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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
        ConnectionFactory factory = new ConnectionFactory(); //Conexión
        factory.setHost("localhost"); // hardcode indicando que RabbitMQ está en localhost

        HttpClient httpClient = HttpClient.newHttpClient(); 
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); //

        String[] bancos = {"BANRURAL", "BAC", "BI", "GYT"};

        Set<String> transaccionesProcesadas = new HashSet<>(); //NO lo hemos visto en clase, pero cuando realice un ejercicio de leetcode, al comprobar mi respuesta con otros, me fije que utilizaban entre un hashmap y un hashset, solo que el hashset sirve para un solo guarda una cosa que nos sirve para comprobar que no hayan duplicados
        
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            System.out.println(" [*] Consumer iniciado. Esperando mensajes. Presiona Ctrl+C para salir.");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String mensajeJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
                long deliveryTag = delivery.getEnvelope().getDeliveryTag();
                String colaOrigen = delivery.getEnvelope().getRoutingKey();

                try {
                    Transaccion tx = mapper.readValue(mensajeJson, Transaccion.class);
                    System.out.println("\n [x] Procesando transacción " + tx.getIdTransaccion() + " de la cola " + colaOrigen);

                    HttpRequest request = HttpRequest.newBuilder() // solicitud HTTP
                            .uri(URI.create(POST_URL)) // endpoint
                            .header("Content-Type", "application/json")  // encabezado indicando que el cuerpo es JSON
                            .POST(HttpRequest.BodyPublishers.ofString(mensajeJson)) // cuerpo
                            .build();

                    boolean exito = false;
                    int intentos = 0;
                    int maxIntentos = 2;

                    while (intentos < maxIntentos && !exito) {
                        intentos++;
                        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                        // Aceptamos 200 o 201 como éxito
                        if (response.statusCode() == 200 || response.statusCode() == 201) { 
                            exito = true;
                            System.out.println("     [v] API respondió " + response.statusCode() + ". Guardado exitoso. Enviando ACK.");
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
                        System.err.println("     [X] Falló tras reintentos. Devolviendo mensaje a RabbitMQ (NACK).");
                        channel.basicNack(deliveryTag, false, true); 
                    }

                } catch (Exception e) {
                    System.err.println("     [X] Error procesando mensaje: " + e.getMessage());
                    channel.basicNack(deliveryTag, false, true); 
                }
            };

            for (String banco : bancos) {
                channel.queueDeclare(banco, true, false, false, null); // Declaramos la cola por si no existe
                channel.basicConsume(banco, false, deliverCallback, consumerTag -> { }); //
            }

        } catch (Exception e) {
            System.err.println("Error crítico en la conexión: " + e.getMessage());
        }
    }
}