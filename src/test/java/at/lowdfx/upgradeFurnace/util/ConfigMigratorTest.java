package at.lowdfx.upgradeFurnace.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigMigratorTest {

    private static final String DEFAULT_CONFIG = """
            config-version: 2
            language: "en"
            basic:
              server-name: "MyServer"
            holograms:
              enabled: true
            particles:
              enabled: true
              only_when_active: false
            requirements:
              1:
                material: COPPER_INGOT
                amount: 16
                xp_levels: 5
                speed_multiplier: 1.15
                particle: SMOKE
                bonus_chance: 0.0
                bonus_max_items: 0
            """;

    @TempDir
    Path temporaryDirectory;

    @Test
    void migratesValidValuesAndDefaultsInvalidOrMissingValues() throws Exception {
        Path configPath = temporaryDirectory.resolve("config.yml");
        Files.writeString(configPath, """
                language: "de"
                basic:
                  server-name: "CustomServer"
                  customhelp: true
                requirements:
                  1:
                    material: COPPER_INGOT
                    amount: 32
                    xp_levels: 7
                    speed_multiplier: 0.25
                    particle: SMOKE
                    bonus_chance: 0.0
                    bonus_max_items: 0
                """);

        ConfigMigrator.migrate(plugin());

        YamlConfiguration migrated = YamlConfiguration.loadConfiguration(configPath.toFile());
        assertEquals(2, migrated.getInt("config-version"));
        assertEquals("de", migrated.getString("language"));
        assertEquals("CustomServer", migrated.getString("basic.server-name"));
        assertEquals(32, migrated.getInt("requirements.1.amount"));
        assertEquals(1.15, migrated.getDouble("requirements.1.speed_multiplier"));
        assertTrue(migrated.getBoolean("holograms.enabled"));
        assertFalse(migrated.contains("basic.customhelp"));

        try (var files = Files.list(temporaryDirectory)) {
            assertTrue(files.anyMatch(path ->
                    path.getFileName().toString().startsWith("config.yml.backup-v1-")));
        }
    }

    @Test
    void currentConfigKeepsUnknownValuesAndReceivesMissingDefaults() throws Exception {
        Path configPath = temporaryDirectory.resolve("config.yml");
        Files.writeString(configPath, """
                config-version: 2
                language: "de"
                custom-setting: "keep-me"
                """);

        ConfigMigrator.migrate(plugin());

        YamlConfiguration current = YamlConfiguration.loadConfiguration(configPath.toFile());
        assertEquals("de", current.getString("language"));
        assertEquals("keep-me", current.getString("custom-setting"));
        assertEquals("MyServer", current.getString("basic.server-name"));
        assertTrue(current.getBoolean("holograms.enabled"));

        try (var files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(path ->
                    path.getFileName().toString().contains(".backup-")));
        }
    }

    private JavaPlugin plugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(temporaryDirectory.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ConfigMigratorTest"));
        when(plugin.getResource("config.yml")).thenAnswer(ignored ->
                new ByteArrayInputStream(DEFAULT_CONFIG.getBytes(StandardCharsets.UTF_8)));
        return plugin;
    }
}
