package org.servicio.router;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.servicio.router.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import static org.servicio.router.ConstantsService.*;

public class RouterWorker {
    private static Logger logger = LoggerFactory.getLogger(RouterWorker.class);

    private ConfigService configService;

    private Schema routeRegistrationSchema;

    private ConcurrentHashMap<String, String> routeMap;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;

    public RouterWorker(ConfigService configService) throws Exception {
        this.configService = configService;
        setupJsonSchemas();
        setupRouteMap();
        setupRabbit();
    }

    private void setupJsonSchemas() throws Exception {
        try (InputStream inputStream = this.getClass().getResourceAsStream("/route_registration.json")) {
            JSONObject rawSchema = new JSONObject(new JSONTokener(inputStream));
            routeRegistrationSchema = SchemaLoader.load(rawSchema);
        }
    }

    private void setupRouteMap() {
        routeMap = new ConcurrentHashMap<>();
    }

    private void setupRabbit() throws Exception {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(configService.getHost());
        connectionFactory.setUsername(configService.getUsername());
        connectionFactory.setPassword(configService.getPassword());

        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        // route registration
        channel.exchangeDeclare(ROUTE_REGISTRATION_EXCHANGE, FANOUT);
        String routeRegistrationQueue = channel.queueDeclare().getQueue();
        channel.queueBind(routeRegistrationQueue, ROUTE_REGISTRATION_EXCHANGE, EMPTY_ROUTING_KEY);
        channel.basicConsume(routeRegistrationQueue, true, (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(message);
                routeRegistrationSchema.validate(jsonObject);

                String messageType = jsonObject.getString(MESSAGE_TYPE);
                String queueName = jsonObject.getString(QUEUE_NAME);

                channel.queueDeclare(queueName, true, false, false, null);
                routeMap.put(messageType, queueName);
            } catch (Exception e) {
                logger.error("error on consume", e);
            }
        }, (consumerTag, sig) -> {
            logger.info(consumerTag);
            logger.info(sig.toString());
        });

        // messages-for-socket
        channel.exchangeDeclare(MESSAGES_FOR_SOCKET_EXCHANGE, FANOUT);

        // messages-from-socket
        channel.queueDeclare(MESSAGES_FROM_SOCKET_QUEUE, true, false, false, null);
        channel.basicConsume(MESSAGES_FROM_SOCKET_QUEUE, true, (consumerTag, delivery) -> {
            try {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JSONObject jsonObject = new JSONObject(message);

                String socketId = jsonObject.getString(SOCKET_ID);
                String messageType = jsonObject.getString(MESSAGE_TYPE);

                jsonObject.remove(SOCKET_ID);

                if (!routeMap.containsKey(messageType)) {
                    JSONObject payload = new JSONObject();
                    payload.put(MESSAGE_TYPE, ERROR);
                    payload.put(ERROR_MESSAGE, "Unsupported messageType: " + messageType);
                    payload.put(ORIGINAL_MESSAGE, jsonObject.toString());

                    JSONObject errorMessage = new JSONObject();
                    errorMessage.put(SOCKET_ID, socketId);
                    errorMessage.put(PAYLOAD, payload.toString());

                    channel.basicPublish(MESSAGES_FOR_SOCKET_EXCHANGE, EMPTY_ROUTING_KEY, null, errorMessage.toString().getBytes(StandardCharsets.UTF_8));
                } else {
                    String queueName  = routeMap.get(messageType);
                    channel.basicPublish(DEFAULT_EXCHANGE, queueName, null, jsonObject.toString().getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                logger.error("error on consume", e);
            }
        }, (consumerTag, sig) -> {

        });
    }
}
