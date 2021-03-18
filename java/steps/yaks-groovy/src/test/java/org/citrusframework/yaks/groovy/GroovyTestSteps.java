/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.citrusframework.yaks.groovy;

import com.consol.citrus.Citrus;
import com.consol.citrus.annotations.CitrusFramework;
import io.cucumber.java.en.Then;
import org.assertj.core.api.Assertions;

/**
 * @author Christoph Deppisch
 */
public class GroovyTestSteps {

    @CitrusFramework
    private Citrus citrus;

    @Then("^verify endpoint ([^\"\\s]+)$")
    public void verifyEndpointResolvable(String endpoint) {
        Assertions.assertThat(citrus.getCitrusContext().getReferenceResolver().isResolvable(endpoint)).isTrue();
    }

    @Then("^verify bean ([^\"\\s]+)$")
    public void verifyBeanResolvable(String name) {
        Assertions.assertThat(citrus.getCitrusContext().getReferenceResolver().isResolvable(name)).isTrue();
    }
}
