Feature: Http SSL support

  Background:
    Given HTTP server SSL keystore path classpath:keystore/http-server.jks
    Given HTTP server SSL keystore password secret
    Given create HTTP server "secureHttpServer" with configuration
    | secure     | true |
    | securePort | 8443 |
    | timeout    | 1000 |
    And start HTTP server

  Scenario: Secure Http GET
    Given URL: https://localhost:8443
    When send GET /todo
    Then receive HTTP 200 OK
