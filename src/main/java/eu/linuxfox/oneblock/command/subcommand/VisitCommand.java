package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.island.IslandDeletionService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class VisitCommand implements SubCommand {

    private final IslandManager islandManager;
    private final IslandDeletionService islandDeletionService;

    public VisitCommand(IslandManager islandManager, IslandDeletionService islandDeletionService) {
        this.islandManager = islandManager;
        this.islandDeletionService = islandDeletionService;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /oneblock visit <player>", NamedTextColor.RED));
            return true;
        }

        Island island = islandManager.getIslandByOwnerName(args[1]);

        if (island == null) {
            player.sendMessage(Component.text("No island found for that player.", NamedTextColor.RED));
            return true;
        }

        if (islandDeletionService.isDeleting(island)) {
            player.sendMessage(Component.text("This island is being deleted.", NamedTextColor.YELLOW));
            return true;
        }

        player.teleport(island.getHome());
        player.sendMessage(Component.text("Teleported to " + island.getOwnerName() + "'s island.", NamedTextColor.GREEN));

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
}
