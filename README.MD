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
- Per-island biome selection
- Persistent island data
- Tab completion
- Void world generation

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
| `/oneblock home` | Teleport to your island |
| `/oneblock set-home` | Set your island home |
| `/oneblock reset-home` | Reset your island home |
| `/oneblock info [player]` | Show information about an island |
| `/oneblock visit <player>` | Visit another player's island |

### Operator Commands

The following commands require operator privileges.

| Command | Description |
|---|---|
| `/oneblock biome` | Show your island's biome |
| `/oneblock biome <biome>` | Change your island's biome |
| `/oneblock biome reset` | Reset your island biome to Plains |
| `/oneblock admin create <X> <Z> <player> [force]` | Create an island at a specific grid position |
| `/oneblock admin delete <X> <Z>` | Delete an island |
| `/oneblock admin set-stage <X> <Z> <stage>` | Change an island's stage |
| `/oneblock admin set-progress <X> <Z> <progress>` | Change an island's progress |

Additional admin commands may be available depending on the version.

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

Reward chests temporarily replace the OneBlock. When the chest is broken, its remaining contents are dropped and normal OneBlock generation continues.

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

Island information and progression are stored in:

```text
plugins/OneBlock/islands.yml
```

This includes information such as:

- Owner
- Grid position
- Stage
- Progress
- Home location
- Reward chest state
- Island biome

## Biomes

Operators can change the biome of their island:

```text
/oneblock biome desert
```

The biome can be reset to Plains with:

```text
/oneblock biome reset
```

Biome changes are processed over multiple server ticks to avoid performing the entire operation in a single tick.

## Island Deletion

Island deletion requires confirmation:

```text
/oneblock delete
/oneblock confirm
```

The amount of time a player has to confirm the action can be configured in `config.yml`.

Island deletion removes the island area and releases its grid position so that it can be used again.

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
- Gradle
- Paper API 1.21.11

Project structure:

```text
src/main/java/eu/linuxfox/oneblock/
├── bossbar/
├── command/
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

## Configuration

The default configuration is located at:

```text
src/main/resources/config.yml
```

When the plugin is first started, it is copied to:

```text
plugins/OneBlock/config.yml
```

Restart the server after making configuration changes unless otherwise specified.

## License

This project is licensed under the GNU General Public License v3.0.

See [LICENSE](LICENSE) for details.