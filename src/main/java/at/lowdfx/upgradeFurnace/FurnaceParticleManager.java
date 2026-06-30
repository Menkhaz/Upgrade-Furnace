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
 * Verwaltet die kontinuierlichen Partikel-Animationen um upgradete Öfen.
 * Zeigt aufsteigende Spiral-Partikel basierend auf dem Ofen-Level.
 */
public class FurnaceParticleManager {

    // Thread-safe Map für Ofen-Positionen und ihre Level
    private final Map<Location, Integer> furnaces = new ConcurrentHashMap<>();

    // Aktueller Tick-Zähler für Animation
    private long tick = 0;

    // Der laufende Task
    private BukkitTask task;

    /**
     * Startet die Partikel-Animation.
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
     * Stoppt die Partikel-Animation.
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        furnaces.clear();
    }

    /**
     * Registriert einen Ofen für die Partikel-Animation.
     */
    public void registerFurnace(Location location, int level) {
        if (level < 1) return;

        Location blockLoc = location.getBlock().getLocation();
        furnaces.put(blockLoc, level);
    }

    /**
     * Entfernt einen Ofen aus der Animation.
     */
    public void unregisterFurnace(Location location) {
        Location blockLoc = location.getBlock().getLocation();
        furnaces.remove(blockLoc);
    }

    /**
     * Aktualisiert das Level eines Ofens.
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
     * Spawnt aufsteigende Spiral-Partikel um den Ofen.
     */
    private void spawnSpiralParticles(Location furnaceLocation, int level) {
        // Zentrum des Ofens
        Location center = furnaceLocation.clone().add(0.5, 0.5, 0.5);

        // Level-basierte Parameter
        double speed = getSpeedForLevel(level);
        double maxHeight = getHeightForLevel(level);
        double turns = getTurnsForLevel(level);
        Particle particle = Configuration.getParticle(level);

        // Anzahl der Partikel-Punkte pro Frame (höheres Level = mehr Partikel)
        int particleCount = Math.min(level, 3);

        for (int i = 0; i < particleCount; i++) {
            // Offset für mehrere Spiralarme
            double armOffset = (2 * Math.PI / particleCount) * i;

            // Berechne Position auf der Spirale
            double progress = (tick * speed) % (2 * Math.PI * turns);
            double angle = progress + armOffset;

            // Radius variiert leicht für interessantere Animation
            double radius = 0.5 + 0.1 * Math.sin(tick * 0.1);

            // Berechne Y-Position (aufsteigend)
            double heightProgress = (progress / (2 * Math.PI * turns));
            double y = heightProgress * maxHeight;

            // Berechne X und Z auf dem Kreis
            double x = Math.cos(angle) * radius;
            double z = Math.sin(angle) * radius;

            // Spawn-Position
            Location particleLoc = center.clone().add(x, y, z);

            // Partikel spawnen
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
     * Gibt die Rotationsgeschwindigkeit für das Level zurück.
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
     * Gibt die maximale Höhe der Spirale für das Level zurück.
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
     * Gibt die Anzahl der turns für das Level zurück.
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
