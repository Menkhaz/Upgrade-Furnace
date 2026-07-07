package at.lowdfx.upgradeFurnace.commands;

import at.lowdfx.upgradeFurnace.util.Utilities;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import at.lowdfx.upgradeFurnace.util.Configuration;
import at.lowdfx.upgradeFurnace.util.Messages;
import at.lowdfx.upgradeFurnace.util.Perms;
import at.lowdfx.upgradeFurnace.util.Perms.Perm;
import org.bukkit.util.Vector;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class UpgradeCommands implements Listener {
    private static final NamespacedKey KEY_LEVEL = new NamespacedKey("upgradefurnace", "level");
    private static final NamespacedKey KEY_HOLO = new NamespacedKey("upgradefurnace", "hologram");
    private static final NamespacedKey KEY_HOLO_OWNER = new NamespacedKey("upgradefurnace", "hologram_owner");
    private static final Random RANDOM = new Random();

    public static LiteralCommandNode<CommandSourceStack> furnaceCommand() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("upgrade")
                .requires(src -> {
                    CommandSender s = src.getSender();
                    return s instanceof Player && Perms.check(src, Perm.UPGRADE_FURNACE);
                })
                .executes(ctx -> {
                    Player player = (Player) ctx.getSource().getSender();
                    Furnace furnace = getTargetFurnace(player);

                    if (furnace == null) {
                        Utilities.negativeSound(player);
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Messages.component("furnace.no-target").color(NamedTextColor.RED)
                        ));
                        return 0;
                    }

                    PersistentDataContainer pdc = furnace.getPersistentDataContainer();
                    int current = pdc.getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);

                    // UpgradeFurnace is intentionally balanced around five tiers.
                    if (current >= 5) {
                        Utilities.negativeSound(player);
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Messages.component("furnace.max-level").color(NamedTextColor.YELLOW)
                        ));
                        return 1;
                    }

                    int next = current + 1;
                    Material mat = Configuration.getRequirementMaterial(next);
                    int req = Configuration.getRequirementAmount(next);
                    int xpReq = Configuration.getRequirementXpLevels(next);

                    if (mat == null) {
                        Utilities.negativeSound(player);
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Messages.component("furnace.invalid-material").color(NamedTextColor.RED)
                        ));
                        return 0;
                    }

                    if (!player.getInventory().contains(mat, req)) {
                        Utilities.negativeSound(player);
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Messages.component("furnace.missing-material", Map.of(
                                        "amount", String.valueOf(req),
                                        "material", mat.name().toLowerCase(Locale.ROOT),
                                        "level", String.valueOf(next)
                                )).color(NamedTextColor.RED)
                        ));
                        return 0;
                    }

                    if (xpReq > 0 && player.getLevel() < xpReq) {
                        Utilities.negativeSound(player);
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Messages.component("furnace.missing-xp", Map.of(
                                        "xp", String.valueOf(xpReq),
                                        "level", String.valueOf(next)
                                )).color(NamedTextColor.RED)
                        ));
                        return 0;
                    }

                    player.getInventory().removeItem(new ItemStack(mat, req));
                    if (xpReq > 0) player.giveExpLevels(-xpReq);

                    pdc.set(KEY_LEVEL, PersistentDataType.INTEGER, next);
                    furnace.customName(getFurnaceName(furnace.getBlock().getType(), next));
                    furnace.update();

                    removeHologram(furnace);
                    ensureHologram(furnace, next);
                    if (UpgradeFurnace.PARTICLE_MANAGER != null) {
                        UpgradeFurnace.PARTICLE_MANAGER.updateFurnace(furnace.getLocation(), next);
                    }

                    Utilities.positiveSound(player);
                    player.sendMessage(UpgradeFurnace.serverMessage(
                            Messages.component("furnace.upgraded", Map.of("level", String.valueOf(next))).color(NamedTextColor.GREEN)
                    ));

                    return 1;
                })
                .build();
    }

    @EventHandler
    public void onStartSmelt(FurnaceStartSmeltEvent evt) {
        Furnace furnace = (Furnace) evt.getBlock().getState();
        int lvl = furnace.getPersistentDataContainer().getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);
        if (lvl < 1) return;
        double speedMultiplier = Configuration.getSpeedMultiplier(lvl);
        int newCookTime = Math.max(1, (int) Math.round(evt.getTotalCookTime() / speedMultiplier));
        evt.setTotalCookTime(newCookTime);
    }

    @EventHandler
    public void onSmelt(FurnaceSmeltEvent evt) {
        Block block = evt.getBlock();
        if (!(block.getState() instanceof Furnace furnace)) return;
        Integer level = furnace.getPersistentDataContainer().get(KEY_LEVEL, PersistentDataType.INTEGER);
        if (level == null || level <= 1) return;
        ItemStack result = evt.getResult();
        double bonusChance = Configuration.getBonusChance(level);
        int maxBonusItems = Configuration.getBonusMaxItems(level);
        if (bonusChance <= 0.0 || maxBonusItems <= 0 || RANDOM.nextDouble() >= bonusChance) return;

        int amount = result.getAmount() + 1 + RANDOM.nextInt(maxBonusItems);
        amount = Math.min(amount, result.getMaxStackSize());
        result.setAmount(amount);
        evt.setResult(result);
        spawnSmeltParticles(furnace, level);
    }

    @EventHandler
    public void onFurnaceBreak(BlockBreakEvent evt) {
        Block block = evt.getBlock();
        if (!(block.getState() instanceof Furnace furnace)) return;
        PersistentDataContainer pdc = furnace.getPersistentDataContainer();
        int level = pdc.getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);
        if (level <= 0) return;

        removeHologram(furnace);
        // Aus Partikel-Animation entfernen
        if (UpgradeFurnace.PARTICLE_MANAGER != null) {
            UpgradeFurnace.PARTICLE_MANAGER.unregisterFurnace(furnace.getLocation());
        }
        evt.setDropItems(false);

        for (ItemStack content : furnace.getInventory().getContents()) {
            if (content != null && !content.getType().isAir()) {
                block.getWorld().dropItemNaturally(block.getLocation(), content);
            }
        }

        Material furnaceType = block.getType();
        ItemStack dropped = new ItemStack(furnaceType);
        ItemMeta meta = dropped.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(KEY_LEVEL, PersistentDataType.INTEGER, level);
            meta.displayName(getFurnaceName(block.getType(), level));
            dropped.setItemMeta(meta);
        }
        block.getWorld().dropItemNaturally(block.getLocation(), dropped);
    }

    @EventHandler
    public void onFurnacePlace(BlockPlaceEvent evt) {
        Block block = evt.getBlockPlaced();
        if (!(block.getState() instanceof Furnace furnace)) return;
        ItemStack item = evt.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (container.has(KEY_LEVEL, PersistentDataType.INTEGER)) {
            Integer level = container.get(KEY_LEVEL, PersistentDataType.INTEGER);
            if (level == null) return;
            PersistentDataContainer pdc = furnace.getPersistentDataContainer();
            pdc.set(KEY_LEVEL, PersistentDataType.INTEGER, level);
            furnace.customName(getFurnaceName(block.getType(), level));
            furnace.update();
            ensureHologram(furnace, level);
            // Für Partikel-Animation registrieren
            if (UpgradeFurnace.PARTICLE_MANAGER != null) {
                UpgradeFurnace.PARTICLE_MANAGER.registerFurnace(furnace.getLocation(), level);
            }
        }
    }

    private static Furnace getTargetFurnace(Player player) {
        var block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
        if (block == null || !(block.getState() instanceof Furnace)) return null;
        return (Furnace) block.getState();
    }

    private static Component getFurnaceName(Material type, int level) {
        String furnaceName = switch (type) {
            case BLAST_FURNACE -> "Blast Furnace";
            case SMOKER -> "Smoker";
            default -> "Furnace";
        };

        String tierName = switch (level) {
            case 1 -> "Copper";
            case 2 -> "Iron";
            case 3 -> "Gold";
            case 4 -> "Diamond";
            case 5 -> "Netherite";
            default -> "";
        };

        return Component.text(tierName + " " + furnaceName, NamedTextColor.GOLD);
    }

    private static void removeHologram(Furnace furnace) {
        PersistentDataContainer pdc = furnace.getPersistentDataContainer();

        String uuidStr = pdc.get(KEY_HOLO, PersistentDataType.STRING);
        if (uuidStr != null) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Entity e = furnace.getWorld().getEntity(uuid);
                if (e != null) e.remove();
            } catch (Exception ignored) {}
        }

        removeOwnedHolograms(furnace, null);
        pdc.remove(KEY_HOLO);
        furnace.update();
    }

    public static void ensureHologram(Furnace furnace, int level) {
        String ownerKey = ownerKey(furnace);
        PersistentDataContainer pdc = furnace.getPersistentDataContainer();
        String uuidStr = pdc.get(KEY_HOLO, PersistentDataType.STRING);

        if (uuidStr != null) {
            try {
                Entity entity = furnace.getWorld().getEntity(UUID.fromString(uuidStr));
                if (entity instanceof ArmorStand armorStand
                        && ownerKey.equals(armorStand.getPersistentDataContainer().get(KEY_HOLO_OWNER, PersistentDataType.STRING))) {
                    armorStand.customName(Component.text("Level " + level, NamedTextColor.RED));
                    removeOwnedHolograms(furnace, armorStand.getUniqueId());
                    return;
                }
                if (entity != null) entity.remove();
            } catch (IllegalArgumentException ignored) {}
        }

        removeOwnedHolograms(furnace, null);
        spawnHologram(furnace, level);
    }

    private static void spawnHologram(Furnace furnace, int level) {
        Location loc = furnace.getBlock().getLocation().add(0.5, 1.2, 0.5);
        ArmorStand holo = (ArmorStand) furnace.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        holo.customName(Component.text("Level " + level, NamedTextColor.RED));
        holo.setCustomNameVisible(true);
        holo.setGravity(false);
        holo.setVisible(false);
        holo.setMarker(true);
        holo.getPersistentDataContainer().set(KEY_HOLO_OWNER, PersistentDataType.STRING, ownerKey(furnace));
        PersistentDataContainer pdc = furnace.getPersistentDataContainer();
        pdc.set(KEY_HOLO, PersistentDataType.STRING, holo.getUniqueId().toString());
        furnace.update();
        if (Configuration.PARTICLES_ENABLED) {
            Location center = furnace.getBlock().getLocation().add(0.5, 0.5, 0.5);
            Particle particle = Configuration.getParticle(level);
            furnace.getWorld().spawnParticle(particle, center, 20, 1.0, 0.5, 1.0, 0.05);
        }
    }

    private static void removeOwnedHolograms(Furnace furnace, UUID keep) {
        String ownerKey = ownerKey(furnace);
        Location loc = furnace.getBlock().getLocation().add(0.5, 1.2, 0.5);
        for (Entity entity : furnace.getWorld().getNearbyEntities(loc, 0.75, 0.75, 0.75)) {
            if (!(entity instanceof ArmorStand armorStand)) continue;
            if (keep != null && keep.equals(armorStand.getUniqueId())) continue;
            String owner = armorStand.getPersistentDataContainer().get(KEY_HOLO_OWNER, PersistentDataType.STRING);
            if (ownerKey.equals(owner)) {
                armorStand.remove();
            }
        }
    }

    private static String ownerKey(Furnace furnace) {
        Location loc = furnace.getBlock().getLocation();
        return loc.getWorld().getUID() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private void spawnSmeltParticles(Furnace furnace, int level) {
        if (!Configuration.PARTICLES_ENABLED) return;

        Location loc = furnace.getBlock().getLocation().add(0.5, 0.3, 0.5);
        BlockData data = furnace.getBlock().getBlockData();
        if (data instanceof Directional directional) {
            Vector dir = directional.getFacing().getDirection();
            loc.add(dir.multiply(0.52));
        }
        Particle particle = Configuration.getParticle(level);
        furnace.getWorld().spawnParticle(particle, loc, 8, 0.15, 0.15, 0.15, 0.02);
    }
}
