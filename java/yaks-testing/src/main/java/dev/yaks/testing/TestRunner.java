package dev.yaks.testing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import cucumber.runtime.ClassFinder;
import cucumber.runtime.Env;
import cucumber.runtime.Runtime;
import cucumber.runtime.RuntimeOptions;
import cucumber.runtime.io.MultiLoader;
import cucumber.runtime.io.ResourceLoader;
import cucumber.runtime.io.ResourceLoaderClassFinder;

public class TestRunner {

    public TestRunner() {
    }

    public static void main(String[] args) throws IOException {
        ClassLoader classLoader = TestRunner.class.getClassLoader();
        ResourceLoader resourceLoader = new MultiLoader(classLoader);
        ClassFinder classFinder = new ResourceLoaderClassFinder(resourceLoader, classLoader);

        Properties properties = new Properties();
        try (InputStream in = TestRunner.class.getResourceAsStream("/cucumber.properties")) {
            properties.load(in);
        }

        List<String> params = new ArrayList<>();
        params.add("--glue");
        params.add("com.consol.citrus.cucumber.step.runner.core");

        params.add("--glue");
        params.add("dev.yaks.testing.http");

        params.add("--glue");
        params.add("dev.yaks.testing.camel.k");

        params.add("--glue");
        params.add("dev.yaks.testing.jdbc");

        params.add("--glue");
        params.add("dev.yaks.testing.standard");

        params.add("--plugin");
        params.add("com.consol.citrus.cucumber.CitrusReporter");

        params.add("--strict");

        params.add("/etc/yaks/test/");
        //params.add(".");

        Env env = new Env(properties);
        RuntimeOptions options = new RuntimeOptions(env, params);

        Runtime runtime = new Runtime(resourceLoader, classFinder, classLoader, options);
        runtime.run();

        System.exit(runtime.exitStatus());
    }
}
