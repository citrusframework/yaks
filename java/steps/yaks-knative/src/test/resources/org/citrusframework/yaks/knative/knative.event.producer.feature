Feature: Knative event producer

  Background:
    Given variable port is "8080"
    Given Knative event producer timeout is 5000 ms
    Given Knative broker URL: http://localhost:${port}

  Scenario: Send event
    When send Knative event
      | specversion     | 1.0 |
      | type            | greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello |
      | id              | say-hello |
      | time            | citrus:currentDate('yyyy-MM-dd'T'HH:mm:ss') |
      | datacontenttype | application/json |
      | data            | {"msg": "Hello Knative!"} |

  Scenario: Send event data
    Given Knative event data: {"msg": "Hello Knative!"}
    When send Knative event
      | type            | greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello |
      | id              | say-hello |

  Scenario: Send event http
    Given Knative event data: {"msg": "Hello Knative!"}
    When send Knative event
      | Ce-Specversion     | 1.0 |
      | Ce-Type            | greeting |
      | Ce-Source          | https://github.com/citrusframework/yaks |
      | Ce-Subject         | hello |
      | Ce-Id              | say-hello |
      | Ce-Time            | citrus:currentDate('yyyy-MM-dd'T'HH:mm:ss') |
      | Content-Type       | application/json |

  Scenario: Send multiline event data
    Given Knative event data
    """
    {"msg": "Hello Knative!"}
    """
    When send Knative event
      | type            | greeting |
      | source          | https://github.com/citrusframework/yaks |
      | subject         | hello |
      | id              | say-hello |

  Scenario: Send event json
    When send Knative event as json
    """
    {
      "specversion" : "1.0",
      "type" : "greeting",
      "source" : "https://github.com/citrusframework/yaks",
      "subject" : "hello",
      "id" : "say-hello",
      "time" : "citrus:currentDate('yyyy-MM-dd'T'HH:mm:ss')",
      "datacontenttype" : "application/json",
      "data" : "{\"msg\": \"Hello Knative!\"}"
    }
    """
