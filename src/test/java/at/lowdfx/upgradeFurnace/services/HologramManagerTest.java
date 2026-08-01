package at.lowdfx.upgradeFurnace.services;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HologramManagerTest {

    @Test
    void setsAndPersistsPlayerVisibilityWithoutCreatingAFile() {
        UpgradeFurnace plugin = mock(UpgradeFurnace.class);
        Player player = mock(Player.class);
        PersistentDataContainer playerData = mock(PersistentDataContainer.class);
        World world = mock(World.class);
        when(player.getPersistentDataContainer()).thenReturn(playerData);
        when(player.getWorld()).thenReturn(world);
        when(world.getEntitiesByClass(ArmorStand.class)).thenReturn(List.of());
        HologramManager manager = new HologramManager(plugin);

        manager.setPlayerVisibility(player, false);
        manager.setPlayerVisibility(player, true);

        verify(playerData).set(any(), eq(PersistentDataType.BYTE), eq((byte) 1));
        verify(playerData).remove(any());
    }
}
