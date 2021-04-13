Feature: Custom resources

  Scenario: Create inline
    Given create Kubernetes custom resource in foos.yaks.dev
"""
apiVersion: yaks.dev/v1alpha1
kind: Foo
metadata:
  name: inline-foo
spec:
  message: Hello
status:
  conditions:
  - type: Ready
    status: true
"""

  Scenario: Create from file
    Given load Kubernetes custom resource foo.yaml in foos.yaks.dev
