package at.lowdfx.upgradeFurnace.services;

import at.lowdfx.upgradeFurnace.util.Configuration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/** Loads the public API only when mcMMO is installed, without bundling mcMMO. */
public final class McMmoIntegration implements SmeltingLevelProvider {
    private final JavaPlugin plugin;
    private Plugin mcMmo;
    private Method getLevel;

    public McMmoIntegration(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        mcMmo = null;
        getLevel = null;
        if (!Configuration.MCMMO_SMELTING) {
            return;
        }
        Plugin candidate = plugin.getServer().getPluginManager().getPlugin("mcMMO");
        if (candidate != null && candidate.isEnabled()) {
            try {
                Class<?> api = Class.forName("com.gmail.nossr50.api.ExperienceAPI", true,
                        candidate.getClass().getClassLoader());
                getLevel = api.getMethod("getLevel", Player.class, String.class);
                mcMmo = candidate;
                plugin.getLogger().info("Upgrade progression: mcMMO Smelting (no vanilla XP cost).");
                return;
            } catch (ReflectiveOperationException | LinkageError e) {
                plugin.getLogger().warning("Could not load the mcMMO Smelting API: " + e);
            }
        }
        plugin.getLogger().warning("mcMMO Smelting progression is configured but unavailable. "
                + "Falling back to configured vanilla XP costs.");
    }

    @Override
    public boolean isAvailable() {
        return getLevel != null && mcMmo != null && mcMmo.isEnabled();
    }

    @Override
    public int getLevel(Player player) {
        try {
            return Math.max(0, ((Number) getLevel.invoke(null, player, "SMELTING")).intValue());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            plugin.getLogger().warning("Could not read mcMMO Smelting level for "
                    + player.getName() + ": " + e);
            throw new IllegalStateException("mcMMO player skill is unavailable", e);
        }
    }
}
