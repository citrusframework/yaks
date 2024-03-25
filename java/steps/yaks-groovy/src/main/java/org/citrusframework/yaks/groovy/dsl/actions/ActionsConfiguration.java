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

package org.citrusframework.yaks.groovy.dsl.actions;

import java.util.function.Supplier;

import org.citrusframework.TestAction;
import org.citrusframework.TestActionBuilder;
import org.citrusframework.TestActionRunner;
import org.citrusframework.actions.SleepAction;
import org.citrusframework.container.FinallySequence;
import org.citrusframework.container.Wait;
import org.citrusframework.exceptions.CitrusRuntimeException;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingMethodException;

/**
 * @author Christoph Deppisch
 */
public class ActionsConfiguration {

    private final TestActionRunner runner;

    public ActionsConfiguration(TestActionRunner runner) {
        this.runner = runner;
    }

    public void $actions(@DelegatesTo(ActionsBuilder.class) Closure<?> callable) {
        ActionsBuilder builder = new ActionsBuilder();
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.setDelegate(builder);
        callable.call();
    }

    public void $finally(@DelegatesTo(FinallyActionsBuilder.class) Closure<?> callable) {
        FinallyActionsBuilder builder = new FinallyActionsBuilder();
        callable.setResolveStrategy(Closure.DELEGATE_FIRST);
        callable.setDelegate(builder);
        callable.call();

        runner.run(builder.get());
    }

    public class FinallyActionsBuilder extends ActionsBuilder implements Supplier<FinallySequence.Builder> {

        private final FinallySequence.Builder builder = new FinallySequence.Builder();

        @Override
        public Wait.Builder waitFor() {
            Wait.Builder waitFor = super.waitFor();
            builder.actions(waitFor);
            return waitFor;
        }

        @Override
        public SleepAction.Builder delay() {
            SleepAction.Builder delay = super.delay();
            builder.actions(delay);
            return delay;
        }

        @Override
        public <T extends TestAction> T $(TestActionBuilder<T> builder) {
            throw new CitrusRuntimeException("Nested test action should not use run shortcut '$()' " +
                    "please just use the test action builder method");
        }

        @Override
        public Object methodMissing(String name, Object argLine) {
            TestActionBuilder<?> actionBuilder = (TestActionBuilder<?>) super.methodMissing(name, argLine);
            builder.actions(actionBuilder);
            return actionBuilder;
        }

        @Override
        public FinallySequence.Builder get() {
            return builder;
        }
    }

    public class ActionsBuilder extends GroovyObjectSupport {

        /**
         * Short hand method running given test action builder.
         * @param builder
         * @param <T>
         * @return
         */
        public <T extends TestAction> T $(TestActionBuilder<T> builder) {
            return runner.run(builder);
        }

        /**
         * Workaround method selection errors because Object classes do also use sleep method
         * signatures and Groovy may not know which one of them to invoke.
         * @return
         */
        public SleepAction.Builder delay() {
            return new SleepAction.Builder();
        }

        /**
         * Workaround method selection errors because Object classes do also use wait method
         * signatures and Groovy may not know which one of them to invoke.
         * @return
         */
        public Wait.Builder waitFor() {
            return new Wait.Builder();
        }

        public void $finally(@DelegatesTo(FinallyActionsBuilder.class) Closure<?> callable) {
            ActionsConfiguration.this.$finally(callable);
        }

        public Object methodMissing(String name, Object argLine) {
            if (argLine == null) {
                throw new MissingMethodException(name, ActionsConfiguration.class, null);
            }

            Object[] args = (Object[]) argLine;
            TestActionBuilder<?> actionBuilder = findTestActionBuilder(name, args);
            if (actionBuilder == null) {
                throw new MissingMethodException(name, ActionsConfiguration.class, args);
            }

            return actionBuilder;
        }

        private TestActionBuilder<?> findTestActionBuilder(String id, Object[] args) {
            if (args == null || args.length == 0) {
                return Actions.fromId(id).getActionBuilder();
            }
            return Actions.fromId(id).getActionBuilder(args);
        }
    }
}
