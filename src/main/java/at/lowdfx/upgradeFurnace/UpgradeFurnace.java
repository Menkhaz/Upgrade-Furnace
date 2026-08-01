package at.lowdfx.upgradeFurnace;

import at.lowdfx.upgradeFurnace.commands.UpgradeCommand;
import at.lowdfx.upgradeFurnace.commands.FurnaceHelpTopic;
import at.lowdfx.upgradeFurnace.listeners.FurnacePersistenceListener;
import at.lowdfx.upgradeFurnace.listeners.FurnaceSmeltListener;
import at.lowdfx.upgradeFurnace.listeners.HologramVisibilityListener;
import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService;
import at.lowdfx.upgradeFurnace.services.HologramManager;
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

    private FurnaceLoader furnaceLoader;

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
        getServer().getHelpMap().addTopic(new FurnaceHelpTopic());

        // Start particle manager if enabled
        PARTICLE_MANAGER = new FurnaceParticleManager();
        if (Configuration.PARTICLES_ENABLED) {
            LOG.info("Starting FurnaceParticleManager...");
            PARTICLE_MANAGER.start();
        }

        HologramManager hologramManager = new HologramManager(this);
        FurnaceUpgradeService upgradeService =
                new FurnaceUpgradeService(hologramManager, PARTICLE_MANAGER);
        furnaceLoader =
                new FurnaceLoader(upgradeService, hologramManager, PARTICLE_MANAGER);
        UpgradeCommand upgradeCommand =
                new UpgradeCommand(this, upgradeService, hologramManager);

        furnaceLoader.registerLoadedFurnaces();
        getServer().getPluginManager().registerEvents(furnaceLoader, this);
        getServer().getPluginManager().registerEvents(
                new FurnaceSmeltListener(upgradeService), this);
        getServer().getPluginManager().registerEvents(
                new FurnacePersistenceListener(
                        upgradeService, hologramManager, PARTICLE_MANAGER),
                this);
        if (Configuration.HOLOGRAMS_ENABLED) {
            getServer().getPluginManager().registerEvents(
                    new HologramVisibilityListener(this, hologramManager),
                    this);
        }

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            registrar.register(upgradeCommand.command(), Messages.text("help.command-description"));
            registrar.register(
                    upgradeCommand.upgradeShortcut(),
                    Messages.text("help.upgrade-description"));
        });

        LOG.info("UpgradeFurnace plugin enabled!");
    }

    public void reloadPluginSettings() {
        ConfigMigrator.migrate(this);
        Configuration.reload(this);
        Messages.init(this);

        if (Configuration.PARTICLES_ENABLED) {
            PARTICLE_MANAGER.start();
        } else {
            PARTICLE_MANAGER.stop();
        }

        furnaceLoader.registerLoadedFurnaces();
        LOG.info("UpgradeFurnace configuration reloaded.");
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
