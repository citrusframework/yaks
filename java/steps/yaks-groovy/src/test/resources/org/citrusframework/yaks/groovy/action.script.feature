Feature: Run script actions

  Scenario: Load actions
    Given load actions actions.groovy
    Then apply actions.groovy
    And variable greeting is "Hey there"

  Scenario: Inline actions
    Given create actions basic.groovy
    """
    $actions {
      $(doFinally().actions(
          echo('${greeting} in finally!')
      ))

      $(echo('Hello from Groovy script'))
      $(delay().seconds(1))

      $(createVariables()
          .variable('foo', 'bar'))

      $(echo('Variable foo=${foo}'))
    }
    """
    Then apply basic.groovy
    And variable greeting is "Hello"

  Scenario: Messaging actions
    Given create actions messaging.groovy
    """
    $actions {
      $(send('direct:myQueue')
        .message()
        .body('Hello from Groovy script!'))

      $(receive('direct:myQueue')
        .message()
        .body('Hello from Groovy script!'))
    }
    """
    Then apply messaging.groovy

  Scenario: Run actions
    Given $(doFinally().actions(echo('${greeting} in finally!')))
    When $(createVariable('greeting', 'Hello from YAKS!'))
    Then $(echo('${greeting}'))
    And print '${greeting}'
    And variable greeting is "Ciao"

  Scenario: Run actions multiline
    Given apply script
    """
    $(doFinally().actions(
        echo('${greeting} in finally!')
    ))
    """
    When apply script
    """
    send('direct:myQueue')
      .message()
      .body('Hello from Groovy script!')
    """
    Then apply script
    """
    receive('direct:myQueue')
      .message()
      .body('Hello from Groovy script!')
    """
    And variable greeting is "Bye"
