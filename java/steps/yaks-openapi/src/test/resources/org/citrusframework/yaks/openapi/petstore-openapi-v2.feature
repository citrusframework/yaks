Feature: Petstore API V2

  Background:
    Given OpenAPI specification: http://localhost:8080/petstore/v2/openapi.json
    Given variable petId is "citrus:randomNumber(5)"
    Given inbound dictionary
      | $.name          | @assertThat(anyOf(is(hasso),is(cutie),is(fluffy)))@ |
      | $.category.name | @assertThat(anyOf(is(dog),is(cat),is(fish)))@ |
    Given outbound dictionary
      | $.name          | citrus:randomEnumValue('hasso','cutie','fluffy') |
      | $.category.name | citrus:randomEnumValue('dog', 'cat', 'fish') |

  Scenario: getPet
    When invoke operation: getPetById
    Then verify operation result: 200 OK

  Scenario: petNotFound
    Given variable petId is "0"
    When invoke operation: getPetById
    Then verify operation result: 404 NOT_FOUND

  Scenario: addPet
    When invoke operation: addPet
    Then verify operation result: 201 CREATED

  Scenario: updatePet
    When invoke operation: updatePet
    Then verify operation result: 200 OK

  Scenario: deletePet
    When invoke operation: deletePet
    Then verify operation result: 204 NO_CONTENT
