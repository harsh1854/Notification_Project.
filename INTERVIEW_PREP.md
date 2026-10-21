# Scalable Notification System — Interview Preparation

**Target:** Mid-level Java Backend Developer (2–3 years experience)

**Tech stack:** Java 17, Spring Boot 4.1.1, Spring Web MVC, Bean Validation, Apache Kafka, Docker

## 1. Project objective

Build a notification system that accepts notification requests through a REST API and processes notification events asynchronously using Kafka. The intended channels are EMAIL, SMS and PUSH.

The planned evolution includes a Kafka consumer/worker, retry handling, dead-letter topics, monitoring, Redis and deployment infrastructure. These are planned capabilities, not yet completed features.

## 2. Current architecture

Client → Spring Boot REST API → Notification Service → Kafka Producer → `notification-events` topic

The Kafka consumer/worker is the next planned component.

### Why Kafka?

Kafka allows the API to hand off notification events to a messaging system rather than requiring the API to perform the entire notification-delivery operation synchronously.

**Benefits:** Asynchronous processing, buffering, partition-based parallelism and decoupling producers from consumers.

**Trade-offs:** Additional infrastructure, serialization/configuration complexity, delivery guarantees and operational monitoring requirements.

## 3. Codebase deep dive

- `NotificationController`: Exposes `POST /api/notifications` and accepts a validated request.
- `NotificationRequest`: DTO containing `recipient`, `message` and `channel`; uses `@NotBlank` for required text fields.
- `NotificationChannel`: Enum containing `EMAIL`, `SMS` and `PUSH`.
- `NotificationResponse`: Represents the API response.
- `NotificationService`: Coordinates notification request processing.
- `GlobalExceptionHandler`: Uses `@RestControllerAdvice` to return structured validation errors.
- `NotificationEvent`: Java record carrying `recipient`, `message` and `channel`.
- `NotificationProducer`: Uses `KafkaTemplate` to publish an event to `notification-events`, using the recipient as the Kafka record key.
- `KafkaTopicConfig`: Declares a topic with three partitions and one replica.
- `application.properties`: Configures the Kafka bootstrap server and producer serialization.

### Important design decisions

**DTOs:** Separate the external API contract from the internal event representation.

**Enum for channel:** Restricts channel values to a defined set rather than accepting arbitrary strings.

**Validation:** `@Valid` triggers request validation, while `@NotBlank` rejects blank or missing text values. Channel validation and default handling still need consideration.

**Constructor injection:** Makes dependencies explicit and supports straightforward testing.

**Kafka record key:** Using the recipient as the key can keep events for the same recipient in the same partition, provided the partitioning strategy and partition count remain compatible. It can also create uneven partition loads if a few recipients dominate traffic.

**Three partitions, one replica:** Provides multiple partitions for potential parallelism, but a single replica does not provide broker-level replication resilience.

## 4. Challenges and solutions encountered

### Challenge A: Missing KafkaTemplate bean

**Symptom:** Spring could not find a `KafkaTemplate` bean.

**Resolution:** Replaced the direct `spring-kafka` dependency with `spring-boot-starter-kafka` and confirmed that the application started.

**Learning:** Spring Boot starters can supply the integration and auto-configuration dependencies expected by the framework version. A running application alone does not prove that publishing works.

### Challenge B: Jackson class missing

**Symptom:** `ClassNotFoundException: com.fasterxml.jackson.databind.JavaType`, followed by `Failed to construct kafka producer`.

**Resolution:** Updated the Jackson dependency and JSON serializer configuration for the Spring Boot 4 setup.

**Learning:** Serializer and library versions must be compatible. JSON serialization is a separate concern from network connectivity to Kafka.

### Current verification status

The API has returned:

`success: true` and `Notification accepted for processing`.

This confirms the observed API response, but the message's successful arrival in Kafka must be verified by reading the topic. The exact producer acknowledgement and failure-handling behavior also need improvement before treating acceptance as a confirmed delivery to Kafka.

