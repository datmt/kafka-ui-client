package com.kafka.ui.service;

import com.kafka.ui.config.ConnectionConfig;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigService {
    private static final String CONFIG_FILE = System.getProperty("user.home") + "/.kafka-ui/connections.dat";

    public void saveConnections(List<ConnectionConfig> connections) {
        File configDir = new File(CONFIG_FILE).getParentFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(CONFIG_FILE))) {
            oos.writeObject(connections);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public List<ConnectionConfig> loadConnections() {
        if (!new File(CONFIG_FILE).exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(CONFIG_FILE))) {
            return (List<ConnectionConfig>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
