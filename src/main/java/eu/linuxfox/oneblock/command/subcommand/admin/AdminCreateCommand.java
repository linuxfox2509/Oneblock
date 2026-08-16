package eu.linuxfox.oneblock.command.subcommand.admin;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.command.CommandUtils;
import eu.linuxfox.oneblock.island.GridPosition;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandAllocator;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Locale;

public class AdminCreateCommand implements AdminSubCommand {
    private static final String PERMISSION = "oneblock.admin.create";

    private final IslandManager islandManager;
    private final IslandAllocator islandAllocator;
    private final IslandStorage islandStorage;
    private final OneBlockBossBar oneBlockBossBar;
    private final World oneBlockWorld;

    public AdminCreateCommand(
            IslandManager islandManager,
            IslandAllocator islandAllocator,
            IslandStorage islandStorage,
            OneBlockBossBar oneBlockBossBar,
            World oneBlockWorld
    ) {
        this.islandManager = islandManager;
        this.islandAllocator = islandAllocator;
        this.islandStorage = islandStorage;
        this.oneBlockBossBar = oneBlockBossBar;
        this.oneBlockWorld = oneBlockWorld;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sendUsage(sender);
            return;
        }

        Integer gridX = CommandUtils.parseInteger(args[2]);
        Integer gridZ = CommandUtils.parseInteger(args[3]);

        if (gridX == null || gridZ == null) {
            sender.sendMessage(Component.text("X and Z must be valid integers.", NamedTextColor.RED));
            return;
        }

        GridPosition position = new GridPosition(gridX, gridZ);

        if (islandManager.hasIslandAtPosition(position)) {
            sender.sendMessage(Component.text("An island already exists at that grid position.", NamedTextColor.RED));
            return;
        }

        OfflinePlayer targetPlayer = Bukkit.getOfflinePlayer(args[4]);

        if (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline()) {
            sender.sendMessage(Component.text("That player has never joined this server.", NamedTextColor.RED));
            return;
        }

        if (islandManager.hasIsland(targetPlayer.getUniqueId())) {
            sender.sendMessage(Component.text(getPlayerName(targetPlayer, args[4]) + " already owns an island.", NamedTextColor.RED));
            return;
        }

        boolean force = args.length >= 6 && args[5].equalsIgnoreCase("force");

        if (!force && !islandAllocator.isAreaClear(position)) {
            sender.sendMessage(Component.text("The 200x200 clearance area is not empty. Use 'force' to override this check.", NamedTextColor.RED));
            return;
        }

        Island island = createIsland(targetPlayer, args[4], gridX, gridZ);

        createStartingPlatform(island);
        registerIsland(island);

        sender.sendMessage(Component.text("Created island at grid (" + gridX + ", " + gridZ + ") for " + island.getOwnerName() + ".", NamedTextColor.GREEN));
    }

    @Override
    public String getPermission() {
        return PERMISSION;
    }

    private Island createIsland(
            OfflinePlayer targetPlayer,
            String fallbackName,
            int gridX,
            int gridZ
    ) {
        return new Island(
                targetPlayer.getUniqueId(),
                getPlayerName(targetPlayer, fallbackName),
                gridX,
                gridZ,
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

    private String getPlayerName(OfflinePlayer player, String fallbackName) {
        return player.getName() != null
                ? player.getName()
                : fallbackName;
    }

    private List<String> getForceSuggestion(String input) {
        if ("force".startsWith(input.toLowerCase(Locale.ROOT))) {
            return List.of("force");
        }

        return List.of();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /oneblock admin create <X> <Z> <player> [force]", NamedTextColor.RED));
    }

}
