package at.lowdfx.upgradeFurnace;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class FurnaceLoader implements Listener {

    private static final NamespacedKey KEY_LEVEL =
            new NamespacedKey("upgradefurnace", "level");

    public static void registerLoadedFurnaces() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                registerFurnacesInChunk(chunk);
            }
        }
    }

    private static void registerFurnacesInChunk(Chunk chunk) {
        if (UpgradeFurnace.PARTICLE_MANAGER == null) return;

        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Furnace furnace)) continue;

            int level = furnace.getPersistentDataContainer()
                    .getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);

            if (level > 0) {
                UpgradeFurnace.PARTICLE_MANAGER.registerFurnace(furnace.getLocation(), level);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        registerFurnacesInChunk(event.getChunk());
    }
}