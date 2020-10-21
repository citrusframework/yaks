Feature: JMS multiline steps

  Background:
    Given JMS connection factory
      | type       | org.apache.activemq.ActiveMQConnectionFactory |
      | brokerUrl  | tcp://localhost:61616 |

  Scenario: Predefined multiline body
    Given JMS message body
      """
      {
        "message": "Hello from YAKS!"
      }
      """
    When send JMS message to destination hello
    Then verify JMS message body
      """
      { "message": "Hello from YAKS!" }
      """
    And receive JMS message on destination hello

  Scenario: Multiline body
    When send JMS message with body
      """
      {
        "message": "Hello from YAKS!"
      }
      """
    Then expect JMS message with body
      """
      { "message": "Hello from YAKS!" }
      """
