Feature: Standard steps

  Scenario: Create variables
    Given variable text is "YAKS rocks!"
    Given variables
    | name     | Christoph |
    | greeting | Hello |
    Then log '${greeting} ${name}: ${text}'

  Scenario: print log messages
    Given print 'BDD testing on Kubernetes with YAKS'
    Then print 'YAKS rocks!'

  Scenario: print multiline log messages
    Given print
    """
    YAKS rocks!
    """
