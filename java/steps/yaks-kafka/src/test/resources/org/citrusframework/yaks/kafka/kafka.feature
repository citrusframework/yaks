Feature: Kafka steps

  Background:
    Given Kafka connection
        | url       | localhost:9092 |
        | topic     | test|

  Scenario: Send and receive body and headers
    Given variable body is "citrus:randomString(10)"
    Given variable key is "citrus:randomString(10)"
    Given variable value is "citrus:randomString(10)"
    When send message to Kafka with body and headers: ${body}
      | ${key} | ${value} |
    Then expect message in Kafka with body and headers: ${body}
      | ${key} | ${value} |

