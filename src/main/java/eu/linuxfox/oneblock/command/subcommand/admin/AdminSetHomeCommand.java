package eu.linuxfox.oneblock.command.subcommand.admin;

import eu.linuxfox.oneblock.command.CommandUtils;
import eu.linuxfox.oneblock.island.GridPosition;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;

public class AdminSetHomeCommand implements AdminSubCommand {
    private static final String PERMISSION = "oneblock.admin.set-home";

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final IslandDeletionService islandDeletionService;
    private final World oneBlockWorld;

    public AdminSetHomeCommand(
            IslandManager islandManager,
            IslandStorage islandStorage,
            IslandDeletionService islandDeletionService,
            World oneBlockWorld
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.islandDeletionService = islandDeletionService;
        this.oneBlockWorld = oneBlockWorld;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command", NamedTextColor.RED));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /oneblock admin set-home <X> <Z>", NamedTextColor.RED));
            return;
        }

        Integer gridX = CommandUtils.parseInteger(args[2]);
        Integer gridZ = CommandUtils.parseInteger(args[3]);

        if (gridX == null || gridZ == null) {
            sender.sendMessage(Component.text("X and Z must be valid integers.",  NamedTextColor.RED));
            return;
        }

        Island island = islandManager.getIslandByPosition(
                new GridPosition(gridX, gridZ)
        );

        if (island == null) {
            sender.sendMessage(Component.text("No island exists at that grid position.", NamedTextColor.RED));
            return;
        }

        if (islandDeletionService.isDeleting(island)) {
            sender.sendMessage(Component.text("This island is currently being deleted.", NamedTextColor.YELLOW));
            return;
        }

        if (!player.getWorld().equals(oneBlockWorld) || !island.containsLocation(player.getLocation())) {
            sender.sendMessage(Component.text("You must be standing inside that island's 500x500 area.", NamedTextColor.RED));
            return;
        }

        Location location = player.getLocation();

        if (location.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
            sender.sendMessage(Component.text("You cannot set the island home while standing in air.",  NamedTextColor.RED));
            return;
        }

        island.setHome(location.clone());
        islandStorage.saveIsland(island);

        sender.sendMessage(Component.text("Set the home for island (" + gridX + ", " + gridZ + ").", NamedTextColor.GREEN));
    }

    @Override
    public String getPermission() {
        return PERMISSION;
    }

}