## 5. STAR interview story — Kafka integration troubleshooting

**Situation:** While integrating Kafka into a Spring Boot notification API, the application initially lacked a usable Kafka producer and later failed when constructing the producer because a required Jackson class was missing.

**Task:** Make the producer integration operational so notification requests could be published as events.

**Action:** Investigated the stack traces, corrected the Spring Boot Kafka dependency setup, and aligned the JSON serialization configuration with the framework version.

**Result:** The application started and returned the expected notification API response. End-to-end message publication still requires confirmation through the Kafka topic consumer.

**Follow-up questions to prepare:**
- Why was `KafkaTemplate` unavailable?
- What does a serializer do?
- How would you detect asynchronous send failures?
- How would you prevent an API from claiming success before the required Kafka acknowledgement?

## 6. Interview Q&A

**Q1. Why use Kafka in a notification system?**

Kafka decouples the API from downstream processing. The API can publish an event, and a separate consumer can process it independently. This helps with buffering and scaling, but requires proper failure handling and monitoring.

**Q2. What is the role of `KafkaTemplate`?**

It is Spring Kafka's abstraction for sending records to Kafka. It supports asynchronous send operations and returns a future that can be used to observe completion or failure.

**Q3. Why use a DTO?**

A DTO defines the API's request or response contract without exposing internal implementation details. It also provides a natural place to apply validation.

**Q4. What does `@Valid` do?**

It triggers Bean Validation on the request object. Constraints such as `@NotBlank` determine which values are invalid.

**Q5. Why use an enum for notification channels?**

It provides a controlled set of values and reduces errors from arbitrary strings. Invalid enum values still need appropriate API error handling.

**Q6. Why are there three Kafka partitions?**

Partitions allow records to be distributed and consumed in parallel by consumers in the same group, subject to partition assignment and ordering constraints. Three partitions do not automatically mean three active consumers or guarantee higher throughput.

**Q7. Does an HTTP success response guarantee Kafka delivery?**

Not necessarily. A send can complete asynchronously, and failures may occur after the HTTP response is formed. The application must define what acceptance means and handle producer acknowledgements and failures accordingly.

**Q8. What is the next architectural step?**

Implement a Kafka consumer that reads `notification-events`, processes each event, and logs the result. Then add error handling, retries and dead-letter handling.

## 7. Outstanding work

- Verify the event actually appears in the Kafka topic.
- Implement the Kafka consumer/worker.
- Handle asynchronous producer success and failure.
- Add tests for validation, service logic and Kafka integration.
- Add retries and dead-letter handling.
- Consider idempotency, observability, security and production deployment.


 

# Kafka Setup, Commands & Troubleshooting

## 1. Project Architecture

Our Notification System follows this flow:

```text
Client / Postman
       |
       v
Spring Boot REST API
       |
       v
NotificationService
       |
       v
NotificationProducer
       |
       v
Kafka Topic: notification-events
       |
       v
NotificationConsumer
       |
       v
Notification Dispatcher (Next Step)
       |
       +----> EMAIL
       +----> SMS
       +----> PUSH
```

### Components

- **REST API:** Accepts notification requests from clients.
- **NotificationService:** Handles notification business logic.
- **Kafka Producer:** Publishes notification events to Kafka.
- **Kafka Topic:** Stores notification events for consumers.
- **Kafka Consumer:** Reads events and processes them asynchronously.
- **Notification Dispatcher:** Will route notifications to the appropriate channel.

## 2. How to Start Kafka

Kafka runs inside a Docker container named `notification-kafka`.

Open PowerShell and run:

### Check all containers

```powershell
docker ps -a
```

- `Up` means the container is running.
- `Exited` means the container is stopped.

### Start Kafka

```powershell
docker start notification-kafka
```

### Verify Kafka is running

```powershell
docker ps
```

