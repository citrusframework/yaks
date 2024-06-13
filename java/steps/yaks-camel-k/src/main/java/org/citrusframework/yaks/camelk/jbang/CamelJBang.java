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

package org.citrusframework.yaks.camelk.jbang;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

public class CamelJBang {

    /** Logger */
    private static final Logger LOG = LoggerFactory.getLogger(CamelJBang.class);

    private static final boolean IS_OS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("windows");

    private static final int OK_EXIT_CODE = 0;

    private static Path installDir;

    private static CamelJBang INSTANCE;

    /**
     * Prevent direct instantiation.
     */
    private CamelJBang() {
    }

    public static CamelJBang camel() {
        if (INSTANCE == null) {
            detectJBang();
            addTrust();
            INSTANCE = new CamelJBang();
        }

        return INSTANCE;
    }

    /**
     * Run given integration with JBang Camel app.
     * @param name
     * @param path
     * @param args
     * @return
     */
    public ProcessAndOutput run(String name, Path path, String... args) {
        return run(name, path.toAbsolutePath().toString(), args);
    }

    /**
     * Run given integration with JBang Camel app.
     * @param name
     * @param file
     * @param args
     * @return
     */
    public ProcessAndOutput run(String name, String file, String... args) {
        List<String> runArgs = new ArrayList<>();
        runArgs.add("run");
        runArgs.add("--name");
        runArgs.add(name);

        if (CamelJBangSettings.getKameletsLocalDir() != null) {
            runArgs.add("--local-kamelet-dir");
            runArgs.add(CamelJBangSettings.getKameletsLocalDir().toString());
        }

        runArgs.addAll(Arrays.asList(args));

        runArgs.add(file);

        if (CamelJBangSettings.isCamelDumpIntegrationOutput()) {
            Path workDir = CamelJBangSettings.getWorkDir();
            File outputFile = workDir.resolve(String.format("i-%s-output.txt", name)).toFile();

            if (Stream.of(args).noneMatch(it -> it.contains("--logging-color"))) {
                // disable logging colors when writing logs to file
                runArgs.add("--logging-color=false");
            }

            return executeAsync(camel(runArgs.toArray(String[]::new)), outputFile);
        } else {
            return executeAsync(camel(runArgs.toArray(String[]::new)));
        }
    }

    /**
     * Get details for integration previously run via JBang Camel app. Integration is identified by its process id.
     * @param pid
     */
    public void stop(Long pid) {
        ProcessAndOutput p = execute(camel("stop", String.valueOf(pid))) ;
        if (p.getProcess().exitValue() != OK_EXIT_CODE) {
            throw new CitrusRuntimeException(String.format("Failed to stop Camel K integration - exit code %d", p.getProcess().exitValue()));
        }
    }

    /**
     * Get information on running integrations.
     */
    public String ps() {
        ProcessAndOutput p = execute(camel("ps"));
        return p.getOutput();
    }

    /**
     * Get Camel JBang version.
     */
    public String version() {
        ProcessAndOutput p = execute(camel("--version"));
        return p.getOutput();
    }

