package at.lowdfx.upgradeFurnace.util;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Migrates config.yml onto the latest bundled schema while preserving valid settings.
 */
public final class ConfigMigrator {

    private static final String FILE_NAME = "config.yml";
    private static final int CURRENT_VERSION = 2;
    private static final DateTimeFormatter BACKUP_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ConfigMigrator() {
    }

    public static void migrate(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), FILE_NAME);
        if (!configFile.exists()) {
            plugin.saveResource(FILE_NAME, false);
            return;
        }

        YamlConfiguration defaults = loadDefaults(plugin);
        if (defaults == null) {
            return;
        }

        YamlConfiguration current = loadFile(plugin, configFile);
        if (current == null) {
            Path backup = createBackup(plugin, configFile.toPath(), "invalid");
            if (backup != null && saveAtomically(plugin, defaults, configFile.toPath())) {
                plugin.getLogger().warning(
                        "Invalid config.yml was replaced with defaults. Backup: " + backup.getFileName());
            }
            return;
        }

        int existingVersion = current.getInt("config-version", 1);
        if (existingVersion > CURRENT_VERSION) {
            plugin.getLogger().warning(
                    "config.yml uses version " + existingVersion
                            + ", but this plugin supports version " + CURRENT_VERSION
                            + ". The newer configuration was left unchanged.");
            return;
        }

        if (existingVersion == CURRENT_VERSION) {
            if (mergeMissingValues(current, defaults)
                    && saveAtomically(plugin, current, configFile.toPath())) {
                plugin.getLogger().info("Missing config.yml values were restored from defaults.");
            }
            return;
        }

        Path backup = createBackup(plugin, configFile.toPath(), String.valueOf(existingVersion));
        if (backup == null) {
            plugin.getLogger().severe("Config migration was cancelled because a backup could not be created.");
            return;
        }

        migrateKnownValues(plugin, current, defaults);
        defaults.set("config-version", CURRENT_VERSION);

        if (saveAtomically(plugin, defaults, configFile.toPath())) {
            plugin.getLogger().info(
                    "Migrated config.yml from version " + existingVersion
                            + " to " + CURRENT_VERSION
                            + ". Backup: " + backup.getFileName());
        }
    }

    private static YamlConfiguration loadDefaults(JavaPlugin plugin) {
        try (InputStream stream = plugin.getResource(FILE_NAME)) {
            if (stream == null) {
                plugin.getLogger().severe("Bundled config.yml was not found.");
                return null;
            }

            YamlConfiguration defaults = new YamlConfiguration();
            defaults.options().indent(2).parseComments(true);
            defaults.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return defaults;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Could not load bundled config.yml: " + e.getMessage());
            return null;
        }
    }

    private static YamlConfiguration loadFile(JavaPlugin plugin, File file) {
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.options().indent(2).parseComments(true);
            config.load(file);
            return config;
        } catch (IOException | InvalidConfigurationException e) {
            plugin.getLogger().severe("Could not load config.yml: " + e.getMessage());
            return null;
        }
    }

    private static void migrateKnownValues(
            JavaPlugin plugin,
            YamlConfiguration oldConfig,
            YamlConfiguration newConfig
    ) {
        for (String path : newConfig.getKeys(true)) {
            if ("config-version".equals(path)
                    || newConfig.isConfigurationSection(path)
                    || !oldConfig.contains(path)) {
                continue;
            }

            Object oldValue = oldConfig.get(path);
            Object defaultValue = newConfig.get(path);
            if (isValidValue(path, oldValue, defaultValue)) {
                newConfig.set(path, oldValue);
            } else {
                plugin.getLogger().warning(
                        "Config value '" + path + "' was not migrated because it is invalid. Using the default.");
            }
        }
    }

    private static boolean mergeMissingValues(
            YamlConfiguration current,
            YamlConfiguration defaults
    ) {
        boolean changed = false;
        for (String path : defaults.getKeys(true)) {
            if (!current.contains(path)) {
                current.set(path, defaults.get(path));
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isValidValue(String path, Object value, Object defaultValue) {
        if (value == null || defaultValue == null) {
            return false;
        }

        if (defaultValue instanceof Number) {
            if (!(value instanceof Number number)) {
                return false;
            }

            double numericValue = number.doubleValue();
            if (!Double.isFinite(numericValue)) {
                return false;
            }

            if (path.endsWith(".amount")) {
                return numericValue > 0 && numericValue == Math.rint(numericValue);
            }
            if (path.endsWith(".xp_levels") || path.endsWith(".bonus_max_items")) {
                return numericValue >= 0 && numericValue == Math.rint(numericValue);
            }
            if (path.endsWith(".speed_multiplier")) {
                return numericValue >= 1.0 && numericValue <= 64.99;
            }
            if (path.endsWith(".bonus_chance")) {
                return numericValue >= 0.0 && numericValue <= 1.0;
            }
            return true;
        }

        if (defaultValue instanceof Boolean) {
            return value instanceof Boolean;
        }

        if (defaultValue instanceof String && value instanceof String stringValue) {
            if ("language".equals(path)) {
                return !stringValue.isBlank();
            }
            if (path.endsWith(".material")) {
                return isValidMaterial(stringValue);
            }
            if (path.endsWith(".particle")) {
                return isValidParticle(stringValue);
            }
            return true;
        }

        return defaultValue.getClass().isInstance(value);
    }

    private static boolean isValidMaterial(String value) {
        try {
            Material.valueOf(value.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isValidParticle(String value) {
        try {
            Particle.valueOf(value.toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static Path createBackup(JavaPlugin plugin, Path configPath, String version) {
        String timestamp = LocalDateTime.now().format(BACKUP_TIMESTAMP);
        Path backup = configPath.resolveSibling(
                FILE_NAME + ".backup-v" + version + "-" + timestamp);
        try {
            return Files.copy(configPath, backup, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not back up config.yml: " + e.getMessage());
            return null;
        }
    }

    private static boolean saveAtomically(
            JavaPlugin plugin,
            YamlConfiguration config,
            Path destination
    ) {
        Path temporary = null;
        try {
            temporary = Files.createTempFile(destination.getParent(), "config-", ".tmp");
            config.save(temporary.toFile());
            try {
                Files.move(
                        temporary,
                        destination,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save migrated config.yml: " + e.getMessage());
            return false;
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }
}
