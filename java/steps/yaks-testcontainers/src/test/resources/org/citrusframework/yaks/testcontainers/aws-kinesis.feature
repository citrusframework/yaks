Feature: AWS KINESIS

  Background:
    Given Disable auto removal of Testcontainers resources
    Given variable streamName="mystream"
    Given variable partitionKey="partition-1"

  Scenario: Start container
    Given Enable service KINESIS
    Given start LocalStack container
    And log 'Started LocalStack container: ${YAKS_TESTCONTAINERS_LOCALSTACK_CONTAINER_NAME}'

  Scenario: Create Kinesis client
    Given New global Camel context
    Given load to Camel registry amazonKinesisClient.groovy

  Scenario: Create event listener
    Given Camel route kinesisEventListener.groovy
    """
    from("aws2-kinesis://${streamName}?amazonKinesisClient=#amazonKinesisClient")
       .convertBodyTo(String.class)
       .to("seda:result")
    """
    Given sleep 5000 ms

  Scenario: Publish event
    Given Camel exchange message header CamelAwsKinesisPartitionKey="${partitionKey}"
    Given send Camel exchange to("aws2-kinesis://${streamName}?amazonKinesisClient=#amazonKinesisClient") with body: YAKS rocks!

  Scenario: Verify event
    Given Camel exchange message header CamelAwsKinesisPartitionKey="${partitionKey}"
    Then receive Camel exchange from("seda:result") with body: YAKS rocks!

  Scenario: Stop event listener
    Given stop Camel route kinesisEventListener

  Scenario: Stop container
    Given stop LocalStack container
