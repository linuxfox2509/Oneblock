# OneBlock

A configurable OneBlock plugin for Minecraft servers running Paper 1.21.11.

Each player can create their own island in a shared void world. Breaking the central OneBlock generates a new block and progresses the island through configurable stages.

## Features

- Individual OneBlock islands for each player
- Automatic island allocation on a 500×500 grid
- Configurable progression stages
- Weighted random block generation
- Progress boss bar
- Stage advancement
- Configurable milestone reward chests
- Configurable chest loot
- Island homes
- Island visiting
- Island deletion with confirmation
- Configurable confirmation timeout
- Offline player support for island information
- Admin island management
- Granular admin permissions
- Per-island biome selection
- Persistent island data
- Tab completion
- Void world generation
- Automatic teleport to the normal world spawn when joining

## Requirements

- Java 21
- Paper 1.21.11

The plugin is built against the Paper API and is intended for Paper servers.

## Installation

1. Download the plugin JAR.
2. Place it in your server's `plugins` directory.
3. Start the server.
4. The plugin will automatically create its configuration files and the `oneblock` world.

## Commands

### Player Commands

| Command | Description |
|---|---|
| `/oneblock` | Show OneBlock commands |
| `/oneblock help` | Show command help |
| `/oneblock create` | Create your island |
| `/oneblock delete` | Request deletion of your island |
| `/oneblock confirm` | Confirm a pending action |
| `/oneblock home` | Teleport to your island home |
| `/oneblock set-home` | Set your island home |
| `/oneblock reset-home` | Reset your island home |
| `/oneblock info [player]` | Show information about an island |
| `/oneblock visit <player>` | Visit another player's island |

### Biome Commands

Biome commands require the `oneblock.biome` permission.

| Command | Description |
|---|---|
| `/oneblock biome` | Show your island's biome |
| `/oneblock biome <biome>` | Change your island's biome |
| `/oneblock biome reset` | Reset your island biome to Plains |

### Admin Commands

Admin commands use individual permission nodes, allowing server administrators to control access to each action separately.

| Command | Permission | Description |
|---|---|---|
| `/oneblock admin create <X> <Z> <player> [force]` | `oneblock.admin.create` | Create an island at a specific grid position |
| `/oneblock admin delete <X> <Z>` | `oneblock.admin.delete` | Delete an island |
| `/oneblock admin set-home <X> <Z>` | `oneblock.admin.set-home` | Set an island's home to your current location |
| `/oneblock admin reset-home <X> <Z>` | `oneblock.admin.reset-home` | Reset an island's home to its OneBlock |
| `/oneblock admin set-stage <X> <Z> <stage>` | `oneblock.admin.set-stage` | Change an island's stage |
| `/oneblock admin set-progress <X> <Z> <progress>` | `oneblock.admin.set-progress` | Change an island's progress |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `oneblock.biome` | OP | Allows changing an island biome |
| `oneblock.admin` | OP | Grants all OneBlock admin permissions |
| `oneblock.admin.create` | False | Allows creating islands for players, including use of the `force` option |
| `oneblock.admin.delete` | False | Allows deleting islands |
| `oneblock.admin.set-home` | False | Allows setting another island's home |
| `oneblock.admin.reset-home` | False | Allows resetting another island's home |
| `oneblock.admin.set-stage` | False | Allows changing an island's stage |
| `oneblock.admin.set-progress` | False | Allows changing an island's progress |

Operators receive `oneblock.biome` and `oneblock.admin` by default. The `oneblock.admin` permission grants all individual admin permissions through child permissions.

Individual permissions can be granted or denied using a permissions plugin.

## Progression

Every island starts at Stage 1.

When the OneBlock is broken, the plugin:

1. Calculates the normal Minecraft drops for the block.
2. Increases the island's progress.
3. Generates a new block from the current stage.
4. Advances to the next stage once the required progress is reached.

Blocks are selected using configurable weighted probabilities.

Example:

```yaml
progression:
  base-required: 5
  multiplier: 1.5

stages:
  1:
    name: "Plains"
    blocks:
      GRASS_BLOCK: 25
      DIRT: 30
      STONE: 20
      OAK_LOG: 10
      OAK_LEAVES: 10
      COAL_ORE: 5

  2:
    name: "Underground"
    blocks:
      STONE: 35
      DEEPSLATE: 20
      COAL_ORE: 15
      IRON_ORE: 10
      COPPER_ORE: 10
      ANDESITE: 5
      GRAVEL: 5
```

## Milestone Chests

Progression can generate reward chests containing configurable loot.

Reward chests temporarily replace the OneBlock. When a reward chest is broken:

- Its remaining contents are dropped.
- The chest itself is dropped.
- Normal OneBlock generation continues.
- Breaking the reward chest does not increase island progress.

Milestones and their loot can be configured in `config.yml`.

## Island System

Islands are placed on a grid with 500 blocks between island centers.

The first island is located at:

```text
X: 0
Y: 100
Z: 0
```

Additional islands are allocated around the origin.

Players are sent to the normal world spawn when they join the server. They can return to their island using:

```text
/oneblock home
```

Island information and progression are stored in:

```text
plugins/OneBlock/islands.yml
```

This includes information such as:

- Owner
- Owner name
- Grid position
- Stage
- Progress
- Home location
- Reward chest state
- Island biome

## Biomes

Players with the `oneblock.biome` permission can change the biome of their island:

```text
/oneblock biome desert
```

The biome can be reset to Plains with:

```text
/oneblock biome reset
```

Biome changes are processed over multiple server ticks to avoid performing the entire operation in a single tick.

## Island Deletion

Players can delete their own island using:

```text
/oneblock delete
/oneblock confirm
```

Administrators with the required permission can request deletion of an island at a specific grid position:

```text
/oneblock admin delete <X> <Z>
/oneblock confirm
```

The amount of time available to confirm an action can be configured in `config.yml`.

Island deletion removes the island area and releases its grid position so that it can be used again.

## Configuration

The default configuration is located at:

```text
src/main/resources/config.yml
```

When the plugin is first started, it is copied to:

```text
plugins/OneBlock/config.yml
```

The configuration controls features such as:

- Confirmation timeout
- Progression requirements
- Stage names
- Block pools and weights
- Reward chest intervals
- Reward chest rolls
- Reward chest loot

Restart the server after making configuration changes unless otherwise specified.

## Building

Clone the repository:

```bash
git clone https://github.com/linuxfox2509/OneBlock.git
cd OneBlock
```

Build using the included Gradle wrapper.

### Linux / macOS

```bash
./gradlew build
```

### Windows

```powershell
.\gradlew.bat build
```

The compiled JAR will be located in:

```text
build/libs/
```

## Development

The project uses:

- Java 21
- Gradle with Kotlin DSL
- Paper API 1.21.11

Project structure:

```text
src/main/java/eu/linuxfox/oneblock/
├── bossbar/
├── command/
│   └── subcommand/
│       └── admin/
├── island/
├── listener/
├── progression/
├── storage/
├── world/
└── OneBlockPlugin.java

src/main/resources/
├── config.yml
└── plugin.yml
```

## License

This project is licensed under the GNU General Public License v3.0.

See [LICENSE](LICENSE) for details.