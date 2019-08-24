Feature: Camel groovy route

  Background:
    Given Camel route hello.groovy
    """
    from("direct:hello")
     .to("log:dev.yaks.testing.camel?level=INFO")
     .split(body().tokenize(" "))
       .to("seda:tokens")
     .end()
    """

  Scenario: Send body
    When send to route direct:hello body: Hello Camel from Groovy!
    And receive from route seda:tokens body: Hello
    And receive from route seda:tokens body: Camel
    And receive from route seda:tokens body: from
    And receive from route seda:tokens body: Groovy!

  Scenario: Expect body received
    Given request body: Hi Camel!
    When send to route direct:hello
    Then expect body received: Hi
    And receive from route seda:tokens
    Then expect body received: Camel!
    And receive from route seda:tokens

  Scenario: Body multiline
    Given request body
    """
    Howdy Camel!
    """
    When send to route direct:hello
    Then expect body received
    """
    Howdy
    """
    And receive from route seda:tokens
    Then expect body received
    """
    Camel!
    """
    And receive from route seda:tokens
