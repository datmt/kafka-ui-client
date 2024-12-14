package com.kafka.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.kafka.ui.frame.MainFrame;

import javax.swing.*;

public class KafkaUIMain {
    public static void main(String[] args) {
        try {
            // Set FlatLaf look and feel
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf");
        }

        // Create and show GUI on EDT
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
