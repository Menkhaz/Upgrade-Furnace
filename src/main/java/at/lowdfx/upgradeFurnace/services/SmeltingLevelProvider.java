package at.lowdfx.upgradeFurnace.services;

import org.bukkit.entity.Player;

/** Optional skill integration; levels are read, never spent. */
public interface SmeltingLevelProvider {
    boolean isAvailable();

    int getLevel(Player player);
}
