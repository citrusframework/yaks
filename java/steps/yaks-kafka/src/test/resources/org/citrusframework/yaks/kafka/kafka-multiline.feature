Feature: Kafka multiline steps

  Background:
    Given Kafka consumer timeout is 5000 milliseconds
    Given Kafka connection
        | url           | localhost:9092 |
        | topic         | hello          |
        | consumerGroup | hello-group    |

  Scenario: Predefined multiline body
    Given Kafka message body
      """
      {
        "message": "Hello from YAKS!"
      }
      """
    When send Kafka message to topic hello
    Then verify Kafka message body
      """
      { "message": "Hello from YAKS!" }
      """
    And receive Kafka message on topic hello

  Scenario: Multiline body
    When send Kafka message with body
      """
      {
        "message": "Hello from YAKS!"
      }
      """
    Then expect Kafka message with body
      """
      { "message": "Hello from YAKS!" }
      """
