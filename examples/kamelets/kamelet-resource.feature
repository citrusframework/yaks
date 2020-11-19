Feature: Kamelet resource

  Background:
    Given Disable auto removal of Camel-K resources
    Given Disable auto removal of Kamelet resources
    Given Camel-K resource polling configuration
      | maxAttempts          | 20   |
      | delayBetweenAttempts | 1000 |

  Scenario: Create Kamelet from file
    Given load Kamelet timer-source.kamelet.yaml
    Then Kamelet timer-source should be available

  Scenario: Use Kamelet
    Given load Camel-K integration timer-to-log.groovy
    Then Camel-K integration timer-to-log should be running
    Then Camel-K integration timer-to-log should print Hello Kamelets
    Then delete Camel-K integration timer-to-log
