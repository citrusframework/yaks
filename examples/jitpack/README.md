# Jitpack extensions

This example shows the usage of YAKS extensions with Jitpack. 

Jitpack allows you to load custom steps from an external GitHub repository in order to use
custom step definitions in your feature file.

_jitpack.feature_
```gherkin
Scenario: Use custom steps
    Given My steps are loaded
    Then I can do whatever I want!
```

The steps `My steps are loaded` and `I can do whatever I want!` live in a separate repository on 
GitHub ([https://github.com/citrusframework/yaks-step-extension](https://github.com/citrusframework/yaks-step-extension)).

We need to add the Jitpack Maven repository, the dependency and the additional glue code in the `yaks-config.yaml`.

_yaks-config.yaml_
```yaml
config:
  runtime:
    cucumber:
      glue:
      - "org.citrusframework.yaks"
      - "dev.yaks.testing.standard"
    settings:
      repositories:
        - id: "central"
          name: "Maven Central"
          url: "https://repo.maven.apache.org/maven2/"
        - id: "jitpack.io"
          name: "JitPack Repository"
          url: "https://jitpack.io"
      dependencies:
        - groupId: com.github.citrusframework
          artifactId: yaks-step-extension
          version: "0.0.1"
```     

The additional glue code `dev.yaks.testing.standard` should match the package name where to find the custom step definitions in the library. The Jitpack
Maven repository makes sure the library gets resolved at runtime.

With that you are all set and can run the test as usual:

```shell script
$ yaks test jitpack.feature
```

In the logs you will see that Jitpack automatically loads the additional dependency before the test.
