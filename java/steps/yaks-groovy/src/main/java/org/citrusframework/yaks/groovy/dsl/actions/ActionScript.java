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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.consol.citrus.TestActionRunner;
import org.citrusframework.yaks.groovy.GroovyShellUtils;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * @author Christoph Deppisch
 */
public class ActionScript {

    private static final Pattern COMMENTS = Pattern.compile("^(?:\\s*//|/\\*|\\s+\\*).*$", Pattern.MULTILINE);

    private final String script;

    public ActionScript(String script) {
        this.script = script;
    }

    public void execute(TestActionRunner runner) {
        ImportCustomizer ic = new ImportCustomizer();
        GroovyShellUtils.run(ic, new ActionsConfiguration(runner), normalize(script));
    }

    private String normalize(String script) {
        Matcher matcher = COMMENTS.matcher(script);
        String normalized;
        if (matcher.find()) {
            normalized = matcher.replaceAll("").trim();
        } else {
            normalized = script.trim();
        }

        if (normalized.startsWith("$actions {") ||
                normalized.startsWith("$finally {")) {
            return normalized;
        } else if (normalized.startsWith("$(")) {
            return String.format("$actions { %s }", normalized);
        } else {
            return String.format("$actions { $(%s) }", normalized);
        }
    }
}
