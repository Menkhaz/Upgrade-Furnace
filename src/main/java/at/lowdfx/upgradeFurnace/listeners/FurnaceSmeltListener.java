package at.lowdfx.upgradeFurnace.listeners;

import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService;
import at.lowdfx.upgradeFurnace.util.Configuration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Applies upgraded furnace speed and bonus-output behavior.
 */
public final class FurnaceSmeltListener implements Listener {

    private static final Random RANDOM = new Random();

    private final FurnaceUpgradeService upgradeService;

    public FurnaceSmeltListener(FurnaceUpgradeService upgradeService) {
        this.upgradeService = upgradeService;
    }

    @EventHandler
    public void onStartSmelt(FurnaceStartSmeltEvent event) {
        Furnace furnace = (Furnace) event.getBlock().getState();
        int level = upgradeService.getLevel(furnace);
        if (level < 1) {
            return;
        }

        double speedMultiplier = Configuration.getSpeedMultiplier(level);
        int newCookTime =
                Math.max(1, (int) Math.round(event.getTotalCookTime() / speedMultiplier));
        event.setTotalCookTime(newCookTime);
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Furnace furnace)) {
            return;
        }

        int level = upgradeService.getLevel(furnace);
        if (level <= 1) {
            return;
        }

        double bonusChance = Configuration.getBonusChance(level);
        int maxBonusItems = Configuration.getBonusMaxItems(level);
        if (bonusChance <= 0.0
                || maxBonusItems <= 0
                || RANDOM.nextDouble() >= bonusChance) {
            return;
        }

        ItemStack result = event.getResult();
        int amount = result.getAmount() + 1 + RANDOM.nextInt(maxBonusItems);
        result.setAmount(Math.min(amount, result.getMaxStackSize()));
        event.setResult(result);
        spawnSmeltParticles(furnace, level);
    }

    private void spawnSmeltParticles(Furnace furnace, int level) {
        if (!Configuration.PARTICLES_ENABLED) {
            return;
        }

        Location location = furnace.getBlock().getLocation().add(0.5, 0.3, 0.5);
        BlockData blockData = furnace.getBlock().getBlockData();
        if (blockData instanceof Directional directional) {
            Vector direction = directional.getFacing().getDirection();
            location.add(direction.multiply(0.52));
        }

        Particle particle = Configuration.getParticle(level);
        furnace.getWorld().spawnParticle(
                particle, location, 8, 0.15, 0.15, 0.15, 0.02);
    }
}
