<p align="center">
  <img src="logo.png" alt="UpgradeFurnace Logo" width="200">
</p>

# UpgradeFurnace

_**A PaperMC 1.21 - 1.21.11 plugin to upgrade furnaces for faster smelting and bonus yields.**_

## Features

- **5 Tiered Furnace Upgrades**: Upgrade a furnace from level 1 to 5 using configurable materials.
- **Configurable Smelting Speed**: Each level has its own speed multiplier.
- **Bonus Yield**: Higher-level furnaces can produce extra output when smelting.
- **Holographic Display**: Shows the current upgrade level above upgraded furnaces.
- **Spiral Particle Animation**: Displays continuous ascending spiral particles around upgraded furnaces.
- **Configurable Particles**: Define custom particles for each level in `config.yml`.
- **Configurable Requirements**: Define materials, amounts, XP levels, speed, and particles per level.
- **Particle Controls**: Enable/disable particles globally or only show them while a furnace is active.
- **Brigadier Command**: `/upgrade` upgrades the furnace you are looking at.

## Requirements

- Java 21+
- PaperMC 1.21 - 1.21.11

## Installation

1. Download the latest `UpgradeFurnace.jar` from the [SpigotMC](https://www.spigotmc.org/resources/upgrade-furnace.124315/) page.
2. Place the JAR into your server's `plugins` folder.
3. Start the server to generate the default configuration and permissions files.

## Configuration (`config.yml`)

These are the current default settings shipped with the plugin:

```yaml
language: "en"

basic:
  server-name: "MyServer"
  customhelp: true

particles:
  enabled: true
  only_when_active: false

requirements:
  1:
    material: COPPER_INGOT
    amount: 16
    xp_levels: 5
    speed_multiplier: 1.15      # Divides cook time (higher = faster)
    particle: SMOKE             # Particle effect for this level

  2:
    material: IRON_INGOT
    amount: 24
    xp_levels: 10
    speed_multiplier: 1.35
    particle: FLAME

  3:
    material: GOLD_INGOT
    amount: 16
    xp_levels: 15
    speed_multiplier: 2.0
    particle: CLOUD

  4:
    material: DIAMOND
    amount: 8
    xp_levels: 20
    speed_multiplier: 4.0
    particle: SOUL_FIRE_FLAME

  5:
    material: NETHERITE_INGOT
    amount: 1
    xp_levels: 30
    speed_multiplier: 8.0
    particle: TOTEM_OF_UNDYING
```

### Configuration Options

| Option | Description |
|--------|-------------|
| `language` | Message language for player-facing plugin text. Supported defaults are `en` and `de`. |
| `basic.server-name` | Prefix shown before plugin messages. |
| `basic.customhelp` | Enables registration with the optional LowdFX custom help system when available. |
| `particles.enabled` | Enables or disables all furnace particle effects. |
| `particles.only_when_active` | If enabled, spiral particles only appear while the furnace is actively burning. |
| `requirements.<level>.material` | Material needed for that upgrade level. |
| `requirements.<level>.amount` | Amount of material required. |
| `requirements.<level>.xp_levels` | XP levels required for the upgrade. |
| `requirements.<level>.speed_multiplier` | Cook time divisor. Higher values mean faster smelting. |
| `requirements.<level>.particle` | Particle effect displayed for that upgrade level. |

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/upgrade` | `upgradefurnace.upgrade.furnace` | Upgrade the furnace you are looking at. |

**Usage**: Look at a furnace within range and run `/upgrade`. The plugin checks your inventory and XP levels for the configured upgrade requirements.

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `upgradefurnace.upgrade.furnace` | `true` | Allows players to upgrade furnaces. |

## Events & Effects

- **FurnaceStartSmeltEvent**: Reduces cook time based on `speed_multiplier`.
- **FurnaceSmeltEvent**: Applies bonus yield at higher levels.
- **BlockBreakEvent**: Saves the furnace upgrade level onto the dropped furnace item.
- **BlockPlaceEvent**: Restores the upgrade level when a saved furnace item is placed again.
- **ChunkLoadEvent**: Re-registers upgraded furnaces for particle effects when chunks load.
- **Spiral Particles**: Continuous ascending spiral animation around upgraded furnaces.
- **Upgrade Particles**: Burst effect when upgrading a furnace.

## Localization

The plugin ships with English and German message files:

- `messages_en.yml`
- `messages_de.yml`

Set the language in `config.yml`:

```yaml
language: "en"
```

Use `language: "de"` for German. If a selected language file is missing, the plugin falls back to English. Missing message keys also fall back to English.

## Development & Contribution

1. Fork the repository.
2. Clone your fork and create a feature branch.
3. Implement changes and update the README if needed.
4. Submit a pull request describing your changes.

## License

This plugin is released under the GPL License. See [LICENSE](LICENSE) for details.

---
Made by LowdFX and Menkhaz
