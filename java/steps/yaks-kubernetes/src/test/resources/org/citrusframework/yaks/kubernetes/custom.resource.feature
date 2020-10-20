Feature: Kubernetes custom resource

  Background:
    Given Kubernetes namespace crd-example

  Scenario: Create custom resource
    Given create Kubernetes custom resource in brokers.eventing.knative.dev
"""
apiVersion: eventing.knative.dev/v1
kind: Broker
metadata:
  name: my-broker
"""
    Then verify broker my-broker exists

  Scenario: Create from file resource
    Given load Kubernetes custom resource broker.yaml in brokers.eventing.knative.dev
    Then verify broker my-broker-resource exists
