@require(org.apache.camel:camel-http:@camel.version@)
Feature: Kubernetes service

  Background:
    Given HTTP server "greeting-service"
    And HTTP server timeout is 10000 ms
    And HTTP server listening on port 8080

  Scenario:
    Given Camel route greeting.groovy
    """
    from('timer:tick?period=5000')
      .setHeader("CamelHttpMethod", constant("POST"))
      .setBody().constant('YAKS rocks!')
      .to('http://greeting-service/')
    """

  Scenario: Create service
    Given create Kubernetes service greeting-service with target port 8080
    Then expect HTTP request body: YAKS rocks!
    And receive POST
