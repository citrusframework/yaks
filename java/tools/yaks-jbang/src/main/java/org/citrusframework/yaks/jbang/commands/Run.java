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
package org.citrusframework.yaks.jbang.commands;

import org.citrusframework.yaks.jbang.YaksJBangMain;
import picocli.CommandLine.Command;

@Command(name = "run", description = "Run as local YAKS test")
public class Run extends org.citrusframework.jbang.commands.Run {

    public Run(YaksJBangMain main) {
        super(main);
    }

    @Override
    public Integer call() throws Exception {
        System.setProperty("yaks.cluster.type", "LOCAL");
        System.setProperty("citrus.spring.java.config", "org.citrusframework.yaks.config.YaksAutoConfiguration");

        return super.call();
    }
}
