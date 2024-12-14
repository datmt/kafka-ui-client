package com.kafka.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.kafka.ui.frame.MainFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

public class KafkaUIMain {
    private static final Logger log = LoggerFactory.getLogger(KafkaUIMain.class);

    public static void main(String[] args) {
        log.info("Starting Kafka UI application");
        try {
            // Set FlatLaf look and feel
            UIManager.setLookAndFeel(new FlatLightLaf());
            log.debug("Set FlatLaf look and feel");
        } catch (Exception e) {
            log.error("Failed to initialize FlatLaf", e);
        }

        // Create and show GUI on EDT
        SwingUtilities.invokeLater(() -> {
            log.debug("Creating main frame");
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
            log.info("Application UI initialized");
        });
    }
}
