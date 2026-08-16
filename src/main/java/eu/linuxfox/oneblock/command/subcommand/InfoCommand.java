package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InfoCommand implements SubCommand {

    private final IslandManager islandManager;
    private final World oneBlockWorld;

    public InfoCommand(IslandManager islandManager, World oneBlockWorld) {
        this.islandManager = islandManager;
        this.oneBlockWorld = oneBlockWorld;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Island island;

        if (args.length == 1) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(Component.text("Only players can use this command!", NamedTextColor.RED));
                return true;
            }

            island = islandManager.getIsland(player.getUniqueId());
        } else {
            island = islandManager.getIslandByOwnerName(args[1]);
        }

        if (island == null) {
            sender.sendMessage(Component.text("No island found for that player.", NamedTextColor.RED));
            return true;
        }

        sendIslandInfo(sender, island);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length != 2) {
            return List.of();
        }

        String input = args[1].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();

        for (Island island : islandManager.getAllIslands()) {
            String ownerName = island.getOwnerName();

            if (ownerName.toLowerCase(Locale.ROOT).startsWith(input)) {
                suggestions.add(ownerName);
            }
        }

        return suggestions;
    }

    private void sendIslandInfo(CommandSender sender, Island island) {
        Location oneBlockLocation = island.getOneBlockLocation(oneBlockWorld);

        sender.sendMessage(Component.text(
                "----- OneBlock Island -----",
                NamedTextColor.GOLD
        ));

        sender.sendMessage(Component.text(
                "Owner: " + island.getOwnerName(),
                NamedTextColor.GRAY
        ));

        sender.sendMessage(Component.text(
                "Grid: (" + island.getGridX() + ", " + island.getGridZ() + ")",
                NamedTextColor.GRAY
        ));

        sender.sendMessage(Component.text(
                "OneBlock: (" +
                        oneBlockLocation.getBlockX() + ", " +
                        oneBlockLocation.getBlockY() + ", " +
                        oneBlockLocation.getBlockZ() + ")",
                NamedTextColor.GRAY
        ));

        sender.sendMessage(Component.text(
                "Stage: " + island.getStage(),
                NamedTextColor.GRAY
        ));

        sender.sendMessage(Component.text(
                "Progress: " + island.getProgress(),
                NamedTextColor.GRAY
        ));

        sender.sendMessage(Component.text(
                "Biome: " + island.getBiome().getKey().getKey(),
                NamedTextColor.GRAY
        ));
    }
}



