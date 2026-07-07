package at.lowdfx.upgradeFurnace.util;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;

public class UpdaterJoinListener implements Listener {

    private final JavaPlugin plugin;
    private final String updateUrl;
    private final String downloadLink;

    /**
     * Creates a new updater join listener.
     *
     * @param plugin       the main plugin
     * @param updateUrl    URL where the latest version is available as text
     * @param downloadLink download link for the new version
     */
    public UpdaterJoinListener(JavaPlugin plugin, String updateUrl, String downloadLink) {
        this.plugin = plugin;
        this.updateUrl = updateUrl;
        this.downloadLink = downloadLink;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.isOp()) return; // Only OPs should be notified.

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = URI.create(updateUrl).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String latestVersion = reader.readLine().trim();
                reader.close();

                String currentVersion = plugin.getPluginMeta().getVersion();
                if (!currentVersion.equals(latestVersion)) {
                    // New version available; send the message on the main thread.
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Messages.component("update.available", Map.of(
                                        "version", latestVersion,
                                        "link", downloadLink
                                ))
                        ));
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().warning(Messages.text("update.check-failed", Map.of(
                        "player", player.getName(),
                        "error", String.valueOf(e.getMessage())
                )));
            }
        });
    }
}
