package org.servicio.router.config;

public class LocalConfigService implements ConfigService {
    private String host;
    private String username;
    private String password;

    public LocalConfigService() {
        host = "localhost";
        username = "guest";
        password = "guest";
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
}
