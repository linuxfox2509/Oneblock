package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.Location;

public class SetHomeCommand implements SubCommand {

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final World oneBlockWorld;

    public SetHomeCommand(IslandManager islandManager, IslandStorage islandStorage, World oneBlockWorld) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.oneBlockWorld = oneBlockWorld;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command", NamedTextColor.RED));
            return true;
        }

        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) {
            sender.sendMessage(Component.text("You don't have an island.", NamedTextColor.RED));
            return true;
        }

        if (!player.getWorld().equals(oneBlockWorld) || !island.containsLocation(player.getLocation())) {

            player.sendMessage(Component.text("You must be on your own island to set your home.", NamedTextColor.RED));
            return true;
        }

        Location homeLocation = player.getLocation();

        if (homeLocation.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
            player.sendMessage(Component.text("You can't set your home while standing in the air.", NamedTextColor.RED));
            return true;
        }

        island.setHome(homeLocation.clone());
        islandStorage.saveIsland(island);

        player.sendMessage(Component.text("Home has been set.", NamedTextColor.GREEN));

        return true;
    }
}
