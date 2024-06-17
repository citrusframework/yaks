Feature: KameletBinding

  Scenario: Bind Kamelet to Kafka
    Given KameletBinding source properties
      | message  | Hello World |
    Given KameletBinding event source Kamelet timer-source
    And KameletBinding event sink Kafka topic hello-topic
    When create KameletBinding timer-source-kafka
    Then KameletBinding timer-source-kafka should be available

  Scenario: Bind Kamelet to Knative channel
    Given KameletBinding source properties
      | message  | Hello World |
    Given KameletBinding event source Kamelet timer-source
    And KameletBinding event sink Knative channel hello-topic of kind InMemoryChannel
    When create KameletBinding timer-source-knative
    Then KameletBinding timer-source-knative should be available

  Scenario: Bind Kamelet to Uri
    Given KameletBinding source properties
      | message  | Hello World |
    Given KameletBinding event source Kamelet timer-source
    And KameletBinding event sink uri https://greeting-service.svc.cluster.local
    When create KameletBinding timer-source-uri
    Then KameletBinding timer-source-uri should be available

  Scenario: Create KameletBinding from file
    Given load KameletBinding kamelet-binding.yaml
    Then KameletBinding timer-source-binding should be available

