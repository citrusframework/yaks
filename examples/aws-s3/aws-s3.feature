Feature: AWS S3

  Background:
    Given Disable auto removal of Testcontainers resources
    Given variable bucketName="mybucket"

  Scenario: Start container
    Given Enable service S3
    Given start LocalStack container
    Given HTTP request timeout is 20000 ms
    And wait for URL ${YAKS_TESTCONTAINERS_LOCALSTACK_SERVICE_URL}

  Scenario: Verify AWS S3 events
    # Create S3 client
    Given load to Camel registry amazonS3Client.groovy

    # Publish event
    Given Camel exchange message header CamelAwsS3Key="yaks.txt"
    Given send Camel exchange to("aws2-s3://${bucketName}?amazonS3Client=#amazonS3Client") with body: YAKS rocks!

    # Verify uploaded file
    Given Camel route getS3Object.groovy
    """
    from("direct:getObject")
       .to("aws2-s3://${bucketName}?amazonS3Client=#amazonS3Client&operation=getObject")
       .convertBodyTo(String.class)
       .to("seda:result")
    """
    Given Camel exchange message header CamelAwsS3Key="yaks.txt"
    When send Camel exchange to("direct:getObject")
    Then receive Camel exchange from("seda:result") with body: YAKS rocks!

  Scenario: Stop container
    Given stop LocalStack container
