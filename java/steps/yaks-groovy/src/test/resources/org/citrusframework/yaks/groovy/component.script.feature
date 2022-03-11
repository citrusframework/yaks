Feature: Component script

  Scenario: Component configuration
    Given bind component foo.groovy
    """
    import org.citrusframework.yaks.groovy.Foo
    Foo.create()
    """
    When verify bean foo

  Scenario: Use new component
    Given $(echo(foo.text))
