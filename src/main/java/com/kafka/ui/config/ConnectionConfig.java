package com.kafka.ui.config;

import java.io.Serializable;

public class ConnectionConfig implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public enum SecurityProtocol {
        PLAINTEXT,
        SASL_PLAINTEXT,
        SASL_SSL,
        SSL
    }

    public enum SaslMechanism {
        PLAIN,
        SCRAM_SHA_256,
        SCRAM_SHA_512
    }
    
    private String name;
    private String bootstrapServers;
    private SecurityProtocol securityProtocol = SecurityProtocol.PLAINTEXT;
    private SaslMechanism saslMechanism;
    private String username;
    private String password;
    private String sslTruststorePath;
    private String sslTruststorePassword;
    private String sslKeystorePath;
    private String sslKeystorePassword;
    private String sslKeyPassword;

    public ConnectionConfig() {}

    public ConnectionConfig(String name, String bootstrapServers) {
        this.name = name;
        this.bootstrapServers = bootstrapServers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public SecurityProtocol getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(SecurityProtocol securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public SaslMechanism getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(SaslMechanism saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSslTruststorePath() {
        return sslTruststorePath;
    }

    public void setSslTruststorePath(String sslTruststorePath) {
        this.sslTruststorePath = sslTruststorePath;
    }

    public String getSslTruststorePassword() {
        return sslTruststorePassword;
    }

    public void setSslTruststorePassword(String sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

    public String getSslKeystorePath() {
        return sslKeystorePath;
    }

    public void setSslKeystorePath(String sslKeystorePath) {
        this.sslKeystorePath = sslKeystorePath;
    }

    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    public String getSslKeyPassword() {
        return sslKeyPassword;
    }

    public void setSslKeyPassword(String sslKeyPassword) {
        this.sslKeyPassword = sslKeyPassword;
    }

    @Override
    public String toString() {
        return name + " (" + bootstrapServers + ")" + 
               (securityProtocol != SecurityProtocol.PLAINTEXT ? " [" + securityProtocol + "]" : "");
    }
}
