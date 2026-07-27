package at.lowdfx.upgradeFurnace;

import at.lowdfx.metrics.Metrics;
import at.lowdfx.upgradeFurnace.commands.UpgradeCommands;
import at.lowdfx.upgradeFurnace.util.ConfigMigrator;
import at.lowdfx.upgradeFurnace.util.Configuration;
import at.lowdfx.upgradeFurnace.util.FileUpdater;
import at.lowdfx.upgradeFurnace.util.Messages;
import at.lowdfx.upgradeFurnace.util.Perms;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Path;

@SuppressWarnings("UnstableApiUsage")
public final class UpgradeFurnace extends JavaPlugin {
    public static Logger LOG;
    public static UpgradeFurnace PLUGIN;
    public static Path PLUGIN_DIR;
    public static FurnaceParticleManager PARTICLE_MANAGER;

    @Override
    public void onEnable() {
        // Migrate config.yml and merge defaults into support files.
        ConfigMigrator.migrate(this);
        FileUpdater.updateJson(this, "permissions.json");
        LOG = getSLF4JLogger();
        PLUGIN = this;
        PLUGIN_DIR = getDataPath();

        Configuration.init(this);
        Messages.init(this);

        Perms.loadPermissions();

        // Start particle manager if enabled
        if (Configuration.PARTICLES_ENABLED) {
            LOG.info("Starting FurnaceParticleManager...");
            PARTICLE_MANAGER = new FurnaceParticleManager();
            PARTICLE_MANAGER.start();
        }
        FurnaceLoader.registerLoadedFurnaces();
        getServer().getPluginManager().registerEvents(new FurnaceLoader(), this);
        getServer().getPluginManager().registerEvents(new UpgradeCommands(), this);

        // Start bStats.
        int pluginId = 25566;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new Metrics.SimplePie("language", () -> getConfig().getString("language")));

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            registrar.register(UpgradeCommands.furnaceCommand(), Messages.text("help.command-description"));
        });

        LOG.info("UpgradeFurnace plugin enabled!");
    }

    @Override
    public void onDisable() {
        // Stop the particle manager.
        if (PARTICLE_MANAGER != null) {
            PARTICLE_MANAGER.stop();
        }
        LOG.info("UpgradeFurnace plugin disabled!");
    }

    public static @NotNull Component serverMessage(@NotNull Component message) {
        return Component.text(Configuration.BASIC_SERVER_NAME, NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" >> ", NamedTextColor.GRAY))
                .append(message.decoration(TextDecoration.BOLD, false));
    }
}
