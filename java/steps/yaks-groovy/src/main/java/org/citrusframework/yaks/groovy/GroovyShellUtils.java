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

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.util.DelegatingScript;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

/**
 * @author Christoph Deppisch
 */
public final class GroovyShellUtils {

    private GroovyShellUtils() {
        // prevent instantiation of utility class
    }

    /**
     * Run given scriptCode with GroovyShell.
     * @param ic import customizer
     * @param scriptCode code to evaluate in shell
     * @param <T> return type
     * @return script result
     */
    public static <T> T run(ImportCustomizer ic, String scriptCode) {
        return run(ic, null, scriptCode);
    }

    /**
     * Run given scriptCode with GroovyShell and delegate execution to given instance.
     * @param ic import customizer
     * @param delegate instance providing methods and properties
     * @param scriptCode code to evaluate in shell
     * @param <T> return type
     * @return script result
     */
    public static <T> T run(ImportCustomizer ic, Object delegate, String scriptCode) {
        CompilerConfiguration cc = new CompilerConfiguration();
        cc.addCompilationCustomizers(ic);
        cc.setScriptBaseClass(DelegatingScript.class.getName());

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        GroovyShell sh = new GroovyShell(cl, new Binding(), cc);

        Script script = sh.parse(scriptCode);

        if (delegate != null && script instanceof DelegatingScript) {
            // set the delegate target
            ((DelegatingScript) script).setDelegate(delegate);
        }
        return (T) script.run();
    }
}
