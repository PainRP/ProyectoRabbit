package com.universidad.proyecto._24_5462.TransaccionesProducer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP;
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
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    LoteTransacciones lote = mapper.readValue(response.body(), LoteTransacciones.class);
                    
                    
                    channel.queueDeclare("cola_duplicados", true, false, false, null);
                    channel.queueDeclare("cola_errores", true, false, false, null); 

                    
                    Map<String, Object> queueArgs = new HashMap<>();
                    queueArgs.put("x-max-priority", 10); 

                    int transaccionesEnviadas = 0;

                    for (Transaccion tx : lote.getTransacciones()) { // Iteramos sobre cada transacción del lote y se asignan los datos
                        if (transaccionesEnviadas >= 100) break; // si son más de 100 transacciones, se detiene el proceso
                     
                        tx.setNombreEstudiante("Josué Rafael Pérez Aguirre"); 
                        tx.setCarnetEstudiante("0905-24-5462");  
                        tx.setCorreoEstudiante("jpereza62@miumg.edu.gt");
                        
                        String nombreCola = tx.getBancoDestino();
                        
                        
                        channel.queueDeclare(nombreCola, true, false, false, queueArgs); 

                       
                        int prioridad = (tx.getMonto() > 5000) ? 10 : 1;

                        AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                                .priority(prioridad)
                                .build();

                        String jsonMensaje = mapper.writeValueAsString(tx);
                        
                        
                        channel.basicPublish("", nombreCola, props, jsonMensaje.getBytes(StandardCharsets.UTF_8));
                        
                        
                        System.out.println(" [v] Transacción " + tx.getIdTransaccion() + " enviada a cola: " + nombreCola);
                        
                        transaccionesEnviadas++;
                    }
                    System.out.println("\nProceso finalizado. Se enviaron exitosamente " + transaccionesEnviadas + " transacciones.");
                }

            } catch (Exception e) {
                System.err.println("Error durante el procesamiento: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Error crítico de conexión: " + e.getMessage());
        }
    }
}