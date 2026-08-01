package at.lowdfx.upgradeFurnace.services;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import at.lowdfx.upgradeFurnace.util.Configuration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Furnace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Creates, updates, restores, and removes furnace level holograms.
 */
public final class HologramManager {

    private static final NamespacedKey KEY_HOLOGRAM =
            new NamespacedKey("upgradefurnace", "hologram");
    private static final NamespacedKey KEY_HOLOGRAM_OWNER =
            new NamespacedKey("upgradefurnace", "hologram_owner");
    private static final NamespacedKey KEY_HOLOGRAMS_HIDDEN =
            new NamespacedKey("upgradefurnace", "holograms_hidden");

    private final UpgradeFurnace plugin;

    public HologramManager(UpgradeFurnace plugin) {
        this.plugin = plugin;
    }

    public void setPlayerVisibility(Player player, boolean visible) {
        if (visible) {
            player.getPersistentDataContainer().remove(KEY_HOLOGRAMS_HIDDEN);
        } else {
            player.getPersistentDataContainer()
                    .set(KEY_HOLOGRAMS_HIDDEN, PersistentDataType.BYTE, (byte) 1);
        }

        applyVisibilityPreference(player);
    }

    public boolean isVisibleTo(Player player) {
        return !player.getPersistentDataContainer()
                .has(KEY_HOLOGRAMS_HIDDEN, PersistentDataType.BYTE);
    }

    public void applyVisibilityPreference(Player player) {
        for (ArmorStand hologram : player.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (!hologram.getPersistentDataContainer()
                    .has(KEY_HOLOGRAM_OWNER, PersistentDataType.STRING)) {
                continue;
            }

            if (Configuration.HOLOGRAMS_ENABLED && isVisibleTo(player)) {
                player.showEntity(plugin, hologram);
            } else {
                player.hideEntity(plugin, hologram);
            }
        }
    }

    public void ensureHologram(Furnace furnace, int level) {
        if (!Configuration.HOLOGRAMS_ENABLED) {
            removeHologram(furnace);
            return;
        }

        String ownerKey = ownerKey(furnace);
        PersistentDataContainer furnaceData = furnace.getPersistentDataContainer();
        String hologramId = furnaceData.get(KEY_HOLOGRAM, PersistentDataType.STRING);

        if (hologramId != null) {
            try {
                Entity entity = furnace.getWorld().getEntity(UUID.fromString(hologramId));
                if (entity instanceof ArmorStand armorStand
                        && ownerKey.equals(armorStand.getPersistentDataContainer()
                                .get(KEY_HOLOGRAM_OWNER, PersistentDataType.STRING))) {
                    armorStand.customName(hologramName(level));
                    removeOwnedHolograms(furnace, armorStand.getUniqueId());
                    applyVisibilityToOnlinePlayers(armorStand);
                    return;
                }
                if (entity != null) {
                    entity.remove();
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid legacy UUID; a replacement hologram is created below.
            }
        }

        removeOwnedHolograms(furnace, null);
        spawnHologram(furnace, level);
    }

    public void removeHologram(Furnace furnace) {
        PersistentDataContainer furnaceData = furnace.getPersistentDataContainer();
        String hologramId = furnaceData.get(KEY_HOLOGRAM, PersistentDataType.STRING);

        if (hologramId != null) {
            try {
                Entity entity = furnace.getWorld().getEntity(UUID.fromString(hologramId));
                if (entity != null) {
                    entity.remove();
                }
            } catch (IllegalArgumentException ignored) {
                // Invalid legacy UUID; nearby owned holograms are still removed below.
            }
        }

        removeOwnedHolograms(furnace, null);
        furnaceData.remove(KEY_HOLOGRAM);
        furnace.update();
    }

    private void spawnHologram(Furnace furnace, int level) {
        Location location = furnace.getBlock().getLocation().add(0.5, 1.2, 0.5);
        ArmorStand hologram =
                (ArmorStand) furnace.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        hologram.customName(hologramName(level));
        hologram.setCustomNameVisible(true);
        hologram.setGravity(false);
        hologram.setVisible(false);
        hologram.setMarker(true);
        hologram.getPersistentDataContainer()
                .set(KEY_HOLOGRAM_OWNER, PersistentDataType.STRING, ownerKey(furnace));
        applyVisibilityToOnlinePlayers(hologram);

        furnace.getPersistentDataContainer()
                .set(KEY_HOLOGRAM, PersistentDataType.STRING, hologram.getUniqueId().toString());
        furnace.update();

        if (Configuration.PARTICLES_ENABLED) {
            Location center = furnace.getBlock().getLocation().add(0.5, 0.5, 0.5);
            Particle particle = Configuration.getParticle(level);
            furnace.getWorld().spawnParticle(particle, center, 20, 1.0, 0.5, 1.0, 0.05);
        }
    }

    private void removeOwnedHolograms(Furnace furnace, UUID hologramToKeep) {
        String ownerKey = ownerKey(furnace);
        Location location = furnace.getBlock().getLocation().add(0.5, 1.2, 0.5);

        for (Entity entity : furnace.getWorld().getNearbyEntities(location, 0.75, 0.75, 0.75)) {
            if (!(entity instanceof ArmorStand armorStand)) {
                continue;
            }
            if (hologramToKeep != null && hologramToKeep.equals(armorStand.getUniqueId())) {
                continue;
            }

            String owner = armorStand.getPersistentDataContainer()
                    .get(KEY_HOLOGRAM_OWNER, PersistentDataType.STRING);
            if (ownerKey.equals(owner)) {
                armorStand.remove();
            }
        }
    }

    private String ownerKey(Furnace furnace) {
        Location location = furnace.getBlock().getLocation();
        return location.getWorld().getUID()
                + ":" + location.getBlockX()
                + ":" + location.getBlockY()
                + ":" + location.getBlockZ();
    }

    private Component hologramName(int level) {
        return Component.text("Level " + level, NamedTextColor.RED);
    }

    private void applyVisibilityToOnlinePlayers(ArmorStand hologram) {
        for (Player player : hologram.getWorld().getPlayers()) {
            if (isVisibleTo(player)) {
                player.showEntity(plugin, hologram);
            } else {
                player.hideEntity(plugin, hologram);
            }
        }
    }
}
