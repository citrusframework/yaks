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
    When create Kamelet tick-source with template
"""
from:
  uri: timer:tick
  parameters:
    period: "{{period}}"
  steps:
  - set-body:
      constant: "{{message}}"
  - to: "kamelet:sink"
"""
    Then Kamelet tick-source should be available

  Scenario: Use Kamelet
    Given create Camel K integration tick-to-log.groovy
    """
    from('kamelet:tick-source?message=Hello+Kamelets&period=2000')
        .log('${//body//}')
    """
    Then Camel K integration tick-to-log should be running
    Then Camel K integration tick-to-log should print Hello Kamelets

  Scenario: Remove Camel K resources
    Given delete Kamelet tick-source
    Given delete Camel K integration tick-to-log
