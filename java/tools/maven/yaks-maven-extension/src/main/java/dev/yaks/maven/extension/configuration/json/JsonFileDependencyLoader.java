package dev.yaks.maven.extension.configuration.json;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import dev.yaks.maven.extension.configuration.AbstractConfigFileDependencyLoader;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.maven.lifecycle.LifecycleExecutionException;
import org.apache.maven.model.Dependency;
import org.codehaus.plexus.logging.Logger;

/**
 * Load dependencies from Json configuration file. The configuration should reside as list of Maven artifact dependencies.
 *
 * {
 *   "dependencies": [
 *     {
 *       "groupId": "org.foo",
 *       "artifactId": "foo-artifact",
 *       "version": "1.0.0"
 *     },
 *     {
 *       "groupId": "org.bar",
 *       "artifactId": "bar-artifact",
 *       "version": "1.5.0"
 *     }
 *   ]
 * }
 *
 * Each dependency value should be a proper Maven coordinate with groupId, artifactId and version.
 * @author Christoph Deppisch
 */
public class JsonFileDependencyLoader extends AbstractConfigFileDependencyLoader {

    @Override
    protected List<Dependency> load(Path filePath, Properties properties, Logger logger) throws LifecycleExecutionException {
        List<Dependency> dependencyList = new ArrayList<>();

        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        try {
            JSONObject root = (JSONObject) parser.parse(new StringReader(new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8)));
            JSONArray dependencies = (JSONArray) root.get("dependencies");
            for (Object o : dependencies) {
                JSONObject coordinates = (JSONObject) o;
                Dependency dependency = new Dependency();

                dependency.setGroupId(coordinates.get("groupId").toString());
                dependency.setArtifactId(coordinates.get("artifactId").toString());
                dependency.setVersion(resolveVersionProperty(coordinates.get("version").toString(), properties));

                logger.info(String.format("Add %s", dependency));
                dependencyList.add(dependency);
            }
        } catch (ParseException | IOException e) {
            throw new LifecycleExecutionException("Failed to read json dependency config file", e);
        }

        return dependencyList;
    }
}
