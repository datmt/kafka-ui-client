package com.kafka.ui.panel;

import net.miginfocom.swing.MigLayout;
import com.kafka.ui.components.StatusBar;
import com.kafka.ui.service.KafkaService;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.SwingWorker;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class BrokerPanel extends JPanel {
    private final JTree brokerTree;
    private final DefaultTreeModel treeModel;
    private final StatusBar statusBar;
    private KafkaService kafkaService;
    private Runnable onTopicChangeCallback;

    public BrokerPanel(StatusBar statusBar) {
        this.statusBar = statusBar;
        setBorder(BorderFactory.createTitledBorder("Broker Management"));
        setLayout(new MigLayout("fill"));

        // Create tree for broker hierarchy
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Clusters");
        treeModel = new DefaultTreeModel(root);
        brokerTree = new JTree(treeModel);

        // Control panel
        JPanel controlPanel = new JPanel(new MigLayout("insets 0"));
        JButton refreshButton = new JButton("Refresh");
        JButton createTopicButton = new JButton("Create Topic");
        JButton deleteTopicButton = new JButton("Delete Topic");

        refreshButton.addActionListener(e -> refreshBrokerInfo());
        createTopicButton.addActionListener(e -> showCreateTopicDialog());
        deleteTopicButton.addActionListener(e -> deleteSelectedTopic());

        controlPanel.add(refreshButton);
        controlPanel.add(createTopicButton);
        controlPanel.add(deleteTopicButton);

        // Add components
        add(new JScrollPane(brokerTree), "grow, push, wrap");
        add(controlPanel, "growx");
    }

    private void refreshBrokerInfo() {
        if (kafkaService == null || !kafkaService.isConnected()) {
            statusBar.setStatus("Not connected to Kafka", true);
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Not Connected");
            treeModel.setRoot(root);
            return;
        }

        statusBar.showProgress("Refreshing broker info...");
        SwingWorker<Set<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected Set<String> doInBackground() throws Exception {
                return kafkaService.listTopics();
            }

            @Override
            protected void done() {
                try {
                    Set<String> topics = get();
                    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Kafka Cluster");
                    DefaultMutableTreeNode topicsNode = new DefaultMutableTreeNode("Topics (" + topics.size() + ")");
                    root.add(topicsNode);

                    // Sort topics alphabetically
                    topics.stream()
                          .sorted()
                          .forEach(topic -> topicsNode.add(new DefaultMutableTreeNode(topic)));

                    treeModel.setRoot(root);
                    brokerTree.expandRow(0); // Auto-expand the root node
                    statusBar.hideProgress();
                    statusBar.setStatus("Found " + topics.size() + " topics", false);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    statusBar.setStatus("Operation interrupted", true);
                } catch (ExecutionException e) {
                    String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    statusBar.setStatus("Failed to refresh topics: " + errorMsg, true);
                } finally {
                    statusBar.hideProgress();
                }
            }
        };
        worker.execute();
    }

    private void showCreateTopicDialog() {
        if (kafkaService == null || !kafkaService.isConnected()) {
            statusBar.setStatus("Not connected to Kafka", true);
            return;
        }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create Topic", true);
        dialog.setLayout(new MigLayout("fillx, wrap 2", "[][grow,fill]"));

        JTextField nameField = new JTextField(20);
        JSpinner partitionsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        JSpinner replicationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));

        dialog.add(new JLabel("Topic Name:"));
        dialog.add(nameField);
        dialog.add(new JLabel("Partitions:"));
        dialog.add(partitionsSpinner);
        dialog.add(new JLabel("Replication Factor:"));
        dialog.add(replicationSpinner);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton createButton = new JButton("Create");
        JButton cancelButton = new JButton("Cancel");

        createButton.addActionListener(e -> {
            String topicName = nameField.getText().trim();
            if (topicName.isEmpty()) {
                statusBar.setStatus("Please enter a topic name", true);
                return;
            }

            int partitions = (Integer) partitionsSpinner.getValue();
            short replicationFactor = ((Integer) replicationSpinner.getValue()).shortValue();

            statusBar.showProgress("Creating topic: " + topicName);
            
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    kafkaService.createTopic(topicName, partitions, replicationFactor);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Check for exceptions
                        dialog.dispose();
                        statusBar.hideProgress();
                        statusBar.setStatus("Topic created: " + topicName, false);
                        refreshBrokerInfo();
                        notifyTopicChange();
                    } catch (Exception ex) {
                        statusBar.hideProgress();
                        statusBar.setStatus("Failed to create topic: " + ex.getMessage(), true);
                    }
                }
            };
            worker.execute();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, "span 2");

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void deleteSelectedTopic() {
        if (kafkaService == null || !kafkaService.isConnected()) {
            statusBar.setStatus("Not connected to Kafka", true);
            return;
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) brokerTree.getLastSelectedPathComponent();
        if (node == null || node.getParent() == null || !node.getParent().toString().startsWith("Topics")) {
            statusBar.setStatus("Please select a topic to delete", true);
            return;
        }

        String topicName = node.toString();
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete topic '" + topicName + "'?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            statusBar.showProgress("Deleting topic: " + topicName);
            
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    kafkaService.deleteTopic(topicName);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusBar.hideProgress();
                        statusBar.setStatus("Topic deleted successfully", false);
                        refreshBrokerInfo();
                        notifyTopicChange();
                    } catch (Exception e) {
                        statusBar.hideProgress();
                        statusBar.setStatus("Failed to delete topic: " + e.getMessage(), true);
                    }
                }
            };
            worker.execute();
        }
    }

    public void setKafkaService(KafkaService kafkaService) {
        this.kafkaService = kafkaService;
        refreshBrokerInfo();
    }

    public void setOnTopicChangeCallback(Runnable callback) {
        this.onTopicChangeCallback = callback;
    }

    private void notifyTopicChange() {
        if (onTopicChangeCallback != null) {
            SwingUtilities.invokeLater(onTopicChangeCallback);
        }
    }
}
