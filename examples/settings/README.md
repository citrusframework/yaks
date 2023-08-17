# Settings

This example shows the usage of settings when running a test. Users can add settings to the test
execution via CLI options.

## Properties file

You can add settings such as Maven repositories and additional dependencies as properties.

*yaks.properties*
```properties
# Maven repositories
yaks.repository.central=https://repo.maven.apache.org/maven2/
yaks.repository.jboss-ea=https://repository.jboss.org/nexus/content/groups/ea/

# Additional dependencies
yaks.dependency.camel-groovy=org.apache.camel:camel-groovy:@camel.version@
```

Just add the properties file when running the test.

```shell script
$ yaks --settings yaks.properties test camel-route.feature
```

## Json settings

You can provide settings in form of a Json file.

*yaks.settings.json*
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
  ],
  "dependencies": [
    {
      "groupId": "org.apache.camel",
      "artifactId": "camel-groovy",
      "version": "@camel.version@"
    }
  ]
}
```

Just add the settings file when running the test.

```shell script
$ yaks --settings yaks.settings.json test camel-route.feature
```   

## Yaml settings

You can provide settings in form of a YAML file.

*yaks.settings.yaml*
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
dependencies:
  - groupId: org.apache.camel
    artifactId: camel-groovy
    version: "@camel.version@"
```

Just add the settings file when running the test.

```shell script
$ yaks --settings yaks.settings.yaml test camel-route.feature
```

## Using yaks-config.yaml

You can also add settings files to the `yaks-config.xml`

*yaks-config.yaml*
```yaml
config:
  runtime:
    resources:
    - examples/resources/some.groovy 
    - examples/resources/another.yaml 
```

With this configuration in place you can just run the test and the settings file will be added automatically.

```shell script
$ yaks run camel-route.feature
```
