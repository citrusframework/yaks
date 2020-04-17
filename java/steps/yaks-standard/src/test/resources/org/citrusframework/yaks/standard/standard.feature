Feature: Standard steps

  Scenario: print log messages
    Given print 'BDD testing on Kubernetes with YAKS'
    Then print 'YAKS rocks!'

  Scenario: print multiline log messages
    Given print
    """
    YAKS rocks!
    """
