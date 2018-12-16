package org.servicio.socket.config;

import java.net.InetSocketAddress;

public class LocalConfigService implements ConfigService {
    private int socketPort;
    private InetSocketAddress socketAddress;
    private String host;
    private String username;
    private String password;
    private String exchangeFromSocket;
    private String exchangeToSocket;


    public LocalConfigService() {
        socketPort = 8888;
        socketAddress = new InetSocketAddress(socketPort);
        host = "localhost";
        username = "guest";
        password = "guest";
        exchangeFromSocket = "messages-from-socket";
        exchangeToSocket = "messages-to-socket";
    }

    @Override
    public int getSocketPort() {
        return socketPort;
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        return socketAddress;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getExchangeFromSocket() {
        return exchangeFromSocket;
    }

    @Override
    public String getExchangeToSocket() {
        return exchangeToSocket;
    }
}
