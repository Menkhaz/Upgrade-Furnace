package at.lowdfx.upgradeFurnace;

import at.lowdfx.metrics.Metrics;
import at.lowdfx.upgradeFurnace.commands.UpgradeCommands;
import at.lowdfx.upgradeFurnace.util.*;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import xyz.xenondevs.invui.InvUI;

import java.lang.reflect.Method;
import java.nio.file.Path;

@SuppressWarnings({ "UnstableApiUsage", "ResultOfMethodCallIgnored" })
public final class UpgradeFurnace extends JavaPlugin {

    public static Logger LOG;
    public static UpgradeFurnace PLUGIN;
    public static Path PLUGIN_DIR;
    public static Path DATA_DIR;
    public static FurnaceParticleManager PARTICLE_MANAGER;



    @Override
    public void onEnable() {
        // Standardkonfigurationen und Dateien werden gemerged
        FileUpdater.updateYaml(this, "config.yml");
        FileUpdater.updateJson(this, "permissions.json");
        LOG = getSLF4JLogger();
        PLUGIN = this;
        PLUGIN_DIR = getDataPath();

        DATA_DIR = PLUGIN_DIR.resolve("data");
        DATA_DIR.toFile().mkdirs();

        InvUI.getInstance().setPlugin(this);
        Configuration.init(this);

        Perms.loadPermissions();

        // Start particle manager if enabled
        if (Configuration.PARTICLES_ENABLED) {
            LOG.info("Starting FurnaceParticleManager...");
            PARTICLE_MANAGER = new FurnaceParticleManager();
            PARTICLE_MANAGER.start();
            FurnaceLoader.registerLoadedFurnaces();

            getServer().getPluginManager().registerEvents(new FurnaceLoader(), this);
        }

        // Plugin Updater
        String updateUrl = "https://raw.githubusercontent.com/LowdFX/Upgrade-Furnace/refs/heads/main/update.txt";
        String downloadLink = "https://www.spigotmc.org/resources/upgrade-furnace.124315/";
        getServer().getPluginManager().registerEvents(new UpdaterJoinListener(this, updateUrl, downloadLink), this);
        getServer().getPluginManager().registerEvents(new UpgradeCommands(), this);


        // bStats starten
        int pluginId = 25566;
        Metrics metrics = new Metrics(this, pluginId);
        metrics.addCustomChart(new Metrics.SimplePie("language", () -> getConfig().getString("language")));


        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            registrar.register(UpgradeCommands.furnaceCommand(), "Upgrade einen Ofen für eine schnellere Produktion und im letzten Level zufällig mehr Ertrag.");

            Plugin lowd = getServer()
                    .getPluginManager()
                    .getPlugin("LowdFX"); // genau so, wie es in der Konsole steht

            if (lowd != null && Configuration.BASIC_CUSTOM_HELP) {
                try {
                    // 2) Lade die API‐Klasse aus dem ClassLoader von LowdFX
                    ClassLoader loader = lowd.getClass().getClassLoader();
                    Class<?> api = loader.loadClass("at.lowdfx.lowdfx.util.LowdFXAPI");

                    // 3) Hole die Methode registerHelpEntry(...)
                    Method reg = api.getMethod(
                            "registerHelpEntry",
                            String.class,
                            Component.class,
                            Component.class,
                            Component.class,
                            String.class,
                            String.class
                    );

                    // 4) Rufe sie statisch auf
                    reg.invoke(
                            null,                            // static
                            "upgrade",               // command
                            MiniMessage.miniMessage().deserialize("Ofen upgraden"), // usage
                            MiniMessage.miniMessage().deserialize("<gray>Mit diesem Befehl kannst du deinen Ofen upgraden für eine schnellere Produktion und im letzten Level für zufällig mehr Ertrag.<newline></gray>" +
                                    "<yellow>· /upgrade furnace</yellow>"),
                            null,                            // adminDetailed
                            "upgradefurnace.upgrade.furnace",        // permission
                            null                             // adminPermission
                    );
                } catch (ReflectiveOperationException e) {
                    getLogger().warning("Konnte LowdFXAPI nicht aufrufen: " + e.getMessage());
                }
            }

        });

        LOG.info("UpgradeFurnace Plugin gestartet!");

    }


    @Override
    public void onDisable() {
        // Partikel-Manager stoppen
        if (PARTICLE_MANAGER != null) {
            PARTICLE_MANAGER.stop();
        }
        LOG.info("UpgradeFurnace Plugin deaktiviert!");
    }

    public static @NotNull Component serverMessage(@NotNull Component message) {
        return Component.text(Configuration.BASIC_SERVER_NAME, NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text(" >> ", NamedTextColor.GRAY))
                .append(message.decoration(TextDecoration.BOLD, false));
    }


}
