package at.lowdfx.upgradeFurnace.util;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class Configuration {
    public static FileConfiguration CONFIG;

    public static String BASIC_SERVER_NAME;
    public static boolean BASIC_CUSTOM_HELP;

    public static boolean PARTICLES_ENABLED;
    public static boolean PARTICLES_ONLY_WHEN_ACTIVE;

    // Upgrade requirements loaded from the config
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

        // Load upgrade requirements
        REQUIRE_MATERIAL.clear();
        REQUIRE_AMOUNT.clear();
        REQUIRE_XP_LEVELS.clear();
        SPEED_MULTIPLIER.clear();
        PARTICLE.clear();

        ConfigurationSection section = CONFIG.getConfigurationSection("requirements");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    int level = Integer.parseInt(key);
                    String matName = CONFIG.getString("requirements." + key + ".material");
                    int amount = CONFIG.getInt("requirements." + key + ".amount");
                    int xp = CONFIG.getInt("requirements." + key + ".xp_levels", 0);
                    double speed = CONFIG.getDouble("requirements." + key + ".speed_multiplier", level + 1.0);
                    String particleName = CONFIG.getString("requirements." + key + ".particle", "SMOKE");

                    REQUIRE_MATERIAL.put(level, Material.valueOf(matName.toUpperCase()));
                    REQUIRE_AMOUNT.put(level, amount);
                    REQUIRE_XP_LEVELS.put(level, xp);
                    SPEED_MULTIPLIER.put(level, speed);

                    try {
                        PARTICLE.put(level, Particle.valueOf(particleName.toUpperCase()));
                    } catch (IllegalArgumentException e) {
                        PARTICLE.put(level, Particle.SMOKE);
                        UpgradeFurnace.LOG.warn("Invalid particle '{}' for level {}, using SMOKE instead.", particleName, level);
                    }
                } catch (Exception ignored) {
                    // Skip invalid entries
                }
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