The `notification-kafka` container should appear with an `Up` status.

### Check Kafka logs

```powershell
docker logs --tail 100 notification-kafka
```

Use these logs if Kafka fails to start or Spring Boot cannot connect to the broker.

## 3. Kafka Topic Commands

Our topic is named `notification-events`.

### List all topics

```powershell
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Expected topic:

```text
notification-events
```

### Describe the topic

```powershell
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic notification-events
```

Our topic is configured with:

- **Partitions:** 3
- **Replication factor:** 1

### What is a Kafka topic?

A topic is a named stream in which Kafka stores records.

### What is a partition?

A partition divides a topic into separate ordered logs. Partitions allow Kafka to distribute data and processing across consumers.

### What is a consumer group?

A consumer group allows multiple consumers to coordinate consumption of topic partitions. Each partition is assigned to at most one consumer within a group at a time.

Our consumer group is:

```text
notification-group
```

## 4. How to Read Messages Stored in Kafka

Run this command in PowerShell:

```powershell
docker exec -it notification-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic notification-events --from-beginning
```

Expected example:

```json
{"recipient":"harsh@example.com","message":"Your order has been placed","channel":"EMAIL"}
```

Keep this terminal open while sending requests from Postman.

- `--topic` specifies which topic to read.
- `--from-beginning` reads available retained messages from the beginning.
- Press `Ctrl + C` to stop the console consumer.

**Important:** This console consumer is separate from the Spring Boot `NotificationConsumer`. It is a debugging tool for inspecting Kafka records.

## 5. How to Test the Complete Flow

### Step 1: Start Kafka

```powershell
docker start notification-kafka
```

If it is already running, Docker may report that the container is already started.

### Step 2: Start the Spring Boot application

Run the application from the IDE or using the project's Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Run this command from the project root, where `mvnw.cmd` exists.

Check the startup logs for the port on which the application started.

### Step 3: Send a request from Postman

Method:

```text
POST
```

URL example:

```text
http://localhost:8081/api/notifications
```

Use the actual port configured for your application. If it uses the default port, use `8080`.

Header:

```text
Content-Type: application/json
```

Request body:

```json
{
  "recipient": "harsh@example.com",
  "message": "Your order has been placed",
  "channel": "EMAIL"
}
```

### Step 4: Verify the API response

Expected response:

```json
{
  "success": true,
  "message": "Notification accepted for processing",
  "recipient": "harsh@example.com",
  "channel": "EMAIL"
}
```

This confirms that the API returned a successful response. It does not, by itself, prove that the consumer processed the event.

### Step 5: Verify the Kafka record

Use the Kafka console consumer command in Section 4. Confirm that the new notification appears in the output.

### Step 6: Verify the Spring Boot consumer

Check the terminal where the Spring Boot application is running.

Expected output:

```text
Notification received!
Recipient: harsh@example.com
Message: Your order has been placed
Channel: EMAIL
```

If the new message appears here, the Spring Boot consumer has received the event.

## 6. Troubleshooting

### Problem 1: Kafka connection refused or broker disconnected

**Possible cause:** Kafka container is stopped or not ready.

Check:

```powershell
docker ps -a
```

Start Kafka:

```powershell
docker start notification-kafka
```

If it still fails, inspect:

```powershell
docker logs --tail 100 notification-kafka
```

Also verify that Spring Boot uses:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

This address is appropriate for a Spring Boot application running on the host machine with the current Docker port mapping.

### Problem 2: Postman returns connection refused

Check the Spring Boot startup logs and confirm the port.

If the application is configured to run on port `8081`, the URL should be:

```text
http://localhost:8081/api/notifications
```

If it runs on port `8080`, use:

```text
http://localhost:8080/api/notifications
```

The Postman port must match the port on which Spring Boot is listening.

### Problem 3: API returns success, but consumer output is missing

Check the following:

1. Kafka container is running.
2. The API is publishing to `notification-events`.
3. `NotificationConsumer` is registered as a Spring bean using `@Service`.
4. The consumer uses `@KafkaListener` with the correct topic.
5. JSON deserialization into `NotificationEvent` is configured correctly.
6. Spring Boot logs do not contain deserialization or listener errors.

### Problem 4: Topic not found

List topics:

```powershell
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Our `KafkaTopicConfig` declares the topic with three partitions and one replica. Verify that the application starts successfully and that the topic exists.

