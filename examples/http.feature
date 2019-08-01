Feature: Integration Works

  Background:
    Given URL: https://swapi.co/api/films

  Scenario: Get a result from API
    When send GET /
    Then receive HTTP 200 OK
