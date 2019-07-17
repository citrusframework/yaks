Feature: Integration Works

  Background:
    Given URL: https://www.wikipedia.org

  Scenario: Get a result from API
    When send GET /
    Then receive status 200 OK