### Problem 5: Kafka container stops again

Inspect the logs:

```powershell
docker logs --tail 100 notification-kafka
```

Restart it if appropriate:

```powershell
docker start notification-kafka
```

If it repeatedly exits, diagnose the container logs rather than repeatedly restarting it.

## 7. Understanding Consumer Offsets

Kafka tracks a consumer's position in each partition using offsets.

- An offset identifies a record's position within a partition.
- A committed offset records a consumer group's progress.
- A new consumer group may not have committed offsets yet.
- Depending on configuration, a consumer can start from the earliest available record or the latest record.

For our consumer group:

```text
notification-group
```

Kafka may display messages such as `Found no committed offset` when the group has no previously committed position for a partition. This is not necessarily an error.

**Note:** A console consumer started with `--from-beginning` is independent of the Spring Boot consumer group's committed offsets.

## 8. Important Interview Questions

### Q1. Why are we using Kafka in this project?

Kafka decouples the REST API from notification processing. The API can publish an event without waiting for the complete notification delivery process to finish.

### Q2. What is the difference between a producer and a consumer?

A producer publishes records to Kafka topics. A consumer reads records from those topics and processes them.

### Q3. What happens if the consumer is temporarily unavailable?

Kafka retains records according to its retention configuration. When the consumer becomes available again, it can continue processing records according to its offset and consumer configuration, provided the records are still retained.

### Q4. Does a successful API response guarantee email delivery?

No. In our current implementation, the response indicates that the API accepted the request. We must separately verify publishing, consuming, and eventually actual delivery through an email, SMS, or push provider.

### Q5. Why do we use a message key?

Our producer uses the recipient as the Kafka record key:

```java
kafkaTemplate.send("notification-events", event.recipient(), event);
```

The key helps Kafka choose a partition. Records with the same key are generally routed to the same partition under the default partitioning strategy, preserving their order within that partition.

### Q6. Why use three partitions?

Multiple partitions allow Kafka to distribute records and support parallel processing by consumers. Actual parallelism depends on the number of consumers and their partition assignments.

### Q7. What is asynchronous processing?

The API publishes a notification event, while a consumer processes it separately. This reduces the need for the API request to wait for downstream notification work.

## 9. Current Project Status

Completed:

- Spring Boot REST endpoint.
- Request DTO and validation.
- Global validation exception handling.
- Kafka topic configuration.
- Kafka producer.
- Kafka event model.
- Docker-based Kafka setup.
- Kafka consumer class.
- Initial producer-to-topic verification.

Verification to maintain:

- Confirm that new requests appear in the Kafka console consumer.
- Confirm that the Spring Boot consumer prints the corresponding event.

Next development milestones:

1. Implement a notification dispatcher for EMAIL, SMS, and PUSH.
2. Add channel-specific service abstractions.
3. Introduce persistent notification status tracking.
4. Add retry handling and a dead-letter topic.
5. Add structured logging and monitoring.
6. Write integration tests and document design trade-offs.

# Scalable Notification System — Interview Preparation

**Target:** Mid-level Java Backend Developer (2–3 years experience)

**Tech stack:** Java 17, Spring Boot 4.1.1, Spring Web MVC, Bean Validation, Apache Kafka, Docker

## 1. Project objective

Build a notification system that accepts notification requests through a REST API and processes notification events asynchronously using Kafka. The intended channels are EMAIL, SMS and PUSH.

