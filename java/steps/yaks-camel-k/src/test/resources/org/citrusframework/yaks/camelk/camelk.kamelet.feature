Feature: Kamelet

  Scenario: Create Kamelet
    Given Kamelet property definition
    | name     | message       |
    | type     | string        |
    | required | true          |
    | example  | "hello world" |
    And Kamelet property definition
    | name     | period        |
    | type     | integer       |
    | default  | 1000          |
    And Kamelet source helloworld.groovy
    """
    from('timer:tick?period=#property:period')
      .setBody().constant('{{message}}')
      .to('kamelet:sink')
    """
    And Kamelet type out="test/plain"
    When create Kamelet timer-source
    Then Kamelet timer-source should be available

  Scenario: Create Kamelet with flow
    Given Kamelet property definition
      | name     | message       |
      | type     | string        |
      | required | true          |
      | example  | "hello world" |
    And Kamelet property definition
      | name     | period        |
      | type     | integer       |
      | default  | 1000          |
    And Kamelet type out="test/plain"
    When create Kamelet timer-source with flow
"""
from:
  uri: timer:tick
  parameters:
    period: "#property:period"
  steps:
  - set-body:
      constant: "{{message}}"
  - to: "kamelet:sink"
"""
    Then Kamelet timer-source should be available

  Scenario: Create Kamelet from file
    Given load Kamelet timer-source.kamelet.yaml
    And load Camel-K integration timer-to-log.groovy
    Then Kamelet timer-source should be available

