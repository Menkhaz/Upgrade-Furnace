package at.lowdfx.upgradeFurnace.services;

import at.lowdfx.upgradeFurnace.FurnaceParticleManager;
import at.lowdfx.upgradeFurnace.util.Configuration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Owns furnace upgrade state, costs, and tier naming.
 */
public final class FurnaceUpgradeService {

    private static final int MAX_LEVEL = 5;
    private static final NamespacedKey KEY_LEVEL =
            new NamespacedKey("upgradefurnace", "level");

    private final HologramManager hologramManager;
    private final FurnaceParticleManager particleManager;
    private final SmeltingLevelProvider smelting;

    public FurnaceUpgradeService(
            HologramManager hologramManager,
            FurnaceParticleManager particleManager
    ) {
        this(hologramManager, particleManager, null);
    }

    public FurnaceUpgradeService(HologramManager hologramManager,
            FurnaceParticleManager particleManager, SmeltingLevelProvider smelting) {
        this.hologramManager = hologramManager;
        this.particleManager = particleManager;
        this.smelting = smelting;
    }

    public boolean usesSmelting() {
        return Configuration.MCMMO_SMELTING && smelting != null && smelting.isAvailable();
    }

    public int getSmeltingLevel(Player player) {
        return smelting.getLevel(player);
    }

    public UpgradeResult upgrade(Player player, Furnace furnace) {
        int currentLevel = getLevel(furnace);
        if (currentLevel >= MAX_LEVEL) {
            return new UpgradeResult(UpgradeStatus.MAX_LEVEL, currentLevel, null, 0, 0);
        }

        int nextLevel = currentLevel + 1;
        Material material = Configuration.getRequirementMaterial(nextLevel);
        int amount = Configuration.getRequirementAmount(nextLevel);
        boolean useSmelting = usesSmelting();
        int xpLevels = useSmelting ? 0 : Configuration.getRequirementXpLevels(nextLevel);

        if (material == null) {
            return new UpgradeResult(
                    UpgradeStatus.INVALID_MATERIAL, nextLevel, null, amount, xpLevels);
        }
        if (!player.getInventory().contains(material, amount)) {
            return new UpgradeResult(
                    UpgradeStatus.MISSING_MATERIAL, nextLevel, material, amount, xpLevels);
        }
        if (useSmelting) {
            int required = Configuration.getRequirementSmeltingLevel(nextLevel);
            int current;
            try {
                current = getSmeltingLevel(player);
            } catch (IllegalStateException e) {
                return new UpgradeResult(UpgradeStatus.SKILL_UNAVAILABLE,
                        nextLevel, material, amount, 0);
            }
            if (current < required) {
                return new UpgradeResult(UpgradeStatus.MISSING_SMELTING,
                        nextLevel, material, amount, 0, required, current);
            }
        }
        if (xpLevels > 0 && player.getLevel() < xpLevels) {
            return new UpgradeResult(
                    UpgradeStatus.MISSING_XP, nextLevel, material, amount, xpLevels);
        }

        player.getInventory().removeItem(new ItemStack(material, amount));
        if (xpLevels > 0) {
            player.giveExpLevels(-xpLevels);
        }

        setLevel(furnace, nextLevel);

        return new UpgradeResult(
                UpgradeStatus.SUCCESS, nextLevel, material, amount, xpLevels);
    }

    public FurnaceInfo inspect(Furnace furnace) {
        int level = getLevel(furnace);
        int nextLevel = level + 1;
        Material nextMaterial =
                level < MAX_LEVEL ? Configuration.getRequirementMaterial(nextLevel) : null;

        return new FurnaceInfo(
                level,
                MAX_LEVEL,
                Configuration.getSpeedMultiplier(level),
                Configuration.getBonusChance(level),
                Configuration.getBonusMaxItems(level),
                nextMaterial,
                level < MAX_LEVEL ? Configuration.getRequirementAmount(nextLevel) : 0,
                level < MAX_LEVEL && !usesSmelting()
                        ? Configuration.getRequirementXpLevels(nextLevel) : 0);
    }

