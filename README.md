# YAKS ![logo][1] 

[![build](https://github.com/citrusframework/yaks/workflows/build/badge.svg?branch=master)](https://github.com/citrusframework/yaks/actions) 
[![Licensed under Apache License version 2.0](https://img.shields.io/github/license/openshift/origin.svg?maxAge=2592000)](https://www.apache.org/licenses/LICENSE-2.0")
[![Chat on Zulip](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://citrusframework.zulipchat.com)

YAKS is a platform to enable Cloud Native BDD testing on Kubernetes!

* [Getting started](#getting-started)
  * [Installation](#installation)
  * [Running](#running-the-hello-world)
* [Steps](#steps)
  * [Citrus Steps](#citrus-steps)
  * Apache Camel Steps
  * [Camel K Steps](#camel-k-steps)
  * [JDBC Steps](#jdbc-steps)
  * [Http Steps](#http-steps)
  * [Open API Steps](#openapi-steps)
  * Kafka Steps
  * Jms Steps
  * [Groovy Steps](#groovy-steps)
  * [Custom Steps](#custom-steps)
* [Runtime configuration](#runtime-configuration)
  * [Add custom runtime dependencies](#add-custom-runtime-dependencies)
  * [Add custom Maven repositories](#add-custom-maven-repositories)
  * [Using secrets](#using-secrets)
* [Pre/Post scripts](#prepos-scripts)
* [Reporting options](#reporting-options)
* [For YAKS developers](#for-yaks-developers)

## Getting Started

YAKS allows you to perform Could Native BDD testing. Cloud Native here means that your tests executes within a Kubernetes POD. 

With the YAKS operator installed, you can run tests by creating a `Test` custom resource on the cluster.

Tests are defined using [Gherkin](https://cucumber.io/docs/gherkin/) syntax. As a framework YAKS provides a set of predefined steps which
help to connect with different messaging transports (Http REST, JMS, Kafka, Knative eventing) and verify responses with
assertions on message header and body content.

### Windows prerequisite
For full support of Yaks on Windows please enable "Windows Subsystem for Linux". You can do it manually by heading to Control Panel > Programs > Turn 
Windows Features On or Off and checking "Windows Subsystem for Linux". Or you can simply execute this command in powershell:

`Enable-WindowsOptionalFeature -Online -FeatureName Microsoft-Windows-Subsystem-Linux`

This action requires a full reboot of the system.

### Installation

The easiest way to getting started with YAKS is using the **YAKS CLI**.
You can download the CLI from the [release page](https://github.com/citrusframework/yaks/releases/).

To install the `yaks` binary, just make it runnable and move it to a location in your `$PATH`, e.g. on linux:

```          
# Make executable and move to usr/local/bin
$ chmod a+x yaks-0.0.x-linux-64bit
$ mv yaks-0.0.x-linux-64bit /usr/local/bin/yaks

# Alternatively, set a symbolic link to "yaks" 
$ mv yaks-0.0.x-linux-64bit yaks
$ ln -s $(pwd)/yaks /usr/local/bin
```

YAKS tests can be executed on any Kubernetes or OpenShift environment.

You need to connect to the cluster and switch to the namespace where you want YAKS to be installed.
You can also create a new namespace:

```
oc new-project my-yaks-project
``` 

To install YAKS into your namespace, just run:

```
# If it's the first time you install it on a cluster, make sure you're cluster admin.

yaks install
```

This will install the YAKS operator in the selected namespace. If not already installed, the command will also install
the YAKS custom resource definitions in the cluster (in this case, the user needs cluster-admin permissions).

### Running the Hello World!

_examples/helloworld.feature_
```
Feature: hello world

  Scenario: print slogan
    Given YAKS does Cloud-Native BDD testing
    Then YAKS rocks!

```

The `helloworld.feature` file is present in the `examples` directory of this repository: you can clone the repo or just 
download it to your host. 

Once you have your first test in the `helloworld.feature` file, you can **run it** using: 

```
yaks test helloworld.feature
```

This is an example of output you should get:

```
[user@localhost yaks]$ ./yaks test hello.feature 
test "hello" created
+ test-hello-bldrmuj9nakdqqrj7eag › test
[INFO] Scanning for projects...
[INFO] Add dynamic project dependencies ...
[INFO] Add mounted test resources in directory: /deployments
[INFO] 
[INFO] ------------< org.citrusframework.yaks:yaks-runtime-maven >-------------
[INFO] Building YAKS :: Runtime :: Maven 1.0.0-SNAPSHOT
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- maven-enforcer-plugin:1.4.1:enforce (enforce-maven-version) @ yaks-runtime-maven ---
[INFO] 
[INFO] --- maven-remote-resources-plugin:1.5:process (default) @ yaks-runtime-maven ---
[INFO] 
[INFO] --- maven-remote-resources-plugin:1.5:process (process-resource-bundles) @ yaks-runtime-maven ---
[INFO] 
[INFO] --- maven-resources-plugin:3.1.0:resources (default-resources) @ yaks-runtime-maven ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] skip non existing resourceDirectory /deployments/data/yaks-runtime-maven/src/main/resources
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.1:compile (default-compile) @ yaks-runtime-maven ---
[INFO] No sources to compile
[INFO] 
[INFO] --- maven-resources-plugin:3.1.0:testResources (default-testResources) @ yaks-runtime-maven ---
[INFO] Using 'UTF-8' encoding to copy filtered resources.
[INFO] Copying 4 resources
[INFO] skip non existing resourceDirectory /deployments/..data
[INFO] 
[INFO] --- maven-compiler-plugin:3.8.1:testCompile (default-testCompile) @ yaks-runtime-maven ---
[INFO] Nothing to compile - all classes are up to date
[INFO] 
[INFO] --- maven-surefire-plugin:2.22.2:test (default-test) @ yaks-runtime-maven ---
[INFO] 
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running org.citrusframework.yaks.YaksTest
2020-03-06 15:30:51.125|INFO |main|CitrusObjectFactory - Initializing injection mode 'RUNNER' for Citrus 2.8.0
Mar 06, 2020 3:30:51 PM org.springframework.context.support.AbstractApplicationContext prepareRefresh
INFO: Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@6f7923a5: startup date [Fri Mar 06 15:30:51 UTC 2020]; root of context hierarchy
2020-03-06 15:30:52.711|INFO |main|LoggingReporter - 
2020-03-06 15:30:52.711|INFO |main|LoggingReporter - ------------------------------------------------------------------------
2020-03-06 15:30:52.711|INFO |main|LoggingReporter -        .__  __                       
2020-03-06 15:30:52.711|INFO |main|LoggingReporter -   ____ |__|/  |________ __ __  ______
2020-03-06 15:30:52.711|INFO |main|LoggingReporter - _/ ___\|  \   __\_  __ \  |  \/  ___/
2020-03-06 15:30:52.712|INFO |main|LoggingReporter - \  \___|  ||  |  |  | \/  |  /\___ \ 
2020-03-06 15:30:52.712|INFO |main|LoggingReporter -  \___  >__||__|  |__|  |____//____  >
2020-03-06 15:30:52.712|INFO |main|LoggingReporter -      \/                           \/
2020-03-06 15:30:52.712|INFO |main|LoggingReporter - 
2020-03-06 15:30:52.712|INFO |main|LoggingReporter - C I T R U S  T E S T S  2.8.0
2020-03-06 15:30:52.712|INFO |main|LoggingReporter - 
2020-03-06 15:30:52.712|INFO |main|LoggingReporter - ------------------------------------------------------------------------
2020-03-06 15:30:52.712|INFO |main|LoggingReporter - 
2020-03-06 15:30:52.712|INFO |main|LoggingReporter - 
2020-03-06 15:30:52.712|INFO |main|LoggingReporter - BEFORE TEST SUITE: SUCCESS
2020-03-06 15:30:52.713|INFO |main|LoggingReporter - ------------------------------------------------------------------------
2020-03-06 15:30:52.713|INFO |main|LoggingReporter - 
2020-03-06 15:30:52.713|INFO |main|CitrusBackend - Loading XML step definitions classpath*:com/consol/citrus/cucumber/step/runner/core/**/*Steps.xml
Mar 06, 2020 3:30:52 PM org.springframework.context.support.AbstractApplicationContext prepareRefresh
INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@5d52e3ef: startup date [Fri Mar 06 15:30:52 UTC 2020]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@6f7923a5
2020-03-06 15:30:52.729|INFO |main|CitrusBackend - Loading XML step definitions classpath*:org/citrusframework/yaks/http/**/*Steps.xml
Mar 06, 2020 3:30:52 PM org.springframework.context.support.AbstractApplicationContext prepareRefresh
INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@2c0f7678: startup date [Fri Mar 06 15:30:52 UTC 2020]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@6f7923a5
2020-03-06 15:30:52.732|INFO |main|CitrusBackend - Loading XML step definitions classpath*:org/citrusframework/yaks/swagger/**/*Steps.xml
Mar 06, 2020 3:30:52 PM org.springframework.context.support.AbstractApplicationContext prepareRefresh
INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@23c650a3: startup date [Fri Mar 06 15:30:52 UTC 2020]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@6f7923a5
2020-03-06 15:30:52.734|INFO |main|CitrusBackend - Loading XML step definitions classpath*:org/citrusframework/yaks/camel/**/*Steps.xml
Mar 06, 2020 3:30:52 PM org.springframework.context.support.AbstractApplicationContext prepareRefresh
INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@50b1f030: startup date [Fri Mar 06 15:30:52 UTC 2020]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@6f7923a5
2020-03-06 15:30:52.736|INFO |main|CitrusBackend - Loading XML step definitions classpath*:org/citrusframework/yaks/jdbc/**/*Steps.xml
Mar 06, 2020 3:30:52 PM org.springframework.context.support.AbstractApplicationContext prepareRefresh
INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@3e681bc: startup date [Fri Mar 06 15:30:52 UTC 2020]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@6f7923a5
2020-03-06 15:30:52.739|INFO |main|CitrusBackend - Loading XML step definitions classpath*:org/citrusframework/yaks/standard/**/*Steps.xml
Mar 06, 2020 3:30:52 PM org.springframework.context.support.AbstractApplicationContext prepareRefresh
INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@5f574cc2: startup date [Fri Mar 06 15:30:52 UTC 2020]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@6f7923a5
2020-03-06 15:30:52.930|INFO |main|EchoAction - YAKS does Cloud-Native BDD testing
.2020-03-06 15:30:52.931|INFO |main|EchoAction - YAKS rocks!
.2020-03-06 15:30:53.077|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.077|INFO |main|LoggingReporter - TEST SUCCESS org/citrusframework/yaks/helloworld.feature:3 (com.consol.citrus.dsl.runner)
2020-03-06 15:30:53.077|INFO |main|LoggingReporter - ------------------------------------------------------------------------
2020-03-06 15:30:53.078|INFO |main|LoggingReporter - 

2020-03-06 15:30:53.082|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.082|INFO |main|LoggingReporter - ------------------------------------------------------------------------
2020-03-06 15:30:53.083|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.083|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.083|INFO |main|LoggingReporter - AFTER TEST SUITE: SUCCESS
2020-03-06 15:30:53.083|INFO |main|LoggingReporter - ------------------------------------------------------------------------
2020-03-06 15:30:53.083|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.083|INFO |main|LoggingReporter - ------------------------------------------------------------------------
2020-03-06 15:30:53.083|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.084|INFO |main|LoggingReporter - CITRUS TEST RESULTS
2020-03-06 15:30:53.084|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.087|INFO |main|LoggingReporter -  org/citrusframework/yaks/helloworld.feature:3 .................. SUCCESS
2020-03-06 15:30:53.087|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.087|INFO |main|LoggingReporter - TOTAL:	1
2020-03-06 15:30:53.088|INFO |main|LoggingReporter - FAILED:	0 (0.0%)
2020-03-06 15:30:53.088|INFO |main|LoggingReporter - SUCCESS:	1 (100.0%)
2020-03-06 15:30:53.088|INFO |main|LoggingReporter - 
2020-03-06 15:30:53.088|INFO |main|LoggingReporter - ------------------------------------------------------------------------
2020-03-06 15:30:53.122|INFO |main|AbstractOutputFileReporter - Generated test report: target/citrus-reports/citrus-test-results.html

1 Scenarios (1 passed)
2 Steps (2 passed)
0m1.711s

[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.274 s - in org.citrusframework.yaks.YaksTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 6.453 s
[INFO] Finished at: 2020-03-06T15:30:53Z
[INFO] ------------------------------------------------------------------------
Test result: Passed
```

The log ends with the result of the tests you've executed.

To check the status of all tests in the namespace, you can run:

```
oc get test
```

This is an example of output you should get:

```
[user@localhost yaks]$ kubectl get test
NAME    PHASE
hello   Passed
```

You can now change the test to use more complex steps and run it again with `./yaks test hello.feature`.

## Steps

Each line in a BDD feature file is backed by a step implementation that covers the actual runtime logic executed. YAKS
provides a set of out-of-the-box step implementations that you can just use in your feature file.

### Citrus steps

The Citrus framework provides a lot of features and predefined steps that can be used to write feature files.

More details can be found in the official [Citrus documentation on BDD testing](https://citrusframework.org/citrus/reference/2.8.0/html/index.html#cucumber).

YAKS provide by default the [Citrus Cucumber HTTP steps](https://citrusframework.org/citrus/reference/2.8.0/html/index.html#http-steps). 
The http binding allows to test some REST API, writing feature files like:

```
Feature: Integration Works

  Background:
    Given URL: https://swapi.co/api/films

  Scenario: Get a result from API
    When send GET /
    Then receive HTTP 200 OK

```

### Camel K steps

If the subject under test is a Camel K integration, you can leverage the YAKS Camel K bindings
that provide useful steps for checking the status of integrations.

For example:

```
   ...
   Given integration xxx is running
   Then integration xxx should print Hello world!
```

The Camel K extension library is provided by default in YAKS. 

### JDBC steps

YAKS provides a library that allows to execute SQL actions on relational DBs (limited to PostgreSQL for this POC).

You can find examples of JDBC steps in the [examples](/examples/jdbc/jdbc.feature) file.

There's also an example that uses [JDBC and REST together](/examples/openapi/task-api.feature) and targets the 
[Syndesis TODO App](https://github.com/syndesisio/todo-example) database.

### Http steps

The Http protocol is a widely used communication protocol when it comes to exchanging data between systems. REST Http services 
are very prominent and producing/consuming those services is a common task in software development these days. YAKS provides
ready to use steps that are able to exchange request/response messages via Http during the test.

As a client you can specify the server URL and send requests to it.

```gherkin
Feature: Http client

  Background:
    Given URL: http://localhost:8080

  Scenario: Health check
    Given path /health is healthy

  Scenario: GET request
    When send GET /todo
    Then verify HTTP response body: {"id": "@ignore@", "task": "Sample task", "completed": 0}
    And receive HTTP 200 OK
```

The example above sets a base request URL to `http://localhost:8080` and performs a health check on path `/health`. After that we can
send any request to the server and verify the response body and status code.

All these steps are part of the core YAKS framework and you can just use them.

On the server side we can start a new Http server instance on a given port and listen for incoming requests. These request can be verified and
the test can provide a simulated response message with body and header data.

```gherkin
Feature: Http server

  Background:
    Given HTTP server listening on port 8080 

  Scenario: Expect GET request
    When receive GET /todo
    Then HTTP response body:  {"id": 1000, "task": "Sample task", "completed": 0}
    And send HTTP 200 OK

  Scenario: Expect POST request
    Given expect HTTP request body: {"id": "@isNumber()@", "task": "New task", "completed": "@matches(0|1)@"}
    When receive POST /todo
    Then send HTTP 201 CREATED
```

In the HTTP server sample above we create a new server instance listening on port `8080`. Then we expect a `GET` request on path `/todo`. The server responds with
a Http `200 OK` response message and given Json body as payload.

The second scenario expects a POST request with a given body as Json payload. The expected request payload is verified with the powerful Citrus JSON 
message validator being able to compare JSON tree structures in combination with validation matchers such as `isNumber()` or `matches(0|1)`.

Once the request is verified the server responds with a simple Http `201 CREATED`. 

### OpenAPI steps

OpenAPI documents specify RESTful Http services in a standardized, language-agnostic way. The specifications describe 
resources, path items, operations, security schemes and many more components that are part of the REST service. YAKS as a 
framework is able to use these information in order to generate proper request and response data for your test.

You can find examples of how to use OpenAPI specifications in YAKS in the [examples](/examples/openapi).

Given an OpenAPI specification that you can access via Http URL or local file system you can load all available operations 
into the test. Once this is completed you can invoke operations by name and verify the response status codes. YAKS will automatically
generate proper request/response data for you.

```gherkin
Feature: Petstore API V3

  Background:
    Given OpenAPI specification: http://localhost:8080/petstore/v3/openapi.json

  Scenario: getPet
    When invoke operation: getPetById
    Then verify operation result: 200 OK

  Scenario: petNotFound
    Given variable petId is "0"
    When invoke operation: getPetById
    Then verify operation result: 404 NOT_FOUND

  Scenario: addPet
    When invoke operation: addPet
    Then verify operation result: 201 CREATED

  Scenario: updatePet
    When invoke operation: updatePet
    Then verify operation result: 200 OK

  Scenario: deletePet
    When invoke operation: deletePet
    Then verify operation result: 204 NO_CONTENT
```        

The request/response data is generated from the OpenAPI specification rules and holds randomized values. The following sample  shows a generated
request for the `addPet` operation where a new pet is transmitted via Http POST. 

```json
{
  "photoUrls": [
    "XHAGIyFcyh"
  ],
  "name": "mGNTgkfxgg",
  "id": 26866048,
  "category": {
    "name": "konwOUYwMo",
    "id": 18676332
  },
  "tags": [
    {
      "name": "KDnoWCfUBn",
      "id": 31444049
    }
  ],
  "status": "sold"
}
```

The generated request should be valid according to the rules in the OpenAPI specification. You can overwrite the 
randomized values with test variables and inbound/outbound data dictionaries in order to have more human readable test data.

```gherkin
Feature: Petstore API V3

  Background:
    Given OpenAPI specification: http://localhost:8080/petstore/v3/openapi.json
    Given variable petId is "citrus:randomNumber(5)"
    Given inbound dictionary
      | $.name          | @assertThat(anyOf(is(hasso),is(cutie),is(fluffy)))@ |
      | $.category.name | @assertThat(anyOf(is(dog),is(cat),is(fish)))@ |
    Given outbound dictionary
      | $.name          | citrus:randomEnumValue('hasso','cutie','fluffy') |
      | $.category.name | citrus:randomEnumValue('dog', 'cat', 'fish') |
 
  [...]
``` 

With this data dictionaries in place the generated request looks like follows:

```json
{
  "photoUrls": [
    "aaKoEDhLYc"
  ],
  "name": "hasso",
  "id": 12337393,
  "category": {
    "name": "cat",
    "id": 23927231
  },
  "tags": [
    {
      "name": "FQxvuCbcqT",
      "id": 58291150
    }
  ],
  "status": "pending"
}
```

You see that we are now using more human readable values for `$.name` and `$.category.name`.

The same mechanism applies for inbound messages that are verified by YAKS. The framework will generate an expected response 
data structure coming from the OpenAPI specification. Below is a sample Json payload that verifies the response for the `getPetById` operation.

```json
{
  "photoUrls": "@ignore@",
  "name": "@assertThat(anyOf(is(hasso),is(cutie),is(fluffy)))@",
  "id": "@isNumber()@",
  "category": {
    "name": "@assertThat(anyOf(is(dog),is(cat),is(fish)))@",
    "id": "@isNumber()@"
  },
  "tags": "@ignore@",
  "status": "@matches(available|pending|sold)@"
}
```

All mandatory fields need to be in the received json document. Also enumerations and number values are checked to meet the expected
values coming form the OpenAPI specification (e.g. `status=@matches(available|pending|sold)@`). This ensures that the response respects the rules
defined in the specification.

In case you also want to validate the exact values on each field please use the generic Http steps where you can provide a complete expected
Http response with payload and header data. 

### Groovy steps

The Groovy support in YAKS allows to add framework configuration, bean configuration and test actions via script snippets. 
In particular you can easily add customized endpoints that send/receive data over various messaging transports. 

#### Framework configuration scripts

You can add endpoints and beans as Citrus framework configuration like follows:

```gherkin
Scenario: Endpoint script config
  Given URL: http://localhost:18080
  Given create configuration
  """
  citrus {
      endpoints {
          http {
              server('helloServer') {
                  port = 18080
                  autoStart = true
              }
          }
      }
  }
  """
  When send GET /hello
  Then receive HTTP 200 OK
``` 

In the example above the scenario creates a new Citrus endpoint named `helloServer` with given properties (`port`, `autoStart`) in form of a Groovy configuration script. 
The endpoint is a Http server component that is automatically started with the given port. In the following the scenario can send messages to that server endpoint.

The Groovy configuration script adds Citrus components to the test context and supports following elements:

* `endpoints`: Configure Citrus endpoint components that can be used to exchange data over various messaging transports
* `queues`: In memory queues to handle message forwarding for incoming messages
* `beans`: Custom beans configuration (e.g. data source, SSL context, request factory) that can be used in Citrus endpoint components 

Let's quickly have a look at a bean configuration where a new JDBC data source is added to the test suite.

```gherkin
Scenario: Bean configuration
  Given create configuration
  """
  citrus {
      beans {
          dataSource(org.apache.commons.dbcp2.BasicDataSource) {
              driverClassName = "org.h2.Driver"
              url = "jdbc:h2:mem:camel"
              username = "sa"
              password = ""
          }
      }
  }
  """
```

The data source will be added as a bean named `dataSource` and can be referenced in all Citrus SQL test actions.

All Groovy configuration scripts that we have seen so far can also be loaded from external file resources, too.

```gherkin
Scenario: Endpoint script config
  Given load configuration citrus.configuration.groovy
  When endpoint hello sends payload Hello from new direct endpoint!
  Then endpoint hello should receive payload Hello from new direct endpoint!
```      

_citrus.configuration.groovy_
```
citrus {
    queues {
        queue('say-hello')
    }

    endpoints {
        direct {
            asynchronous {
                name = 'hello'
                queue = 'say-hello'
            }
        }
    }
}
```

#### Endpoint configuration scripts

Endpoints describe an essential part in terms of messaging integration during a test. There are multiple ways to add custom endpoints
to a test so you exchange and verify message data. Endpoint Groovy scripts is one comfortable way to add custom endpoint configurations
in a test scenario.

```gherkin
Scenario: Create Http endpoint
  Given URL: http://localhost:18081
  Given create endpoint helloServer.groovy
  """
  http()
    .server()
    .port(18081)
    .autoStart(true)
  """
  When send GET /hello
  Then receive HTTP 200 OK
```     

The scenario creates a new Http server endpoint named `helloServer`. This server component can be used directly in the
scenario to receive and verify messages sent to that endpoint.

You can also load the endpoint configuration from external file resources.

```gherkin
Scenario: Load endpoint
  Given URL: http://localhost:18088
  Given load endpoint fooServer.groovy
  When send GET /hello
  Then receive HTTP 200 OK
``` 

_fooServer.groovy_
```
http()
    .server()
    .port(18088)
    .autoStart(true)
```  

#### Test action scripts

YAKS provides a huge set of predefined test actions that users can add to the Gherkin feature files out of the box.
However there might be situations where you want to run a customized test action code as a step in your feature scenario.

With the Groovy script support in YAKS you can add such customized test actions via script snippets:

```gherkin
Scenario: Custom test actions
  Given create actions basic.groovy
  """
  actions {
    echo('Hello from Groovy script')
    sleep().seconds(1)

    createVariables()
        .variable('foo', 'bar')

    echo('Variable foo=${foo}')
  }
  """
  Then apply basic.groovy
```

Users familiar with Citrus will notice immediately that the action script is using the Citrus actions DSL to describe
what should be done when running the Groovy script as part of the test. The Citrus action DSL is quite powerful and allows to
perform complex actions such as iterations, conditionals and send/receive operations.

```gherkin
Scenario: Messaging actions
  Given create actions messaging.groovy
  """
  actions {
    send('direct:myQueue')
      .payload('Hello from Groovy script!')

    receive('direct:myQueue')
      .payload('Hello from Groovy script!')
  }
  """
  Then apply messaging.groovy
```

### Custom steps

It's often useful to plug some custom steps into the testing environment. Custom steps help keeping the 
tests short and self-explanatory and at the same time help teams to add generic assertions that are meaningful in their 
environment.

To add custom steps in YAKS, you can look at the example provided in the [examples/extension](/examples/extension) directory.
The example consists of a feature file ([examples/extension/extension.feature](/examples/extension/extension.feature)) using a custom step from a local project 
([examples/extension/steps](/examples/extension/steps)).

To run the example:

```
yaks test extension.feature -u steps/
```

The `-u` flag stands for "upload". The steps project is built before running the test and the artifacts are uploaded to a
[Snap](https://github.com/container-tools/snap) Minio server, in order for the test to retrieve them
when needed.
This happens transparently to the user.

The local library can also be uploaded to the Snap Minio server prior to running the test, using the `yaks upload` command.  

## Runtime configuration

There are several runtime options that you can set in order to configure which tests to run for instance. Each test directory
can have its own `yaks-config.yaml` configuration file that holds the runtime options for this specific test suite.

```yaml
config:
  runtime:
    cucumber:
      tags:
      - "not @ignored"
      glue:
      - "org.citrusframework.yaks"
      - "com.company.steps.custom"
```

The sample above uses different runtime options for Cucumber to specify a tag filter and some custom glue packages that 
should be loaded. The given runtime options will be set as environment variables in the YAKS runtime pod.

You can also specify the Cucumber options that get passed to the Cucumber runtime.

```yaml
config:
  runtime:
    cucumber:
      options: "--strict --monochrome --glue org.citrusframework.yaks"
```

Also we can make use of command line options when using the `yaks` binary.

```bash
$ yaks test hello-world.feature --tag @regression --glue org.citrusframework.yaks
```

### Add custom runtime dependencies

The YAKS testing framework provides a base runtime image that holds all required libraries and artifacts to execute tests. You may need to add
additional runtime dependencies though in order to extend the framework capabilities.

For instance when using a Camel route in your test you may need to add additional Camel components that are not part in the
basic YAKS runtime (e.g. camel-groovy). You can add the runtime dependency to the YAKS runtime image in multiple ways:

#### Load dependencies via Cucumber tags

You can simply add a tag to your BDD feature specification in order to declare a runtime dependency for your test.

```gherkin
@require('org.apache.camel:camel-groovy:@camel.version@')
Feature: Camel route testing

  Background:
    Given Camel route hello.xml
    """
    <route>
      <from uri="direct:hello"/>
      <filter>
        <groovy>request.body.startsWith('Hello')</groovy>
        <to uri="log:org.citrusframework.yaks.camel?level=INFO"/>
      </filter>
      <split>
        <tokenize token=" "/>
        <to uri="seda:tokens"/>
      </split>
    </route>
    """

  Scenario: Hello route
    When send to route direct:hello body: Hello Camel!
    And receive from route seda:tokens body: Hello
    And receive from route seda:tokens body: Camel!
```

The given Camel route uses the groovy language support and this is not part in the basic YAKS runtime image. So we add
the tag `@require('org.apache.camel:camel-groovy:@camel.version@')`. This tag will load the Maven dependency at runtime 
before the test is executed in the YAKS runtime image.

Note that you have to provide proper Maven artifact coordinates with proper `groupId`, `artifactId` and `version`. You can make 
use of version properties for these versions available in the YAKS base image:

* citrus.version
* camel.version
* spring.version
* cucumber.version

#### Load dependencies via System property or environment setting

You can add dependencies also by specifying the dependencies as command line parameter when running the test via `yaks` CLI.

```bash
$ yaks test --dependency org.apache.camel:camel-groovy:@camel.version@ camel-route.feature
```

This will add a environment setting in the YAKS runtime container and the dependency will be loaded automatically
at runtime.

#### Load dependencies via property file

YAKS supports adding runtime dependency information to a property file called `yaks.properties`. The dependency is added through
Maven coordinates in the property file using a common property key prefix `yaks.dependency.`

```properties
# include these dependencies
yaks.dependency.foo=org.foo:foo-artifact:1.0.0
yaks.dependency.bar=org.bar:bar-artifact:1.5.0
```

You can add the property file when running the test via `yaks` CLI like follows:

```bash
$ yaks test --settings yaks.properties camel-route.feature
```

#### Load dependencies via configuration file

When more dependencies are required to run a test you may consider to add a configuration file as `.yaml` or `.json`.

The configuration file is able to declare multiple dependencies:

```yaml
dependencies:
  - groupId: org.foo
    artifactId: foo-artifact
    version: 1.0.0
  - groupId: org.bar
    artifactId: bar-artifact
    version: 1.5.0
```

```json
{
  "dependencies": [
    {
      "groupId": "org.foo",
      "artifactId": "foo-artifact",
      "version": "1.0.0"
    },
    {
      "groupId": "org.bar",
      "artifactId": "bar-artifact",
      "version": "1.5.0"
    }
  ]
}
```

You can add the configuration file when running the test via `yaks` CLI like follows:

```bash
$ yaks test --settings yaks.settings.yaml camel-route.feature
```

### Add custom Maven repositories

When adding custom runtime dependencies those artifacts might not be available on the public central Maven repository.
Instead you may need to add a custom repository that holds your artifacts.

You can do this with several configuration options:

#### Add Maven repository via System property or environment setting

You can add repositories also by specifying the repositories as command line parameter when running the test via `yaks` CLI.

```bash
$ yaks test --maven-repository jboss-ea=https://repository.jboss.org/nexus/content/groups/ea/ my.feature
```

This will add a environment setting in the YAKS runtime container and the repository will be added to the Maven runtime project model.

#### Add Maven repository via property file

YAKS supports adding Maven repository information to a property file called `yaks.properties`. The dependency is added through
Maven repository id and url in the property file using a common property key prefix `yaks.repository.`

```properties
# Maven repositories
yaks.repository.central=https://repo.maven.apache.org/maven2/
yaks.repository.jboss-ea=https://repository.jboss.org/nexus/content/groups/ea/
```

You can add the property file when running the test via `yaks` CLI like follows:

```bash
$ yaks test --settings yaks.properties my.feature
```

#### Add Maven repository via configuration file

More complex repository configuration might require to add a configuration file as `.yaml` or `.json`.

The configuration file is able to declare multiple repositories:

```yaml
repositories:
  - id: "central"
    name: "Maven Central"
    url: "https://repo.maven.apache.org/maven2/"
    releases:
      enabled: "true"
      updatePolicy: "daily"
    snapshots:
      enabled: "false"
  - id: "jboss-ea"
    name: "JBoss Community Early Access Release Repository"
    url: "https://repository.jboss.org/nexus/content/groups/ea/"
    layout: "default"
```

```json
{
  "repositories": [
      {
        "id": "central",
        "name": "Maven Central",
        "url": "https://repo.maven.apache.org/maven2/",
        "releases": {
          "enabled": "true",
          "updatePolicy": "daily"
        },
        "snapshots": {
          "enabled": "false"
        }
      },
      {
        "id": "jboss-ea",
        "name": "JBoss Community Early Access Release Repository",
        "url": "https://repository.jboss.org/nexus/content/groups/ea/",
        "layout": "default"
      }
    ]
}
```

You can add the configuration file when running the test via `yaks` CLI like follows:

```bash
$ yaks test --settings yaks.settings.yaml my.feature
```

### Using secrets

Tests usually need to use credentials and connection URLs in order to connect to infrastructure components and services. 
This might be sensitive data that should not go into the test configuration directly as hardcoded value. You should rather load the
credentials from a secret volume source.

To use the implicit configuration via secrets, we first need to create a configuration file holding the properties of a named configuration.

*mysecret.properties*
```properties
# Only configuration related to the "mysecret" named config
database.url=jdbc:postgresql://syndesis-db:5432/sampledb
database.user=admin
database.password=special
```

We can create a secret from that file and label it so that it will be picked up automatically by the YAKS operator:

```bash
# Create the secret from the property file
kubectl create secret generic my-secret --from-file=mysecret.properties
```

Once the secret is created you can bind it to tests by their name. Given the test `my-test.feature` you can bind the secret to the test
by adding a label as follows:

```bash
# Bind secret to the "my-test" test case
kubectl label secret my-secret yaks.citrusframework.org/test=my-test
``` 

For multiple secrets and variants of secrets on different environments (e.g. dev, test, staging) you can add a secret id and label that one
explicitly in addition to the test name. 

```bash
# Bind secret to the named configuration "staging" of the "my-test" test case
kubectl label secret my-secret yaks.citrusframework.org/test=my-test yaks.citrusframework.org/test.configuration=staging
```

With that in place you just need to set the secret id in your `yaks-config.yaml` for that test.

*yaks-config.yaml*
```yaml
config:
  runtime:
    secret: staging
```

You can now write a test and use the secret properties as normal test variables: 

*my-test.feature*
```gherkin
Feature: JDBC API

  Background:
    Given Database connection
      | url       | ${database.url} |
      | username  | ${database.user} |
      | password  | ${database.password} |
```

## Pre/Post scripts

You can run scripts before/after a test group. Just add your commands to the `yaks-config.yaml` configuration for the test group.

```yaml
config:
  namespace:
    temporary: false
    autoRemove: true
pre:
  - script: prepare.sh
  - run: echo Start!
  - name: Optional name
    timeout: 30m
    run: |
      echo "Multiline"
      echo "Commands are also"
      echo "Supported!"
post:
  - script: finish.sh
  - run: echo Bye!
```

The section `pre` runs before a test group and `post` is added after the test group has finished. The post steps are run even if the tests or pre steps fail
for some reason. This ensures that cleanup tasks are performed also in case of errors. 

The `script` option provides a file path to bash script to execute. The user has to make sure that the script is executable. If no absolute file path is 
given it is assumed to be a file path relative to the current test group directory.

With `run` you can add any shell command. At the moment only single line commands are supported here. You can add multiple `run` commands in a `pre`
or `post` section.

Each step can also define a human readable `name` that will be printed before its execution.

By default a step must complete within 30 minutes (`30m`). The timeout can be changed using the `timeout` option in the step declaration (in Golang duration format).

Scripts can leverage the following environment variables that are set automatically by the Yaks runtime:

- **YAKS_NAMESPACE**: always contains the namespace where the tests will be executed, no matter if the namespace is fixed or temporary

## Reporting options

After running some YAKS tests you may want to review the test results and generate a summary report. As we are using CRDs on the Kubernetes or OpenShift platform we
can review the status of the custom resources after the test run in order to get some test results.

```bash
$ oc get tests

NAME         PHASE     TOTAL     PASSED    FAILED    SKIPPED
helloworld   Passed    2         2         0         0
foo-test     Passed    1         1         0         0
bar-test     Passed    1         1         0         0
```

You can also view error details when adding the `wide` option

```bash
$ oc get tests -o wide

NAME         PHASE     TOTAL     PASSED    FAILED    SKIPPED    ERRORS
helloworld   Passed    2         1         1         0          [ "helloworld.feature:10 Failed caused by ValidationException - Expected 'foo' but was 'bar'" ]
foo-test     Passed    1         1         0         0
bar-test     Passed    1         1         0         0
```

The YAKS CLI is able to fetch those results in order to generate a summary report locally:

```bash
$ yaks report --fetch

Test results: Total: 4, Passed: 4, Failed: 0, Skipped: 0
	classpath:org/citrusframework/yaks/helloworld.feature:3: Passed
	classpath:org/citrusframework/yaks/helloworld.feature:7: Passed
	classpath:org/citrusframework/yaks/foo-test.feature:3: Passed
	classpath:org/citrusframework/yaks/bar-test.feature:3: Passed
```

The report supports different output formats (summary, json, junit). For JUnit style reports use the `junit` output.

```bash
$ yaks report --fetch --output junit

<?xml version="1.0" encoding="UTF-8"?><testsuite name="org.citrusframework.yaks.JUnitReport" errors="0" failures="0" skipped="0" tests="4" time="0">
  <testcase name="helloworld.feature:3" classname="classpath:org/citrusframework/yaks/helloworld.feature:3" time="0"></testcase>
  <testcase name="helloworld.feature:7" classname="classpath:org/citrusframework/yaks/helloworld.feature:7" time="0"></testcase>
  <testcase name="foo-test.feature:3" classname="classpath:org/citrusframework/yaks/foo-test.feature:3" time="0"></testcase>
  <testcase name="bar-test.feature:3" classname="classpath:org/citrusframework/yaks/bar-test.feature:3" time="0"></testcase>
</testsuite>
```

The JUnit report is also saved to the local disk in the file `_output/junit-reports.xml`.

The `_output` directory is also used to store individual test results for each test executed via the YAKS CLI. 
So after a test run you can also review the results in that `_output` directory. The YAKS report command can also view those results in `_output` directory 
in any given output format. Simply leave out the `--fetch` option when generating the report and YAKS will use the test results stored in the 
local `_output` folder.

```bash
$ yaks report
Test results: Total: 5, Passed: 5, Failed: 0, Skipped: 0
	classpath:org/citrusframework/yaks/helloworld.feature:3: Passed
	classpath:org/citrusframework/yaks/helloworld.feature:7: Passed
	classpath:org/citrusframework/yaks/test1.feature:3: Passed
	classpath:org/citrusframework/yaks/test2.feature:3: Passed
	classpath:org/citrusframework/yaks/test3.feature:3: Passed
```

## For YAKS developers

Requirements:
- Go 1.12+
- Operator SDK 0.9.0
- Maven 3.5.0+
- Git client
- Mercurial client (ng)

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
