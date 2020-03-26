Feature: Kafka steps

  Background:
    Given Kafka connection
        | url           | localhost:9092 |
        | topic         | hello |
        | consumerGroup | hello-group |

  Scenario: Send and receive multiline body
    When send message to Kafka with body
      """
      {
        "message": "Hello from YAKS!"
      }
      """
    Then expect message in Kafka with body
      """
      {
        "message": "Hello from YAKS!"
      }
      """