    public int getLevel(Furnace furnace) {
        int storedLevel = furnace.getPersistentDataContainer()
                .getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);
        return normalizeLevel(storedLevel);
    }

    public int sanitizeLevel(Furnace furnace) {
        int storedLevel = furnace.getPersistentDataContainer()
                .getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);
        int normalizedLevel = normalizeLevel(storedLevel);
        if (storedLevel == normalizedLevel) {
            return normalizedLevel;
        }

        if (normalizedLevel == 0) {
            furnace.getPersistentDataContainer().remove(KEY_LEVEL);
            furnace.customName(null);
            furnace.update();
        } else {
            applyLevel(furnace, normalizedLevel);
        }
        return normalizedLevel;
    }

    public int getStoredLevel(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        int storedLevel = meta.getPersistentDataContainer()
                .getOrDefault(KEY_LEVEL, PersistentDataType.INTEGER, 0);
        return normalizeLevel(storedLevel);
    }

    public void setLevel(Furnace furnace, int level) {
        if (level < 1 || level > MAX_LEVEL) {
            throw new IllegalArgumentException(
                    "Furnace level must be between 1 and " + MAX_LEVEL);
        }

        applyLevel(furnace, level);
        hologramManager.removeHologram(furnace);
        hologramManager.ensureHologram(furnace, level);
        if (particleManager != null) {
            particleManager.updateFurnace(furnace.getLocation(), level);
        }
    }

    public boolean resetLevel(Furnace furnace) {
        if (getLevel(furnace) <= 0) {
            return false;
        }

        hologramManager.removeHologram(furnace);
        furnace.getPersistentDataContainer().remove(KEY_LEVEL);
        furnace.customName(null);
        furnace.update();
        if (particleManager != null) {
            particleManager.unregisterFurnace(furnace.getLocation());
        }
        return true;
    }

    public void applyLevel(Furnace furnace, int level) {
        furnace.getPersistentDataContainer()
                .set(KEY_LEVEL, PersistentDataType.INTEGER, level);
        furnace.customName(furnaceName(furnace.getBlock().getType(), level));
        furnace.update();
    }

    public ItemStack createUpgradedFurnaceItem(Material furnaceType, int level) {
        ItemStack item = new ItemStack(furnaceType);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.getPersistentDataContainer()
                .set(KEY_LEVEL, PersistentDataType.INTEGER, level);
        meta.displayName(furnaceName(furnaceType, level));
        item.setItemMeta(meta);
        return item;
    }

    private Component furnaceName(Material type, int level) {
        String furnaceType = switch (type) {
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

        return Component.text(tierName + " " + furnaceType, NamedTextColor.GOLD);
    }

    private int normalizeLevel(int level) {
        return Math.max(0, Math.min(level, MAX_LEVEL));
    }

    public enum UpgradeStatus {
        SUCCESS,
        MAX_LEVEL,
        INVALID_MATERIAL,
        MISSING_MATERIAL,
        MISSING_XP,
        MISSING_SMELTING,
        SKILL_UNAVAILABLE
    }

    public record UpgradeResult(
            UpgradeStatus status,
            int level,
            Material material,
            int requiredAmount,
            int requiredXp,
            int requiredSmelting,
            int currentSmelting
    ) {
        public UpgradeResult(UpgradeStatus status, int level, Material material,
                int requiredAmount, int requiredXp) {
            this(status, level, material, requiredAmount, requiredXp, 0, 0);
        }
    }

    public record FurnaceInfo(
            int level,
            int maxLevel,
            double speedMultiplier,
            double bonusChance,
            int bonusMaxItems,
            Material nextMaterial,
            int nextAmount,
            int nextXp
    ) {
    }
}