The planned evolution includes a Kafka consumer/worker, retry handling, dead-letter topics, monitoring, Redis and deployment infrastructure. These are planned capabilities, not yet completed features.

## 2. Current architecture

Client → Spring Boot REST API → Notification Service → Kafka Producer → `notification-events` topic

The Kafka consumer/worker is the next planned component.

### Why Kafka?

Kafka allows the API to hand off notification events to a messaging system rather than requiring the API to perform the entire notification-delivery operation synchronously.

**Benefits:** Asynchronous processing, buffering, partition-based parallelism and decoupling producers from consumers.

**Trade-offs:** Additional infrastructure, serialization/configuration complexity, delivery guarantees and operational monitoring requirements.

## 3. Codebase deep dive

- `NotificationController`: Exposes `POST /api/notifications` and accepts a validated request.
- `NotificationRequest`: DTO containing `recipient`, `message` and `channel`; uses `@NotBlank` for required text fields.
- `NotificationChannel`: Enum containing `EMAIL`, `SMS` and `PUSH`.
- `NotificationResponse`: Represents the API response.
- `NotificationService`: Coordinates notification request processing.
- `GlobalExceptionHandler`: Uses `@RestControllerAdvice` to return structured validation errors.
- `NotificationEvent`: Java record carrying `recipient`, `message` and `channel`.
- `NotificationProducer`: Uses `KafkaTemplate` to publish an event to `notification-events`, using the recipient as the Kafka record key.
- `KafkaTopicConfig`: Declares a topic with three partitions and one replica.
- `application.properties`: Configures the Kafka bootstrap server and producer serialization.

### Important design decisions

**DTOs:** Separate the external API contract from the internal event representation.

**Enum for channel:** Restricts channel values to a defined set rather than accepting arbitrary strings.

**Validation:** `@Valid` triggers request validation, while `@NotBlank` rejects blank or missing text values. Channel validation and default handling still need consideration.

**Constructor injection:** Makes dependencies explicit and supports straightforward testing.

**Kafka record key:** Using the recipient as the key can keep events for the same recipient in the same partition, provided the partitioning strategy and partition count remain compatible. It can also create uneven partition loads if a few recipients dominate traffic.

**Three partitions, one replica:** Provides multiple partitions for potential parallelism, but a single replica does not provide broker-level replication resilience.

## 4. Challenges and solutions encountered

### Challenge A: Missing KafkaTemplate bean

**Symptom:** Spring could not find a `KafkaTemplate` bean.

**Resolution:** Replaced the direct `spring-kafka` dependency with `spring-boot-starter-kafka` and confirmed that the application started.

**Learning:** Spring Boot starters can supply the integration and auto-configuration dependencies expected by the framework version. A running application alone does not prove that publishing works.

### Challenge B: Jackson class missing

**Symptom:** `ClassNotFoundException: com.fasterxml.jackson.databind.JavaType`, followed by `Failed to construct kafka producer`.

**Resolution:** Updated the Jackson dependency and JSON serializer configuration for the Spring Boot 4 setup.

**Learning:** Serializer and library versions must be compatible. JSON serialization is a separate concern from network connectivity to Kafka.

### Current verification status

The API has returned:

`success: true` and `Notification accepted for processing`.

This confirms the observed API response, but the message's successful arrival in Kafka must be verified by reading the topic. The exact producer acknowledgement and failure-handling behavior also need improvement before treating acceptance as a confirmed delivery to Kafka.

## 5. STAR interview story — Kafka integration troubleshooting

**Situation:** While integrating Kafka into a Spring Boot notification API, the application initially lacked a usable Kafka producer and later failed when constructing the producer because a required Jackson class was missing.

**Task:** Make the producer integration operational so notification requests could be published as events.

**Action:** Investigated the stack traces, corrected the Spring Boot Kafka dependency setup, and aligned the JSON serialization configuration with the framework version.

