package at.lowdfx.upgradeFurnace.commands;

import at.lowdfx.upgradeFurnace.UpgradeFurnace;
import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService;
import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService.FurnaceInfo;
import at.lowdfx.upgradeFurnace.services.FurnaceUpgradeService.UpgradeResult;
import at.lowdfx.upgradeFurnace.services.HologramManager;
import at.lowdfx.upgradeFurnace.util.Configuration;
import at.lowdfx.upgradeFurnace.util.Messages;
import at.lowdfx.upgradeFurnace.util.Perms;
import at.lowdfx.upgradeFurnace.util.Perms.Perm;
import at.lowdfx.upgradeFurnace.util.Utilities;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.block.Furnace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

/**
 * Registers and handles the /furnace command tree and /upgrade shortcut.
 */
@SuppressWarnings("UnstableApiUsage")
public final class UpgradeCommand {

    private final UpgradeFurnace plugin;
    private final FurnaceUpgradeService upgradeService;
    private final HologramManager hologramManager;

    public UpgradeCommand(
            UpgradeFurnace plugin,
            FurnaceUpgradeService upgradeService,
            HologramManager hologramManager
    ) {
        this.plugin = plugin;
        this.upgradeService = upgradeService;
        this.hologramManager = hologramManager;
    }

    public LiteralCommandNode<CommandSourceStack> command() {
        LiteralArgumentBuilder<CommandSourceStack> command =
                LiteralArgumentBuilder.<CommandSourceStack>literal("furnace")
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("upgrade")
                        .requires(source -> source.getSender() instanceof Player
                                && Perms.check(source, Perm.UPGRADE_FURNACE))
                        .executes(context -> executeUpgrade(context.getSource())))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("info")
                        .requires(source -> source.getSender() instanceof Player
                                && Perms.check(source, Perm.UPGRADE_FURNACE))
                        .executes(context ->
                                executeInfo((Player) context.getSource().getSender())))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("admin")
                        .requires(this::hasAnyAdminPermission)
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reload")
                                .requires(source -> Perms.check(source, Perm.ADMIN_RELOAD))
                                .executes(context ->
                                        executeReload(context.getSource().getSender())))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("set")
                                .requires(source -> source.getSender() instanceof Player
                                        && Perms.check(source, Perm.ADMIN_FURNACE))
                                .then(com.mojang.brigadier.builder.RequiredArgumentBuilder
                                        .<CommandSourceStack, Integer>argument(
                                                "level",
                                                IntegerArgumentType.integer(1, 5))
                                        .executes(context -> executeSet(
                                                (Player) context.getSource().getSender(),
                                                IntegerArgumentType.getInteger(
                                                        context, "level")))))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("reset")
                                .requires(source -> source.getSender() instanceof Player
                                        && Perms.check(source, Perm.ADMIN_FURNACE))
                                .executes(context ->
                                        executeReset(
                                                (Player) context.getSource().getSender()))));

        if (Configuration.HOLOGRAMS_ENABLED) {
            command.then(LiteralArgumentBuilder.<CommandSourceStack>literal("hologram")
                    .requires(source -> source.getSender() instanceof Player
                            && Perms.check(source, Perm.UPGRADE_FURNACE))
                    .executes(context ->
                            executeHologramStatus(
                                    (Player) context.getSource().getSender()))
                    .then(LiteralArgumentBuilder.<CommandSourceStack>literal("on")
                            .executes(context -> executeHologramVisibility(
                                    (Player) context.getSource().getSender(),
                                    true)))
                    .then(LiteralArgumentBuilder.<CommandSourceStack>literal("off")
                            .executes(context -> executeHologramVisibility(
                                    (Player) context.getSource().getSender(),
                                    false))));
        }

        return command.build();
    }

    public LiteralCommandNode<CommandSourceStack> upgradeShortcut() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal("upgrade")
                .requires(source -> source.getSender() instanceof Player
                        && Perms.check(source, Perm.UPGRADE_FURNACE))
                .executes(context -> executeUpgrade(context.getSource()))
                .build();
    }

    private boolean hasAnyAdminPermission(CommandSourceStack source) {
        return Perms.check(source, Perm.ADMIN_RELOAD)
                || Perms.check(source, Perm.ADMIN_FURNACE);
    }

    private int executeUpgrade(CommandSourceStack source) {
        if (!(source.getSender() instanceof Player player)
                || !Perms.check(source, Perm.UPGRADE_FURNACE)) {
            return 0;
        }
        return execute(player);
    }

    private int execute(Player player) {
        Furnace furnace = getTargetFurnace(player);
        if (furnace == null) {
            Utilities.negativeSound(player);
            player.sendMessage(UpgradeFurnace.serverMessage(
                    Messages.component("furnace.no-target").color(NamedTextColor.RED)));
            return 0;
        }

        UpgradeResult result = upgradeService.upgrade(player, furnace);
        return switch (result.status()) {
            case SUCCESS -> {
                Utilities.positiveSound(player);
                player.sendMessage(UpgradeFurnace.serverMessage(
                        Messages.component(
                                        "furnace.upgraded",
                                        Map.of("level", String.valueOf(result.level())))
                                .color(NamedTextColor.GREEN)));
                yield 1;
            }
            case MAX_LEVEL -> {
                Utilities.negativeSound(player);
                player.sendMessage(UpgradeFurnace.serverMessage(
                        Messages.component("furnace.max-level").color(NamedTextColor.YELLOW)));
                yield 1;
            }
            case INVALID_MATERIAL -> {
                Utilities.negativeSound(player);
                player.sendMessage(UpgradeFurnace.serverMessage(
                        Messages.component("furnace.invalid-material").color(NamedTextColor.RED)));
                yield 0;
            }
            case MISSING_MATERIAL -> {
                Utilities.negativeSound(player);
                player.sendMessage(UpgradeFurnace.serverMessage(
                        Messages.component(
                                        "furnace.missing-material",
                                        Map.of(
                                                "amount", String.valueOf(result.requiredAmount()),
                                                "material", result.material().name()
                                                        .toLowerCase(Locale.ROOT),
                                                "level", String.valueOf(result.level())))
                                .color(NamedTextColor.RED)));
                yield 0;
            }
            case MISSING_SMELTING -> {
                Utilities.negativeSound(player);
                player.sendMessage(UpgradeFurnace.serverMessage(
                        Messages.component("furnace.missing-smelting", Map.of(
                                "required", String.valueOf(result.requiredSmelting()),
                                "current", String.valueOf(result.currentSmelting()),
                                "level", String.valueOf(result.level())))
                                .color(NamedTextColor.RED)));
                yield 0;
            }
            case SKILL_UNAVAILABLE -> {
                Utilities.negativeSound(player);
                player.sendMessage(UpgradeFurnace.serverMessage(
                        Messages.component("furnace.skill-unavailable").color(NamedTextColor.RED)));
                yield 0;
            }
            case MISSING_XP -> {
                Utilities.negativeSound(player);
                player.sendMessage(UpgradeFurnace.serverMessage(
                        Messages.component(
                                        "furnace.missing-xp",
                                        Map.of(
                                                "xp", String.valueOf(result.requiredXp()),
                                                "level", String.valueOf(result.level())))
                                .color(NamedTextColor.RED)));
                yield 0;
            }
        };
    }

    private int executeInfo(Player player) {
        Furnace furnace = requireTargetFurnace(player);
        if (furnace == null) {
            return 0;
        }

        FurnaceInfo info = upgradeService.inspect(furnace);
        Component message = Messages.component("furnace.info-header")
                .color(NamedTextColor.AQUA)
                .append(Component.newline())
                .append(Messages.component(
                                "furnace.info-level",
                                Map.of(
                                        "level", String.valueOf(info.level()),
                                        "max", String.valueOf(info.maxLevel())))
                        .color(NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Messages.component(
                                "furnace.info-speed",
                                Map.of("speed", formatNumber(info.speedMultiplier())))
                        .color(NamedTextColor.WHITE))
                .append(Component.newline())
                .append(Messages.component(
                                "furnace.info-bonus",
                                Map.of(
                                        "chance", formatNumber(info.bonusChance() * 100.0),
                                        "items", String.valueOf(info.bonusMaxItems())))
                        .color(NamedTextColor.WHITE));

        if (info.level() >= info.maxLevel()) {
            message = message.append(Component.newline())
                    .append(Messages.component("furnace.info-max")
                            .color(NamedTextColor.GREEN));
        } else if (info.nextMaterial() == null) {
            message = message.append(Component.newline())
                    .append(Messages.component("furnace.invalid-material")
                            .color(NamedTextColor.RED));
        } else {
            String nextKey = "furnace.info-next";
            Map<String, String> nextValues = Map.of(
                    "amount", String.valueOf(info.nextAmount()),
                    "material", info.nextMaterial().name().toLowerCase(Locale.ROOT),
                    "xp", String.valueOf(info.nextXp()));
            if (upgradeService.usesSmelting()) {
                String current;
                try {
                    current = String.valueOf(upgradeService.getSmeltingLevel(player));
                } catch (IllegalStateException e) {
                    player.sendMessage(UpgradeFurnace.serverMessage(
                            Messages.component("furnace.skill-unavailable").color(NamedTextColor.RED)));
                    return 0;
                }
                nextKey = "furnace.info-next-smelting";
                nextValues = Map.of(
                        "amount", String.valueOf(info.nextAmount()),
                        "material", info.nextMaterial().name().toLowerCase(Locale.ROOT),
                        "required", String.valueOf(Configuration.getRequirementSmeltingLevel(info.level() + 1)),
                        "current", current);
            }
            message = message.append(Component.newline())
                    .append(Messages.component(
                                    nextKey, nextValues)
                            .color(NamedTextColor.GOLD));
        }

        player.sendMessage(UpgradeFurnace.serverMessage(message));
        return 1;
    }

    private int executeReload(CommandSender sender) {
        plugin.reloadPluginSettings();
        if (sender instanceof Player player) {
            Utilities.positiveSound(player);
        }
        sender.sendMessage(UpgradeFurnace.serverMessage(
                Messages.component("admin.reloaded").color(NamedTextColor.GREEN)));
        return 1;
    }

    private int executeSet(Player player, int level) {
        Furnace furnace = requireTargetFurnace(player);
        if (furnace == null) {
            return 0;
        }

        upgradeService.setLevel(furnace, level);
        Utilities.positiveSound(player);
        player.sendMessage(UpgradeFurnace.serverMessage(
                Messages.component(
                                "admin.level-set",
                                Map.of("level", String.valueOf(level)))
                        .color(NamedTextColor.GREEN)));
        return 1;
    }

    private int executeReset(Player player) {
        Furnace furnace = requireTargetFurnace(player);
        if (furnace == null) {
            return 0;
        }

        if (!upgradeService.resetLevel(furnace)) {
            Utilities.negativeSound(player);
            player.sendMessage(UpgradeFurnace.serverMessage(
                    Messages.component("admin.not-upgraded").color(NamedTextColor.YELLOW)));
            return 0;
        }

        Utilities.positiveSound(player);
        player.sendMessage(UpgradeFurnace.serverMessage(
                Messages.component("admin.reset").color(NamedTextColor.GREEN)));
        return 1;
    }

    private int executeHologramStatus(Player player) {
        if (!Configuration.HOLOGRAMS_ENABLED) {
            player.sendMessage(UpgradeFurnace.serverMessage(
                    Messages.component("holograms.globally-disabled")
                            .color(NamedTextColor.YELLOW)));
            return 0;
        }

        boolean visible = hologramManager.isVisibleTo(player);
        player.sendMessage(UpgradeFurnace.serverMessage(
                Messages.component(visible ? "holograms.status-shown" : "holograms.status-hidden")
                        .color(NamedTextColor.GREEN)));
        return 1;
    }

    private int executeHologramVisibility(Player player, boolean visible) {
        if (!Configuration.HOLOGRAMS_ENABLED) {
            player.sendMessage(UpgradeFurnace.serverMessage(
                    Messages.component("holograms.globally-disabled")
                            .color(NamedTextColor.YELLOW)));
            return 0;
        }

        hologramManager.setPlayerVisibility(player, visible);
        Utilities.positiveSound(player);
        player.sendMessage(UpgradeFurnace.serverMessage(
                Messages.component(visible ? "holograms.shown" : "holograms.hidden")
                        .color(NamedTextColor.GREEN)));
        return 1;
    }

    private Furnace requireTargetFurnace(Player player) {
        Furnace furnace = getTargetFurnace(player);
        if (furnace == null) {
            Utilities.negativeSound(player);
            player.sendMessage(UpgradeFurnace.serverMessage(
                    Messages.component("furnace.no-target").color(NamedTextColor.RED)));
        }
        return furnace;
    }

    private Furnace getTargetFurnace(Player player) {
        Block block = player.getTargetBlockExact(5, FluidCollisionMode.NEVER);
        if (block == null || !(block.getState() instanceof Furnace furnace)) {
            return null;
        }
        return furnace;
    }

    private String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
