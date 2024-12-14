package com.kafka.ui.frame;

import com.kafka.ui.panel.BrokerPanel;
import com.kafka.ui.panel.ConnectionPanel;
import com.kafka.ui.panel.MessagePanel;
import com.kafka.ui.panel.LogPanel;
import com.kafka.ui.components.StatusBar;
import com.kafka.ui.service.KafkaService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private final ConnectionPanel connectionPanel;
    private final MessagePanel messagePanel;
    private final BrokerPanel brokerPanel;
    private final LogPanel logPanel;
    private final StatusBar statusBar;
    private final JTabbedPane tabbedPane;

    public MainFrame() {
        setTitle("Kafka UI Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);

        // Initialize status bar
        statusBar = new StatusBar();

        // Initialize panels
        messagePanel = new MessagePanel(statusBar);
        brokerPanel = new BrokerPanel(statusBar);
        logPanel = new LogPanel();
        connectionPanel = new ConnectionPanel(statusBar, kafkaService -> {
            brokerPanel.setKafkaService(kafkaService);
            messagePanel.setKafkaService(kafkaService);
        });

        // Initialize tabbed pane
        tabbedPane = new JTabbedPane();

        // Set up topic change callback
        brokerPanel.setOnTopicChangeCallback(() -> messagePanel.refreshTopics());

        // Setup layout
        setupLayout();

        // Setup menu
        setupMenu();
    }

    private void setupLayout() {
        setLayout(new MigLayout("fill, wrap", "[grow]", "[grow][]"));
        
        // Left side panel for connections and broker management
        JPanel leftPanel = new JPanel(new MigLayout("fillx, wrap", "[grow]", "[][]"));
        leftPanel.add(connectionPanel, "grow");
        leftPanel.add(brokerPanel, "grow");

        // Main content panel with messages
        JPanel messageContentPanel = new JPanel(new MigLayout("fill"));
        messageContentPanel.add(messagePanel, "grow");

        // Create tabs
        tabbedPane.addTab("Messages", messageContentPanel);
        tabbedPane.addTab("Logs", logPanel);

        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, tabbedPane);
        splitPane.setDividerLocation(300);
        
        add(splitPane, "grow");
        add(statusBar, "growx");
    }

    private void setupMenu() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        // Connection menu
        JMenu connectionMenu = new JMenu("Connection");
        JMenuItem newConnectionItem = new JMenuItem("New Connection");
        newConnectionItem.addActionListener(e -> connectionPanel.showNewConnectionDialog());
        connectionMenu.add(newConnectionItem);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(connectionMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void showAboutDialog() {
        JOptionPane.showMessageDialog(this,
                "Kafka UI Client\nVersion 1.0\n 2024",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }
}