    /**
     * Get details for integration previously run via JBang Camel app. Integration is identified by its process id.
     * @param pid
     */
    public Map<String, String> get(Long pid) {
        Map<String, String> properties = new HashMap<>();

        String output = ps();
        if (output.isBlank()) {
            return properties;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8))))) {
            String line = reader.readLine();

            List<String> names = new ArrayList<>(Arrays.asList(line.trim().split("\\s+")));

            while ((line = reader.readLine()) != null) {
                List<String> values = new ArrayList<>(Arrays.asList(line.trim().split("\\s+")));
                if (!values.isEmpty() && values.get(0).equals(String.valueOf(pid))) {
                    for (int i=0; i < names.size(); i++) {
                        if (i < values.size()) {
                            properties.put(names.get(i), values.get(i));
                        } else {
                            properties.put(names.get(i), "");
                        }
                    }
                    break;
                }
            }

            return properties;
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to get integration details from JBang", e);
        }
    }

    /**
     * Get list of integrations previously run via JBang Camel app.
     */
    public List<Map<String, String>> getAll() {
        List<Map<String, String>> integrations = new ArrayList<>();

        String output = ps();
        if (output.isBlank()) {
            return integrations;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8))))) {
            String line = reader.readLine();

            List<String> names = new ArrayList<>(Arrays.asList(line.trim().split("\\s+")));

            while ((line = reader.readLine()) != null) {
                Map<String, String> properties = new HashMap<>();
                List<String> values = new ArrayList<>(Arrays.asList(line.trim().split("\\s+")));
                for (int i=0; i < names.size(); i++) {
                    if (i < values.size()) {
                        properties.put(names.get(i), values.get(i));
                    } else {
                        properties.put(names.get(i), "");
                    }
                }

                integrations.add(properties);
            }

            return integrations;
        } catch (IOException e) {
            throw new CitrusRuntimeException("Failed to list integrations from JBang", e);
        }
    }

    private static void detectJBang() {
        ProcessAndOutput result = getVersion();
        if (result.getProcess().exitValue() == OK_EXIT_CODE) {
            LOG.info("Found JBang v" + result.getOutput());
        } else {
            LOG.warn("JBang not found. Downloading ...");
            download();
            result = getVersion();
            if (result.getProcess().exitValue() == OK_EXIT_CODE) {
                LOG.info("Using JBang v" + result.getOutput());
            }
        }
    }

    private static void download() {
        String homePath = "jbang";

        Path installPath = Paths.get(System.getProperty("user.home")).toAbsolutePath().resolve(".jbang").toAbsolutePath();

        if (installPath.resolve(homePath).toFile().exists()) {
            LOG.info("Using local JBang in " + installPath);
            installDir = installPath.resolve(homePath);
            return;
        }

        LOG.info("Downloading JBang from " + CamelJBangSettings.getJBangDownloadUrl() + " and installing in " + installPath);

        try {
            Files.createDirectories(installPath);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(CamelJBangSettings.getJBangDownloadUrl()))
                    .GET()
                    .build();

            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFileDownload(installPath,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING));

            if (response.statusCode() != 200) {
                throw new CitrusRuntimeException(String.format("Failed to download JBang - response code %d", response.statusCode()));
            }

            unzip(response.body(), installPath);
        } catch (URISyntaxException | IOException | InterruptedException e) {
            throw new CitrusRuntimeException("Failed to download JBang", e);
        }

        installDir = installPath.resolve(homePath);
    }

    private static ProcessAndOutput getVersion() {
        return execute(jBang("version"));
    }

    /**
     * Execute "jbang trust add URL..."
     *
     * @throws CitrusRuntimeException if the exit value is different from
     *                                0: success
     *                                1: Already trusted source(s)
     */
    private static void addTrust() {
        for (String url : CamelJBangSettings.getTrustUrl()) {
            ProcessAndOutput result = execute(jBang("trust", "add", url));
            int exitValue = result.getProcess().exitValue();
            if (exitValue != OK_EXIT_CODE && exitValue != 1) {
                throw new CitrusRuntimeException("Error while trusting JBang URLs. Exit code: " + exitValue);
            }
        }
    }

    /**
     * @return JBang camel command with given arguments.
     */
    private static List<String> camel(String... args) {
        List<String> jBangArgs = new ArrayList<>();
        jBangArgs.add(String.format("-Dcamel.jbang.version=%s", CamelJBangSettings.getCamelVersion()));

        if (!CamelJBangSettings.getKameletsVersion().isBlank()) {
            jBangArgs.add(String.format("-Dcamel-kamelets.version=%s", CamelJBangSettings.getKameletsVersion()));
        }

        jBangArgs.add(CamelJBangSettings.getCamelApp());
        jBangArgs.addAll(List.of(args));

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Full Camel JBang command is: %s", String.join(" ", jBangArgs)));
        }

        return jBang(jBangArgs);
    }

    /**
     * @return JBang command with given arguments.
     */
    private static List<String> jBang(String... args) {
        return jBang(List.of(args));
    }

    /**
     * @return JBang command with given arguments.
     */
    private static List<String> jBang(List<String> args) {
        List<String> command = new ArrayList<>();
        if (IS_OS_WINDOWS) {
            command.add("cmd.exe");
            command.add("/c");
        } else {
            command.add("sh");
            command.add("-c");
        }

        String jBangCommand = getJBangExecutable() + " " + String.join(" ", args);
        command.add(jBangCommand);

        return command;
    }

    /**
     * Execute JBang command using the process API. Waits for the process to complete and returns the process instance so
     * caller is able to access the exit code and process output.
     * @param command
     * @return
     */
    private static ProcessAndOutput execute(List<String> command) {
        try {
            Process p = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            String output = StreamUtils.copyToString(p.getInputStream(), StandardCharsets.UTF_8);
            p.waitFor();

            if (CamelJBangSettings.isDumpProcessOutput()) {
                Path workDir = CamelJBangSettings.getWorkDir();
                FileUtils.writeToFile(output, workDir.resolve(String.format("%s-output.txt", p.pid())).toFile());
            }

            if (LOG.isDebugEnabled() && p.exitValue() != OK_EXIT_CODE) {
                LOG.debug("Command failed: " + String.join(" ", command));
                LOG.debug(output);
            }

            return new ProcessAndOutput(p, output);
        } catch (IOException | InterruptedException e) {
            throw new CitrusRuntimeException("Error while executing JBang", e);
        }
    }

    /**
     * Execute JBang command using the process API. Waits for the process to complete and returns the process instance so
     * caller is able to access the exit code and process output.
     * @param command
     * @return
     */
    private static ProcessAndOutput executeAsync(List<String> command) {
        try {
            Process p = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            return new ProcessAndOutput(p);
        } catch (IOException e) {
            throw new CitrusRuntimeException("Error while executing JBang", e);
        }
    }

    /**
     * Execute JBang command using the process API. Waits for the process to complete and returns the process instance so
     * caller is able to access the exit code and process output.
     * @param command
     * @param outputFile
     * @return
     */
    private static ProcessAndOutput executeAsync(List<String> command, File outputFile) {
        try {
            Process p = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(outputFile)
                .start();

            return new ProcessAndOutput(p, outputFile);
        } catch (IOException e) {
            throw new CitrusRuntimeException("Error while executing JBang", e);
        }
    }

    /**
     * Gets the JBang executable name.
     * @return
     */
    private static String getJBangExecutable() {
        if (installDir != null) {
            if (IS_OS_WINDOWS) {
                return installDir.resolve("bin/jbang.cmd").toString();
            } else {
                return installDir.resolve("bin/jbang").toString();
            }
        } else {
            if (IS_OS_WINDOWS) {
                return "jbang.cmd";
            } else {
                return "jbang";
            }
        }
    }

    /**
     * Extract JBang download.zip to install directory.
     * @param downloadZip
     * @param installPath
     * @throws IOException
     */
    private static void unzip(Path downloadZip, Path installPath) throws IOException {
        ZipInputStream zis = new ZipInputStream(new FileInputStream(downloadZip.toFile()));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            Path filePath = newFile(installPath, zipEntry);
            File newFile = filePath.toFile();
            if (zipEntry.isDirectory()) {
                if (!newFile.isDirectory() && !newFile.mkdirs()) {
                    throw new IOException("Failed to create directory " + newFile);
                }
            } else {
                // fix for Windows-created archives
                File parent = newFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Failed to create directory " + parent);
                }

                // write file content
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    StreamUtils.copy(zis, fos);
                }

                if ("jbang".equals(filePath.getFileName().toString())) {
                    Files.setPosixFilePermissions(filePath, PosixFilePermissions.fromString("rwxr--r--"));
                }
            }
            zipEntry = zis.getNextEntry();
        }

        zis.closeEntry();
        zis.close();
    }

    /**
     * Guards against writing files to the file system outside the target folder also known as Zip slip vulnerability.
     * @param destinationDir
     * @param zipEntry
     * @return
     * @throws IOException
     */
    private static Path newFile(Path destinationDir, ZipEntry zipEntry) throws IOException {
        Path destFile = destinationDir.resolve(zipEntry.getName());

        String destDirPath = destinationDir.toFile().getCanonicalPath();
        String destFilePath = destFile.toFile().getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }

}
