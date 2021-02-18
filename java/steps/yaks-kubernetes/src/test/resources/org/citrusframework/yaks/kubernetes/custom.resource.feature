Feature: Kubernetes custom resource

  Background:
    Given Kubernetes namespace crd-example
    Given Kubernetes resource polling configuration
      | maxAttempts          | 10   |
      | delayBetweenAttempts | 1000 |

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

  Scenario: Wait for custom resource
    Given create Kubernetes custom resource in foos.foo.dev
"""
apiVersion: foo.dev/v1
kind: Foo
metadata:
  name: test-resource
status:
  conditions:
  - type: Ready
    status: true
"""
    Then wait for condition=Ready on Kubernetes custom resource foo/test-resource in foos.foo.dev/v1
    Then Kubernetes custom resource foo/test-resource in foos.foo.dev/v1 should be ready

  Scenario: Wait for labeled custom resource
    Given create Kubernetes custom resource in foos.foo.dev
"""
apiVersion: foo.dev/v1
kind: Foo
metadata:
  name: bar-resource
  labels:
    app: foo-app
status:
  conditions:
  - type: Ready
    status: true
"""
    Then wait for condition=Ready on Kubernetes custom resource in foos.foo.dev/v1 labeled with app=foo-app
    Then Kubernetes custom resource in foos.foo.dev/v1 labeled with app=foo-app should be ready

  Scenario: Wait for completed custom resource
    Given create Kubernetes custom resource in foos.foo.dev
"""
apiVersion: foo.dev/v1
kind: Foo
metadata:
  name: job-resource
status:
  conditions:
  - type: Completed
    status: true
"""
    Then wait for condition=Completed on Kubernetes custom resource foo/job-resource in foos.foo.dev/v1
