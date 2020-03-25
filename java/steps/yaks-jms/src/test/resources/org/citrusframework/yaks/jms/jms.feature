Feature: JMS steps

  Background:
    Given JMS connection factory
      | type       | org.apache.activemq.ActiveMQConnectionFactory |
      | brokerUrl  | tcp://localhost:61616 |
    Given JMS destination: test

  Scenario: Send and receive body
    Given variable body is "citrus:randomString(10)"
    When send message to JMS broker with body: ${body}
    Then expect message in JMS broker with body: ${body}

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

