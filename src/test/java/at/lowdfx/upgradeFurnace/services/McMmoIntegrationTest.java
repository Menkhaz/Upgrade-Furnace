package at.lowdfx.upgradeFurnace.services;

import at.lowdfx.upgradeFurnace.util.Configuration;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class McMmoIntegrationTest {
    @AfterEach
    void resetMode() {
        Configuration.MCMMO_SMELTING = false;
    }

    @Test
    void vanillaModeDoesNotLoadOptionalPlugin() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        McMmoIntegration integration = new McMmoIntegration(plugin);
        integration.refresh();
        assertFalse(integration.isAvailable());
        verify(plugin, never()).getServer();
    }

    @Test
    void missingOrDisabledMcMmoWarnsAndLeavesVanillaFallbackAvailable() {
        Configuration.MCMMO_SMELTING = true;
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        PluginManager manager = mock(PluginManager.class);
        Logger logger = mock(Logger.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(manager);
        when(plugin.getLogger()).thenReturn(logger);
        McMmoIntegration integration = new McMmoIntegration(plugin);

        integration.refresh();
        assertFalse(integration.isAvailable());
        Plugin disabled = mock(Plugin.class);
        when(manager.getPlugin("mcMMO")).thenReturn(disabled);
        integration.refresh();
        assertFalse(integration.isAvailable());
        verify(logger, times(2)).warning(contains("Falling back to configured vanilla XP costs"));
    }
}
