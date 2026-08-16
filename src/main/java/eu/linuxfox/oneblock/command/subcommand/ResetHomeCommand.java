package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetHomeCommand implements SubCommand {

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final World oneBlockWorld;

    public ResetHomeCommand(
            IslandManager islandManager,
            IslandStorage islandStorage,
            World oneBlockWorld
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.oneBlockWorld = oneBlockWorld;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "Only players can execute this command.",
                    NamedTextColor.RED
            ));
            return true;
        }

        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) {
            player.sendMessage(Component.text(
                    "You don't have an island.",
                    NamedTextColor.RED
            ));
            return true;
        }

        island.resetHome(oneBlockWorld);
        islandStorage.saveIsland(island);

        player.sendMessage(Component.text(
                "Island home reset to the OneBlock.",
                NamedTextColor.GREEN
        ));

        return true;
    }
}