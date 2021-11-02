Feature: Kubernetes service

  Background:
    Given Kubernetes namespace service-example
    And Kubernetes service "hello-service"
    And Kubernetes service port 8080

  Scenario: Create service
    Given create Kubernetes service hello-service with target port 8080
    Then verify Kubernetes service hello-service exists

  Scenario: Create service with port mapping
    Given create Kubernetes service http-service-1 with port mapping 80:8080
    Then verify Kubernetes service http-service-1 exists

  Scenario: Create service with port mappings
    Given create Kubernetes service http-service-2 with port mappings
    | 80   | 8080 |
    | 8081 | 8081 |
    Then verify Kubernetes service http-service-2 exists

  Scenario: Create service with variables
    Given variable port="80"
    Given variable targetPort="8080"
    Given create Kubernetes service http-service-3 with port mapping ${port}:${targetPort}
    Then verify Kubernetes service http-service-3 exists
