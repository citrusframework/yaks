///usr/bin/env jbang "$0" "$@" ; exit $?

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//JAVA 11+
//REPOS mavencentral
//DEPS org.citrusframework.yaks:yaks-parent:${yaks.jbang.version:0.12.2}@pom
//DEPS org.citrusframework.yaks:yaks-jbang:${yaks.jbang.version:0.12.2}
//DEPS org.citrusframework.yaks:yaks-runtime-core:${yaks-jbang.version:0.12.2}
package main;

import org.citrusframework.yaks.jbang.YaksJBangMain;

/**
 * Main to run YaksJBang
 */
public class YaksJBang {

    public static void main(String... args) {
        YaksJBangMain.run(args);
    }

}
