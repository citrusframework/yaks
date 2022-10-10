Feature: Selenium feature

  Background:
    Given load endpoint web-server.groovy
    Given HTTP request timeout is 60000 ms
    Given wait for GET on URL http://localhost:4444/wd/hub/status
    Given start browser

  Scenario: Index page
    Given user navigates to "http://localhost:8080/"
    And browser page should display heading with tag-name="h1" having
    | text   | Welcome!       |
    | styles | font-size=40px |
    And browser page should display element with id="hello-text" having
    | text   | Hello! |
    | styles | background-color=rgba(0, 0, 0, 0) |
    And browser page should display element with id="counter" having
      | text   | 0 |
    Then sleep 1000 ms
    When user clicks element with id="count"
    Then browser page should display element with id="counter" having
      | text   | 1 |
    When user clicks element with id="count"
    Then sleep 1000 ms
    When user clicks element with id="count"
    Then sleep 1000 ms
    When user clicks element with id="count"
    Then sleep 1000 ms
    When user clicks element with id="count"
    Then sleep 1000 ms
    When user clicks element with id="open-alert"
    And sleep 500 ms
    Then browser page should display alert with text "Hello"
