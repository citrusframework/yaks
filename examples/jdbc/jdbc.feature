Feature: JDBC API

  Scenario: Start container
    Given Database init script
    """
    CREATE TABLE IF NOT EXISTS todo (id SERIAL PRIMARY KEY, task VARCHAR, completed INTEGER);
    """
    Then start PostgreSQL container
    And log 'Started PostgreSQL container: ${YAKS_TESTCONTAINERS_POSTGRESQL_CONTAINER_NAME}'

  Scenario: INSERT task
    Given Data source: postgreSQL
    Given variables
      | taskId  | citrus:randomNumber(4) |
      | task    | Test CamelK with YAKS! |
    Given SQL update: INSERT INTO todo (id, task, completed) VALUES (${taskId}, '${task}', 0)
    When SQL query: SELECT COUNT(task) AS FOUND_TASKS FROM todo WHERE id='${taskId}'
    Then verify column FOUND_TASKS=1

  Scenario: DELETE tasks
    Given Data source: postgreSQL
    Given SQL update: DELETE FROM todo
    Given SQL query: SELECT COUNT(task) AS FOUND_TASKS FROM todo
    Then verify column FOUND_TASKS=0