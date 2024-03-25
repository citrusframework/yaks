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

package org.citrusframework.yaks.jbang;

import org.citrusframework.jbang.CitrusJBangMain;
import org.citrusframework.jbang.commands.Init;
import org.citrusframework.yaks.jbang.commands.Complete;
import org.citrusframework.yaks.jbang.commands.ListTests;
import org.citrusframework.yaks.jbang.commands.Run;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "yaks", description = "YAKS CLI", mixinStandardHelpOptions = true)
public class YaksJBangMain extends CitrusJBangMain {
    private static CommandLine commandLine;

    public static void run(String... args) {
        YaksJBangMain main = new YaksJBangMain();
        commandLine = new CommandLine(main)
                .addSubcommand("init", new CommandLine(new Init(main)))
                .addSubcommand("run", new CommandLine(new Run(main)))
                .addSubcommand("ls", new CommandLine(new ListTests(main)))
                .addSubcommand("completion", new CommandLine(new Complete(main)));

        commandLine.getCommandSpec().versionProvider(() -> new String[] { "0.19.0-SNAPSHOT" });

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        commandLine.execute("--help");
        return 0;
    }

}
