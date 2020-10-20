Feature: Kamelet

  Background:
    Given Disable auto removal of Kamelet resources
    Given Camel-K resource polling configuration
      | maxAttempts          | 20   |
      | delayBetweenAttempts | 1000 |

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
    And Kamelet type out="text/plain"
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

  Scenario: Use Kamelet
    Given create Camel-K integration timer-to-log.groovy
    """
    from('kamelet:timer-source?message=Hello+Kamelets&period=2000')
        .log('${//body//}')
    """
    Then Camel-K integration timer-to-log should be running
    Then Camel-K integration timer-to-log should print Hello Kamelets