**Result:** The application started and returned the expected notification API response. End-to-end message publication still requires confirmation through the Kafka topic consumer.

**Follow-up questions to prepare:**
- Why was `KafkaTemplate` unavailable?
- What does a serializer do?
- How would you detect asynchronous send failures?
- How would you prevent an API from claiming success before the required Kafka acknowledgement?

## 6. Interview Q&A

**Q1. Why use Kafka in a notification system?**

Kafka decouples the API from downstream processing. The API can publish an event, and a separate consumer can process it independently. This helps with buffering and scaling, but requires proper failure handling and monitoring.

**Q2. What is the role of `KafkaTemplate`?**

It is Spring Kafka's abstraction for sending records to Kafka. It supports asynchronous send operations and returns a future that can be used to observe completion or failure.

**Q3. Why use a DTO?**

A DTO defines the API's request or response contract without exposing internal implementation details. It also provides a natural place to apply validation.

**Q4. What does `@Valid` do?**

It triggers Bean Validation on the request object. Constraints such as `@NotBlank` determine which values are invalid.

**Q5. Why use an enum for notification channels?**

It provides a controlled set of values and reduces errors from arbitrary strings. Invalid enum values still need appropriate API error handling.

**Q6. Why are there three Kafka partitions?**

Partitions allow records to be distributed and consumed in parallel by consumers in the same group, subject to partition assignment and ordering constraints. Three partitions do not automatically mean three active consumers or guarantee higher throughput.

**Q7. Does an HTTP success response guarantee Kafka delivery?**

Not necessarily. A send can complete asynchronously, and failures may occur after the HTTP response is formed. The application must define what acceptance means and handle producer acknowledgements and failures accordingly.

**Q8. What is the next architectural step?**

Implement a Kafka consumer that reads `notification-events`, processes each event, and logs the result. Then add error handling, retries and dead-letter handling.

## 7. Outstanding work

- Verify the event actually appears in the Kafka topic.
- Implement the Kafka consumer/worker.
- Handle asynchronous producer success and failure.
- Add tests for validation, service logic and Kafka integration.
- Add retries and dead-letter handling.
- Consider idempotency, observability, security and production deployment.


 

# Kafka Setup, Commands & Troubleshooting

## 1. Project Architecture

Our Notification System follows this flow:

```text
Client / Postman
       |
       v
Spring Boot REST API
       |
       v
NotificationService
       |
       v
NotificationProducer
       |
       v
Kafka Topic: notification-events
       |
       v
NotificationConsumer
       |
       v
Notification Dispatcher (Next Step)
       |
       +----> EMAIL
       +----> SMS
       +----> PUSH
```

### Components

- **REST API:** Accepts notification requests from clients.
- **NotificationService:** Handles notification business logic.
- **Kafka Producer:** Publishes notification events to Kafka.
- **Kafka Topic:** Stores notification events for consumers.
- **Kafka Consumer:** Reads events and processes them asynchronously.
- **Notification Dispatcher:** Will route notifications to the appropriate channel.

## 2. How to Start Kafka

Kafka runs inside a Docker container named `notification-kafka`.

Open PowerShell and run:

### Check all containers

```powershell
docker ps -a
```

- `Up` means the container is running.
- `Exited` means the container is stopped.

### Start Kafka

```powershell
docker start notification-kafka
```

### Verify Kafka is running

```powershell
docker ps
```

The `notification-kafka` container should appear with an `Up` status.

### Check Kafka logs

```powershell
docker logs --tail 100 notification-kafka
```

Use these logs if Kafka fails to start or Spring Boot cannot connect to the broker.

## 3. Kafka Topic Commands

Our topic is named `notification-events`.

### List all topics

