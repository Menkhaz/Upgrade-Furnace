package at.lowdfx.upgradeFurnace.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public final class FileUpdater {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Compares an existing YAML file with the bundled default resource and adds missing entries.
     *
     * @param plugin   the plugin instance
     * @param fileName YAML file name, for example "config.yml"
     */
    public static void updateYaml(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        YamlConfiguration currentConfig = YamlConfiguration.loadConfiguration(file);

        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
            mergeDefaultsYaml(currentConfig, defaultConfig);
            try {
                currentConfig.save(file);
                plugin.getLogger().info(fileName + " was updated.");
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save " + fileName + ": " + e.getMessage());
            }
        } else {
            plugin.getLogger().warning("Default configuration file " + fileName + " was not found.");
        }
    }

    private static void mergeDefaultsYaml(YamlConfiguration current, YamlConfiguration defaults) {
        Set<String> keys = defaults.getKeys(true);
        for (String key : keys) {
            if (!current.contains(key)) {
                current.set(key, defaults.get(key));
            }
        }
    }

    /**
     * Compares an existing JSON file with the bundled default resource and adds missing entries.
     *
     * @param plugin   the plugin instance
     * @param fileName JSON file name, for example "permissions.json"
     */
    public static void updateJson(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        JsonObject currentJson = null;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                currentJson = GSON.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                plugin.getLogger().severe("Error reading " + fileName + ": " + e.getMessage());
            }
        }
        if (currentJson == null) {
            currentJson = new JsonObject();
        }

        InputStream defStream = plugin.getResource(fileName);
        if (defStream != null) {
            JsonObject defaultJson = null;
            try (InputStreamReader reader = new InputStreamReader(defStream, StandardCharsets.UTF_8)) {
                defaultJson = GSON.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                plugin.getLogger().severe("Error reading default " + fileName + ": " + e.getMessage());
            }
            if (defaultJson != null) {
                mergeDefaultsJson(currentJson, defaultJson);
                try (FileWriter writer = new FileWriter(file)) {
                    GSON.toJson(currentJson, writer);
                    plugin.getLogger().info(fileName + " was updated.");
                } catch (IOException e) {
                    plugin.getLogger().severe("Error saving " + fileName + ": " + e.getMessage());
                }
            }
        } else {
            // No warning is logged when no default resource exists.
            // plugin.getLogger().warning("Default resource for " + fileName + " was not found.");
        }
    }

    private static void mergeDefaultsJson(JsonObject current, JsonObject defaults) {
        for (Map.Entry<String, JsonElement> entry : defaults.entrySet()) {
            if (!current.has(entry.getKey())) {
                current.add(entry.getKey(), entry.getValue());
            } else if (entry.getValue().isJsonObject() && current.get(entry.getKey()).isJsonObject()) {
                // Recursively merge when both values are JSON objects.
                mergeDefaultsJson(current.getAsJsonObject(entry.getKey()), entry.getValue().getAsJsonObject());
            }
        }
    }
}
