Feature: Integration Works

  Background:
    Given URL: https://swapi.co/api/films

  Scenario: Get a result from API
    When send GET /
    Then receive status 200 OK
