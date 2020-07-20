Feature: Knative event consumer

  Background:
    Given variable knativeServicePort is "8181"
    And Knative service port 8181
    And Knative service "hello-service"
    And Knative event consumer timeout is 5000 ms
    And variable id is "citrus:randomNumber(4)"
    And create test event
    """
    {
      "specversion" : "1.0",
      "type" : "greeting",
      "source" : "https://github.com/citrusframework/yaks",
      "subject" : "hello",
      "id" : "say-hello-${id}",
      "datacontenttype" : "application/json",
      "data" : "{\"msg\": \"Hello Knative!\"}"
    }
    """

  Scenario: Receive event
    When receive Knative event
      | specversion     | 1.0 |
      | type            | greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello |
      | id              | say-hello-${id} |
      | datacontenttype | application/json;charset=UTF-8 |
      | data            | {"msg": "Hello Knative!"} |
    Then verify test event accepted

  Scenario: Receive event data
    Given expect Knative event data: {"msg": "Hello Knative!"}
    When receive Knative event
      | type            | greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello |
      | id              | say-hello-${id} |
    Then verify test event accepted

  Scenario: Receive event http
    Given expect Knative event data: {"msg": "Hello Knative!"}
    When receive Knative event
      | Ce-Specversion     | 1.0 |
      | Ce-Type            | greeting |
      | Ce-Source          | https://github.com/citrusframework/yaks |
      | Ce-Subject         | hello |
      | Ce-Id              | say-hello-${id} |
      | Content-Type       | application/json;charset=UTF-8 |
    Then verify test event accepted

  Scenario: Receive multiline event data
    Given expect Knative event data
    """
    {"msg": "Hello Knative!"}
    """
    When receive Knative event
      | type            | greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello |
      | id              | say-hello-${id} |
    Then verify test event accepted

  Scenario: Receive event json
    When receive Knative event as json
    """
    {
      "specversion" : "1.0",
      "type" : "greeting",
      "source" : "https://github.com/citrusframework/yaks",
      "subject" : "hello",
      "id" : "say-hello-${id}",
      "time": "@matchesDatePattern('yyyy-MM-dd'T'HH:mm:ss')@",
      "datacontenttype" : "application/json;charset=UTF-8",
      "data" : "{\"msg\": \"Hello Knative!\"}"
    }
    """
    Then verify test event accepted
