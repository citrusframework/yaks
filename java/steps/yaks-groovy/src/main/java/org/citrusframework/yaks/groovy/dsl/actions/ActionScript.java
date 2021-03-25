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

package org.citrusframework.yaks.groovy.dsl.actions;

import com.consol.citrus.TestActionRunner;
import org.citrusframework.yaks.groovy.GroovyShellUtils;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * @author Christoph Deppisch
 */
public class ActionScript {

    private final String code;

    public ActionScript(String code) {
        if (code.contains("actions {")) {
            this.code = code;
        } else if (code.trim().startsWith("$(")) {
            this.code = String.format("actions { %s }", code);
        } else {
            this.code = String.format("actions { $(%s) }", code);
        }
    }

    public void execute(TestActionRunner runner) {
        ImportCustomizer ic = new ImportCustomizer();
        // need to workaround the static sleep test action method because this causes method selection error with
        // too many static methods using this signature
        ic.addStaticImport(ActionsConfiguration.ActionsBuilder.class.getName(), "sleep");

        GroovyShellUtils.run(ic, new ActionsConfiguration(runner), code);
    }
}
