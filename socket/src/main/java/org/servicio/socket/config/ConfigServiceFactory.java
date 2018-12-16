package org.servicio.socket.config;

public class ConfigServiceFactory {
    public static ConfigService getConfigService() {
        return new LocalConfigService();
    }
}
