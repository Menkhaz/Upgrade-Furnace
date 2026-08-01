package at.lowdfx.upgradeFurnace.listeners;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import at.lowdfx.upgradeFurnace.services.HologramManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Restores each player's hologram visibility preference after joining.
 */
public final class HologramVisibilityListener implements Listener {

    private final UpgradeFurnace plugin;
    private final HologramManager hologramManager;

    public HologramVisibilityListener(
            UpgradeFurnace plugin,
            HologramManager hologramManager
    ) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyNextTick(event.getPlayer());
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        applyNextTick(event.getPlayer());
    }

    private void applyNextTick(Player player) {
        plugin.getServer().getScheduler().runTask(
                plugin,
                () -> hologramManager.applyVisibilityPreference(player));
    }
}
