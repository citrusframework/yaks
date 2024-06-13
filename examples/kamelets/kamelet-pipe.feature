Feature: Kamelet pipe

  Scenario: Bind Kamelet to service
    # Create Kamelet from file
    Given load Kamelet hello-source.kamelet.yaml
    Then Kamelet hello-source should be available

    # Create the binding
    Given create Kubernetes service greeting-service with target port 8080
    And Pipe source properties
      | message  | Hello World |
    And bind Kamelet hello-source to uri http://greeting-service.${YAKS_NAMESPACE}/greeting
    When create Pipe hello-source-uri
    Then Pipe hello-source-uri should be available

    # Verify binding
    Given HTTP server "greeting-service"
    And HTTP server timeout is 600000 ms
    Then expect HTTP request body: Hello World!
    And receive POST /greeting

  Scenario: Create binding from YAML
    Given load Pipe hello-to-log-binding.yaml
    Then Pipe hello-to-log-binding should be available
    And Camel K integration hello-to-log-binding should print Hello World!

  Scenario: Remove Camel K resources
    Given delete Kamelet hello-source
    Given delete Pipe hello-source-uri
    Given delete Pipe hello-to-log-binding
