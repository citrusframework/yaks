Feature: Beans script

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
    When verify bean dataSource
