package com.kafka.ui.service;

import com.kafka.ui.config.ConnectionConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class KafkaService implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);
    private final AdminClient adminClient;
    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;
    private boolean connected = false;

    public KafkaService(ConnectionConfig config) {
        log.info("Initializing Kafka service for {} ({})", config.getName(), config.getBootstrapServers());
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());

        // Add connection timeout settings
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");  // 30 seconds
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "30000");       // 30 seconds
        props.put("default.api.timeout.ms", "30000");                 // 30 seconds
        props.put("connections.max.idle.ms", "30000");                // 30 seconds
        props.put("metadata.max.age.ms", "30000");                    // 30 seconds

        log.debug("Using security protocol: {}", config.getSecurityProtocol());
        // Configure security settings
        props.put("security.protocol", config.getSecurityProtocol().name());
        
        if (config.getSecurityProtocol() != ConnectionConfig.SecurityProtocol.PLAINTEXT) {
            // Configure SASL settings
            if (config.getSecurityProtocol() == ConnectionConfig.SecurityProtocol.SASL_PLAINTEXT 
                || config.getSecurityProtocol() == ConnectionConfig.SecurityProtocol.SASL_SSL) {
                props.put(SaslConfigs.SASL_MECHANISM, config.getSaslMechanism().name());
                String jaasTemplate = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";";
                String jaasConfig = String.format(jaasTemplate, config.getUsername(), config.getPassword());
                props.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
            }

            // Configure SSL settings
            if (config.getSecurityProtocol() == ConnectionConfig.SecurityProtocol.SSL 
                || config.getSecurityProtocol() == ConnectionConfig.SecurityProtocol.SASL_SSL) {
                if (config.getSslTruststorePath() != null) {
                    props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, config.getSslTruststorePath());
                    props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, config.getSslTruststorePassword());
                }
                if (config.getSslKeystorePath() != null) {
                    props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, config.getSslKeystorePath());
                    props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, config.getSslKeystorePassword());
                    props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, config.getSslKeyPassword());
                }
            }
        }

        // Producer specific settings
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        // Consumer specific settings
        Properties consumerProps = new Properties();
        consumerProps.putAll(props);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-ui-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        
        // Add consumer-specific timeout settings
        consumerProps.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "30000");    // 30 seconds
        consumerProps.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");        // 30 seconds
        consumerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "30000");      // 30 seconds
        consumerProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, "10000");     // 10 seconds
        consumerProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");        // 30 seconds

        try {
            log.debug("Creating Kafka admin client...");
            this.adminClient = AdminClient.create(props);
            log.debug("Creating Kafka producer...");
            this.producer = new KafkaProducer<>(props);
            log.debug("Creating Kafka consumer...");
            this.consumer = new KafkaConsumer<>(consumerProps);

            // Test connection
            log.info("Testing connection by listing topics...");
            adminClient.listTopics().names().get();
            connected = true;
            log.info("Successfully connected to Kafka cluster");
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to connect to Kafka cluster: {}", e.getMessage(), e);
            close();
            throw new RuntimeException("Failed to connect to Kafka", e);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public Set<String> listTopics() throws Exception {
        log.debug("Listing topics...");
        try {
            ListTopicsResult topics = adminClient.listTopics();
            Set<String> topicNames = topics.names().get();
            log.info("Found {} topics", topicNames.size());
            log.debug("Topics: {}", topicNames);
            return topicNames;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to list topics: {}", e.getMessage(), e);
            throw new Exception("Failed to list topics", e);
        }
    }

    public void createTopic(String topicName, int partitions, short replicationFactor) throws Exception {
        log.info("Creating topic '{}' with {} partitions and replication factor {}", 
                topicName, partitions, replicationFactor);
        try {
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            adminClient.createTopics(Collections.singleton(newTopic)).all().get();
            log.info("Successfully created topic '{}'", topicName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to create topic '{}': {}", topicName, e.getMessage(), e);
            throw new Exception("Failed to create topic", e);
        }
    }

    public void deleteTopic(String topicName) throws Exception {
        log.info("Deleting topic '{}'", topicName);
        try {
            adminClient.deleteTopics(Collections.singleton(topicName)).all().get();
            log.info("Successfully deleted topic '{}'", topicName);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to delete topic '{}': {}", topicName, e.getMessage(), e);
            throw new Exception("Failed to delete topic", e);
        }
    }

    public void sendMessage(String topic, String key, String value) throws Exception {
        log.debug("Sending message to topic '{}' with key '{}': {}", topic, key, value);
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
            producer.send(record).get();
            log.info("Successfully sent message to topic '{}'", topic);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to send message to topic '{}': {}", topic, e.getMessage(), e);
            throw new Exception("Failed to send message", e);
        }
    }

    public List<ConsumerRecord<String, String>> consumeMessages(String topic) throws Exception {
        log.info("Consuming messages from topic '{}'", topic);
        List<ConsumerRecord<String, String>> result = new ArrayList<>();
        try {
            // Seek to beginning of topic
            consumer.subscribe(Collections.singletonList(topic));
            log.debug("Polling for initial assignment...");
            consumer.poll(Duration.ofMillis(0)); // Get assignment
            consumer.seekToBeginning(consumer.assignment());
            
            // Poll for messages with a longer timeout
            log.debug("Polling for messages with 5 second timeout...");
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            records.forEach(result::add);
            
            log.info("Retrieved {} messages from topic '{}'", result.size(), topic);
            log.debug("Message offsets: {}", 
                     result.stream()
                           .map(r -> String.valueOf(r.offset()))
                           .collect(Collectors.joining(", ")));
            
            return result;
        } catch (Exception e) {
            log.error("Failed to consume messages from topic '{}': {}", topic, e.getMessage(), e);
            throw new Exception("Failed to consume messages: " + e.getMessage(), e);
        } finally {
            try {
                log.debug("Unsubscribing from topic '{}'", topic);
                consumer.unsubscribe();
            } catch (Exception e) {
                log.warn("Error unsubscribing from topic '{}': {}", topic, e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        log.info("Closing Kafka service connections");
        if (consumer != null) {
            try {
                consumer.close();
                log.debug("Closed consumer");
            } catch (Exception e) {
                log.error("Error closing consumer", e);
            }
        }
        if (producer != null) {
            try {
                producer.close();
                log.debug("Closed producer");
            } catch (Exception e) {
                log.error("Error closing producer", e);
            }
        }
        if (adminClient != null) {
            try {
                adminClient.close();
                log.debug("Closed admin client");
            } catch (Exception e) {
                log.error("Error closing admin client", e);
            }
        }
        connected = false;
        log.info("Kafka service connections closed");
    }
}
