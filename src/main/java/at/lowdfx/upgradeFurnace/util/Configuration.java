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

    public static String LANGUAGE;
    public static String BASIC_SERVER_NAME;
    public static boolean BASIC_CUSTOM_HELP;

    public static boolean PARTICLES_ENABLED;
    public static boolean PARTICLES_ONLY_WHEN_ACTIVE;

    // Upgrade requirements from the config.
    public static final Map<Integer, Material> REQUIRE_MATERIAL = new HashMap<>();
    public static final Map<Integer, Integer> REQUIRE_AMOUNT = new HashMap<>();
    public static final Map<Integer, Integer> REQUIRE_XP_LEVELS = new HashMap<>();
    public static final Map<Integer, Double> SPEED_MULTIPLIER = new HashMap<>();
    public static final Map<Integer, Particle> PARTICLE = new HashMap<>();
    public static final Map<Integer, Double> BONUS_CHANCE = new HashMap<>();
    public static final Map<Integer, Integer> BONUS_MAX_ITEMS = new HashMap<>();

    public static void init(@NotNull JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        CONFIG = plugin.getConfig();
        loadValues();
    }

    private static void loadValues() {
        LANGUAGE = CONFIG.getString("language", "en");
        BASIC_SERVER_NAME = CONFIG.getString("basic.server-name", "Server");
        BASIC_CUSTOM_HELP = CONFIG.getBoolean("basic.customhelp", true);

        PARTICLES_ENABLED = CONFIG.getBoolean("particles.enabled", true);
        PARTICLES_ONLY_WHEN_ACTIVE = CONFIG.getBoolean("particles.only_when_active", false);

        REQUIRE_MATERIAL.clear();
        REQUIRE_AMOUNT.clear();
        REQUIRE_XP_LEVELS.clear();
        SPEED_MULTIPLIER.clear();
        PARTICLE.clear();
        BONUS_CHANCE.clear();
        BONUS_MAX_ITEMS.clear();

        ConfigurationSection section = CONFIG.getConfigurationSection("requirements");
        if (section == null) {
            UpgradeFurnace.LOG.warn("The config section 'requirements' was not found.");
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int level = Integer.parseInt(key);
                String path = "requirements." + key + ".";

                String matName = CONFIG.getString(path + "material");
                if (matName == null || matName.isBlank()) {
                    UpgradeFurnace.LOG.warn("No material set for upgrade level {}. Skipping this level.", level);
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(matName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    UpgradeFurnace.LOG.warn("Invalid material '{}' for upgrade level {}. Skipping this level.", matName, level);
                    continue;
                }

                int amount = CONFIG.getInt(path + "amount");
                if (amount <= 0) {
                    UpgradeFurnace.LOG.warn("Invalid amount '{}' for upgrade level {}. The amount must be greater than 0. Skipping this level.", amount, level);
                    continue;
                }

                int xp = CONFIG.getInt(path + "xp_levels", 0);
                if (xp < 0) {
                    UpgradeFurnace.LOG.warn("Invalid XP level cost '{}' for upgrade level {}. Using 0.", xp, level);
                    xp = 0;
                }

                double speed = CONFIG.getDouble(path + "speed_multiplier", level + 1.0);
                if (!Double.isFinite(speed) || speed < 1.0) {
                    UpgradeFurnace.LOG.warn("Invalid speed_multiplier '{}' for upgrade level {}. Using 1.00.", speed, level);
                    speed = 1.0;
                }

                speed = Math.round(speed * 100.0) / 100.0;

                if (speed > 64.99) {
                    UpgradeFurnace.LOG.warn("speed_multiplier '{}' for upgrade level {} is too high. Using 64.99.", speed, level);
                    speed = 64.99;
                }

                String particleName = CONFIG.getString(path + "particle", "SMOKE");
                Particle particle;
                try {
                    particle = Particle.valueOf(particleName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException e) {
                    particle = Particle.SMOKE;
                    UpgradeFurnace.LOG.warn("Invalid particle '{}' for upgrade level {}. Using SMOKE.", particleName, level);
                }

                double bonusChance = CONFIG.getDouble(path + "bonus_chance", 0.0);
                if (!Double.isFinite(bonusChance) || bonusChance < 0.0) {
                    UpgradeFurnace.LOG.warn("Invalid bonus_chance '{}' for upgrade level {}. Using 0.00.", bonusChance, level);
                    bonusChance = 0.0;
                }
                if (bonusChance > 1.0) {
                    UpgradeFurnace.LOG.warn("bonus_chance '{}' for upgrade level {} is too high. Using 1.00.", bonusChance, level);
                    bonusChance = 1.0;
                }
                bonusChance = Math.round(bonusChance * 100.0) / 100.0;

                int bonusMaxItems = CONFIG.getInt(path + "bonus_max_items", 0);
                if (bonusMaxItems < 0) {
                    UpgradeFurnace.LOG.warn("Invalid bonus_max_items '{}' for upgrade level {}. Using 0.", bonusMaxItems, level);
                    bonusMaxItems = 0;
                }

                REQUIRE_MATERIAL.put(level, material);
                REQUIRE_AMOUNT.put(level, amount);
                REQUIRE_XP_LEVELS.put(level, xp);
                SPEED_MULTIPLIER.put(level, speed);
                PARTICLE.put(level, particle);
                BONUS_CHANCE.put(level, bonusChance);
                BONUS_MAX_ITEMS.put(level, bonusMaxItems);
            } catch (NumberFormatException e) {
                UpgradeFurnace.LOG.warn("Invalid upgrade level '{}'. Level keys must be numbers.", key);
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

    public static double getBonusChance(int level) {
        return BONUS_CHANCE.getOrDefault(level, 0.0);
    }

    public static int getBonusMaxItems(int level) {
        return BONUS_MAX_ITEMS.getOrDefault(level, 0);
    }

    public static FileConfiguration get() {
        return CONFIG;
    }
}
