package at.lowdfx.upgradeFurnace.listeners;

import at.lowdfx.upgradeFurnace.FurnaceParticleManager;
import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService;
import at.lowdfx.upgradeFurnace.services.HologramManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

/**
 * Preserves furnace upgrades when upgraded blocks are broken and placed.
 */
public final class FurnacePersistenceListener implements Listener {

    private final FurnaceUpgradeService upgradeService;
    private final HologramManager hologramManager;
    private final FurnaceParticleManager particleManager;

    public FurnacePersistenceListener(
            FurnaceUpgradeService upgradeService,
            HologramManager hologramManager,
            FurnaceParticleManager particleManager
    ) {
        this.upgradeService = upgradeService;
        this.hologramManager = hologramManager;
        this.particleManager = particleManager;
    }

    @EventHandler
    public void onFurnaceBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Furnace furnace)) {
            return;
        }

        int level = upgradeService.getLevel(furnace);
        if (level <= 0) {
            return;
        }

        hologramManager.removeHologram(furnace);
        if (particleManager != null) {
            particleManager.unregisterFurnace(furnace.getLocation());
        }
        event.setDropItems(false);
        dropContentsAndFurnace(block, furnace, level);
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        preserveExplodedFurnaces(event.blockList());
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        preserveExplodedFurnaces(event.blockList());
    }

    private void preserveExplodedFurnaces(java.util.List<Block> explodedBlocks) {
        for (Block block : new ArrayList<>(explodedBlocks)) {
            if (!(block.getState() instanceof Furnace furnace)) {
                continue;
            }

            int level = upgradeService.getLevel(furnace);
            if (level <= 0) {
                continue;
            }

            explodedBlocks.remove(block);
            hologramManager.removeHologram(furnace);
            if (particleManager != null) {
                particleManager.unregisterFurnace(furnace.getLocation());
            }
            dropContentsAndFurnace(block, furnace, level);
            block.setType(Material.AIR, false);
        }
    }

    private void dropContentsAndFurnace(Block block, Furnace furnace, int level) {
        for (ItemStack content : furnace.getInventory().getContents()) {
            if (content != null && !content.getType().isAir()) {
                block.getWorld().dropItemNaturally(block.getLocation(), content);
            }
        }

        Material furnaceType = block.getType();
        ItemStack dropped = upgradeService.createUpgradedFurnaceItem(furnaceType, level);
        block.getWorld().dropItemNaturally(block.getLocation(), dropped);
    }

    @EventHandler
    public void onFurnacePlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (!(block.getState() instanceof Furnace furnace)) {
            return;
        }

        int level = upgradeService.getStoredLevel(event.getItemInHand());
        if (level <= 0) {
            return;
        }

        upgradeService.applyLevel(furnace, level);
        hologramManager.ensureHologram(furnace, level);
        if (particleManager != null) {
            particleManager.registerFurnace(furnace.getLocation(), level);
        }
    }
}
