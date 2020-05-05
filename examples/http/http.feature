Feature: Http client

  Background:
    Given URL: https://github.com/citrusframework/yaks

  Scenario: Get a result from API
    When send GET /
    Then receive HTTP 200 OK
