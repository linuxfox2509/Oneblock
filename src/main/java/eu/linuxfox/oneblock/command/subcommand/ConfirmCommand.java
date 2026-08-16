package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.command.PendingAction;
import eu.linuxfox.oneblock.island.GridPosition;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

public class ConfirmCommand implements SubCommand {
    private final IslandManager islandManager;
    private final IslandDeletionService islandDeletionService;
    private final Map<UUID, PendingAction> pendingActions;

    public ConfirmCommand(
            IslandManager islandManager,
            IslandDeletionService islandDeletionService,
            Map<UUID, PendingAction> pendingActions
    ) {
        this.islandManager = islandManager;
        this.islandDeletionService = islandDeletionService;
        this.pendingActions = pendingActions;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command.", NamedTextColor.RED));
            return true;
        }

        PendingAction action = pendingActions.get(player.getUniqueId());

        if (action == null) {
            player.sendMessage(Component.text("You don't have an action waiting for confirmation.",  NamedTextColor.RED));
            return true;
        }

        if (action.isExpired()) {
            pendingActions.remove(player.getUniqueId());
            player.sendMessage(Component.text("Your pending action has expired.", NamedTextColor.RED));
            return true;
        }

        switch (action.type()) {
            case DELETE_OWN_ISLAND -> confirmOwnIslandDeletion(player, action);
            case ADMIN_DELETE_ISLAND -> confirmAdminIslandDeletion(player, action);
        }

        return true;
    }

    private void confirmOwnIslandDeletion(Player player, PendingAction action) {
        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) {
            clearPendingAction(player);
            player.sendMessage(Component.text("You don't have an island or it no longer exists.", NamedTextColor.RED));
            return;
        }

        if (!matchesPosition(island, action)) {
            clearPendingAction(player);
            player.sendMessage(Component.text("The island has changed. Deletion cancelled.",  NamedTextColor.RED));
            return;
        }

        if (islandDeletionService.isDeleting(island)) {
            clearPendingAction(player);
            player.sendMessage(Component.text("This island is already being deleted.", NamedTextColor.YELLOW));
            return;
        }

        clearPendingAction(player);

        player.sendMessage(Component.text("Deleting your island...", NamedTextColor.YELLOW));

        islandDeletionService.deleteIsland(
                island,
                () -> player.sendMessage(Component.text("This island has been deleted.", NamedTextColor.GREEN))
        );
    }

    private void confirmAdminIslandDeletion(Player player, PendingAction action) {
        GridPosition position = new GridPosition(action.gridX(), action.gridZ());
        Island island = islandManager.getIslandByPosition(position);

        if (island == null) {
            clearPendingAction(player);
            player.sendMessage(Component.text("That island no longer exists.", NamedTextColor.RED));
            return;
        }

        if (islandDeletionService.isDeleting(island)) {
            clearPendingAction(player);
            player.sendMessage(Component.text("That island is already being deleted.", NamedTextColor.YELLOW));
            return;
        }

        int gridX = island.getGridX();
        int gridZ = island.getGridZ();

        clearPendingAction(player);

        player.sendMessage(Component.text("Deleting island at grid (" + gridX + ", " + gridZ + ")...", NamedTextColor.YELLOW));

        islandDeletionService.deleteIsland(
                island,
                () -> player.sendMessage(Component.text("Island at grid (" + gridX + ", " + gridZ + ") has been deleted.", NamedTextColor.GREEN))
        );
    }

    private boolean matchesPosition(Island island, PendingAction action) {
        return island.getGridX() == action.gridX() && island.getGridZ() == action.gridZ();
    }

    private void clearPendingAction(Player player) {
        pendingActions.remove(player.getUniqueId());
    }
}
