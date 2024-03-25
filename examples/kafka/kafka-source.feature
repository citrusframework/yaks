# ---------------------------------------------------------------------------
# Copyright the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ---------------------------------------------------------------------------

Feature: Kafka Kamelet source

  Background:
    Given variable user is ""
    Given variable password is ""
    Given variables
      | securityProtocol   | PLAINTEXT |
      | deserializeHeaders | true |
      | topic              | my-topic |
      | source             | Kafka Kamelet source |
      | message            | Camel K rocks! |
    Given Kafka topic: ${topic}
    Given Kafka topic partition: 0
    Given HTTP server timeout is 15000 ms
    Given HTTP server "test-service"

  Scenario: Create infrastructure
    Given start Kafka container
    Given create Kubernetes service test-service with target port 8080

  Scenario: Create Kamelet binding
    When load KameletBinding kafka-source-binding.yaml
    Then Camel K integration kafka-source-binding should be running
    And Camel K integration kafka-source-binding should print Subscribing ${topic}-Thread 0 to topic ${topic}
    And sleep 10sec

  Scenario: Send message to Kafka topic and verify sink output
    Given variable key is "citrus:randomNumber(4)"
    Given Kafka connection
      | url | ${YAKS_TESTCONTAINERS_KAFKA_LOCAL_BOOTSTRAP_SERVERS} |
    Given Kafka message key: ${key}
    When send Kafka message with body and headers: ${message}
      | event-source | ${source} |
    Then expect HTTP request body: ${message}
    Then expect HTTP request headers
      | event-source    | ${source} |
      | kafka.TOPIC     | ${topic}  |
      | kafka.KEY       | ${key}    |
      | kafka.PARTITION | 0         |
    And receive POST /result
    And send HTTP 200 OK

  Scenario: Remove resources
    Given delete KameletBinding kafka-source-binding
    And delete Kubernetes service test-service
    And stop Kafka container
