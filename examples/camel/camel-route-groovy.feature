Feature: Camel groovy route

  Background:
    Given Disable auto removal of Camel resources
    Given Camel route hello.groovy
    """
    from("direct:hello")
     .to("log:org.citrusframework.yaks.camel?level=INFO")
     .split(body().tokenize(" "))
       .to("seda:tokens")
     .end()
    """

  Scenario: Hello route
    When send Camel exchange to("direct:hello") with body: Hello Camel!
    And receive Camel exchange from("seda:tokens") with body: Hello
    And receive Camel exchange from("seda:tokens") with body: Camel!
