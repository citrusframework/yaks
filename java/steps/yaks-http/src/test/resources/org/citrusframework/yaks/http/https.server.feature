Feature: Https support

  Background:
    Given create HTTP server "secureHttpServer" with configuration
    | secure     | true |
    | securePort | 8443 |
    | timeout    | 1000 |
    And start HTTP server

  Scenario: Secure Http GET
    Given URL: https://localhost:8443
    When send GET /todo
    Then receive HTTP 200 OK
