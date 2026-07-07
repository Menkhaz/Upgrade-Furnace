package at.lowdfx.upgradeFurnace;

import at.lowdfx.upgradeFurnace.util.Configuration;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Furnace;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages continuous particle animations around upgraded furnaces.
 * Shows ascending spiral particles based on the furnace level.
 */
public class FurnaceParticleManager {

    // Thread-safe map for furnace locations and levels.
    private final Map<Location, Integer> furnaces = new ConcurrentHashMap<>();

    // Current animation tick counter.
    private long tick = 0;

    // Currently running task.
    private BukkitTask task;

    /**
     * Starts the particle animation.
     */
    public void start() {
        if (task != null) return;

        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick++;

                furnaces.forEach((location, level) -> {
                    if (location.getWorld() == null) return;
                    if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return;

                    if (Configuration.PARTICLES_ONLY_WHEN_ACTIVE) {
                        if (!(location.getBlock().getState() instanceof Furnace furnace)) return;
                        if (furnace.getBurnTime() <= 0) return;
                    }

                    spawnSpiralParticles(location, level);
                });
            }
        }.runTaskTimer(UpgradeFurnace.PLUGIN, 0L, 2L);
    }

    /**
     * Stops the particle animation.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        furnaces.clear();
    }

    /**
     * Registers a furnace for particle animation.
     */
    public void registerFurnace(Location location, int level) {
        if (level < 1) return;

        Location blockLoc = location.getBlock().getLocation();
        furnaces.put(blockLoc, level);
    }

    /**
     * Removes a furnace from the animation.
     */
    public void unregisterFurnace(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        furnaces.remove(blockLoc);
    }

    /**
     * Updates the level of a furnace.
     */
    public void updateFurnace(Location location, int newLevel) {
        Location blockLoc = location.getBlock().getLocation();
        if (newLevel < 1) {
            furnaces.remove(blockLoc);
        } else {
            furnaces.put(blockLoc, newLevel);
        }
    }

    /**
     * Spawns ascending spiral particles around the furnace.
     */
    private void spawnSpiralParticles(Location furnaceLocation, int level) {
        // Furnace center.
        Location center = furnaceLocation.clone().add(0.5, 0.5, 0.5);

        // Level-based parameters.
        double speed = getSpeedForLevel(level);
        double maxHeight = getHeightForLevel(level);
        double turns = getTurnsForLevel(level);
        Particle particle = Configuration.getParticle(level);

        // Number of particle points per frame. Higher levels show more particles.
        int particleCount = Math.min(level, 3);

        for (int i = 0; i < particleCount; i++) {
            // Offset for multiple spiral arms.
            double armOffset = (2 * Math.PI / particleCount) * i;

            // Calculate the position on the spiral.
            double progress = (tick * speed) % (2 * Math.PI * turns);
            double angle = progress + armOffset;

            // Slightly vary radius for a more interesting animation.
            double radius = 0.5 + 0.1 * Math.sin(tick * 0.1);

            // Calculate ascending Y position.
            double heightProgress = (progress / (2 * Math.PI * turns));
            double y = heightProgress * maxHeight;

            // Calculate X and Z on the circle.
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            // Spawn position.
            Location particleLoc = center.clone().add(x, y, z);

            // Spawn particle.
            furnaceLocation.getWorld().spawnParticle(
                particle,
                particleLoc,
                1,      // count
                0, 0, 0, // offset
                0       // extra (speed)
            );
        }
    }

    /**
     * Returns the rotation speed for the level.
     */
    private double getSpeedForLevel(int level) {
        return switch (level) {
            case 1 -> 0.1;
            case 2 -> 0.15;
            case 3 -> 0.2;
            case 4 -> 0.25;
            case 5 -> 0.3;
            default -> 0.1;
        };
    }

    /**
     * Returns the maximum spiral height for the level.
     */
    private double getHeightForLevel(int level) {
        return switch (level) {
            case 1 -> 1.0;
            case 2 -> 1.2;
            case 3 -> 1.5;
            case 4 -> 1.8;
            case 5 -> 2.0;
            default -> 1.0;
        };
    }

    /**
     * Returns the number of turns for the level.
     */
    private double getTurnsForLevel(int level) {
        return switch (level) {
            case 1 -> 1.0;
            case 2 -> 1.5;
            case 3 -> 2.0;
            case 4 -> 2.5;
            case 5 -> 3.0;
            default -> 1.0;
        };
    }
}
