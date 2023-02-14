Feature: Redpanda

  Background:
    Given Disable auto removal of Testcontainers resources
    Given Kafka consumer timeout is 5000 milliseconds
    Given Kafka topic: hello

  Scenario: Start container
    Given start Redpanda container

  Scenario: Send and receive Kafka message
    Given variables
      | key     | citrus:randomNumber(4) |
      | message | Hello Redpanda |
    Given Kafka connection
      | url | ${YAKS_TESTCONTAINERS_REDPANDA_LOCAL_BOOTSTRAP_SERVERS} |
    And Kafka message key: ${key}
    When send Kafka message with body and headers: ${message}
      | messageId | ${key} |
    Then expect Kafka message with body and headers: ${message}
      | messageId | ${key} |

  Scenario: Stop container
    Given stop Redpanda container
