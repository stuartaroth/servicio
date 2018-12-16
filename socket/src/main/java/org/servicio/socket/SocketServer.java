package org.servicio.socket;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.servicio.socket.config.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

public class SocketServer extends WebSocketServer {
    private static Logger logger = LoggerFactory.getLogger(SocketServer.class);

    private ConfigService configService;

    private ConcurrentHashMap<String, WebSocket> connectionsById;
    private ConcurrentHashMap<WebSocket, String> connectionsByConnection;

    private ConnectionFactory connectionFactory;
    private Connection connection;
    private Channel channel;

    public SocketServer(ConfigService configService) throws Exception {
        super(configService.getSocketAddress());
        this.configService = configService;
        setupConnectionMaps();
        setupRabbit();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        createEntry(conn);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        removeEntry(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            channel.basicPublish(configService.getExchangeFromSocket(), "", null, message.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {

        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn.isClosing() || conn.isClosed()) {
            removeEntry(conn);
        }
    }

    @Override
    public void onStart() {

    }

    private void setupConnectionMaps() {
        connectionsById = new ConcurrentHashMap<>();
        connectionsByConnection = new ConcurrentHashMap<>();
    }

    private void createEntry(WebSocket conn) {
        String id = IdService.generateId();
        connectionsById.put(id, conn);
        connectionsByConnection.put(conn, id);
    }

    private void removeEntry(WebSocket conn) {
        String id = connectionsByConnection.get(conn);
        connectionsById.remove(id);
        connectionsByConnection.remove(conn);
    }

    private void setupRabbit() throws Exception {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(configService.getHost());
        connectionFactory.setUsername(configService.getUsername());
        connectionFactory.setPassword(configService.getPassword());


        connection = connectionFactory.newConnection();
        channel = connection.createChannel();

        // messages-to-socket
        channel.exchangeDeclare(configService.getExchangeToSocket(), "fanout");

        // messages-from-socket
        channel.exchangeDeclare(configService.getExchangeFromSocket(), "direct");
    }
}
