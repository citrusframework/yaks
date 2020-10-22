Feature: Http client

  Background:
    Given URL: https://api.github.com/

  Scenario: Get a result from API
    When send GET /
    Then receive HTTP 200 OK
