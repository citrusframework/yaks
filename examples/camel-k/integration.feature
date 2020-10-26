Feature: Camel-K integration

  Scenario:
    Given create Camel-K integration helloworld.groovy
    """
    from('timer:tick?period=1000')
      .setBody().constant('Hello world from Camel K')
      .to('log:info')
    """
    Given Camel-K integration helloworld is running
    Then Camel-K integration helloworld should print Hello world from Camel K
