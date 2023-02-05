Feature: PostgreSQL

  Background:
    Given Disable auto removal of Testcontainers resources

  Scenario: Start container
    Given Database init script
    """
    CREATE TABLE IF NOT EXISTS todo (id SERIAL PRIMARY KEY, task VARCHAR, completed INTEGER);
    """
    Then start PostgreSQL container
    And log 'Started PostgreSQL container: ${YAKS_TESTCONTAINERS_POSTGRESQL_CONTAINER_NAME}'

  Scenario: Connect and insert
    Given Database connection
      | driver    | ${YAKS_TESTCONTAINERS_POSTGRESQL_DRIVER} |
      | url       | ${YAKS_TESTCONTAINERS_POSTGRESQL_LOCAL_URL} |
      | username  | ${YAKS_TESTCONTAINERS_POSTGRESQL_USERNAME} |
      | password  | ${YAKS_TESTCONTAINERS_POSTGRESQL_PASSWORD} |
    When execute SQL update: INSERT INTO todo VALUES (1, 'Write YAKS test', 0)
    Then SQL query: SELECT * FROM todo WHERE ID=1
    And verify column TASK=Write YAKS test

  Scenario: Data Source and insert
    Given Data source: postgreSQL
    And execute SQL update: INSERT INTO todo VALUES (2, 'Write YAKS blog', 0)
    And SQL query: SELECT * FROM todo WHERE ID=2
    And verify column TASK=Write YAKS blog

  Scenario: Stop container
    Given stop PostgreSQL container
