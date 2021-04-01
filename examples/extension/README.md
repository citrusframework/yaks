# Extensions

This example shows the usage of extensions in YAKS. Extensions add custom steps to the test runtime so you can use
custom step definitions in your feature file.

_extension.feature_
```gherkin
Scenario: print extended slogan
    Given YAKS does Cloud-Native BDD testing
    Then YAKS can be extended!
```

The step `YAKS can be extended!` is not available in the default step implementations provided by YAKS. The step definition
is implemented in a separate custom Maven module ([steps](steps)) and gets uploaded to the Kubernetes cluster using the 
[container-tools/snap](https://github.com/container-tools/snap) library.

Snap uses a [Minio](https://min.io/) object storage that is automatically installed in the current namespace. You can build and upload
custom Maven modules with:

```shell script
$ yaks upload examples/extensions/steps
```                                    

This will create the Minio storage and perform the upload. After that you can use the custom steps in your feature file. Be sure to add
the dependency and the additional glue code in `yaks-config.yaml`.

_yaks-config.yaml_
```yaml
config:
  runtime:
    cucumber:
      glue:
      - "org.citrusframework.yaks"
      - "com.company.steps.custom" 
dependencies:
  - groupId: com.company
    artifactId: steps
    version: "1.0.0-SNAPSHOT"
```     

The additional glue code should match the package name where to find the custom step definitions in your custom code.

With that you are all set and can run the test as usual:

```shell script
$ yaks run extension.feature
```

You can also use the upload as part of the test command:

```shell script
$ yaks run extension.feature --upload steps
```                                         

The `--upload` option builds and uploads the custom Maven module automatically before the test.
