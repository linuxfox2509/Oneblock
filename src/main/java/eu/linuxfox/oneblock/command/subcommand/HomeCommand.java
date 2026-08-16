package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class HomeCommand implements SubCommand {

    private final IslandManager islandManager;

    public HomeCommand(IslandManager islandManager) {
        this.islandManager = islandManager;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command.", NamedTextColor.RED));
            return true;
        }

        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) {
            player.sendMessage(Component.text("You don't have an Island.", NamedTextColor.RED));
            return true;
        }

        player.teleport(island.getHome());
        player.sendMessage(Component.text("Teleported to Island.", NamedTextColor.GREEN));

        return true;
    }
}
