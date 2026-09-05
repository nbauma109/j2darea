package com.github.nbauma109.j2darea;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Stores lightweight editor preferences in a file-backed properties store.
 */
public final class UserPreferences {

    private static final String KEY_EXPORT_PREFIX = "exportPrefix";
    private static final String KEY_KNOWN_OWNED_AREAS = "knownOwnedAreas";
    private static final String KEY_GAME_INSTALL_PATH = "gameInstallPath";
    private static final String KEY_ZOOM_FACTOR = "zoomFactor";
    private static final String KEY_GOOGLE_AI_API_KEY = "googleAiApiKey";
    private static final String KEY_FILE_CHOOSER_DIRECTORY_PREFIX = "fileChooserDirectory.";
    private static final File STORAGE_FILE = resolveStorageFile();
    private static final Properties PROPERTIES = loadProperties();

    private UserPreferences() {
    }

    public static String getExportPrefix() {
        return get(KEY_EXPORT_PREFIX, "");
    }

    public static void setExportPrefix(String prefix) {
        put(KEY_EXPORT_PREFIX, prefix != null ? prefix : "");
    }

    public static String getGameInstallPath() {
        return get(KEY_GAME_INSTALL_PATH, "");
    }

    public static void setGameInstallPath(String gameInstallPath) {
        put(KEY_GAME_INSTALL_PATH, gameInstallPath != null ? gameInstallPath.trim() : "");
    }

    public static double getZoomFactor() {
        return getDouble(KEY_ZOOM_FACTOR, 1.1d);
    }

    public static void setZoomFactor(double zoomFactor) {
        put(KEY_ZOOM_FACTOR, String.valueOf(clampZoomFactor(zoomFactor)));
    }

    public static String getGoogleAiApiKey() {
        return get(KEY_GOOGLE_AI_API_KEY, "");
    }

    public static void setGoogleAiApiKey(String googleAiApiKey) {
        put(KEY_GOOGLE_AI_API_KEY, googleAiApiKey != null ? googleAiApiKey.trim() : "");
    }

    public static String getStorageLocation() {
        return STORAGE_FILE.getAbsolutePath();
    }

    public static String getFileChooserDirectory(FileChooserLocation location) {
        if (location == null) {
            return "";
        }
        return get(KEY_FILE_CHOOSER_DIRECTORY_PREFIX + location.name(), "");
    }

    public static void setFileChooserDirectory(FileChooserLocation location, String directory) {
        if (location == null) {
            return;
        }
        put(KEY_FILE_CHOOSER_DIRECTORY_PREFIX + location.name(), directory != null ? directory : "");
    }

    public static List<String> getKnownOwnedAreas() {
        String raw = get(KEY_KNOWN_OWNED_AREAS, "");
        Set<String> values = new LinkedHashSet<String>();
        if (!raw.trim().isEmpty()) {
            String[] tokens = raw.split(",");
            for (String token : tokens) {
                String normalized = normalizeOwnedAreaResref(token);
                if (!normalized.isEmpty()) {
                    values.add(normalized);
                }
            }
        }
        return new ArrayList<String>(values);
    }

    public static void addKnownOwnedArea(String areaResref) {
        String normalized = normalizeOwnedAreaResref(areaResref);
        if (normalized.isEmpty()) {
            return;
        }
        Set<String> values = new LinkedHashSet<String>(getKnownOwnedAreas());
        values.add(normalized);
        saveKnownOwnedAreas(values);
    }

    private static synchronized String get(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    private static synchronized double getDouble(String key, double defaultValue) {
        String value = PROPERTIES.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return clampZoomFactor(Double.parseDouble(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static synchronized void put(String key, String value) {
        PROPERTIES.setProperty(key, value != null ? value : "");
        saveProperties();
    }

    private static void saveKnownOwnedAreas(Set<String> values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(value);
        }
        put(KEY_KNOWN_OWNED_AREAS, out.toString());
    }

    private static String normalizeOwnedAreaResref(String areaResref) {
        if (areaResref == null) {
            return "";
        }
        areaResref = areaResref.trim();
        return areaResref.matches("[^\\\\/:*?\"<>|\\s]{2,8}") ? areaResref : "";
    }

    private static File resolveStorageFile() {
        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.trim().isEmpty()) {
            return new File(new File(appData, "J2DArea"), "user-preferences.properties");
        }
        return new File(new File(System.getProperty("user.home"), ".j2darea"), "user-preferences.properties");
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        migrateLegacyPreferences(properties);
        if (STORAGE_FILE.isFile()) {
            try (FileInputStream inputStream = new FileInputStream(STORAGE_FILE)) {
                properties.load(inputStream);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return properties;
    }

    private static void migrateLegacyPreferences(Properties properties) {
        Preferences legacyPrefs = Preferences.userNodeForPackage(UserPreferences.class);
        copyLegacyValue(legacyPrefs, properties, KEY_EXPORT_PREFIX);
        copyLegacyValue(legacyPrefs, properties, KEY_KNOWN_OWNED_AREAS);
        copyLegacyValue(legacyPrefs, properties, KEY_GAME_INSTALL_PATH);
        copyLegacyValue(legacyPrefs, properties, KEY_GOOGLE_AI_API_KEY);
    }

    private static void copyLegacyValue(Preferences legacyPrefs, Properties properties, String key) {
        String legacyValue = legacyPrefs.get(key, null);
        if (legacyValue != null && !legacyValue.trim().isEmpty() && !properties.containsKey(key)) {
            properties.setProperty(key, legacyValue);
        }
    }

    private static synchronized void saveProperties() {
        File parent = STORAGE_FILE.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            parent.mkdirs();
        }
        try (FileOutputStream outputStream = new FileOutputStream(STORAGE_FILE)) {
            PROPERTIES.store(outputStream, "J2DArea user preferences");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static double clampZoomFactor(double zoomFactor) {
        return Math.max(1.01d, Math.min(zoomFactor, 2.0d));
    }
}
