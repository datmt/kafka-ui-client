package com.kafka.ui.components;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogAppender extends AppenderBase<ILoggingEvent> {
    private static final BlockingQueue<String> logQueue = new LinkedBlockingQueue<>();
    private static JTextArea logArea;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int MAX_LINES = 1000; // Maximum number of lines to keep in the log area

    public static void setLogArea(JTextArea area) {
        logArea = area;
        // Display any logs that were queued before the log area was set
        while (!logQueue.isEmpty()) {
            String log = logQueue.poll();
            if (log != null) {
                appendToLogArea(log);
            }
        }
    }

    @Override
    protected void append(ILoggingEvent event) {
        String timestamp = dateFormat.format(new Date(event.getTimeStamp()));
        String formattedLog = String.format("[%s] %s - %s: %s%n",
                timestamp,
                event.getLevel(),
                event.getLoggerName(),
                event.getFormattedMessage());

        if (logArea != null) {
            appendToLogArea(formattedLog);
        } else {
            logQueue.offer(formattedLog);
        }
    }

    private static void appendToLogArea(String log) {
        SwingUtilities.invokeLater(() -> {
            if (logArea != null) {
                logArea.append(log);
                
                // Limit the number of lines
                String text = logArea.getText();
                String[] lines = text.split("\n");
                if (lines.length > MAX_LINES) {
                    StringBuilder newText = new StringBuilder();
                    for (int i = lines.length - MAX_LINES; i < lines.length; i++) {
                        newText.append(lines[i]).append("\n");
                    }
                    logArea.setText(newText.toString());
                }
                
                // Auto-scroll to bottom
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }
}
