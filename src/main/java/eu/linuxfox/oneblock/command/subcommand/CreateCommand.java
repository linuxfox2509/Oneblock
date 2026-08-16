package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.island.GridPosition;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.island.IslandAllocator;
import eu.linuxfox.oneblock.storage.IslandStorage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.Material;
import org.bukkit.Location;

public class CreateCommand implements SubCommand {
    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final IslandAllocator islandAllocator;
    private final OneBlockBossBar oneBlockBossBar;
    private final World oneBlockWorld;

    public CreateCommand(
            IslandManager islandManager,
            IslandStorage islandStorage,
            IslandAllocator islandAllocator,
            OneBlockBossBar oneBlockBossBar,
            World oneBlockWorld
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.islandAllocator = islandAllocator;
        this.oneBlockBossBar = oneBlockBossBar;
        this.oneBlockWorld = oneBlockWorld;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command", NamedTextColor.RED));
            return true;
        }

        if (islandManager.hasIsland(player.getUniqueId())) {
            player.sendMessage(Component.text("You already have an island.", NamedTextColor.RED));
            return true;
        }

        GridPosition position = islandAllocator.findNextPosition();

        if (position == null) {
            player.sendMessage(Component.text("Could not find a suitable location for your island", NamedTextColor.RED));
            return true;
        }

        Island island = createIsland(player, position);

        createStartingPlatform(island);
        registerIsland(island);

        player.teleport(island.getHome());

        player.sendMessage(Component.text("Island created at grid (" + position.x() + ", " + position.z() + ").", NamedTextColor.GREEN));
        return true;
    }

    private Island createIsland(Player player, GridPosition position) {
        return new Island(
                player.getUniqueId(),
                player.getName(),
                position.x(),
                position.z(),
                oneBlockWorld
        );
    }

    private void createStartingPlatform(Island island) {
        Location oneBlockLocation = island.getOneBlockLocation(oneBlockWorld);

        createObsidianPlatform(oneBlockLocation);
        createBedrockFoundation(oneBlockLocation);
        createInitialOneBlock(oneBlockLocation);
    }

    private void createObsidianPlatform(Location oneBlockLocation) {
        for (int xOffset = -1; xOffset <= 1; xOffset++) {
            for (int zOffset = -1; zOffset <= 1; zOffset++) {
                Location location = oneBlockLocation.clone().add(xOffset, -1, zOffset);
                location.getBlock().setType(Material.OBSIDIAN);
            }
        }
    }

    private void createBedrockFoundation(Location oneBlockLocation) {
        oneBlockLocation.clone().add(0, -1, 0).getBlock().setType(Material.BEDROCK);
    }

    private void createInitialOneBlock(Location oneBlockLocation) {
        oneBlockLocation.getBlock().setType(Material.GRASS_BLOCK);
    }

    private void registerIsland(Island island) {
        islandManager.addIsland(island);
        islandStorage.saveIsland(island);
        oneBlockBossBar.update(island);
    }
}
