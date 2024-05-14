Feature: AWS KINESIS

  Background:
    Given Disable auto removal of Testcontainers resources
    Given variable streamName="mystream"
    Given variable partitionKey="partition-1"

  Scenario: Start container
    Given Enable service KINESIS
    Given start LocalStack container
    Given HTTP request timeout is 20000 ms
    And wait for URL ${YAKS_TESTCONTAINERS_LOCALSTACK_SERVICE_URL}

  Scenario: Verify Kinesis events
    # Create AWS-S3 client
    Given load to Camel registry amazonKinesisClient.groovy

    # Create event listener
    Given Camel route kinesisEventListener.groovy
    """
    from("aws2-kinesis://${streamName}?amazonKinesisClient=#amazonKinesisClient")
       .convertBodyTo(String.class)
       .to("seda:result")
    """
    Given sleep 5000 ms

    # Publish event
    Given Camel exchange message header CamelAwsKinesisPartitionKey="${partitionKey}"
    Given send Camel exchange to("aws2-kinesis://${streamName}?amazonKinesisClient=#amazonKinesisClient") with body: YAKS rocks!

    # Verify event
    Given Camel exchange message header CamelAwsKinesisPartitionKey="${partitionKey}"
    Then receive Camel exchange from("seda:result") with body: YAKS rocks!

  Scenario: Remove resources
    Given stop Camel route kinesisEventListener
    Given stop LocalStack container
