package at.lowdfx.upgradeFurnace;

import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService;
import at.lowdfx.upgradeFurnace.services.HologramManager;
import at.lowdfx.upgradeFurnace.util.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public final class FurnaceLoader implements Listener {

    private final FurnaceUpgradeService upgradeService;
    private final HologramManager hologramManager;
    private final FurnaceParticleManager particleManager;

    public FurnaceLoader(
            FurnaceUpgradeService upgradeService,
            HologramManager hologramManager,
            FurnaceParticleManager particleManager
    ) {
        this.upgradeService = upgradeService;
        this.hologramManager = hologramManager;
        this.particleManager = particleManager;
    }

    public void registerLoadedFurnaces() {
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                registerFurnacesInChunk(chunk);
            }
        }
    }

    private void registerFurnacesInChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof Furnace furnace)) {
                continue;
            }

            int level = upgradeService.sanitizeLevel(furnace);
            if (level > 0) {
                hologramManager.ensureHologram(furnace, level);
                if (particleManager != null && Configuration.PARTICLES_ENABLED) {
                    particleManager.registerFurnace(furnace.getLocation(), level);
                }
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        registerFurnacesInChunk(event.getChunk());
    }
}
