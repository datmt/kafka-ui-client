package com.kafka.ui.components;

import net.miginfocom.swing.MigLayout;
import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private Timer clearTimer;

    public StatusBar() {
        setLayout(new MigLayout("insets 2", "[grow][]"));
        setBorder(BorderFactory.createEtchedBorder());

        statusLabel = new JLabel(" ");
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(100, 16));
        progressBar.setVisible(false);

        add(statusLabel, "grow");
        add(progressBar);

        clearTimer = new Timer(3000, e -> clearStatus());
        clearTimer.setRepeats(false);
    }

    public void setStatus(String message, boolean isError) {
        if (clearTimer.isRunning()) {
            clearTimer.stop();
        }
        statusLabel.setText(message);
        statusLabel.setForeground(isError ? Color.RED : Color.BLACK);
        clearTimer.restart();
    }

    public void showProgress(String message) {
        statusLabel.setText(message);
        statusLabel.setForeground(Color.BLACK);
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);
    }

    public void hideProgress() {
        progressBar.setVisible(false);
        progressBar.setIndeterminate(false);
        clearStatus();
    }

    private void clearStatus() {
        statusLabel.setText(" ");
        statusLabel.setForeground(Color.BLACK);
    }

    public boolean isProgressVisible() {
        return progressBar.isVisible();
    }
}
