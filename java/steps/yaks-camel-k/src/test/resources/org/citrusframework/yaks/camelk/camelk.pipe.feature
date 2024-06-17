Feature: Pipe

  Scenario: Bind Kamelet to Kafka
    Given Pipe source properties
      | message  | Hello World |
    And bind Kamelet timer-source to Kafka topic hello-topic
    When create Pipe timer-source-kafka
    Then Pipe timer-source-kafka should be available

  Scenario: Bind Kamelet to Knative
    Given Pipe source properties
      | message  | Hello World |
    And bind Kamelet timer-source to Knative channel hello-topic of kind InMemoryChannel
    When create Pipe timer-source-knative
    Then Pipe timer-source-knative should be available

  Scenario: Bind Kamelet to Uri
    Given Pipe source properties
      | message  | Hello World |
    And bind Kamelet timer-source to uri https://greeting-service.svc.cluster.local
    When create Pipe timer-source-uri
    Then Pipe timer-source-uri should be available

  Scenario: Create Pipe from file
    Given Kamelet API version v1alpha1
    Given load Pipe pipe.yaml
    Then Pipe timer-source-pipe should be available

