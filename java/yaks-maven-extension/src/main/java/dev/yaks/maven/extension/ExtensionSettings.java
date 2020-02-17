package dev.yaks.maven.extension;

/**
 * @author Christoph Deppisch
 */
public final class ExtensionSettings {

    public static final String FEATURE_FILE_EXTENSION = ".feature";

    public static final String TESTS_PATH_KEY = "yaks.tests.path";
    public static final String TESTS_PATH_ENV = "YAKS_TESTS_PATH";

    public static final String SETTINGS_FILE_DEFAULT = "classpath:yaks.properties";
    public static final String SETTINGS_FILE_KEY = "yaks.settings.file";
    public static final String SETTINGS_FILE_ENV = "YAKS_SETTINGS_FILE";

    public static final String DEPENDENCIES_SETTING_KEY = "yaks.dependencies";
    public static final String DEPENDENCIES_SETTING_ENV = "YAKS_DEPENDENCIES";

    /**
     * Prevent instantiation of utility class.
     */
    private ExtensionSettings() {
        // utility class
    }

    /**
     * Gets the external tests path mount. Usually added to the runtime image via volume mount using config map.
     * @return
     */
    public static String getMountedTestsPath() {
        return System.getProperty(ExtensionSettings.TESTS_PATH_KEY, System.getenv(ExtensionSettings.TESTS_PATH_ENV) != null ?
                System.getenv(ExtensionSettings.TESTS_PATH_ENV) : "");
    }

    /**
     * Checks for mounted tests path setting.
     * @return
     */
    public static boolean hasMountedTests() {
        return getMountedTestsPath().length() > 0;
    }

    /**
     * Gets the settings file path from configured in this environment.
     * @return
     */
    public static String getSettingsFilePath() {
        return System.getProperty(ExtensionSettings.SETTINGS_FILE_KEY, System.getenv(ExtensionSettings.SETTINGS_FILE_ENV) != null ?
                System.getenv(ExtensionSettings.SETTINGS_FILE_ENV) : ExtensionSettings.SETTINGS_FILE_DEFAULT);
    }
}
