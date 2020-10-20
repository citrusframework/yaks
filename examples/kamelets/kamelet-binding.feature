Feature: Kamelet resource

  Background:
    Given Disable auto removal of Kamelet resources
    Given Disable auto removal of Kubernetes resources
    Given Camel-K resource polling configuration
      | maxAttempts          | 20   |
      | delayBetweenAttempts | 1000 |

  Scenario: Create Kamelet from file
    Given load Kamelet timer-source.kamelet.yaml
    Then Kamelet timer-source should be available

  Scenario: Bind Kamelet to service
    Given create Kubernetes service greeting-service with target port 8080
    And KameletBinding source properties
      | message  | Hello World |
    And bind Kamelet timer-source to uri https://greeting-service.svc.cluster.local/greeting
    When create KameletBinding timer-source-uri
    Then KameletBinding timer-source-uri should be available

    Scenario: Verify binding
    Given HTTP server "greeting-service"
    Then expect HTTP request body: Hello World
    And receive POST /greeting
