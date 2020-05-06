Feature: Task API

  Background:
    Given URL: << insert URL >>
    Given Database connection
      | url       | jdbc:postgresql://syndesis-db:5432/sampledb |
      | username  | sampledb |
      | password  | << insert password >> |

  Scenario: GET task
    Given variables
      | taskId  | citrus:randomNumber(4) |
      | task    | Test CamelK with YAKS! |
    Given SQL update: INSERT INTO todo (id, task, completed) VALUES (${taskId}, '${task}', '0')
    When send GET /api/${taskId}
    Then verify HTTP response header Content-Type="application/json"
    Then verify HTTP response body
      """
      {
        "id": "${taskId}",
        "task": "${task}",
        "completed": "0"
      }
      """
    And receive HTTP 200 OK

  Scenario: POST task
    Given variable taskId is "citrus:randomNumber(10)"
    Given SQL query: SELECT COUNT(task) AS FOUND_TASKS FROM todo WHERE task='Task #${taskId}'
    Given HTTP request body
      """
      {
        "id": "${taskId}",
        "task": "Task #${taskId}",
        "completed": "0"
      }
      """
    When send POST /api
    Then receive HTTP 201 CREATED
    And verify column FOUND_TASKS=1

  Scenario: DELETE task
    Given variables
      | toDelete  | citrus:randomNumber(4) |
    Given SQL update: INSERT INTO todo (id, task, completed) VALUES (${toDelete}, 'citrus:randomString(10)', '0')
    Given SQL query: SELECT COUNT(task) AS FOUND_TASKS FROM todo WHERE id='${toDelete}'
    Given verify column FOUND_TASKS=1
    When send DELETE /api/${toDelete}
    Then receive HTTP 204 NO_CONTENT
    And verify column FOUND_TASKS=0
