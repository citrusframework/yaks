Feature: Secrets

  Scenario: Print message
    Given print 'This variable ${user} should be coming from a secret volume mount'
    Given print 'This variable ${password} should be coming from a secret volume mount'
