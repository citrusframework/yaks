Feature: JDBC API

  Background:
    Given Database connection
      | url       | jdbc:postgresql://syndesis-db:5432/sampledb |
      | username  | sampledb |
      | password  | << insert password >> |

  Scenario: INSERT task
    Given variables
      | taskId  | citrus:randomNumber(4) |
      | task    | Test CamelK with YAKS! |
    Given SQL update: INSERT INTO todo (id, task, completed) VALUES (${taskId}, '${task}', 0)
    When SQL query: SELECT COUNT(task) AS FOUND_TASKS FROM todo WHERE id='${taskId}'
    Then verify column FOUND_TASKS=1

  Scenario: DELETE tasks
    Given SQL update: DELETE FROM todo
    Given SQL query: SELECT COUNT(task) AS FOUND_TASKS FROM todo
    Then verify column FOUND_TASKS=0