<p align="center">
  <img src="logo.png" alt="UpgradeFurnace Logo" width="200">
</p>

# UpgradeFurnace

_**A PaperMC 1.21.11 and 26.1.2 plugin to upgrade furnaces for faster smelting and bonus yields.**_

## Features

- **5 Tiered Furnace Upgrades**: Upgrade a furnace from level 1 to 5 using configurable materials.
- **Configurable Smelting Speed**: Each level has its own speed multiplier.
- **Configurable Bonus Yield**: Higher-level furnaces can produce controlled extra output when smelting.
- **Holographic Display**: Shows the current upgrade level above upgraded furnaces.
- **Spiral Particle Animation**: Displays continuous ascending spiral particles around upgraded furnaces.
- **Configurable Particles**: Define custom particles for each level in `config.yml`.
- **Configurable Requirements**: Define materials, amounts, XP levels, speed, and particles per level.
- **Particle Controls**: Enable/disable particles globally or only show them while a furnace is active.
- **Organized Brigadier Commands**: `/furnace` groups upgrades, information, holograms, and administration with tab completion. `/upgrade` remains a quick upgrade shortcut.

## Requirements

- PaperMC 1.21.11 with Java 21+
- PaperMC 26.1.2 with Java 25+

## Installation

1. Download or build the latest `UpgradeFurnace.jar` for this repository.
2. Place the JAR into your server's `plugins` folder.
3. Start the server to generate the default configuration and permissions files.

## Configuration (`config.yml`)

These are the current default settings shipped with the plugin:

```yaml
config-version: 2

language: "en"

basic:
  server-name: "MyServer"

holograms:
  enabled: true

particles:
  enabled: true
  only_when_active: false

requirements:
  # UpgradeFurnace is intentionally balanced around levels 1 through 5.
  1:
    material: COPPER_INGOT
    amount: 16
    xp_levels: 5
    speed_multiplier: 1.15      # Divides cook time (higher = faster)
    particle: SMOKE             # Particle effect for this level
    bonus_chance: 0.0           # Chance from 0.0 to 1.0 for bonus output
    bonus_max_items: 0          # Max extra items if bonus output succeeds

  2:
    material: IRON_INGOT
    amount: 24
    xp_levels: 10
    speed_multiplier: 1.35
    particle: FLAME
    bonus_chance: 0.10
    bonus_max_items: 1

  3:
    material: GOLD_INGOT
    amount: 16
    xp_levels: 15
    speed_multiplier: 2.0
    particle: CLOUD
    bonus_chance: 0.16
    bonus_max_items: 1

  4:
    material: DIAMOND
    amount: 8
    xp_levels: 20
    speed_multiplier: 4.0
    particle: SOUL_FIRE_FLAME
    bonus_chance: 0.22
    bonus_max_items: 2

  5:
    material: NETHERITE_INGOT
    amount: 1
    xp_levels: 30
    speed_multiplier: 8.0
    particle: TOTEM_OF_UNDYING
    bonus_chance: 0.30
    bonus_max_items: 2
```

### Configuration Options

| Option | Description |
|--------|-------------|
| `config-version` | Internal schema version used for automatic configuration migrations. |
| `language` | Message language for player-facing plugin text. Supported defaults are `en` and `de`. |
| `basic.server-name` | Prefix shown before plugin messages. |
| `holograms.enabled` | Shows or hides level holograms above upgraded furnaces. Existing holograms are removed as their chunks load. |
| `particles.enabled` | Enables or disables all furnace particle effects. |
| `particles.only_when_active` | If enabled, spiral particles only appear while the furnace is actively burning. |
| `requirements.<level>.material` | Material needed for that upgrade level. |
| `requirements.<level>.amount` | Amount of material required. |
| `requirements.<level>.xp_levels` | XP levels required for the upgrade. |
| `requirements.<level>.speed_multiplier` | Cook time divisor. Higher values mean faster smelting. |
| `requirements.<level>.particle` | Particle effect displayed for that upgrade level. |
| `requirements.<level>.bonus_chance` | Chance from `0.0` to `1.0` to add bonus output after smelting. |
| `requirements.<level>.bonus_max_items` | Maximum extra items added when bonus output succeeds. |

When `holograms.enabled` is `false` at startup, `/furnace hologram` is not
registered. Changing this setting with `/furnace admin reload` updates holograms
immediately, but changing command availability requires a server restart.
Individual visibility preferences are stored in each player's persistent
Minecraft data and do not create another plugin file.

### Configuration Migration

When `config-version` is older than the bundled schema, UpgradeFurnace:

1. Creates a timestamped backup of the existing configuration.
2. Starts with the newest default configuration structure.
3. Migrates recognized values that still have valid types and ranges.
4. Uses current defaults for missing or invalid values.
5. Removes settings that are no longer supported.
6. Replaces `config.yml` only after the migrated file is written successfully.

Current-version configurations are not replaced. If a supported setting is
missing, only that setting is restored from the bundled defaults.

## Commands

Run `/help furnace_commands` for the complete in-game command reference.

| Command | Permission | Description |
|---------|------------|-------------|
| `/furnace upgrade` | `upgradefurnace.upgrade.furnace` | Upgrade the furnace you are looking at. |
| `/upgrade` | `upgradefurnace.upgrade.furnace` | Shortcut for upgrading the furnace you are looking at. |
| `/furnace info` | `upgradefurnace.upgrade.furnace` | Show the targeted furnace's current stats and next upgrade cost. |
| `/furnace hologram` | `upgradefurnace.upgrade.furnace` | Show your current hologram visibility setting. Registered only when server holograms are enabled at startup. |
| `/furnace hologram <on\|off>` | `upgradefurnace.upgrade.furnace` | Show or hide furnace holograms for the current player. |
| `/furnace admin reload` | `upgradefurnace.admin.reload` | Reload configuration, messages, particles, and holograms. |
| `/furnace admin set <1-5>` | `upgradefurnace.admin.furnace` | Set the targeted furnace to a specific level without charging a cost. |
| `/furnace admin reset` | `upgradefurnace.admin.furnace` | Remove the targeted furnace's upgrade. |

**Usage**: Look at a furnace within range and run `/furnace upgrade` or its `/upgrade` shortcut. The plugin checks your inventory and XP levels for the configured upgrade requirements.

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `upgradefurnace.upgrade.furnace` | `true` | Allows players to upgrade and inspect furnaces or toggle their holograms. |
| `upgradefurnace.admin.reload` | `op` | Allows administrators to reload plugin settings. |
| `upgradefurnace.admin.furnace` | `op` | Allows administrators to set and reset furnace levels. |

## Events & Effects

- **FurnaceStartSmeltEvent**: Reduces cook time based on `speed_multiplier`.
- **FurnaceSmeltEvent**: Applies bonus yield at higher levels.
- **BlockBreakEvent**: Saves the furnace upgrade level onto the dropped furnace item.
- **BlockPlaceEvent**: Restores the upgrade level when a saved furnace item is placed again.
- **BlockExplodeEvent / EntityExplodeEvent**: Preserves upgraded furnace items and removes their holograms when explosions destroy them.
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

Run the automated test suite:

```shell
./gradlew test
```

Start either supported Paper test server (each uses its own directory under
`run/`):

```shell
./gradlew runServer       # Paper 1.21.11 / Java 21
./gradlew runServer2612   # Paper 26.1.2 / Java 25
```

## License

This plugin is released under the GPL License. See [LICENSE](LICENSE) for details.

---
Made by LowdFX and Menkhaz
