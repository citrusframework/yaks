# Kamelet sample

This sample shows the usage of Apache Camel-K Kamelets. Users can create Kamelets and verify the ready state
in the current namespace. Once the Kamelet is ready the tests can create a Camel-K integration that uses the Kamelet as source.

## Create Kamelets

The feature creates a new Kamelet in the current namespace and uses the Kamelet source in a
Camel-K integration.

[kamelet.feature](kamelet.feature)
```shell script
$ yaks test examples/kamelets/kamelet.feature
```

## Create Kamelets using file resources

File resources are added to the test with the `--resource` CLI option.

[kamelet-resource.feature](kamelet-resource.feature)
```shell script
$ yaks test examples/kamelets/kamelet-resource.feature \
            --resource timer-to-log.groovy \
            --resource timer-source.kamelet.yaml
```             

The test is able to load the Kamelet using the external file resource. Also the Camel-K integration code
is loaded from a file resource in this example.

You can also add resources to the `yaks-config.xml`

*yaks-config.yaml*
```yaml
config:
  runtime:
    resources:
    - timer-to-log.groovy 
    - timer-source.kamelet.yaml 
```