```powershell
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Expected topic:

```text
notification-events
```

### Describe the topic

```powershell
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic notification-events
```

Our topic is configured with:

- **Partitions:** 3
- **Replication factor:** 1

### What is a Kafka topic?

A topic is a named stream in which Kafka stores records.

### What is a partition?

A partition divides a topic into separate ordered logs. Partitions allow Kafka to distribute data and processing across consumers.

### What is a consumer group?

A consumer group allows multiple consumers to coordinate consumption of topic partitions. Each partition is assigned to at most one consumer within a group at a time.

Our consumer group is:

```text
notification-group
```

## 4. How to Read Messages Stored in Kafka

Run this command in PowerShell:

```powershell
docker exec -it notification-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic notification-events --from-beginning
```

Expected example:

```json
{"recipient":"harsh@example.com","message":"Your order has been placed","channel":"EMAIL"}
```

Keep this terminal open while sending requests from Postman.

- `--topic` specifies which topic to read.
- `--from-beginning` reads available retained messages from the beginning.
- Press `Ctrl + C` to stop the console consumer.

**Important:** This console consumer is separate from the Spring Boot `NotificationConsumer`. It is a debugging tool for inspecting Kafka records.

## 5. How to Test the Complete Flow

### Step 1: Start Kafka

```powershell
docker start notification-kafka
```

If it is already running, Docker may report that the container is already started.

### Step 2: Start the Spring Boot application

Run the application from the IDE or using the project's Maven wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

Run this command from the project root, where `mvnw.cmd` exists.

Check the startup logs for the port on which the application started.

### Step 3: Send a request from Postman

Method:

```text
POST
```

URL example:

```text
http://localhost:8081/api/notifications
```

Use the actual port configured for your application. If it uses the default port, use `8080`.

Header:

```text
Content-Type: application/json
```

Request body:

```json
{
  "recipient": "harsh@example.com",
  "message": "Your order has been placed",
  "channel": "EMAIL"
}
```

### Step 4: Verify the API response

Expected response:

```json
{
  "success": true,
  "message": "Notification accepted for processing",
  "recipient": "harsh@example.com",
  "channel": "EMAIL"
}
```

This confirms that the API returned a successful response. It does not, by itself, prove that the consumer processed the event.

### Step 5: Verify the Kafka record

Use the Kafka console consumer command in Section 4. Confirm that the new notification appears in the output.

### Step 6: Verify the Spring Boot consumer

Check the terminal where the Spring Boot application is running.

Expected output:

```text
Notification received!
Recipient: harsh@example.com
Message: Your order has been placed
Channel: EMAIL
```

If the new message appears here, the Spring Boot consumer has received the event.

## 6. Troubleshooting

### Problem 1: Kafka connection refused or broker disconnected

**Possible cause:** Kafka container is stopped or not ready.

Check:

```powershell
docker ps -a
```

Start Kafka:

```powershell
docker start notification-kafka
```

If it still fails, inspect:

```powershell
docker logs --tail 100 notification-kafka
```

Also verify that Spring Boot uses:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

This address is appropriate for a Spring Boot application running on the host machine with the current Docker port mapping.

### Problem 2: Postman returns connection refused

Check the Spring Boot startup logs and confirm the port.

If the application is configured to run on port `8081`, the URL should be:

```text
http://localhost:8081/api/notifications
```

If it runs on port `8080`, use:

```text
http://localhost:8080/api/notifications
```

The Postman port must match the port on which Spring Boot is listening.

### Problem 3: API returns success, but consumer output is missing

Check the following:

1. Kafka container is running.
2. The API is publishing to `notification-events`.
3. `NotificationConsumer` is registered as a Spring bean using `@Service`.
4. The consumer uses `@KafkaListener` with the correct topic.
5. JSON deserialization into `NotificationEvent` is configured correctly.
6. Spring Boot logs do not contain deserialization or listener errors.

### Problem 4: Topic not found

List topics:

```powershell
docker exec notification-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Our `KafkaTopicConfig` declares the topic with three partitions and one replica. Verify that the application starts successfully and that the topic exists.

### Problem 5: Kafka container stops again

