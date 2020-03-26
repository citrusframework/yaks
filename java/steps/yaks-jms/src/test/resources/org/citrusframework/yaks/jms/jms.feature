Feature: JMS steps

  Background:
    Given JMS connection factory
      | type       | org.apache.activemq.ActiveMQConnectionFactory |
      | brokerUrl  | tcp://localhost:61616 |
    Given JMS destination: test

  Scenario: Send and receive body and headers
    Given variable body is "citrus:randomString(10)"
    Given variable key is "citrus:randomString(10)"
    Given variable value is "citrus:randomString(10)"
    When send message to JMS broker with body and headers: ${body}
      | ${key} | ${value} |
    Then expect message in JMS broker with body and headers: ${body}
      | ${key} | ${value} |

  Scenario: Send and receive multiline body
    When send message to JMS broker with body
    """
    {
      "message": "Hello from YAKS!"
    }
    """
    Then expect message in JMS broker with body
    """
    {
      "message": "Hello from YAKS!"
    }
    """

  Scenario: Send receive message with selector
    Given variable correctBody is "citrus:randomString(10)"
    And variable tag is "citrus:randomString(10)"
    And jms selector: tag='${tag}'
    Given message in JMS broker with body and headers: citrus:randomString(10)
      | tag | citrus:randomString(10) |
    And message in JMS broker with body and headers: ${correctBody}
      | tag | ${tag} |
    And message in JMS broker with body and headers: citrus:randomString(10)
      | tag | citrus:randomString(10) |
    Then expect message in JMS broker with body and headers: ${correctBody}
      | tag | ${tag} |
