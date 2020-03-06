Feature: Camel context

  Background:
    Given New Spring Camel context
    """
    <beans xmlns="http://www.springframework.org/schema/beans"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                              http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd">
      <camelContext id="helloContext" xmlns="http://camel.apache.org/schema/spring">
        <route id="helloRoute">
          <from uri="direct:hello"/>
          <to uri="log:org.citrusframework.yaks.camel?level=INFO"/>
          <split>
            <tokenize token=" "/>
            <to uri="seda:tokens"/>
          </split>
        </route>
      </camelContext>
    </beans>
    """

  Scenario: Hello Context
    Given request body: Hello Camel!
    When send to route direct:hello
    Then expect body received: Hello
    And receive from route seda:tokens
    Then expect body received: Camel!
    And receive from route seda:tokens
