@require(org.apache.camel:camel-http:@camel.version@)
Feature: Kubernetes service

  Background:
    Given Disable auto removal of Camel K resources
    Given HTTP server "greeting-service"
    And HTTP server timeout is 10000 ms
    And HTTP server listening on port 8080
    And start HTTP server

  Scenario:
    Given create Camel K integration hello-world.groovy
    """
    from('timer:tick?period=5000')
      .setHeader("CamelHttpMethod", constant("POST"))
      .setBody().constant('YAKS rocks!')
      .to('http://greeting-service.${YAKS_NAMESPACE}')
    """
    Given Camel K integration hello-world is running

  Scenario: Create service
    Given create Kubernetes service greeting-service with target port 8080
    Then expect HTTP request body: YAKS rocks!
    And receive POST

  Scenario: Remove Camel K resources
    Given delete Camel K integration hello-world
