Feature: Standard steps

  Scenario: Create variables
    Given variable random = "citrus:randomString(10)"
    Given variable text is "YAKS rocks!"
    Given variable multiline is
    """
    I say
    YAKS rocks!
    """
    Given variables
    | name     | Christoph |
    | greeting | Hello |
    Then log '${greeting} ${name}: ${text}'
    And print '${multiline}'
    And log '${random}'

  Scenario: Load variables from properties
    Given load variables variables.properties
    Then log '${greeting} ${name}: ${text}'
    Then log '${message}'

  Scenario: Create variable from file
    Given load variable message.txt
    Given load variable text from message.txt
    Then log '${message}'
    Then log '${text}'

  Scenario: print log messages
    Given print 'BDD testing on Kubernetes with YAKS'
    Then print 'YAKS rocks!'

  Scenario: print multiline log messages
    Given print
    """
    YAKS rocks!
    """

  Scenario: sleep
    Given sleep 100 ms
    Given sleep 2sec 500ms
