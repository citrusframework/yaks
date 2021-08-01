Feature: Camel-K integration

  Background:
    Given Camel-K resource polling configuration
    | maxAttempts          | 10   |
    | delayBetweenAttempts | 1000 |

  Scenario: Create integration
    Given Camel-K integration property file integration.properties
    Given create Camel-K integration helloworld.groovy
    """
    from('timer:tick?period=1000')
      .setBody().constant('Hello world from Camel K')
      .to('log:info')
    """

  Scenario: Create integration with modeline
    Given create Camel-K integration modeline.groovy
    """
    // camel-k: dependency=mvn:fake.dependency:foo:1.0
    // camel-k: dependency=mvn:fake.dependency:bar:0.9
    // camel-k: trait=quarkus.native=true

    from('timer:tick?period=1000')
      .setBody().constant('Hello world from Camel K')
      .to('log:info')
    """

  Scenario: Create integration with config
    When create Camel-K integration timertolog.groovy with configuration
      | dependencies | mvn:fake.dependency:foo:1.0,mvn:fake.dependency:bar:0.9 |
      | traits       | quarkus.native=true,quarkus.enabled=true,route.enabled=true |
      | properties   | foo.key=value,bar.key=value |
      | source       | from('timer:tick?period=1000').setBody().constant('Hello world from Camel K').to('log:info') |

  Scenario: Load integration from file
    Given load Camel-K integration integration.groovy

  Scenario: Verify integration running
    Given Camel-K integration pod i1
    Then Camel-K integration i1 should be running

  Scenario: Verify integration stopped
    Given Camel-K integration pod i2 in phase Stopped
    Then Camel-K integration i2 should be stopped
