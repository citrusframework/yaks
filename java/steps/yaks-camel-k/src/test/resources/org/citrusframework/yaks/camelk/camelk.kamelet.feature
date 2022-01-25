Feature: Kamelet

  Scenario: Create Kamelet
    Given Kamelet property definition
    | name     | message       |
    | type     | string        |
    | required | true          |
    | example  | "hello world" |
    And Kamelet property definition
    | name     | period        |
    | type     | integer       |
    | default  | 1000          |
    And Kamelet source helloworld.groovy
    """
    from('timer:tick?period=#property:period')
      .setBody().constant('{{message}}')
      .to('kamelet:sink')
    """
    And Kamelet type out="test/plain"
    When create Kamelet timer-source
    Then Kamelet timer-source should be available

  Scenario: Create Kamelet with flow
    Given Kamelet property definition
      | name     | message       |
      | type     | string        |
      | required | true          |
      | example  | "hello world" |
    And Kamelet property definition
      | name     | period        |
      | type     | integer       |
      | default  | 1000          |
    And Kamelet type out="test/plain"
    When create Kamelet timer-source with flow
"""
from:
  uri: timer:tick
  parameters:
    period: "#property:period"
  steps:
  - set-body:
      constant: "{{message}}"
  - to: "kamelet:sink"
"""
    Then Kamelet timer-source should be available

  Scenario: Create Kamelet from file
    Given load Kamelet timer-source.kamelet.yaml
    And load Camel K integration timer-to-log.groovy
    Then Kamelet timer-source should be available

  Scenario: Bind Kamelet to Kafka
    Given KameletBinding source properties
      | message  | Hello World |
    And bind Kamelet timer-source to Kafka topic hello-topic
    When create KameletBinding timer-source-kafka
    Then KameletBinding timer-source-kafka should be available

  Scenario: Bind Kamelet to Knative
    Given KameletBinding source properties
      | message  | Hello World |
    And bind Kamelet timer-source to Knative channel hello-topic of kind InMemoryChannel
    When create KameletBinding timer-source-knative
    Then KameletBinding timer-source-knative should be available

  Scenario: Bind Kamelet to Uri
    Given KameletBinding source properties
      | message  | Hello World |
    And bind Kamelet timer-source to uri https://greeting-service.svc.cluster.local
    When create KameletBinding timer-source-uri
    Then KameletBinding timer-source-uri should be available

  Scenario: Create KameletBinding from file
    Given load KameletBinding kamelet-binding.yaml
    Then KameletBinding timer-source-binding should be available

