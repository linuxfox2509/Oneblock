package eu.linuxfox.oneblock.command.subcommand.admin;

import eu.linuxfox.oneblock.command.CommandUtils;
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

public class AdminDeleteCommand implements AdminSubCommand {
    private static final String PERMISSION = "oneblock.admin.delete";

    private final IslandManager islandManager;
    private final IslandDeletionService islandDeletionService;
    private final Map<UUID, PendingAction> pendingActions;
    private final long confirmTimeoutMs;

    public AdminDeleteCommand(
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
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can execute this command.", NamedTextColor.RED));
            return;
        }

        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /admin delete <X> <Z>", NamedTextColor.RED));
        }

        Integer gridX = CommandUtils.parseInteger(args[2]);
        Integer gridZ = CommandUtils.parseInteger(args[3]);

        if (gridX == null || gridZ == null) {
            sender.sendMessage(Component.text("X and Z must be valid integers", NamedTextColor.RED));
            return;
        }

        GridPosition position = new GridPosition(gridX, gridZ);
        Island island = islandManager.getIslandByPosition(position);

        if (island == null) {
            sender.sendMessage(Component.text("No island exists at that grid position", NamedTextColor.RED));
            return;
        }

        if (islandDeletionService.isDeleting(island)) {
            sender.sendMessage(Component.text("That island is already being deleted", NamedTextColor.YELLOW));
            return;
        }

        PendingAction action = new PendingAction(
                PendingAction.ActionType.ADMIN_DELETE_ISLAND,
                gridX,
                gridZ,
                System.currentTimeMillis() + confirmTimeoutMs
        );

        pendingActions.put(player.getUniqueId(), action);

        sendConfirmationMessage(player, gridX, gridZ);
    }

    @Override
    public String getPermission() {
        return PERMISSION;
    }

    private void sendConfirmationMessage(Player player, int gridX, int gridZ) {
        long timeoutSeconds = confirmTimeoutMs / 1000L;

        player.sendMessage(Component.text("Are you sure you want to delete the island at grid (" + gridX + ", " + gridZ + ")?", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Everything inside its 500x500 area will be permanently deleted.", NamedTextColor.RED));
        player.sendMessage(Component.text("Use /oneblock confirm within " + timeoutSeconds + " seconds to continue.", NamedTextColor.YELLOW));
    }
}
