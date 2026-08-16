package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.command.PendingAction;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class DeleteCommand implements SubCommand {
    private final IslandManager islandManager;
    private final IslandDeletionService islandDeletionService;
    private final Map<UUID, PendingAction> pendingActions;
    private final long confirmTimeoutMs;

    public DeleteCommand(
            IslandManager islandManager,
            IslandDeletionService islandDeletionService,
            Map<UUID, PendingAction> pendingActions,
            long confirmTimeoutMs
    ) {
        this.islandManager = islandManager;
        this.islandDeletionService = islandDeletionService;
        this.pendingActions = pendingActions;
        this.confirmTimeoutMs = confirmTimeoutMs;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command", NamedTextColor.RED));
            return true;
        }

        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) {
            player.sendMessage(Component.text("You don't have an island", NamedTextColor.RED));
            return true;
        }

        if (islandDeletionService.isDeleting(island)) {
            player.sendMessage(Component.text("Your island is already being deleted.", NamedTextColor.RED));
            return true;
        }

        PendingAction action = new PendingAction(
                PendingAction.ActionType.DELETE_OWN_ISLAND,
                island.getGridX(),
                island.getGridZ(),
                System.currentTimeMillis() + confirmTimeoutMs
        );

        pendingActions.put(player.getUniqueId(), action);

        sendConfirmationMessage(player);

        return true;
    }

    private void sendConfirmationMessage(Player player) {
        long timeoutSeconds = confirmTimeoutMs / 1000L;

        player.sendMessage(Component.text("Are you sure you want to delete your island?", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Everything inside your 500x500 island area will be permanently deleted.", NamedTextColor.RED));
        player.sendMessage(Component.text("Use /oneblock confirm within " + timeoutSeconds + " seconds to continue.", NamedTextColor.RED));
    }
}
