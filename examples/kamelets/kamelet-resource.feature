Feature: Kamelet resource

  Background:
    Given Disable auto removal of Camel K resources
    Given Disable auto removal of Kamelet resources
    Given Camel K resource polling configuration
      | maxAttempts          | 200  |
      | delayBetweenAttempts | 1000 |

  Scenario: Use Kamelet
    Given load Camel K integration timer-to-log.groovy
    Then Camel K integration timer-to-log should be running
    Then Camel K integration timer-to-log should print Hello Kamelets

  Scenario: Remove Camel K resources
    Given delete Camel K integration timer-to-log
