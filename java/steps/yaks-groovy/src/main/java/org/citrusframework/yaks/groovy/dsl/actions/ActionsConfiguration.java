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

import java.util.LinkedHashSet;
import java.util.Set;

import com.consol.citrus.TestActionBuilder;
import com.consol.citrus.TestActionRunner;
import com.consol.citrus.actions.SleepAction;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;
import org.citrusframework.yaks.groovy.dsl.EndpointsConfiguration;

/**
 * @author Christoph Deppisch
 */
public class ActionsConfiguration {

    private final TestActionRunner runner;

    public ActionsConfiguration(TestActionRunner runner) {
        this.runner = runner;
    }

    public void actions(@DelegatesTo(ActionsBuilder.class) Closure<?> callable) {
        ActionsBuilder builder = new ActionsBuilder();
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.setDelegate(builder);
        callable.call();

        builder.reconcile();
        builder.actions.forEach(runner::run);
    }

    public static class ActionsBuilder extends GroovyObjectSupport {
        private final Set<TestActionBuilder<?>> actions = new LinkedHashSet<>();

        private static SleepAction.Builder sleep;

        /**
         * Workaround method selection errors because Object classes do also use sleep method
         * signatures and Groovy may not know which one of them to invoke. With this explicit static method we do
         * not run into the selection errors.
         * @return
         */
        public static SleepAction.Builder sleep() {
            sleep = new SleepAction.Builder();
            return sleep;
        }

        public Object methodMissing(String name, Object argLine) {
            reconcile();

            if (argLine == null) {
                throw new MissingMethodException(name, EndpointsConfiguration.class, null);
            }

            Object[] args = (Object[]) argLine;
            TestActionBuilder<?> actionBuilder = findTestActionBuilder(name, args);
            if (actionBuilder == null) {
                throw new MissingMethodException(name, EndpointsConfiguration.class, args);
            }

            actions.add(actionBuilder);
            return actionBuilder;
        }

        private TestActionBuilder<?> findTestActionBuilder(String id, Object[] args) {
            if (args == null || args.length == 0) {
                return Actions.fromId(id).getActionBuilder();
            }
            return Actions.fromId(id).getActionBuilder(args);
        }

        /**
         * Reconcile actions and maybe add sleep action if present. The sleep action builder may have been added via
         * static method workaround before.
         */
        public void reconcile() {
            if (sleep != null) {
                actions.add(sleep);
                sleep = null;
            }
        }
    }
}
