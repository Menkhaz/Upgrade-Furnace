package at.lowdfx.upgradeFurnace.services;

import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService.UpgradeStatus;
import at.lowdfx.upgradeFurnace.util.Configuration;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FurnaceUpgradeServiceTest {

    private HologramManager hologramManager;
    private FurnaceUpgradeService upgradeService;
    private Furnace furnace;
    private PersistentDataContainer furnaceData;

    @BeforeEach
    void setUp() {
        hologramManager = mock(HologramManager.class);
        upgradeService = new FurnaceUpgradeService(hologramManager, null);
        furnace = mock(Furnace.class);
        furnaceData = mock(PersistentDataContainer.class);
        when(furnace.getPersistentDataContainer()).thenReturn(furnaceData);
    }

    @AfterEach
    void clearConfiguration() {
        Configuration.REQUIRE_MATERIAL.clear();
        Configuration.REQUIRE_AMOUNT.clear();
        Configuration.REQUIRE_XP_LEVELS.clear();
        Configuration.SPEED_MULTIPLIER.clear();
        Configuration.PARTICLE.clear();
        Configuration.BONUS_CHANCE.clear();
        Configuration.BONUS_MAX_ITEMS.clear();
    }

    @Test
    void rejectsUpgradeAtMaximumLevel() {
        storedLevel(5);
        Player player = mock(Player.class);

        var result = upgradeService.upgrade(player, furnace);

        assertEquals(UpgradeStatus.MAX_LEVEL, result.status());
        verify(player, never()).getInventory();
    }

    @Test
    void reportsMissingMaterialsWithoutChargingPlayer() {
        storedLevel(0);
        configureFirstLevel();
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.contains(Material.COPPER_INGOT, 16)).thenReturn(false);

        var result = upgradeService.upgrade(player, furnace);

        assertEquals(UpgradeStatus.MISSING_MATERIAL, result.status());
        assertEquals(16, result.requiredAmount());
        verify(inventory, never()).removeItem(any(org.bukkit.inventory.ItemStack.class));
    }

    @Test
    void reportsMissingExperienceWithoutChargingPlayer() {
        storedLevel(0);
        configureFirstLevel();
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.contains(Material.COPPER_INGOT, 16)).thenReturn(true);
        when(player.getLevel()).thenReturn(4);

        var result = upgradeService.upgrade(player, furnace);

        assertEquals(UpgradeStatus.MISSING_XP, result.status());
        verify(inventory, never()).removeItem(any(org.bukkit.inventory.ItemStack.class));
        verify(player, never()).giveExpLevels(any(Integer.class));
    }

    @Test
    void successfulUpgradeChargesPlayerAndRefreshesHologram() {
        storedLevel(0);
        configureFirstLevel();
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        Block block = mock(Block.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.contains(Material.COPPER_INGOT, 16)).thenReturn(true);
        when(player.getLevel()).thenReturn(10);
        when(furnace.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.FURNACE);

        FurnaceUpgradeService.UpgradeResult result;
        try (MockedConstruction<ItemStack> ignored = mockConstruction(ItemStack.class)) {
            result = upgradeService.upgrade(player, furnace);
        }

        assertEquals(UpgradeStatus.SUCCESS, result.status());
        assertEquals(1, result.level());
        verify(inventory).removeItem(any(org.bukkit.inventory.ItemStack.class));
        verify(player).giveExpLevels(-5);
        verify(furnaceData).set(any(), eq(PersistentDataType.INTEGER), eq(1));
        verify(hologramManager).removeHologram(furnace);
        verify(hologramManager).ensureHologram(furnace, 1);
    }

    @Test
    void resetOnlyChangesUpgradedFurnaces() {
        storedLevel(0, 3);

        assertFalse(upgradeService.resetLevel(furnace));
        assertTrue(upgradeService.resetLevel(furnace));
        verify(furnaceData).remove(any());
        verify(hologramManager).removeHologram(furnace);
    }

    @Test
    void clampsInvalidStoredLevels() {
        storedLevel(99, -3);

        assertEquals(5, upgradeService.getLevel(furnace));
        assertEquals(0, upgradeService.getLevel(furnace));
    }

    private void configureFirstLevel() {
        Configuration.REQUIRE_MATERIAL.put(1, Material.COPPER_INGOT);
        Configuration.REQUIRE_AMOUNT.put(1, 16);
        Configuration.REQUIRE_XP_LEVELS.put(1, 5);
    }

    private void storedLevel(int... levels) {
        when(furnaceData.getOrDefault(
                        any(),
                        eq(PersistentDataType.INTEGER),
                        eq(0)))
                .thenReturn(levels[0], java.util.Arrays.stream(levels)
                        .skip(1)
                        .boxed()
                        .toArray(Integer[]::new));
    }
}
