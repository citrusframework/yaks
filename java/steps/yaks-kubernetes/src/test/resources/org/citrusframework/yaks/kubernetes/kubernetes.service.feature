Feature: Kubernetes service

  Background:
    Given Kubernetes namespace service-example
    And Kubernetes service "hello-service"
    And Kubernetes service port 8080

  Scenario: Create service
    Given create Kubernetes service hello-service with target port 8080
    Then verify Kubernetes service hello-service exists
