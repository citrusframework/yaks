Feature: JMS steps

  Background:
    Given JMS connection factory
      | type       | org.apache.activemq.ActiveMQConnectionFactory |
      | brokerUrl  | tcp://localhost:61616 |
    Given JMS destination: test
    Given variable body is "citrus:randomString(10)"
    Given variable key is "citrus:randomString(10)"
    Given variable value is "citrus:randomString(10)"
    Given JMS consumer timeout is 5000 milliseconds

  Scenario: Send and receive with predefined body and headers
    Given JMS message body: ${body}
    And JMS message header ${key}="${value}"
    When send JMS message
    Then verify JMS message body: ${body}
    And verify JMS message header ${key} is "${value}"
    And receive JMS message

  Scenario: Send and receive body and headers
    Given variable body is "citrus:randomString(10)"
    Given variable key is "citrus:randomString(10)"
    Given variable value is "citrus:randomString(10)"
    When send JMS message with body and headers: ${body}
      | ${key} | ${value} |
    Then expect JMS message with body and headers: ${body}
      | ${key} | ${value} |

  Scenario: Send and receive body
    When send JMS message with body: {"message": "Hello from YAKS!"}
    Then expect JMS message with body: {"message": "Hello from YAKS!"}

  Scenario: Send receive message with selector
    Given variable correctBody is "citrus:randomString(10)"
    And variable tag is "citrus:randomString(10)"
    And jms selector: tag='${tag}'
    Given JMS message with body and headers: citrus:randomString(10)
      | tag | citrus:randomString(10) |
    And JMS message with body and headers: ${correctBody}
      | tag | ${tag} |
    And JMS message with body and headers: citrus:randomString(10)
      | tag | citrus:randomString(10) |
    Then expect JMS message with body and headers: ${correctBody}
      | tag | ${tag} |
