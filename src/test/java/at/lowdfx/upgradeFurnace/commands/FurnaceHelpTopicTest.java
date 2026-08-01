package at.lowdfx.upgradeFurnace.commands;

import at.lowdfx.upgradeFurnace.util.Configuration;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FurnaceHelpTopicTest {

    @AfterEach
    void restoreDefaultHologramSetting() {
        Configuration.HOLOGRAMS_ENABLED = true;
    }

    @Test
    void listsEveryRegisteredCommandUsingExactSyntax() {
        Configuration.HOLOGRAMS_ENABLED = true;
        FurnaceHelpTopic topic = new FurnaceHelpTopic();
        String help = topic.getFullText(mock(CommandSender.class));

        assertEquals("furnace_commands", topic.getName());
        assertTrue(help.contains("/furnace upgrade"));
        assertTrue(help.contains("/upgrade"));
        assertTrue(help.contains("/furnace info"));
        assertTrue(help.contains("/furnace hologram <on|off>"));
        assertTrue(help.contains("/furnace admin reload"));
        assertTrue(help.contains("/furnace admin set <1-5>"));
        assertTrue(help.contains("/furnace admin reset"));
    }

    @Test
    void omitsHologramCommandsWhenTheyAreNotRegistered() {
        Configuration.HOLOGRAMS_ENABLED = false;
        FurnaceHelpTopic topic = new FurnaceHelpTopic();

        String help = topic.getFullText(mock(CommandSender.class));

        assertFalse(help.contains("/furnace hologram"));
    }

    @Test
    void respectsHelpYmlPermissionAmendments() {
        FurnaceHelpTopic topic = new FurnaceHelpTopic();
        CommandSender sender = mock(CommandSender.class);
        topic.amendCanSee("upgradefurnace.help");
        when(sender.hasPermission("upgradefurnace.help")).thenReturn(false, true);

        assertFalse(topic.canSee(sender));
        assertTrue(topic.canSee(sender));
    }
}
