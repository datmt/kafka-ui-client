package com.kafka.ui.panel;

import net.miginfocom.swing.MigLayout;
import com.kafka.ui.components.StatusBar;
import com.kafka.ui.service.KafkaService;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingWorker;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public class MessagePanel extends JPanel {
    private final JTable messagesTable;
    private final DefaultTableModel tableModel;
    private final JTextArea messageContentArea;
    private final StatusBar statusBar;
    private final JComboBox<String> topicComboBox;
    private KafkaService kafkaService;

    public MessagePanel(StatusBar statusBar) {
        this.statusBar = statusBar;
        setLayout(new MigLayout("fill"));

        // Create table for messages
        String[] columnNames = {"Offset", "Partition", "Timestamp", "Key", "Preview"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        messagesTable = new JTable(tableModel);
        messagesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Create message content viewer
        messageContentArea = new JTextArea();
        messageContentArea.setEditable(false);

        // Topic selection and controls
        JPanel topPanel = new JPanel(new MigLayout("fillx", "[][grow][][]"));
        topicComboBox = new JComboBox<>();
        JButton refreshButton = new JButton("Refresh");
        JButton produceButton = new JButton("Produce Message");

        topPanel.add(new JLabel("Topic:"));
        topPanel.add(topicComboBox, "growx");
        topPanel.add(refreshButton);
        topPanel.add(produceButton);

        // Split pane for messages table and content viewer
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(messagesTable),
                new JScrollPane(messageContentArea));
        splitPane.setResizeWeight(0.6);

        // Add components to panel
        add(topPanel, "wrap, growx");
        add(splitPane, "grow");

        // Add listeners
        messagesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateMessageContent();
            }
        });

        refreshButton.addActionListener(e -> refreshMessages());
        produceButton.addActionListener(e -> showProduceMessageDialog());
        
        // Add topic selection listener
        topicComboBox.addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged") && topicComboBox.getSelectedItem() != null) {
                refreshMessages();
            }
        });
    }

    private void updateMessageContent() {
        int selectedRow = messagesTable.getSelectedRow();
        if (selectedRow != -1) {
            String preview = (String) tableModel.getValueAt(selectedRow, 4);
            if (preview != null) {
                messageContentArea.setText(preview);
            } else {
                messageContentArea.setText("");
            }
        } else {
            messageContentArea.setText("");
        }
    }

    private void updateTopicList() {
        if (kafkaService == null || !kafkaService.isConnected()) {
            statusBar.setStatus("Not connected to Kafka", true);
            return;
        }

        statusBar.showProgress("Refreshing topic list...");
        SwingWorker<Set<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected Set<String> doInBackground() throws Exception {
                return kafkaService.listTopics();
            }

            @Override
            protected void done() {
                try {
                    Set<String> topics = get();
                    String selectedTopic = (String) topicComboBox.getSelectedItem();
                    topicComboBox.removeAllItems();
                    topics.forEach(topic -> topicComboBox.addItem(topic));
                    if (selectedTopic != null && topics.contains(selectedTopic)) {
                        topicComboBox.setSelectedItem(selectedTopic);
                    }
                    statusBar.hideProgress();
                } catch (Exception e) {
                    statusBar.hideProgress();
                    statusBar.setStatus("Failed to refresh topics: " + e.getMessage(), true);
                }
            }
        };
        worker.execute();
    }

    private void refreshMessages() {
        if (kafkaService == null || !kafkaService.isConnected()) {
            statusBar.setStatus("Not connected to Kafka", true);
            statusBar.hideProgress();
            return;
        }

        String selectedTopic = (String) topicComboBox.getSelectedItem();
        if (selectedTopic == null) {
            tableModel.setRowCount(0);
            messageContentArea.setText("");
            statusBar.hideProgress();
            return;
        }

        // Only show progress if it's not already showing
        if (!statusBar.isProgressVisible()) {
            statusBar.showProgress("Loading messages from " + selectedTopic + "...");
        }
        
        // Clear existing messages
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            messageContentArea.setText("");
        });

        SwingWorker<List<ConsumerRecord<String, String>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<ConsumerRecord<String, String>> doInBackground() throws Exception {
                return kafkaService.consumeMessages(selectedTopic);
            }

            @Override
            protected void done() {
                try {
                    List<ConsumerRecord<String, String>> records = get();
                    SwingUtilities.invokeLater(() -> {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        for (ConsumerRecord<String, String> record : records) {
                            String timestamp = sdf.format(new Date(record.timestamp()));
                            String key = record.key() != null ? record.key() : "";
                            String preview = record.value();
                            if (preview != null && preview.length() > 100) {
                                preview = preview.substring(0, 97) + "...";
                            }
                            tableModel.addRow(new Object[]{
                                record.offset(),
                                record.partition(),
                                timestamp,
                                key,
                                preview
                            });
                        }
                        statusBar.setStatus("Loaded " + records.size() + " messages", false);
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        statusBar.setStatus("Failed to load messages: " + e.getMessage(), true);
                    });
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        statusBar.hideProgress();
                    });
                }
            }
        };
        worker.execute();
    }

    private void showProduceMessageDialog() {
        if (kafkaService == null || !kafkaService.isConnected()) {
            statusBar.setStatus("Not connected to Kafka", true);
            return;
        }

        String selectedTopic = (String) topicComboBox.getSelectedItem();
        if (selectedTopic == null) {
            statusBar.setStatus("Please select a topic", true);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Produce Message", true);
        dialog.setLayout(new MigLayout("fillx, wrap 2", "[][grow,fill]"));

        JTextField keyField = new JTextField();
        JTextArea valueArea = new JTextArea(5, 30);
        valueArea.setLineWrap(true);
        valueArea.setWrapStyleWord(true);

        dialog.add(new JLabel("Topic:"));
        dialog.add(new JLabel(selectedTopic));
        dialog.add(new JLabel("Key:"));
        dialog.add(keyField);
        dialog.add(new JLabel("Value:"));
        dialog.add(new JScrollPane(valueArea));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton sendButton = new JButton("Send");
        JButton cancelButton = new JButton("Cancel");

        sendButton.addActionListener(e -> {
            String key = keyField.getText().trim();
            String value = valueArea.getText().trim();

            if (value.isEmpty()) {
                statusBar.setStatus("Message value cannot be empty", true);
                return;
            }

            statusBar.showProgress("Sending message to " + selectedTopic + "...");
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    kafkaService.sendMessage(selectedTopic, key.isEmpty() ? null : key, value);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        dialog.dispose();
                        statusBar.setStatus("Message sent successfully", false);
                        // Refresh messages after sending
                        SwingUtilities.invokeLater(() -> {
                            try {
                                Thread.sleep(500); // Small delay to ensure message is available
                                refreshMessages();
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                            }
                        });
                    } catch (Exception ex) {
                        statusBar.setStatus("Failed to send message: " + ex.getMessage(), true);
                        statusBar.hideProgress();
                    }
                }
            };
            worker.execute();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(sendButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, "span 2");

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public void setKafkaService(KafkaService kafkaService) {
        this.kafkaService = kafkaService;
        updateTopicList();
    }

    public void refreshTopics() {
        updateTopicList();
    }
}
