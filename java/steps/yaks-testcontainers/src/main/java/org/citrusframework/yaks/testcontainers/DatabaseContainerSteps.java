/*
 * Copyright the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.testcontainers;

import java.io.IOException;

import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.util.FileUtils;
import io.cucumber.java.en.Given;
import org.citrusframework.yaks.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseContainerSteps {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseContainerSteps.class);

    @CitrusResource
    private TestContext context;

    @Given("^(?:D|d)atabase init script$")
    public void setInitScript(String initScript) {
        DatabaseContainerSteps.saveInitScript(context, initScript);
    }

    @Given("^load database init script (^\\s+)$")
    public void loadInitScript(String file) throws IOException {
        DatabaseContainerSteps.saveInitScript(context, FileUtils.readToString(ResourceUtils.resolve(file, context)));
    }

    /**
     * Saves given init script as test variable in given test context.
     * @param context the test context.
     * @param initScript the init script.
     */
    protected static void saveInitScript(TestContext context, String initScript) {
        context.setVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "DB_INIT_SCRIPT", initScript);
    }

    /**
     * Retrieves database init script from given test context.
     * Handles test variable not found errors and returns empty script.
     * @param context the context to hold init script as test variable.
     * @return init script or empty
     */
    protected static String getInitScript(TestContext context) {
        try {
            return context.getVariable(TestContainersSteps.TESTCONTAINERS_VARIABLE_PREFIX + "DB_INIT_SCRIPT");
        } catch (CitrusRuntimeException e) {
            LOG.debug("Missing database init script", e);
            return "";
        }
    }
}
