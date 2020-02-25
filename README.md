# Yaks

**Proof of Concept**

YAKS (Yet Another Kamel Subproject)

## Getting Started

Yaks allows you to do BDD testing on Kubernetes using the [Gherkin syntax from Cucumber](https://cucumber.io/docs/gherkin/).

### Installation

The easiest way to getting started with Yaks is using the **Yaks CLI**.
You can download the CLI from the [0.0.2 release page](https://github.com/jboss-fuse/yaks/releases/tag/0.0.2).

To install yaks, just make it runnable and move it to a location in your `$path`, e.g. on linux:

```
chmod a+x yaks-0.0.2-linux-64bit
mv yaks-0.0.2-linux-64bit /usr/local/bin/yaks
```

Yaks tests can be executed on any Kubernetes or OpenShift environment.

You need to connect to the cluster and switch to the namespace where you want Yaks to be installed.
You can also create a new namespace:

```
oc new-project my-yaks-project
``` 

To install Yaks into your namespace, just run:

```
# If it's the first time you install it on a cluster, make sure you're cluster admin.

yaks install
```

This will install the Yaks operator in the selected namespace. If not already installed, the command will also install
the Yaks custom resource definitions in the cluster (in this case, the user needs cluster-admin permissions).

### Running the Hello World!

_examples/helloworld.feature_
```
Feature: hello world

  Scenario: print slogan
    Given Yaks does BDD testing on Kubernetes
    Then Yaks is cool!

```

The `helloworld.feature` file is present in the `examples` directory of this repository: you can clone the repo or just download it to your host. 

Once you have your first test in the `helloworld.feature` file, you can **run it** using: 

```
yaks test helloworld.feature
```

This is an example of output you should get:

```
[user@localhost yaks]$ ./yaks test hello.feature 
test "hello" created
+ test-hello-bldrmuj9nakdqqrj7eag › test
test-hello-bldrmuj9nakdqqrj7eag test Starting the Java application using /opt/run-java/run-java.sh ...
test-hello-bldrmuj9nakdqqrj7eag test exec java -javaagent:/opt/jolokia/jolokia.jar=config=/opt/jolokia/etc/jolokia.properties -javaagent:/opt/prometheus/jmx_prometheus_javaagent.jar=9779:/opt/prometheus/prometheus-config.yml -XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -XX:+ExitOnOutOfMemoryError -cp .:/deployments/dependencies/*:/deployments/* dev.yaks.testing.TestRunner
test-hello-bldrmuj9nakdqqrj7eag test OpenJDK 64-Bit Server VM warning: If the number of processors is expected to increase from one, then you should configure the number of parallel GC threads appropriately using -XX:ParallelGCThreads=N
test-hello-bldrmuj9nakdqqrj7eag test I> No access restrictor found, access to any MBean is allowed
test-hello-bldrmuj9nakdqqrj7eag test Jolokia: Agent started with URL https://172.17.0.10:8778/jolokia/
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:01.529|DEBUG|main|Citrus - Loading Citrus application properties
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:01.534|DEBUG|main|Citrus - Setting application property citrus.default.message.type=JSON
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:01.535|INFO |main|CitrusObjectFactory - Initializing injection mode 'RUNNER' for Citrus 2.8.0
test-hello-bldrmuj9nakdqqrj7eag test Aug 20, 2019 9:21:01 AM org.springframework.context.annotation.AnnotationConfigApplicationContext prepareRefresh
test-hello-bldrmuj9nakdqqrj7eag test INFO: Refreshing org.springframework.context.annotation.AnnotationConfigApplicationContext@674658f7: startup date [Tue Aug 20 09:21:01 UTC 2019]; root of context hierarchy
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter -        .__  __                       
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter -   ____ |__|/  |________ __ __  ______
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - _/ ___\|  \   __\_  __ \  |  \/  ___/
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - \  \___|  ||  |  |  | \/  |  /\___ \ 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter -  \___  >__||__|  |__|  |____//____  >
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter -      \/                           \/
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - C I T R U S  T E S T S  2.8.0
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|DEBUG|main|LoggingReporter - BEFORE TEST SUITE
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.566|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.567|INFO |main|LoggingReporter - BEFORE TEST SUITE: SUCCESS
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.567|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.567|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.567|INFO |main|CitrusBackend - Loading XML step definitions classpath*:com/consol/citrus/cucumber/step/runner/core/**/*Steps.xml
test-hello-bldrmuj9nakdqqrj7eag test Aug 20, 2019 9:21:02 AM org.springframework.context.support.ClassPathXmlApplicationContext prepareRefresh
test-hello-bldrmuj9nakdqqrj7eag test INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@4a7761b1: startup date [Tue Aug 20 09:21:02 UTC 2019]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@674658f7
test-hello-bldrmuj9nakdqqrj7eag test Aug 20, 2019 9:21:02 AM org.springframework.context.support.ClassPathXmlApplicationContext prepareRefresh
test-hello-bldrmuj9nakdqqrj7eag test INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@6a10b263: startup date [Tue Aug 20 09:21:02 UTC 2019]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@674658f7
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.586|INFO |main|CitrusBackend - Loading XML step definitions classpath*:dev/yaks/testing/http/**/*Steps.xml
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.588|INFO |main|CitrusBackend - Loading XML step definitions classpath*:dev/yaks/testing/camel/k/**/*Steps.xml
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.594|INFO |main|CitrusBackend - Loading XML step definitions classpath*:dev/yaks/testing/jdbc/**/*Steps.xml
test-hello-bldrmuj9nakdqqrj7eag test Aug 20, 2019 9:21:02 AM org.springframework.context.support.ClassPathXmlApplicationContext prepareRefresh
test-hello-bldrmuj9nakdqqrj7eag test INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@1d12b024: startup date [Tue Aug 20 09:21:02 UTC 2019]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@674658f7
test-hello-bldrmuj9nakdqqrj7eag test Aug 20, 2019 9:21:02 AM org.springframework.context.support.ClassPathXmlApplicationContext prepareRefresh
test-hello-bldrmuj9nakdqqrj7eag test INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@2c16fadb: startup date [Tue Aug 20 09:21:02 UTC 2019]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@674658f7
test-hello-bldrmuj9nakdqqrj7eag test Aug 20, 2019 9:21:02 AM org.springframework.context.support.ClassPathXmlApplicationContext prepareRefresh
test-hello-bldrmuj9nakdqqrj7eag test INFO: Refreshing org.springframework.context.support.ClassPathXmlApplicationContext@1e9804b9: startup date [Tue Aug 20 09:21:02 UTC 2019]; parent: org.springframework.context.annotation.AnnotationConfigApplicationContext@674658f7
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.597|INFO |main|CitrusBackend - Loading XML step definitions classpath*:dev/yaks/testing/standard/**/*Steps.xml
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.600|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.619|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.623|DEBUG|main|CitrusDslAnnotations - Injecting test runner instance on test class field 'runner'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.623|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.624|DEBUG|main|CitrusAnnotations - Injecting Citrus framework instance on test class field 'citrus'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.625|DEBUG|main|CitrusDslAnnotations - Injecting test runner instance on test class field 'runner'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.674|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.674|DEBUG|main|CitrusAnnotations - Injecting Citrus framework instance on test class field 'citrus'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.674|DEBUG|main|CitrusDslAnnotations - Injecting test runner instance on test class field 'runner'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.677|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.677|DEBUG|main|CitrusAnnotations - Injecting Citrus framework instance on test class field 'citrus'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.677|DEBUG|main|CitrusDslAnnotations - Injecting test runner instance on test class field 'runner'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.677|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.677|DEBUG|main|CitrusAnnotations - Injecting Citrus framework instance on test class field 'citrus'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.678|DEBUG|main|CitrusDslAnnotations - Injecting test runner instance on test class field 'runner'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.678|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.678|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.678|DEBUG|main|LoggingReporter - STARTING TEST /etc/yaks/test/..2019_08_20_09_20_58.256876568/hello.feature:3 <com.consol.citrus.dsl.runner>
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.678|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.678|DEBUG|main|TestCase - Initializing test case
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.679|DEBUG|main|TestContext - Setting variable: citrus.test.name with value: '/etc/yaks/test/..2019_08_20_09_20_58.256876568/hello.feature:3'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.679|DEBUG|main|TestContext - Setting variable: citrus.test.package with value: 'com.consol.citrus.dsl.runner'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.679|DEBUG|main|TestCase - Test variables:
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.679|DEBUG|main|TestCase - citrus.test.package = com.consol.citrus.dsl.runner
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.679|DEBUG|main|TestCase - citrus.test.name = /etc/yaks/test/..2019_08_20_09_20_58.256876568/hello.feature:3
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.679|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test .2019-08-20 09:21:02.681|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test .2019-08-20 09:21:02.682|DEBUG|main|TestContextFactory - Created new test context - using global variables: '{}'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.682|DEBUG|main|CitrusAnnotations - Injecting Citrus framework instance on test class field 'citrus'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.682|DEBUG|main|CitrusDslAnnotations - Injecting test runner instance on test class field 'runner'
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.812|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.813|INFO |main|LoggingReporter - TEST SUCCESS /etc/yaks/test/..2019_08_20_09_20_58.256876568/hello.feature:3 (com.consol.citrus.dsl.runner)
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.813|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.813|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.820|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|DEBUG|main|LoggingReporter - AFTER TEST SUITE
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|INFO |main|LoggingReporter - AFTER TEST SUITE: SUCCESS
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.821|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.822|INFO |main|LoggingReporter - CITRUS TEST RESULTS
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.822|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.826|INFO |main|LoggingReporter -  /etc/yaks/test/..2019_08_20_09_20_58.256876568/hello.feature:3 . SUCCESS
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.827|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.827|INFO |main|LoggingReporter - TOTAL:        1
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.827|DEBUG|main|LoggingReporter - SKIPPED:      0 (0.0%)
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.827|INFO |main|LoggingReporter - FAILED:       0 (0.0%)
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.827|INFO |main|LoggingReporter - SUCCESS:      1 (100.0%)
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.827|INFO |main|LoggingReporter - 
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.827|INFO |main|LoggingReporter - ------------------------------------------------------------------------
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.827|DEBUG|main|HtmlReporter - Generating HTML test report
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.830|DEBUG|main|FileUtils - Reading file resource: 'test-detail.html' (encoding is 'US-ASCII')
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.847|DEBUG|main|FileUtils - Reading file resource: 'test-report.html' (encoding is 'US-ASCII')
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.850|INFO |main|AbstractOutputFileReporter - Generated test report: target/citrus-reports/citrus-test-results.html
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.852|DEBUG|main|JUnitReporter - Generating JUnit test report
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.866|DEBUG|main|FileUtils - Reading file resource: 'junit-test.xml' (encoding is 'US-ASCII')
test-hello-bldrmuj9nakdqqrj7eag test 2019-08-20 09:21:02.873|DEBUG|main|FileUtils - Reading file resource: 'junit-report.xml' (encoding is 'US-ASCII')
test-hello-bldrmuj9nakdqqrj7eag test 1 Scenarios (1 passed)
test-hello-bldrmuj9nakdqqrj7eag test 2 Steps (2 passed)
test-hello-bldrmuj9nakdqqrj7eag test 0m1.162s
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

### Using Citrus features

The Citrus framework provides a lot of features and predefined steps that can be used to write feature files.

More details can be found in the official [Citrus documentation on BDD testing](https://citrusframework.org/citrus/reference/2.8.0/html/index.html#cucumber).

Yaks provide by default the [Citrus Cucumber HTTP steps](https://citrusframework.org/citrus/reference/2.8.0/html/index.html#http-steps). 
The http binding allows to test REST API, writing feature files like:

```
Feature: Integration Works

  Background:
    Given URL: https://swapi.co/api/films

  Scenario: Get a result from API
    When send GET /
    Then receive HTTP 200 OK

```

### Using Camel K steps

If the subject under test is a Camel K integration, you can leverage the Yaks Camel K bindings
that provide useful steps for checking the status of integrations.

For example:

```
   ...
   Given integration xxx is running
   Then integration xxx should print Hello world!
```

The Camel K extension library is provided by default in Yaks. 

### Using JDBC steps

Yaks provides a library that allows to execute SQL actions on relational DBs (limited to PostgreSQL for this POC).

You can find examples of JDBC steps in the [examples](/examples/jdbc.feature) file.

There's also an example that uses [JDBC and REST together](/examples/task-api.feature) and targets the 
[Syndesis TODO App](https://github.com/syndesisio/todo-example) database.

### Using custom steps

It's often useful to plug some custom steps into the testing environment. Custom steps help keeping the 
tests short and self-explanatory and at the same time help teams to add generic assertions that are meaningful in their 
environment.

To add custom steps in Yaks, you can fork + clone the [yaks extension](https://github.com/nicolaferraro/yaks-extension) repository, that provides
an example of how to do that.

You can add your own steps to that project and follow the instructions in order to install them in the Yaks environment.

### Adding custom runtime dependencies

The Yaks testing framework provides a base runtime image that holds all required libraries and artifacts to execute tests. You may need to add
additional runtime dependencies though in order to extend the framework capabilities.

For instance when using a Camel route in your test you may need to add additional Camel components that are not part in the
basic Yaks runtime (e.g. camel-groovy). You can add the runtime dependency to the Yaks runtime image in multiple ways:

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
        <to uri="log:dev.yaks.testing.camel?level=INFO"/>
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

The given Camel route uses the groovy language support and this is not part in the basic Yaks runtime image. So we add
the tag `@require('org.apache.camel:camel-groovy:@camel.version@')`. This tag will load the Maven dependency at runtime 
before the test is executed in the Yaks runtime image.

Note that you have to provide proper Maven artifact coordinates with proper `groupId`, `artifactId` and `version`. You can make 
use of version properties for these versions available in the Yaks base image:

* citrus.version
* camel.version
* spring.version
* cucumber.version

#### Load dependencies via System property or environment setting

You can add dependencies also by specifying the dependencies as command line parameter when running the test via `yaks` CLI.

```bash
yaks test --dependencies org.apache.camel:camel-groovy:@camel.version@ camel-route.feature
```

This will add a environment setting in the Yaks runtime container and the dependency will be loaded automatically
at runtime.

#### Load dependencies via property file

Yaks supports adding runtime dependency information to a property file called `yaks.properties`. The dependency is added through
Maven coordinates in the property file using a common property key prefix `yaks.dependency.`

```properties
# include these dependencies
yaks.dependency.foo=org.foo:foo-artifact:1.0.0
yaks.dependency.bar=org.bar:bar-artifact:1.5.0
```

You can add the property file when running the test via `yaks` CLI like follows:

```bash
yaks test --settings yaks.properties camel-route.feature
```

#### Load dependencies via configuration file

When more dependencies are required to run a test you may consider to add a configuration file as `.yaml` or `.json`.

The configuration file is able to declare multiple dependencies:

```yaml
dependencies:
  - dependency:
      groupId: org.foo
      artifactId: foo-artifact
      version: 1.0.0
  - dependency:
      groupId: org.bar
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
yaks test --settings yaks.dependency.yaml camel-route.feature
```

## For Yaks Developers

Requirements:
- Go 1.12+
- Operator SDK 0.9.0
- Maven 3.5.0+
- Git client
- Mercurial client (ng)

You can build the YAKS project and get the `yaks` CLI by running:

```
make
```

If you want to build the operator image for Minishift, then:

```
# Build binaries and images
eval $(minishift docker-env)
make images-no-test
```

If the operator pod is running, just delete it to let it grab the new image.
