Feature: Knative eventing

  Background:
    Given Knative event consumer timeout is 20000 ms
    Given Knative broker default is running
    Given variable id is "citrus:randomNumber(4)"

  Scenario: Service trigger
    Given create Knative event consumer service hello-service
    Given create Knative trigger hello-service-trigger on service hello-service with filter on attributes
      | type   | service-greeting |
      | source | https://github.com/citrusframework/yaks |
    When Knative event data: {"msg": "Hello Knative!"}
    And send Knative event
      | type            | service-greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello service |
      | id              | say-hello-${id} |
    Then expect Knative event data: {"msg": "Hello Knative!"}
    And verify Knative event
      | type            | service-greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello service |
      | id              | say-hello-${id} |

  Scenario: Channel subscription
    Given create Knative channel hello-channel
    Given create Knative event consumer service hello-service
    Given subscribe service hello-service to Knative channel hello-channel
    Given create Knative trigger hello-channel-trigger on channel hello-channel with filter on attributes
      | type   | channel-greeting |
      | source | https://github.com/citrusframework/yaks |
    When Knative event data: {"msg": "Hello Knative!"}
    And send Knative event
      | type            | channel-greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello channel |
      | id              | say-hello-${id} |
    Then expect Knative event data: {"msg": "Hello Knative!"}
    And verify Knative event
      | type            | channel-greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello channel |
      | id              | say-hello-${id} |
