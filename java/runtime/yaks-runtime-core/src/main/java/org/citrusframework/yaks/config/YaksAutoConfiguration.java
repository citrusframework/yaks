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

package org.citrusframework.yaks.config;

import java.io.File;
import java.net.URL;
import java.util.stream.Stream;

import com.consol.citrus.functions.DefaultFunctionLibrary;
import com.consol.citrus.functions.FunctionLibrary;
import com.consol.citrus.variable.GlobalVariables;
import com.consol.citrus.variable.GlobalVariablesPropertyLoader;
import org.citrusframework.yaks.report.SystemOutTestReporter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Christoph Deppisch
 */
@Configuration
public class YaksAutoConfiguration {

    @Bean
    public GlobalVariables globalVariables() {
        return new GlobalVariables();
    }

    @Bean
    public GlobalVariablesPropertyLoader globalVariablesPropertyLoader() {
        GlobalVariablesPropertyLoader propertyLoader = new GlobalVariablesPropertyLoader();

        // Load mounted secrets as global variables
        URL mountedSecrets = Thread.currentThread().getContextClassLoader().getResource("secrets");
        if (mountedSecrets != null) {
            File secretsDir = new File(mountedSecrets.getPath());
            if (secretsDir.exists() && secretsDir.isDirectory()) {
                File[] propertyFiles = secretsDir.listFiles();
                if (propertyFiles != null) {
                    Stream.of(propertyFiles).forEach(file -> {
                        propertyLoader.getPropertyFiles().add("file:" + file.getPath());
                    });
                }
            }
        }

        return propertyLoader;
    }

    @Bean(destroyMethod = "destroy")
    public SystemOutTestReporter systemOutReporter() {
        return new SystemOutTestReporter();
    }

    @Bean
    public FunctionLibrary yaksFunctionLibrary() {
        FunctionLibrary lib = new FunctionLibrary();
        lib.setPrefix("yaks:");
        lib.getMembers().putAll(new DefaultFunctionLibrary().getMembers());
        return lib;
    }
}
