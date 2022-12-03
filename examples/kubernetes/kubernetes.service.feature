@require(org.apache.camel:camel-http:@camel.version@)
Feature: Kubernetes service

  Background:
    Given HTTP server "greeting-service"
    And HTTP server timeout is 60000 ms
    And HTTP server listening on port 8080
    And Kubernetes timeout is 60000 ms

  Scenario: Call service from Camel K integration
    Given create Kubernetes service greeting-service with target port 8080
    Given create Camel K integration hello-world.groovy
    """
    from('timer:tick?period=5000')
      .setHeader("CamelHttpMethod", constant("POST"))
      .setBody().constant('YAKS rocks!')
      .to('yaks:resolveURL(greeting-service)')
      .to('log:info?showStreams=true')
    """
    When Camel K integration hello-world is running
    Then expect HTTP request body: YAKS rocks!
    And receive POST
    And HTTP response body: Thank You!
    And send HTTP 200 OK
    When Camel K integration hello-world should print Thank You!
