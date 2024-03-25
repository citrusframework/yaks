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
package org.citrusframework.yaks.jbang.commands;

import java.io.File;

import org.citrusframework.jbang.commands.CitrusCommand;
import org.citrusframework.yaks.jbang.YaksJBangMain;

public abstract class YaksCommand extends CitrusCommand {

    private final YaksJBangMain main;
    private File yaksDir;

    public YaksCommand(YaksJBangMain main) {
        super(main);
        this.main = main;
    }

    public YaksJBangMain getMain() {
        return main;
    }

    public File getStatusFile(String pid) {
        return new File(getYaksDir(), pid + "-status.json");
    }

    public File getOutputFile(String pid) {
        return new File(getYaksDir(), pid + "-output.json");
    }

    private File getYaksDir() {
        if (yaksDir == null) {
            yaksDir = new File(System.getProperty("user.home"), ".yaks");
        }

        return yaksDir;
    }

}
