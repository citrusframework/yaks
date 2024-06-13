Feature: Kamelet resource

  Scenario: Use Kamelet
    Given load Camel K integration timer-to-log.groovy
    Then Camel K integration timer-to-log should be running
    Then Camel K integration timer-to-log should print Hello Kamelets

  Scenario: Remove Camel K resources
    Given delete Camel K integration timer-to-log
