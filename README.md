# YAKS

Yet Another Kubernetes Service

## Getting Started

YAKS allows you to perform Could-Native BDD testing. Cloud-Native here means that your tests execute within a POD in a 
Kubernetes cluster. All you need to do is to write some BDD feature specs using the [Gherkin syntax from Cucumber](https://cucumber.io/docs/gherkin/).

### Installation

The easiest way to getting started with YAKS is using the **YAKS CLI**.
You can download the CLI from the [release page](https://github.com/jboss-fuse/yaks/releases/).

To install the `yaks` binary, just make it runnable and move it to a location in your `$path`, e.g. on linux:

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

### Using Citrus features

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

### Using Camel K steps

If the subject under test is a Camel K integration, you can leverage the YAKS Camel K bindings
that provide useful steps for checking the status of integrations.

For example:

```
   ...
   Given integration xxx is running
   Then integration xxx should print Hello world!
```

The Camel K extension library is provided by default in YAKS. 

### Using JDBC steps

YAKS provides a library that allows to execute SQL actions on relational DBs (limited to PostgreSQL for this POC).

You can find examples of JDBC steps in the [examples](/examples/jdbc.feature) file.

There's also an example that uses [JDBC and REST together](/examples/task-api.feature) and targets the 
[Syndesis TODO App](https://github.com/syndesisio/todo-example) database.

### Using custom steps

It's often useful to plug some custom steps into the testing environment. Custom steps help keeping the 
tests short and self-explanatory and at the same time help teams to add generic assertions that are meaningful in their 
environment.

To add custom steps in YAKS, you can fork + clone the [yaks extension](https://github.com/nicolaferraro/yaks-extension) repository, that provides
an example of how to do that.

You can add your own steps to that project and follow the instructions in order to install them in the YAKS environment.

### Adding custom runtime dependencies

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
yaks test --dependencies org.apache.camel:camel-groovy:@camel.version@ camel-route.feature
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

## For YAKS Developers

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
