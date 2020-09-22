Feature: Endpoint script config

  Scenario: Load endpoint
    Given URL: http://localhost:18088
    Given load endpoint fooServer.groovy
    When verify endpoint fooServer
    Then send GET /hello
    And receive HTTP 200 OK

  Scenario: Create Http endpoint
    Given URL: http://localhost:18081
    Given create endpoint helloServer.groovy
    """
    http()
      .server()
      .port(18081)
      .autoStart(true)
    """
    When verify endpoint helloServer
    Then send GET /hello
    And receive HTTP 200 OK

  Scenario: Create direct endpoint
    Given create message queue hello-queue
    Given create endpoint helloEndpoint.groovy
    """
    direct()
      .asynchronous()
      .queue("hello-queue")
    """
    When verify endpoint helloEndpoint
    When endpoint helloEndpoint sends payload Hello from new direct endpoint!
    Then endpoint helloEndpoint should receive payload Hello from new direct endpoint!
