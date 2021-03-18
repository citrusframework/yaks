# YAKS ![logo][1] 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.citrusframework.yaks/yaks-parent/badge.svg?style=flat-square)](https://search.maven.org/search?q=g:org.citrusframework.yaks)
[![build](https://github.com/citrusframework/yaks/workflows/build/badge.svg?branch=main)](https://github.com/citrusframework/yaks/actions) 
[![nightly](https://github.com/citrusframework/yaks/workflows/nightly/badge.svg)](https://github.com/citrusframework/yaks/actions)
[![Licensed under Apache License version 2.0](https://img.shields.io/github/license/openshift/origin.svg?maxAge=2592000)](https://www.apache.org/licenses/LICENSE-2.0")
[![Chat on Zulip](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://citrusframework.zulipchat.com)

## What is YAKS!?

YAKS is a framework to enable Cloud Native BDD testing on Kubernetes! Cloud Native here means that your tests execute
as Kubernetes PODs.

As a user you can run tests by creating a `Test` custom resource on your favorite Kubernetes based cloud provider.
Once the YAKS operator is installed it will listen for custom resources and automatically prepare a test runtime
that runs the test as part of the cloud infrastructure.

Tests in YAKS follow the BDD (Behavior Driven Development) concept and represent feature specifications written
in [Gherkin](https://cucumber.io/docs/gherkin/) syntax.

As a framework YAKS provides a set of predefined [Cucumber](https://cucumber.io/) steps which help you to connect with different
messaging transports (Http REST, JMS, Kafka, Knative eventing) and verify message data with assertions on the header and body content.

YAKS adds its functionality on top of on [Citrus](https://citrusframework.org) for connecting to different endpoints as a client
and/or server.

Read more about YAKS in the [reference manual](https://citrusframework.org/yaks/reference/html/index.html)

## Getting started

Assuming you have a Kubernetes playground and that you are connected to a namespace on that cluster 
just write a `helloworld.feature` BDD file with the following content:

_helloworld.feature_
```gherkin
Feature: Hello

  Scenario: Print hello message
    Given print 'Hello from YAKS!'
```

You can then execute the following command using the [YAKS CLI tool](https://github.com/citrusframework/yaks/releases/):

```bash
yaks test helloworld.feature
```

This runs the test immediately on the current namespace in your connected Kubernetes cluster.
Nothing else is needed.

Please continue reading the [documentation](https://citrusframework.org/yaks/reference/html/index.html) and learn how to 
install and get started working with YAKS.

## For YAKS developers

Requirements:

- Go 1.13+
- Operator SDK 0.19.4+
- Maven 3.6.2+
- Git client

You can build the YAKS project and get the `yaks` CLI by running:

```
make build
```

If you want to build the operator image locally for development in Minishift for instance, then:

```
# Build binaries and images
eval $(minishift docker-env)
make clean images-no-test
```

If the operator pod is running, just delete it to let it grab the new image.

```
oc delete pod yaks
```

 [1]: /docs/logo-30x30.png "YAKS"
