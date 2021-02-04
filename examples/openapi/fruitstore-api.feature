Feature: FruitStore API

  Scenario: Start test server
    Given load endpoint fruitstore-server.groovy

  Scenario: addFruit
    Given OpenAPI specification: http://localhost:8080/openapi
    When invoke operation: addFruit
    Then verify operation result: 201 CREATED

  Scenario: getFruit
    Given OpenAPI specification: http://localhost:8080/openapi
    Given variable id is "1000"
    When invoke operation: getFruitById
    Then verify operation result: 200 OK

  Scenario: fruitNotFound
    Given OpenAPI specification: http://localhost:8080/openapi
    Given variable id is "0"
    When invoke operation: getFruitById
    Then verify operation result: 404 NOT_FOUND
