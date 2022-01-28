Feature: Camel K integration

  Scenario: Create integration from source code
    Given create Camel K integration hello-world.groovy
    """
    from('timer:tick?period=1000')
      .setBody().constant('Hello world from Camel K')
      .to('log:info')
    """
    Given Camel K integration hello-world is running
    Then Camel K integration hello-world should print Hello world from Camel K

  Scenario: Create integration from file resource
    Given Camel K integration property file application.properties
    When load Camel K integration timer-to-log.groovy
    Then Camel K integration timer-to-log is running
    And Camel K integration timer-to-log should print Hello from Camel K!
