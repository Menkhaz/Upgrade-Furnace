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
import at.lowdfx.upgradeFurnace.util.Perms;
import at.lowdfx.upgradeFurnace.util.Perms.Perm;
import org.bukkit.util.Vector;

import java.util.Random;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class UpgradeCommands implements Listener {
    private static final NamespacedKey KEY_LEVEL = new NamespacedKey("upgradefurnace", "level");
    private static final NamespacedKey KEY_HOLO = new NamespacedKey("upgradefurnace", "hologram");
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
                                Component.text("Schau auf einen Ofen!", NamedTextColor.RED)
                        ));
                        return 0;
                    }

                    PersistentDataContainer pdc = furnace.getPersistentDataContainer();
                    int current = pdc.getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);

                    if (current >= 5) {
                        Utilities.negativeSound(player);
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Component.text("Dieser Ofen ist bereits auf höchstem Level!", NamedTextColor.YELLOW)
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
                                Component.text("Fehler in der Config: Material ungültig.", NamedTextColor.RED)
                        ));
                        return 0;
                    }

                    if (!player.getInventory().contains(mat, req)) {
                        Utilities.negativeSound(player);
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Component.text("Du brauchst " + req + " " + mat.name().toLowerCase() + " für Level " + next, NamedTextColor.RED)
                        ));
                        return 0;
                    }

                    if (xpReq > 0 && player.getLevel() < xpReq) {
                        Utilities.negativeSound(player);
                        player.sendMessage(UpgradeFurnace.serverMessage(
                                Component.text("Du benötigst mindestens " + xpReq + " Erfahrungslevel für Level " + next, NamedTextColor.RED)
                        ));
                        return 0;
                    }

                    player.getInventory().removeItem(new ItemStack(mat, req));
                    if (xpReq > 0) player.giveExpLevels(-xpReq);

                    pdc.set(KEY_LEVEL, PersistentDataType.INTEGER, next);
                    furnace.update();

                    removeHologram(furnace);
                    spawnHologram(furnace, next);
                    UpgradeFurnace.PARTICLE_MANAGER.updateFurnace(furnace.getLocation(), next);

                    Utilities.positiveSound(player);
                    player.sendMessage(UpgradeFurnace.serverMessage(
                            Component.text("Ofen auf Level " + next + " geupgraded!", NamedTextColor.GREEN)
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
        int amount = result.getAmount();
        if (level == 5) amount *= (1 + RANDOM.nextInt(4));
        else if (level == 4) amount *= (1 + RANDOM.nextInt(3));
        else for (int i = 0; i < level; i++) if (RANDOM.nextDouble() < 0.3) amount++;
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
        removeHologram(furnace);
        // Aus Partikel-Animation entfernen
        UpgradeFurnace.PARTICLE_MANAGER.unregisterFurnace(furnace.getLocation());
        furnace.update();
        evt.setDropItems(false);
        Material furnaceType = block.getType();
        ItemStack dropped = new ItemStack(furnaceType);
        ItemMeta meta = dropped.getItemMeta();
        meta.getPersistentDataContainer().set(KEY_LEVEL, PersistentDataType.INTEGER, level);
        String furnaceName = switch (block.getType()) {
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

        meta.displayName(Component.text(tierName + " " + furnaceName, NamedTextColor.GOLD));
        dropped.setItemMeta(meta);
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
            furnace.update();
            spawnHologram(furnace, level);
            // Für Partikel-Animation registrieren
            UpgradeFurnace.PARTICLE_MANAGER.registerFurnace(furnace.getLocation(), level);
        }
    }

    private static Furnace getTargetFurnace(Player player) {
        var block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
        if (block == null || !(block.getState() instanceof Furnace)) return null;
        return (Furnace) block.getState();
    }

    private static void removeHologram(Furnace furnace) {
        PersistentDataContainer pdc = furnace.getPersistentDataContainer();

        if (!pdc.has(KEY_HOLO, PersistentDataType.STRING)) return;

        String uuidStr = pdc.get(KEY_HOLO, PersistentDataType.STRING);
        if (uuidStr == null) return;

        try {
            UUID uuid = UUID.fromString(uuidStr);
            Entity e = furnace.getWorld().getEntity(uuid);
            if (e != null) e.remove();
        } catch (Exception ignored) {}

        pdc.remove(KEY_HOLO);
        furnace.update();
    }

    private static void spawnHologram(Furnace furnace, int level) {
        Location loc = furnace.getBlock().getLocation().add(0.5, 1.2, 0.5);
        ArmorStand holo = (ArmorStand) furnace.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        holo.customName(Component.text("Level " + level, NamedTextColor.RED));
        holo.setCustomNameVisible(true);
        holo.setGravity(false);
        holo.setVisible(false);
        holo.setMarker(true);
        PersistentDataContainer pdc = furnace.getPersistentDataContainer();
        pdc.set(KEY_HOLO, PersistentDataType.STRING, holo.getUniqueId().toString());
        furnace.update();
        Location center = furnace.getBlock().getLocation().add(0.5, 0.5, 0.5);
        Particle particle = Configuration.getParticle(level);
        furnace.getWorld().spawnParticle(particle, center, 20, 1.0, 0.5, 1.0, 0.05);
    }

    private void spawnSmeltParticles(Furnace furnace, int level) {
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