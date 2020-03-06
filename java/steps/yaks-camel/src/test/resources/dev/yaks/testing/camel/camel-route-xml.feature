Feature: Camel xml route

  Background:
    Given Camel route hello.xml
    """
    <route>
      <from uri="direct:hello"/>
      <to uri="log:dev.yaks.testing.camel?level=INFO"/>
      <split>
        <tokenize token=" "/>
        <to uri="seda:tokens"/>
      </split>
    </route>
    """

  Scenario: Hello route
    When send to route direct:hello body: Hello Camel!
    And receive from route seda:tokens body: Hello
    And receive from route seda:tokens body: Camel!
