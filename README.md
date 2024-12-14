# Kafka UI Client

A lightweight, user-friendly desktop application for managing and interacting with Apache Kafka clusters.

## Features

- **Connection Management**
  - Save and manage multiple Kafka cluster connections
  - Support for secure connections (SASL, SSL)
  - Quick connection switching

- **Message Management**
  - View messages from Kafka topics
  - Produce new messages to topics
  - Real-time message preview
  - Filter messages by topic

- **Broker Management**
  - View broker information
  - Monitor broker status
  - Manage topics

- **User Interface**
  - Modern, intuitive interface
  - Split-panel design for easy navigation
  - Status bar for operation feedback
  - Comprehensive logging system

## Prerequisites

- Java 11 or higher
- Apache Kafka 2.x or higher
- Maven (for building from source)

## Building from Source

1. Clone the repository:
```bash
git clone https://github.com/yourusername/kafka-ui.git
cd kafka-ui
```

2. Build with Maven:
```bash
mvn clean package
```

The built JAR file will be located in the `target` directory.

## Running the Application

Run the application using:
```bash
java -jar target/kafka-ui-1.0.jar
```

## Usage Guide

1. **Creating a Connection**
   - Click "New Connection" in the Connection menu
   - Enter your Kafka broker details
   - Save the connection for future use

2. **Viewing Messages**
   - Select a connection from the list
   - Choose a topic from the dropdown
   - Messages will be displayed in the main panel
   - Click on a message to view its contents

3. **Producing Messages**
   - Select your target topic
   - Click "Produce Message"
   - Enter your message content
   - Click "Send" to publish

4. **Managing Topics**
   - Use the broker panel to view and manage topics
   - Create new topics
   - Delete existing topics
   - View topic configurations

## Configuration

The application stores connection configurations in the user's home directory under `.kafka-ui/config.json`. This file contains saved connection details and user preferences.

## Security

- Supports SASL/PLAIN authentication
- SSL/TLS encryption for secure connections
- Credentials are stored securely using system keystore

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For issues and feature requests, please use the GitHub issue tracker.

---
Built with ❤️ using Java Swing and Apache Kafka
