Feature: Camel-K integration

  Background:
    Given create Camel-K integration helloworld.groovy
    """
    from('timer:tick?period=1000')
      .setBody().constant('Hello world from Camel K')
      .to('log:info')
    """

  Scenario:
    Given Camel-K integration helloworld is running
    Then Camel-K integration helloworld should print Hello world from Camel K
