package com.kafka.ui.panel;

import com.kafka.ui.components.StatusBar;
import com.kafka.ui.config.ConnectionConfig;
import com.kafka.ui.service.ConfigService;
import com.kafka.ui.service.KafkaService;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConnectionPanel extends JPanel {
    private static final Logger log = LoggerFactory.getLogger(ConnectionPanel.class);
    
    private final DefaultListModel<ConnectionConfig> connectionsModel;
    private final JList<ConnectionConfig> connectionsList;
    private final StatusBar statusBar;
    private final Consumer<KafkaService> onConnect;
    private final ConfigService configService;
    private KafkaService currentKafkaService;

    public ConnectionPanel(StatusBar statusBar, Consumer<KafkaService> onConnect) {
        this.statusBar = statusBar;
        this.onConnect = onConnect;
        this.configService = new ConfigService();
        
        setLayout(new MigLayout("fill, wrap", "[grow]", "[grow][]"));
        setBorder(BorderFactory.createTitledBorder("Connections"));

        // Initialize connection list
        connectionsModel = new DefaultListModel<>();
        connectionsList = new JList<>(connectionsModel);
        connectionsList.setCellRenderer(new ConnectionListCellRenderer());
        connectionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add double-click listener
        connectionsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    connectToSelected();
                }
            }
        });
        
        // Load saved connections
        loadSavedConnections();

        // Add buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton connectButton = new JButton("Connect");
        JButton removeButton = new JButton("Remove");

        addButton.addActionListener(e -> showNewConnectionDialog());
        editButton.addActionListener(e -> editSelectedConnection());
        connectButton.addActionListener(e -> connectToSelected());
        removeButton.addActionListener(e -> removeSelected());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(connectButton);
        buttonPanel.add(removeButton);

        // Add components - list first, then buttons below
        add(new JScrollPane(connectionsList), "grow");
        add(buttonPanel, "growx");
    }

    private void loadSavedConnections() {
        log.debug("Loading saved connections");
        List<ConnectionConfig> savedConnections = configService.loadConnections();
        connectionsModel.clear();
        for (ConnectionConfig config : savedConnections) {
            connectionsModel.addElement(config);
        }
        log.info("Loaded {} connections", savedConnections.size());
    }

    private void saveConnections() {
        log.debug("Saving connections");
        List<ConnectionConfig> connections = new ArrayList<>();
        for (int i = 0; i < connectionsModel.size(); i++) {
            connections.add(connectionsModel.getElementAt(i));
        }
        configService.saveConnections(connections);
        log.info("Saved {} connections", connections.size());
    }

    private void editSelectedConnection() {
        ConnectionConfig selected = connectionsList.getSelectedValue();
        if (selected == null) {
            statusBar.setStatus("Please select a connection to edit", true);
            return;
        }

        log.debug("Editing connection: {}", selected.getName());
        showConnectionDialog("Edit Connection", selected);
    }

    public void showNewConnectionDialog() {
        log.debug("Showing new connection dialog");
        showConnectionDialog("New Connection", null);
    }

    private void showConnectionDialog(String title, ConnectionConfig config) {
        log.debug("Showing {} dialog", title);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), title, true);
        dialog.setLayout(new MigLayout("fillx, wrap 2", "[][grow,fill]", ""));

        // Basic settings
        JTextField nameField = new JTextField(20);
        JTextField serversField = new JTextField(20);

        // Security settings
        JComboBox<ConnectionConfig.SecurityProtocol> protocolCombo = new JComboBox<>(ConnectionConfig.SecurityProtocol.values());
        JComboBox<ConnectionConfig.SaslMechanism> mechanismCombo = new JComboBox<>(ConnectionConfig.SaslMechanism.values());
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        
        // SSL settings
        JTextField truststorePathField = new JTextField();
        JPasswordField truststorePasswordField = new JPasswordField();
        JTextField keystorePathField = new JTextField();
        JPasswordField keystorePasswordField = new JPasswordField();
        JPasswordField keyPasswordField = new JPasswordField();

        // File chooser buttons for SSL files
        JButton chooseTruststoreButton = new JButton("...");
        JButton chooseKeystoreButton = new JButton("...");
        
        chooseTruststoreButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                truststorePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });
        
        chooseKeystoreButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                keystorePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        // Basic settings panel
        dialog.add(new JLabel("Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Bootstrap Servers:"));
        dialog.add(serversField);
        dialog.add(new JLabel("Security Protocol:"));
        dialog.add(protocolCombo);

        // SASL settings panel
        JPanel saslPanel = new JPanel(new MigLayout("fillx, wrap 2", "[][grow,fill]", ""));
        saslPanel.setBorder(BorderFactory.createTitledBorder("SASL Settings"));
        saslPanel.add(new JLabel("SASL Mechanism:"));
        saslPanel.add(mechanismCombo);
        saslPanel.add(new JLabel("Username:"));
        saslPanel.add(usernameField);
        saslPanel.add(new JLabel("Password:"));
        saslPanel.add(passwordField);
        dialog.add(saslPanel, "span 2, growx");

        // SSL settings panel
        JPanel sslPanel = new JPanel(new MigLayout("fillx, wrap 3", "[][grow,fill][]", ""));
        sslPanel.setBorder(BorderFactory.createTitledBorder("SSL Settings"));
        sslPanel.add(new JLabel("Truststore Path:"));
        sslPanel.add(truststorePathField);
        sslPanel.add(chooseTruststoreButton, "width 25!");
        sslPanel.add(new JLabel("Truststore Password:"));
        sslPanel.add(truststorePasswordField, "span 2");
        sslPanel.add(new JLabel("Keystore Path:"));
        sslPanel.add(keystorePathField);
        sslPanel.add(chooseKeystoreButton, "width 25!");
        sslPanel.add(new JLabel("Keystore Password:"));
        sslPanel.add(keystorePasswordField, "span 2");
        sslPanel.add(new JLabel("Key Password:"));
        sslPanel.add(keyPasswordField, "span 2");
        dialog.add(sslPanel, "span 2, growx");

        // Enable/disable panels based on protocol selection
        protocolCombo.addActionListener(e -> {
            ConnectionConfig.SecurityProtocol protocol = (ConnectionConfig.SecurityProtocol) protocolCombo.getSelectedItem();
            boolean isSasl = protocol == ConnectionConfig.SecurityProtocol.SASL_PLAINTEXT || 
                           protocol == ConnectionConfig.SecurityProtocol.SASL_SSL;
            boolean isSsl = protocol == ConnectionConfig.SecurityProtocol.SSL || 
                          protocol == ConnectionConfig.SecurityProtocol.SASL_SSL;
            
            for (Component c : saslPanel.getComponents()) {
                c.setEnabled(isSasl);
            }
            for (Component c : sslPanel.getComponents()) {
                c.setEnabled(isSsl);
            }
        });

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton(config == null ? "Add" : "Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String servers = serversField.getText().trim();

            if (name.isEmpty() || servers.isEmpty()) {
                statusBar.setStatus("Name and bootstrap servers are required", true);
                return;
            }

            ConnectionConfig newConfig = config == null ? new ConnectionConfig() : config;
            newConfig.setName(name);
            newConfig.setBootstrapServers(servers);
            newConfig.setSecurityProtocol((ConnectionConfig.SecurityProtocol) protocolCombo.getSelectedItem());
            
            ConnectionConfig.SecurityProtocol protocol = (ConnectionConfig.SecurityProtocol) protocolCombo.getSelectedItem();
            if (protocol == ConnectionConfig.SecurityProtocol.SASL_PLAINTEXT || 
                protocol == ConnectionConfig.SecurityProtocol.SASL_SSL) {
                newConfig.setSaslMechanism((ConnectionConfig.SaslMechanism) mechanismCombo.getSelectedItem());
                newConfig.setUsername(usernameField.getText().trim());
                newConfig.setPassword(new String(passwordField.getPassword()));
            }
            
            if (protocol == ConnectionConfig.SecurityProtocol.SSL || 
                protocol == ConnectionConfig.SecurityProtocol.SASL_SSL) {
                newConfig.setSslTruststorePath(truststorePathField.getText().trim());
                newConfig.setSslTruststorePassword(new String(truststorePasswordField.getPassword()));
                newConfig.setSslKeystorePath(keystorePathField.getText().trim());
                newConfig.setSslKeystorePassword(new String(keystorePasswordField.getPassword()));
                newConfig.setSslKeyPassword(new String(keyPasswordField.getPassword()));
            }

            if (config == null) {
                connectionsModel.addElement(newConfig);
            } else {
                int index = connectionsModel.indexOf(config);
                connectionsModel.setElementAt(newConfig, index);
            }

            saveConnections();
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, "span 2, growx");

        // Set initial values if editing
        if (config != null) {
            nameField.setText(config.getName());
            serversField.setText(config.getBootstrapServers());
            protocolCombo.setSelectedItem(config.getSecurityProtocol());
            mechanismCombo.setSelectedItem(config.getSaslMechanism());
            usernameField.setText(config.getUsername());
            passwordField.setText(config.getPassword());
            truststorePathField.setText(config.getSslTruststorePath());
            truststorePasswordField.setText(config.getSslTruststorePassword());
            keystorePathField.setText(config.getSslKeystorePath());
            keystorePasswordField.setText(config.getSslKeystorePassword());
            keyPasswordField.setText(config.getSslKeyPassword());
            
            // Trigger the protocol change listener
            protocolCombo.getActionListeners()[0].actionPerformed(
                new ActionEvent(protocolCombo, ActionEvent.ACTION_PERFORMED, null)
            );
        }

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void connectToSelected() {
        ConnectionConfig selected = connectionsList.getSelectedValue();
        if (selected == null) {
            log.warn("No connection selected");
            showErrorDialog("Connection Error", "Please select a connection first.");
            statusBar.setStatus("Please select a connection", true);
            return;
        }

        log.info("Connecting to selected connection: {}", selected.getName());
        statusBar.showProgress("Connecting to " + selected.getName() + "...");

        SwingWorker<KafkaService, Void> worker = new SwingWorker<>() {
            @Override
            protected KafkaService doInBackground() throws Exception {
                log.debug("Creating new KafkaService instance for {}", selected.getName());
                KafkaService service = new KafkaService(selected);
                return service;
            }

            @Override
            protected void done() {
                try {
                    KafkaService service = get();
                    if (currentKafkaService != null) {
                        try {
                            log.debug("Closing existing Kafka service");
                            currentKafkaService.close();
                        } catch (Exception e) {
                            log.warn("Error closing existing Kafka service", e);
                        }
                    }
                    currentKafkaService = service;
                    onConnect.accept(service);
                    log.info("Successfully connected to {}", selected.getName());
                    statusBar.setStatus("Connected to " + selected.getName(), false);
                } catch (Exception e) {
                    String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    log.error("Failed to connect: {}", errorMsg, e);
                    
                    // Create a user-friendly error message
                    String userErrorMsg = getUserFriendlyErrorMessage(e);
                    showErrorDialog("Connection Failed", userErrorMsg);
                    
                    // Update status bar
                    statusBar.setStatus("Failed to connect: " + userErrorMsg, true);
                    
                    // Clear the current service
                    currentKafkaService = null;
                } finally {
                    statusBar.hideProgress();
                }
            }
        };
        worker.execute();
    }

    private String getUserFriendlyErrorMessage(Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String originalMessage = cause.getMessage();

        // Common Kafka connection errors and their user-friendly messages
        if (cause instanceof java.net.ConnectException) {
            return "Could not connect to the Kafka broker. Please check:\n" +
                   "• If the broker address is correct\n" +
                   "• If the broker is running\n" +
                   "• If there are any network issues or firewalls blocking the connection";
        } else if (cause instanceof java.net.UnknownHostException) {
            return "Could not find the Kafka broker. Please check:\n" +
                   "• If the broker address is spelled correctly\n" +
                   "• If your DNS is working properly\n" +
                   "• If you have internet connectivity";
        } else if (originalMessage != null && originalMessage.contains("SASL")) {
            return "Authentication failed. Please check:\n" +
                   "• If your username is correct\n" +
                   "• If your password is correct\n" +
                   "• If you're using the correct authentication mechanism";
        } else if (originalMessage != null && originalMessage.contains("SSL")) {
            return "SSL/TLS connection failed. Please check:\n" +
                   "• If your SSL certificates are valid\n" +
                   "• If the truststore/keystore paths are correct\n" +
                   "• If the certificate passwords are correct";
        } else if (cause instanceof java.util.concurrent.TimeoutException) {
            return "Connection timed out. Please check:\n" +
                   "• If the broker is responding\n" +
                   "• If there are any network issues\n" +
                   "• Try increasing the connection timeout";
        }

        // If we don't have a specific user-friendly message, make the technical message more readable
        return "Connection failed: " + (originalMessage != null ? originalMessage : "Unknown error");
    }

    private void showErrorDialog(String title, String message) {
        SwingUtilities.invokeLater(() -> {
            JTextArea textArea = new JTextArea(message);
            textArea.setEditable(false);
            textArea.setBackground(null);
            textArea.setWrapStyleWord(true);
            textArea.setLineWrap(true);
            
            // Calculate preferred size based on message length
            int preferredWidth = Math.min(400, textArea.getPreferredSize().width);
            int preferredHeight = Math.min(300, textArea.getPreferredSize().height);
            textArea.setPreferredSize(new Dimension(preferredWidth, preferredHeight));

            JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                textArea,
                title,
                JOptionPane.ERROR_MESSAGE
            );
        });
    }

    private void removeSelected() {
        int selectedIndex = connectionsList.getSelectedIndex();
        if (selectedIndex != -1) {
            ConnectionConfig config = connectionsModel.getElementAt(selectedIndex);
            log.debug("Removing connection: {}", config.getName());
            connectionsModel.remove(selectedIndex);
            saveConnections();
            statusBar.setStatus("Connection removed: " + config.getName(), false);
        }
    }

    public void closeCurrentConnection() {
        if (currentKafkaService != null) {
            try {
                log.info("Closing current Kafka connection...");
                currentKafkaService.close();
                log.info("Kafka connection closed successfully");
            } catch (Exception e) {
                log.error("Error closing Kafka connection", e);
                throw e;
            } finally {
                currentKafkaService = null;
            }
        }
    }

    private static class ConnectionListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ConnectionConfig) {
                ConnectionConfig config = (ConnectionConfig) value;
                setText(config.getName() + " (" + config.getBootstrapServers() + ")");
            }
            return this;
        }
    }
}
