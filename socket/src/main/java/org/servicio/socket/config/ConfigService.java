package org.servicio.socket.config;

import java.net.InetSocketAddress;

public interface ConfigService {
    int getSocketPort();
    InetSocketAddress getSocketAddress();
    String getHost();
    String getUsername();
    String getPassword();
    String getExchangeFromSocket();
    String getExchangeToSocket();
}
