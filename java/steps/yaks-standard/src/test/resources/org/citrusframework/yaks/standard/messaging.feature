Feature: Messaging features

Background:
  Given variable text is "Hello from citrus:randomString(10)"
  Given create message queue foo

  Scenario: Send and receive inline
    When endpoint fooEndpoint sends payload {"message": {"text": "${text}"}}
    Then endpoint fooEndpoint should receive payload {"message": {"text": "${text}"}}
    Then endpoint fooEndpoint should send payload {"message": {"text": "${text}"}}
     And endpoint fooEndpoint receives payload {"message": {"text": "${text}"}}

  Scenario: Send and receive multiline
    When endpoint fooEndpoint sends payload
      """
      {
        "message": {
          "text": "${text}"
        }
      }
      """
    Then endpoint fooEndpoint should receive payload
      """
      {
        "message": {
          "text": "${text}"
        }
      }
      """

  Scenario: Send and receive plaintext inline
    When endpoint echoEndpoint sends payload ${text}
    Then endpoint echoEndpoint should receive plaintext payload You just said: ${text}

  Scenario: Send and receive plaintext multiline
    When endpoint echoEndpoint sends payload
      """
      ${text}
      """
    Then endpoint echoEndpoint should receive plaintext payload
      """
      You just said: ${text}
      """

  Scenario: Message definition inline
    Given new message echoRequest
      And $echoRequest has payload ${text}
      And $echoRequest header operation is "sayHello"
    Given new message echoResponse
      And $echoResponse has payload You just said: ${text}
      And $echoResponse header operation is "sayHello"
    When endpoint echoEndpoint sends message $echoRequest
    Then endpoint echoEndpoint should receive plaintext message $echoResponse

  Scenario: Message definition multiline
    Given new message echoRequest
      And $echoRequest has payload
        """
        ${text}
        """
      And $echoRequest header operation="sayHello"
    Given new message echoResponse
      And $echoResponse has payload
        """
        You just said: ${text}
        """
      And $echoResponse header operation="sayHello"
    When endpoint echoEndpoint sends message $echoRequest
    Then endpoint echoEndpoint should receive plaintext message $echoResponse
