@require(org.apache.camel:camel-http:@camel.version@)
Feature: Http server

  Background:
    Given Disable auto removal of Camel resources
    And HTTP server timeout is 10000 ms
    And HTTP server listening on port 8080
    And start HTTP server

  Scenario: Create Camel route
    Given Camel route greeting.groovy
    """
    from('timer:tick?period=5000')
      .setHeader("CamelHttpMethod", constant("POST"))
      .setBody().constant('YAKS rocks!')
      .to('http://localhost:8080')
    """

  Scenario: Send POST request
    Then expect HTTP request body: YAKS rocks!
    And receive POST
