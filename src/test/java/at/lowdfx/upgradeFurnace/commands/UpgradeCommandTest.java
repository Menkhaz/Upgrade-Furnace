package at.lowdfx.upgradeFurnace.commands;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService;
import at.lowdfx.upgradeFurnace.services.HologramManager;
import at.lowdfx.upgradeFurnace.util.Configuration;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class UpgradeCommandTest {

    @AfterEach
    void restoreDefaultHologramSetting() {
        Configuration.HOLOGRAMS_ENABLED = true;
    }

    @Test
    void omitsPlayerHologramCommandWhenServerHologramsAreDisabled() {
        Configuration.HOLOGRAMS_ENABLED = false;

        assertNull(command().getChild("hologram"));
    }

    @Test
    void registersPlayerHologramCommandWhenServerHologramsAreEnabled() {
        Configuration.HOLOGRAMS_ENABLED = true;

        var hologram = command().getChild("hologram");

        assertNotNull(hologram);
        assertNotNull(hologram.getChild("on"));
        assertNotNull(hologram.getChild("off"));
    }

    @Test
    void organizesUpgradeAndAdminCommandsUnderFurnaceRoot() {
        LiteralCommandNode<CommandSourceStack> command = command();

        assertNotNull(command);
        assertNotNull(command.getChild("upgrade"));
        assertNotNull(command.getChild("info"));
        assertNotNull(command.getChild("admin"));
        assertNotNull(command.getChild("admin").getChild("reload"));
        assertNotNull(command.getChild("admin").getChild("set"));
        assertNotNull(command.getChild("admin").getChild("reset"));
        assertNull(command.getChild("reload"));
        assertNull(command.getChild("set"));
        assertNull(command.getChild("reset"));
    }

    @Test
    void providesUpgradeCompatibilityShortcut() {
        LiteralCommandNode<CommandSourceStack> shortcut =
                upgradeCommand().upgradeShortcut();

        assertNotNull(shortcut);
        assertNull(shortcut.getChild("info"));
        assertNull(shortcut.getChild("admin"));
    }

    private LiteralCommandNode<CommandSourceStack> command() {
        return upgradeCommand().command();
    }

    private UpgradeCommand upgradeCommand() {
        return new UpgradeCommand(
                mock(UpgradeFurnace.class),
                mock(FurnaceUpgradeService.class),
                mock(HologramManager.class));
    }
}
