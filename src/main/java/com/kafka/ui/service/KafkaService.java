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

public class KafkaService implements AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(KafkaService.class);
    private final AdminClient adminClient;
    private final KafkaProducer<String, String> producer;
    private final KafkaConsumer<String, String> consumer;
    private boolean connected = false;

    public KafkaService(ConnectionConfig config) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());

        // Add connection timeout settings
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");  // 30 seconds
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "30000");       // 30 seconds
        props.put("default.api.timeout.ms", "30000");                 // 30 seconds
        props.put("connections.max.idle.ms", "30000");                // 30 seconds
        props.put("metadata.max.age.ms", "30000");                    // 30 seconds

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
            this.adminClient = AdminClient.create(props);
            this.producer = new KafkaProducer<>(props);
            this.consumer = new KafkaConsumer<>(consumerProps);

            // Test connection
            adminClient.listTopics().names().get();
            connected = true;
        } catch (InterruptedException | ExecutionException e) {
            close();
            throw new RuntimeException("Failed to connect to Kafka", e);
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public Set<String> listTopics() throws Exception {
        try {
            ListTopicsResult topics = adminClient.listTopics();
            return topics.names().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Exception("Failed to list topics", e);
        }
    }

    public void createTopic(String topicName, int partitions, short replicationFactor) throws Exception {
        try {
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            adminClient.createTopics(Collections.singleton(newTopic)).all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Exception("Failed to create topic", e);
        }
    }

    public void deleteTopic(String topicName) throws Exception {
        try {
            adminClient.deleteTopics(Collections.singleton(topicName)).all().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Exception("Failed to delete topic", e);
        }
    }

    public void sendMessage(String topic, String key, String value) throws Exception {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
            producer.send(record).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new Exception("Failed to send message", e);
        }
    }

    public List<ConsumerRecord<String, String>> consumeMessages(String topic) throws Exception {
        List<ConsumerRecord<String, String>> result = new ArrayList<>();
        try {
            // Seek to beginning of topic
            consumer.subscribe(Collections.singletonList(topic));
            consumer.poll(Duration.ofMillis(0)); // Get assignment
            consumer.seekToBeginning(consumer.assignment());
            
            // Poll for messages with a longer timeout
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            records.forEach(result::add);
            
            // Clean up
            consumer.unsubscribe();
            return result;
        } catch (Exception e) {
            log.error("Failed to consume messages from topic {}: {}", topic, e.getMessage());
            throw new Exception("Failed to consume messages: " + e.getMessage(), e);
        } finally {
            try {
                consumer.unsubscribe();
            } catch (Exception e) {
                log.warn("Error unsubscribing from topic {}: {}", topic, e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        if (consumer != null) {
            try {
                consumer.close();
            } catch (Exception e) {
                log.error("Error closing consumer", e);
            }
        }
        if (producer != null) {
            try {
                producer.close();
            } catch (Exception e) {
                log.error("Error closing producer", e);
            }
        }
        if (adminClient != null) {
            try {
                adminClient.close();
            } catch (Exception e) {
                log.error("Error closing admin client", e);
            }
        }
        connected = false;
    }
}
