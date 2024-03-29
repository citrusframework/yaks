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

package org.citrusframework.yaks.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Christoph Deppisch
 */
public class TestSummary {

    public int passed = 0;
    public int failed = 0;
    public int errors = 0;
    public int skipped = 0;
    public int pending = 0;
    public int undefined = 0;

    public int getPassed() {
        return passed;
    }

    public int getFailed() {
        return failed;
    }

    public int getErrors() {
        return errors;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getPending() {
        return pending;
    }

    public int getUndefined() {
        return undefined;
    }

    @JsonProperty
    public int getTotal() {
        return passed + failed + skipped + pending + undefined;
    }
}
