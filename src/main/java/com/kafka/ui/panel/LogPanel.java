package com.kafka.ui.panel;

import com.kafka.ui.components.LogAppender;
import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

public class LogPanel extends JPanel {
    private final JTextArea logArea;
    private final JButton clearButton;
    private final JComboBox<String> logLevelCombo;
    private final JTextField filterField;

    public LogPanel() {
        setLayout(new MigLayout("fill"));

        // Create toolbar
        JPanel toolbar = new JPanel(new MigLayout("fillx", "[][][][push][]"));
        
        // Log level filter
        toolbar.add(new JLabel("Log Level:"));
        logLevelCombo = new JComboBox<>(new String[]{"ALL", "TRACE", "DEBUG", "INFO", "WARN", "ERROR"});
        logLevelCombo.setSelectedItem("INFO");
        toolbar.add(logLevelCombo);

        // Text filter
        toolbar.add(new JLabel("Filter:"), "gap 10");
        filterField = new JTextField(20);
        toolbar.add(filterField);

        // Clear button
        clearButton = new JButton("Clear");
        toolbar.add(clearButton, "gap 10");

        add(toolbar, "north");

        // Create log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        
        // Set up scroll pane
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scrollPane, "grow");

        // Register the log area with the appender
        LogAppender.setLogArea(logArea);

        // Add listeners
        clearButton.addActionListener(e -> logArea.setText(""));

        logLevelCombo.addActionListener(e -> applyFilters());
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
    }

    private void applyFilters() {
        // TODO: Implement log filtering based on level and text
        // This would require keeping a full copy of logs and reapplying filters
        // For now, we'll just keep it simple with the raw logs
    }
}
