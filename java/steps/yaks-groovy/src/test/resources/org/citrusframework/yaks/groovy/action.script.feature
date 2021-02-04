Feature: Run script actions

  Scenario: Load actions
    Given load actions actions.groovy
    Then apply actions.groovy

  Scenario: Inline actions
    Given create actions basic.groovy
    """
    actions {
      echo('Hello from Groovy script')
      sleep().seconds(1)

      createVariables()
          .variable('foo', 'bar')

      echo('Variable foo=${foo}')
    }
    """
    Then apply basic.groovy

  Scenario: Messaging actions
    Given create actions messaging.groovy
    """
    actions {
      send('direct:myQueue')
        .message()
        .body('Hello from Groovy script!')

      receive('direct:myQueue')
        .message()
        .body('Hello from Groovy script!')
    }
    """
    Then apply messaging.groovy
