Feature: JMS steps

  Background:
    Given JMS connection factory org.apache.activemq.ActiveMQConnectionFactory
        | tcp://localhost:61616 |
    Given jms destination: test

  Scenario: Send and receive body and headers
    Given variable body is "citrus:randomString(10)"
    When send message to JMS broker with body: ${body}
    Then expect message in JMS broker with body: ${body}

