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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.consol.citrus.TestActionBuilder;
import com.consol.citrus.actions.CreateVariablesAction;
import com.consol.citrus.actions.EchoAction;
import com.consol.citrus.actions.ExecutePLSQLAction;
import com.consol.citrus.actions.ExecuteSQLAction;
import com.consol.citrus.actions.ExecuteSQLQueryAction;
import com.consol.citrus.actions.FailAction;
import com.consol.citrus.actions.ReceiveMessageAction;
import com.consol.citrus.actions.SendMessageAction;
import com.consol.citrus.actions.SleepAction;
import com.consol.citrus.container.FinallySequence;
import com.consol.citrus.container.Iterate;
import com.consol.citrus.container.Parallel;
import com.consol.citrus.container.RepeatOnErrorUntilTrue;
import com.consol.citrus.container.RepeatUntilTrue;
import com.consol.citrus.container.Sequence;
import com.consol.citrus.container.Timer;
import groovy.lang.GroovyRuntimeException;
import org.springframework.util.ReflectionUtils;

/**
 * Set of supported test actions that can be used in a Groovy shell script.
 * @author Christoph Deppisch
 */
public enum Actions {

    ECHO("echo", EchoAction.Builder.class),
    SLEEP("sleep", SleepAction.Builder.class),
    SQL("sql", ExecuteSQLAction.Builder.class),
    PLSQL("plsql", ExecutePLSQLAction.Builder.class),
    QUERY("query", ExecuteSQLQueryAction.Builder.class),
    CREATE_VARIABLE("createVariable", CreateVariablesAction.Builder.class),
    CREATE_VARIABLES("createVariables", CreateVariablesAction.Builder.class),
    SEND("send",SendMessageAction.Builder.class),
    RECEIVE("receive", ReceiveMessageAction.Builder.class),
    FAIL("fail", FailAction.Builder.class),
    SEQUENCE("sequence", Sequence.Builder.class),
    ITERATE("iterate", Iterate.Builder.class),
    PARALLEL("parallel", Parallel.Builder.class),
    REPEAT("repeat", RepeatUntilTrue.Builder.class),
    REPEAT_ON_ERROR("repeatOnError", RepeatOnErrorUntilTrue.Builder.class),
    TIMER("timer", Timer.Builder.class),
    DO_FINALLY("doFinally", FinallySequence.Builder.class);

    private final String id;
    private final Class<? extends TestActionBuilder<?>> builderType;

    Actions(String id, Class<? extends TestActionBuilder<?>> builderType) {
        this.id = id;
        this.builderType = builderType;
    }

    public String id() {
        return id;
    }

    public static Actions fromId(String id) {
        return Arrays.stream(values())
                .filter(action -> action.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new GroovyRuntimeException(String.format("No action builder for id %s", id)));
    }

    public TestActionBuilder<?> getActionBuilder(Object... args) {
        try {
            Class<?>[] paramTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
            Method initializer = ReflectionUtils.findMethod(builderType, id, paramTypes);
            if (initializer == null) {
                throw new GroovyRuntimeException(String.format("Failed to find initializing method %s(%s) for action builder type %s", Arrays.toString(paramTypes), id, builderType.getName()));
            }
            return (TestActionBuilder<?>) initializer.invoke(null, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new GroovyRuntimeException("Failed to get action builder", e);
        }
    }
}
