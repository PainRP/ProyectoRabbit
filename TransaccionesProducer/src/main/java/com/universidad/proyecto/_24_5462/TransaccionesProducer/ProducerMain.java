package com.universidad.proyecto._24_5462.TransaccionesProducer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.universidad.proyecto.model.LoteTransacciones;
import com.universidad.proyecto.model.Transaccion;

public class ProducerMain {

    public static void main(String[] args) {
        String url = "https://hly784ig9d.execute-api.us-east-1.amazonaws.com/default/transacciones";
        
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper(); 
        
        ConnectionFactory factory = new ConnectionFactory(); //Sirve para conectarse a RabbitMQ
        factory.setHost("localhost"); // Decimos que el RabbitMQ esta en el localhost

        try (Connection connection = factory.newConnection(); // Creamos la conexión con RabbitMQ
             Channel channel = connection.createChannel()) { // Creamos un canal para enviar mensajes
            
            System.out.println("Iniciando Producer...");

            try {
                HttpRequest request = HttpRequest.newBuilder() 
                        .uri(URI.create(url))
                        .GET()
                        .build();// Construimos la solicitud HTTP para obtener las transacciones

                System.out.println("Consultando API...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());// Enviamos la solicitud y obtenemos la respuesta

                if (response.statusCode() == 200) {
                    LoteTransacciones lote = mapper.readValue(response.body(), LoteTransacciones.class);
                    System.out.println("Lote recibido: " + lote.getLoteId() + " con " + lote.getTransacciones().size() + " transacciones.");

                    int transaccionesEnviadas = 0;

                    for (Transaccion tx : lote.getTransacciones()) { // Iteramos sobre cada transacción del lote y se asignan los datos
                        if (transaccionesEnviadas >= 100) {// si son más de 100 transacciones, se detiene el proceso
                            break;
                        }

                     
                        tx.setNombreEstudiante("Josué Rafael Pérez Aguirre"); 
                        tx.setCarnetEstudiante("0905-24-5462");  
                        tx.setCorreoEstudiante("jpereza62@miumg.edu.gt");
                        
                        String nombreCola = tx.getBancoDestino();
                        
                        channel.queueDeclare(nombreCola, true, false, false, null); // Declaramos la cola con el nombre del banco destino, si no existe se crea, si ya existe se usa

                        String jsonMensaje = mapper.writeValueAsString(tx);
                        
                        channel.basicPublish("", nombreCola, null, jsonMensaje.getBytes(StandardCharsets.UTF_8));// Publicamos el mensaje en la cola correspondiente al banco destino
                        System.out.println(" [v] Transacción " + tx.getIdTransaccion() + " enviada a cola: " + nombreCola);
                        
                        transaccionesEnviadas++;
                    }
                    System.out.println("\nProceso finalizado. Se enviaron exitosamente " + transaccionesEnviadas + " transacciones.");
                } else {
                    System.err.println("Error en API: Status " + response.statusCode());
                }

            } catch (Exception e) {
                System.err.println("Error durante el procesamiento: " + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("Error crítico de conexión: " + e.getMessage());
        }
    }
}