Inspect the logs:

```powershell
docker logs --tail 100 notification-kafka
```

Restart it if appropriate:

```powershell
docker start notification-kafka
```

If it repeatedly exits, diagnose the container logs rather than repeatedly restarting it.

## 7. Understanding Consumer Offsets

Kafka tracks a consumer's position in each partition using offsets.

- An offset identifies a record's position within a partition.
- A committed offset records a consumer group's progress.
- A new consumer group may not have committed offsets yet.
- Depending on configuration, a consumer can start from the earliest available record or the latest record.

For our consumer group:

```text
notification-group
```

Kafka may display messages such as `Found no committed offset` when the group has no previously committed position for a partition. This is not necessarily an error.

**Note:** A console consumer started with `--from-beginning` is independent of the Spring Boot consumer group's committed offsets.

## 8. Important Interview Questions

### Q1. Why are we using Kafka in this project?

Kafka decouples the REST API from notification processing. The API can publish an event without waiting for the complete notification delivery process to finish.

### Q2. What is the difference between a producer and a consumer?

A producer publishes records to Kafka topics. A consumer reads records from those topics and processes them.

### Q3. What happens if the consumer is temporarily unavailable?

Kafka retains records according to its retention configuration. When the consumer becomes available again, it can continue processing records according to its offset and consumer configuration, provided the records are still retained.

### Q4. Does a successful API response guarantee email delivery?

No. In our current implementation, the response indicates that the API accepted the request. We must separately verify publishing, consuming, and eventually actual delivery through an email, SMS, or push provider.

### Q5. Why do we use a message key?

Our producer uses the recipient as the Kafka record key:

```java
kafkaTemplate.send("notification-events", event.recipient(), event);
```

The key helps Kafka choose a partition. Records with the same key are generally routed to the same partition under the default partitioning strategy, preserving their order within that partition.

### Q6. Why use three partitions?

Multiple partitions allow Kafka to distribute records and support parallel processing by consumers. Actual parallelism depends on the number of consumers and their partition assignments.

### Q7. What is asynchronous processing?

The API publishes a notification event, while a consumer processes it separately. This reduces the need for the API request to wait for downstream notification work.

## 9. Current Project Status

Completed:

- Spring Boot REST endpoint.
- Request DTO and validation.
- Global validation exception handling.
- Kafka topic configuration.
- Kafka producer.
- Kafka event model.
- Docker-based Kafka setup.
- Kafka consumer class.
- Initial producer-to-topic verification.

Verification to maintain:

- Confirm that new requests appear in the Kafka console consumer.
- Confirm that the Spring Boot consumer prints the corresponding event.

Next development milestones:

1. Implement a notification dispatcher for EMAIL, SMS, and PUSH.
2. Add channel-specific service abstractions.
3. Introduce persistent notification status tracking.
4. Add retry handling and a dead-letter topic.
5. Add structured logging and monitoring.
6. Write integration tests and document design trade-offs.



**Documentation rule:** Record only features and fixes actually implemented. Label planned work separately, and update this document as each new feature or bug is completed.

## Latest Progress — Consumer Connection Check

- Kafka container `notification-kafka` had stopped; restarted it with `docker start notification-kafka`.
- Spring Boot logs then showed `notification-group` assigned all three topic partitions. This confirms assignment, **not** that a new event was consumed.
- Postman returned `success: true` for `POST /api/notifications`; this alone does not confirm consumer processing.

### Current exact step

Send one fresh Postman request and confirm the same event appears in the Spring Boot terminal as `Notification received!` with the expected recipient, message, and channel. If it does not, troubleshoot that before adding new features.

### Next step after verification

Implement the Notification Dispatcher for EMAIL, SMS, and PUSH. Do not mark consumer delivery as verified until the listener output is observed.


**Documentation rule:** Record only features and fixes actually implemented. Label planned work separately, and update this document as each new feature or bug is completed.