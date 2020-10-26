Feature: Camel xml route

  Background:
    Given Disable auto removal of Camel resources
    Given Camel route hello.xml
    """
    <route>
      <from uri="direct:hello"/>
      <filter>
        <groovy>request.body.startsWith('Hello')</groovy>
        <to uri="log:org.citrusframework.yaks.camel?level=INFO"/>
      </filter>
      <split>
        <tokenize token=" "/>
        <to uri="seda:tokens"/>
      </split>
    </route>
    """

  Scenario: Hello route
    When send Camel exchange to("direct:hello") with body: Hello Camel!
    And receive Camel exchange from("seda:tokens") with body: Hello
    And receive Camel exchange from("seda:tokens") with body: Camel!
