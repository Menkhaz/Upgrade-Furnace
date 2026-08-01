package at.lowdfx.upgradeFurnace.commands;

import at.lowdfx.upgradeFurnace.util.Configuration;
import at.lowdfx.upgradeFurnace.util.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.help.HelpTopic;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the complete UpgradeFurnace command reference through
 * /help furnace_commands.
 */
@SuppressWarnings("deprecation")
public final class FurnaceHelpTopic extends HelpTopic {

    public FurnaceHelpTopic() {
        name = "furnace_commands";
        shortText = Messages.text("help.topic-short");
    }

    @Override
    public boolean canSee(@NotNull CommandSender sender) {
        return amendedPermission == null || sender.hasPermission(amendedPermission);
    }

    @Override
    public @NotNull String getFullText(@NotNull CommandSender sender) {
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "---- " + Messages.text("help.topic-title") + " ----");
        lines.add(command("/furnace upgrade", "help.commands.upgrade"));
        lines.add(command("/upgrade", "help.commands.upgrade-shortcut"));
        lines.add(command("/furnace info", "help.commands.info"));

        if (Configuration.HOLOGRAMS_ENABLED) {
            lines.add(command("/furnace hologram", "help.commands.hologram-status"));
            lines.add(command(
                    "/furnace hologram <on|off>",
                    "help.commands.hologram-toggle"));
        }

        lines.add(command("/furnace admin reload", "help.commands.admin-reload"));
        lines.add(command("/furnace admin set <1-5>", "help.commands.admin-set"));
        lines.add(command("/furnace admin reset", "help.commands.admin-reset"));
        return String.join("\n", lines);
    }

    private String command(String usage, String descriptionKey) {
        return ChatColor.YELLOW + usage
                + ChatColor.GRAY + " - "
                + ChatColor.WHITE + Messages.text(descriptionKey);
    }
}
