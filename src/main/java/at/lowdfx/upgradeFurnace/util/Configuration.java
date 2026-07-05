package at.lowdfx.upgradeFurnace.util;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Configuration {
    public static FileConfiguration CONFIG;

    public static String BASIC_SERVER_NAME;
    public static boolean BASIC_CUSTOM_HELP;

    public static boolean PARTICLES_ENABLED;
    public static boolean PARTICLES_ONLY_WHEN_ACTIVE;

    // Upgrade-Anforderungen aus der Config
    public static final Map<Integer, Material> REQUIRE_MATERIAL = new HashMap<>();
    public static final Map<Integer, Integer> REQUIRE_AMOUNT = new HashMap<>();
    public static final Map<Integer, Integer> REQUIRE_XP_LEVELS = new HashMap<>();
    public static final Map<Integer, Double> SPEED_MULTIPLIER = new HashMap<>();
    public static final Map<Integer, Particle> PARTICLE = new HashMap<>();

    public static void init(@NotNull JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        CONFIG = plugin.getConfig();
        loadValues();
    }

    private static void loadValues() {
        BASIC_SERVER_NAME = CONFIG.getString("basic.server-name", "Server");
        BASIC_CUSTOM_HELP = CONFIG.getBoolean("basic.customhelp", true);

        PARTICLES_ENABLED = CONFIG.getBoolean("particles.enabled", true);
        PARTICLES_ONLY_WHEN_ACTIVE = CONFIG.getBoolean("particles.only_when_active", true);

        REQUIRE_MATERIAL.clear();
        REQUIRE_AMOUNT.clear();
        REQUIRE_XP_LEVELS.clear();
        SPEED_MULTIPLIER.clear();
        PARTICLE.clear();

        ConfigurationSection section = CONFIG.getConfigurationSection("requirements");
        if (section == null) {
            UpgradeFurnace.LOG.warn("Der Config-Bereich 'requirements' wurde nicht gefunden.");
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                String path = "requirements." + key + ".";

                String matName = CONFIG.getString(path + "material");
                if (matName == null || matName.isBlank()) {
                    UpgradeFurnace.LOG.warn("Kein Material für Upgrade-Level {} gesetzt. Dieses Level wird übersprungen.", level);
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    UpgradeFurnace.LOG.warn("Ungültiges Material '{}' für Upgrade-Level {}. Dieses Level wird übersprungen.", matName, level);
                    continue;
                }

                int amount = CONFIG.getInt(path + "amount");
                if (amount <= 0) {
                    UpgradeFurnace.LOG.warn("Ungültige Anzahl '{}' für Upgrade-Level {}. Die Anzahl muss größer als 0 sein. Dieses Level wird übersprungen.", amount, level);
                    continue;
                }

                int xp = CONFIG.getInt(path + "xp_levels", 0);
                if (xp < 0) {
                    UpgradeFurnace.LOG.warn("Ungültige XP-Level '{}' für Upgrade-Level {}. Es wird 0 verwendet.", xp, level);
                    xp = 0;
                }

                double speed = CONFIG.getDouble(path + "speed_multiplier", level + 1.0);
                if (!Double.isFinite(speed) || speed < 1.0) {
                    UpgradeFurnace.LOG.warn("Ungültiger speed_multiplier '{}' für Upgrade-Level {}. Es wird 1.00 verwendet.", speed, level);
                    speed = 1.0;
                }

                speed = Math.round(speed * 100.0) / 100.0;

                if (speed > 64.99) {
                    UpgradeFurnace.LOG.warn("speed_multiplier '{}' für Upgrade-Level {} ist zu hoch. Es wird 64.99 verwendet.", speed, level);
                    speed = 64.99;
                }

                String particleName = CONFIG.getString(path + "particle", "SMOKE");
                Particle particle;
                try {
                    particle = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    particle = Particle.SMOKE;
                    UpgradeFurnace.LOG.warn("Ungültiger Partikel '{}' für Upgrade-Level {}. Es wird SMOKE verwendet.", particleName, level);
                }

                REQUIRE_MATERIAL.put(level, material);
                REQUIRE_AMOUNT.put(level, amount);
                REQUIRE_XP_LEVELS.put(level, xp);
                SPEED_MULTIPLIER.put(level, speed);
                PARTICLE.put(level, particle);
            } catch (NumberFormatException e) {
                UpgradeFurnace.LOG.warn("Ungültiges Upgrade-Level '{}'. Level-Schlüssel müssen Zahlen sein.", key);
            }
        }
    }

    public static Material getRequirementMaterial(int level) {
        return REQUIRE_MATERIAL.get(level);
    }

    public static int getRequirementAmount(int level) {
        return REQUIRE_AMOUNT.getOrDefault(level, 0);
    }

    public static int getRequirementXpLevels(int level) {
        return REQUIRE_XP_LEVELS.getOrDefault(level, 0);
    }

    public static double getSpeedMultiplier(int level) {
        return SPEED_MULTIPLIER.getOrDefault(level, 1.0);
    }

    public static Particle getParticle(int level) {
        return PARTICLE.getOrDefault(level, Particle.SMOKE);
    }

    public static FileConfiguration get() {
        return CONFIG;
    }
}