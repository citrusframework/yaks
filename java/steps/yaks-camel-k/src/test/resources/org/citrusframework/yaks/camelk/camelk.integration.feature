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

  Scenario: Create integration with config
    When create Camel-K integration timertolog.groovy with configuration:
      | dependencies | mvn:fake.dependency:foo:1.0,mvn:fake.dependency:bar:0.9 |
      | traits       | quarkus.native=true,quarkus.enabled=true,route.enabled=true |
      | properties   | foo.key=value,bar.key=value |
      | source       | from('timer:tick?period=1000').setBody().constant('Hello world from Camel K').to('log:info') |

  Scenario: Verify integration state
    Given Camel-K integration pod sample
    Then Camel-K integration sample should be running
