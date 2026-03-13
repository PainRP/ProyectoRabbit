# Proyecto: Procesamiento de Transacciones Bancarias con RabbitMQ y Java

Este proyecto implementa un sistema distribuido para el procesamiento y distribución de transacciones bancarias, utilizando una arquitectura de microservicios basada en el patrón Producer-Consumer. El sistema está diseñado para gestionar el flujo de transacciones desde una entidad central hacia diferentes bancos receptores, garantizando la integridad de los datos y la persistencia de los mensajes mediante el uso de colas.

## Enlace del Video

https://drive.google.com/file/d/1v6CWhtt_3XsN-6iV37DELDEjFSC265TA/view?usp=drive_link

## Arquitectura General e Infraestructura
El sistema se compone de dos componentes principales y una instancia de RabbitMQ que actúa como broker de mensajería:

1.  **Componente A (Producer):** Punto de entrada que obtiene los lotes de transacciones desde una API externa y los distribuye en colas específicas.
2.  **RabbitMQ:** Actúa como un búfer intermedio y gestor de mensajería. Proporciona desacoplamiento: el Productor no necesita saber si el Consumidor está disponible. Si el Consumidor está apagado o la API de destino está caída, los mensajes permanecen seguros en las colas.
3.  **Componente B (Consumer):** Escucha las colas de forma centralizada pero independiente, procesa los mensajes y los persiste mediante una API de destino.

## Tecnologías Utilizadas
* **Java 11**: Lenguaje de programación principal.
* **Maven**: Gestión de dependencias y construcción del proyecto.
* **RabbitMQ**: Middleware de mensajería para el desacoplamiento y gestión de colas dinámicas.
* **Jackson**: Librería para el procesamiento, mapeo y (de)serialización de datos JSON.
* **Java HttpClient**: Utilizado para realizar peticiones síncronas (GET y POST) a las APIs REST.

## Detalle de los Componentes

### 1. TransaccionesProducer (Productor)
Encargado de la ingesta de datos, enriquecimiento y su clasificación inicial.
* **Consumo de API Externa (GET):** Utiliza `HttpClient` para obtener un lote de transacciones desde un endpoint de AWS. La respuesta se recibe como una cadena JSON.
* **Mapeo y Enriquecimiento:** Mediante `ObjectMapper` (Jackson), el JSON se convierte en un objeto `LoteTransacciones`. El código recorre cada transacción (máximo 100) y asigna manualmente los metadatos del estudiante (nombre, carnet y correo).
* **Gestión Dinámica de Colas:** Para cada transacción, identifica el campo `bancoDestino` y declara una cola en RabbitMQ con ese nombre. Si la cola no existe, se crea automáticamente con propiedades de durabilidad.
* **Publicación de Mensajes:** La transacción actualizada se convierte nuevamente a JSON y se publica en la cola específica del banco utilizando codificación UTF-8.

### 2. TransaccionesConsumer (Consumidor)
Responsable del procesamiento final, lógica de resiliencia y persistencia de la información.
* **Suscripción Multicanal:** Al iniciar, se conecta y escucha simultáneamente las colas predefinidas (BANRURAL, BAC, BI, GYT) mediante un `DeliverCallback`.
* **Procesamiento y Envío (POST):** Transforma el JSON recibido en objetos Java y construye una petición `POST` hacia la API de almacenamiento con el encabezado `application/json`.
* **Lógica de Reintento y Resiliencia:** * Si ocurre un error de red o servidor, el sistema implementa reintentos automáticos con una pausa de 2 segundos entre cada uno.
* **Confirmación de Mensaje (ACK/NACK):** * **ACK (Acknowledgment):** Confirmación manual enviada solo si la API de destino responde con éxito (200 o 201), eliminando el mensaje de la cola.
    * **NACK (Negative Acknowledgment):** Si tras los reintentos la operación falla, se envía un NACK con la instrucción de reencolar el mensaje (`requeue=true`) para evitar la pérdida de datos.

## Estructura de Datos (Modelos POJOs)
El proyecto utiliza una jerarquía de clases para representar fielmente la información bancaria:
* **LoteTransacciones:** Clase contenedora para mapear la respuesta masiva inicial, gestionando el ID del lote y fecha de generación.
* **Transaccion:** Entidad raíz que almacena montos, moneda, cuentas de origen, banco destino y los datos del estudiante procesador.
* **Detalle:** Contiene información específica como el nombre del beneficiario, tipo de transferencia (ej. Interbancaria) y descripción.
* **Referencias:** Almacena datos de control como números de factura y códigos internos de referencia.

## Requisitos de Ejecución
1. Disponer de una instancia local de RabbitMQ ejecutándose en el puerto por defecto (15672).
2. Configurar el entorno con Java 11 o superior.
3. Ejecutar `mvn clean install` en ambos proyectos para gestionar las dependencias de Jackson y el cliente de RabbitMQ.
