Feature: Kamelet resource

  Background:
    Given Disable auto removal of Kamelet resources
    Given Disable auto removal of Kubernetes resources
    Given Camel K resource polling configuration
      | maxAttempts          | 20   |
      | delayBetweenAttempts | 1000 |

  Scenario: Bind Kamelet to service
    # Create Kamelet from file
    Given load Kamelet timer-source.kamelet.yaml
    Then Kamelet timer-source should be available

    # Create the binding
    Given create Kubernetes service greeting-service with target port 8080
    And KameletBinding source properties
      | message  | Hello World |
    And bind Kamelet timer-source to uri http://greeting-service.${YAKS_NAMESPACE}/greeting
    When create KameletBinding timer-source-uri
    Then KameletBinding timer-source-uri should be available

    # Verify binding
    Given HTTP server "greeting-service"
    And HTTP server timeout is 600000 ms
    Then expect HTTP request body: Hello World
    And receive POST /greeting

  Scenario: Create binding from YAML
    Given load KameletBinding timer-to-log-binding.yaml
    Then KameletBinding timer-to-log-binding should be available
    And Camel K integration timer-to-log-binding should print Hello world!

  Scenario: Remove Camel K resources
    Given delete Kamelet timer-source
    Given delete KameletBinding timer-source-uri
    Given delete KameletBinding timer-to-log-binding
