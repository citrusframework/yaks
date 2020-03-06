package dev.yaks.testing.jdbc;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.consol.citrus.Citrus;
import com.consol.citrus.annotations.CitrusFramework;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import cucumber.api.Scenario;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import io.cucumber.datatable.DataTable;
import org.postgresql.Driver;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * @author Christoph Deppisch
 */
public class JdbcSteps {

    @CitrusResource
    private TestRunner runner;

    @CitrusFramework
    private Citrus citrus;

    private DataSource dataSource;
    private List<String> sqlQueryStatements = new ArrayList<>();

    @Before
    public void before(Scenario scenario) {
        if (dataSource == null && citrus.getApplicationContext().getBeansOfType(DataSource.class).size() == 1L) {
            dataSource = citrus.getApplicationContext().getBean(DataSource.class);
        }
    }

    @Given("^(?:D|d)ata source: ([^\"\\s]+)$")
    public void setDataSource(String id) {
        if (!citrus.getApplicationContext().containsBean(id)) {
            throw new CitrusRuntimeException("Unable to find data source for id: " + id);
        }

        dataSource = citrus.getApplicationContext().getBean(id, DataSource.class);
    }

    @Given("^(?:D|d)atabase connection$")
    public void setConnection(DataTable properties) {
        Map<String, String> connectionProps = properties.asMap(String.class, String.class);

        String driver = connectionProps.getOrDefault("driver", Driver.class.getName());
        String url = connectionProps.getOrDefault("url", "");
        String username = connectionProps.getOrDefault("username", "test");
        String password = connectionProps.getOrDefault("password", "test");
        boolean suppressClose = Boolean.parseBoolean(connectionProps.getOrDefault("suppressClose", Boolean.TRUE.toString()));

        SingleConnectionDataSource singleConnectionDataSource = new SingleConnectionDataSource(url, username, password, suppressClose);
        singleConnectionDataSource.setDriverClassName(driver);
        this.dataSource = singleConnectionDataSource;
    }

    @Given("^SQL query: (.+)$")
    public void addQueryStatement(String statement) {
        if (statement.trim().toUpperCase().startsWith("SELECT")) {
            sqlQueryStatements.add(statement);
        } else {
            throw new CitrusRuntimeException("Invalid SQL query - please use proper 'SELECT' statement");
        }
    }

    @Given("^SQL query statements:$")
    public void addQueryStatements(DataTable statements) {
        statements.asList().forEach(this::addQueryStatement);
    }

    @Then("^verify column ([^\"\\s]+)=(.+)$")
    public void verifyColumn(String name, String value) {
        runner.query(action -> action.dataSource(dataSource)
                                     .statements(sqlQueryStatements)
                                     .validate(name, value));
        sqlQueryStatements.clear();
    }

    @Then("^verify columns$")
    public void verifyResultSet(DataTable expectedResults) {
        runner.query(action -> {
            action.dataSource(dataSource);
            action.statements(sqlQueryStatements);

            List<List<String>> rows = expectedResults.asLists(String.class);
            rows.forEach(row -> {
                if (!row.isEmpty()) {
                    String columnName = row.remove(0);
                    action.validate(columnName, row.toArray(new String[]{}));
                }
            });
        });

        sqlQueryStatements.clear();
    }

    @Then("^verify result set$")
    public void verifyResultSet(String verifyScript) {
        runner.query(action -> action.dataSource(dataSource)
                                     .statements(sqlQueryStatements)
                                     .groovy(verifyScript));

        sqlQueryStatements.clear();
    }

    @When("^(?:execute |perform )?SQL update: (.+)$")
    public void executeUpdate(String statement) {
        if (statement.trim().toUpperCase().startsWith("SELECT")) {
            throw new CitrusRuntimeException("Invalid SQL update statement - please use SQL query for 'SELECT' statements");
        } else {
            runner.sql(action -> action.dataSource(dataSource)
                                        .statement(statement));
        }

    }

    @When("^(?:execute |perform )?SQL update$")
    public void executeUpdateMultiline(String statement) {
        executeUpdate(statement);
    }

    @When("^(?:execute |perform )?SQL updates$")
    public void executeUpdates(DataTable statements) {
        statements.asList().forEach(this::executeUpdate);
    }
}
