Feature: AWS S3

  Background:
    Given Disable auto removal of Testcontainers resources
    Given variable bucketName="mybucket"

  Scenario: Start container
    Given Enable service S3
    Given start LocalStack container
    And log 'Started LocalStack container: ${YAKS_TESTCONTAINERS_LOCALSTACK_CONTAINER_NAME}'

  Scenario: Create S3 client
    Given New global Camel context
    Given load to Camel registry amazonS3Client.groovy

  Scenario: Upload file
    Given Camel exchange message header CamelAwsS3Key="yaks.txt"
    Given send Camel exchange to("aws2-s3://${bucketName}?amazonS3Client=#amazonS3Client") with body: YAKS rocks!

  Scenario: Verify uploaded file
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
