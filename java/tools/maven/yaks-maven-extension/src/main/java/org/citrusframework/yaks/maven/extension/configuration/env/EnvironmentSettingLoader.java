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

package org.citrusframework.yaks.maven.extension.configuration.env;

import java.util.Optional;

/**
 * @author Christoph Deppisch
 */
public interface EnvironmentSettingLoader {

    /**
     * Read environment setting. If setting is not present default to empty value.
     * @param name
     * @return
     */
    default String getEnvSetting(String name) {
        return Optional.ofNullable(System.getenv(name)).orElse("");
    }
}
