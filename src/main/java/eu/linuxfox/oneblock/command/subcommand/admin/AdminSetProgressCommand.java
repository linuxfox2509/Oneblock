package eu.linuxfox.oneblock.command.subcommand.admin;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.island.GridPosition;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.storage.IslandStorage;
import eu.linuxfox.oneblock.command.CommandUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;

public class AdminSetProgressCommand implements AdminSubCommand {

    private static final String PERMISSION = "oneblock.admin.set-progress";

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final OneBlockBossBar oneBlockBossBar;
    private final IslandDeletionService islandDeletionService;

    public AdminSetProgressCommand(
            IslandManager islandManager,
            IslandStorage islandStorage,
            OneBlockBossBar oneBlockBossBar,
            IslandDeletionService islandDeletionService
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.oneBlockBossBar = oneBlockBossBar;
        this.islandDeletionService = islandDeletionService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Component.text("Usage: /oneblock set-progress <X> <Z> <progress>", NamedTextColor.RED));
            return;
        }

        Integer gridX = CommandUtils.parseInteger(args[2]);
        Integer gridZ = CommandUtils.parseInteger(args[3]);
        Integer progress = CommandUtils.parseInteger(args[4]);

        if (gridX == null || gridZ == null || progress == null) {
            sender.sendMessage(Component.text("X, Z and progress must be valid integers.",  NamedTextColor.RED));
            return;
        }

        if (progress < 0) {
            sender.sendMessage(Component.text("Progress must be a positive integer.", NamedTextColor.RED));
            return;
        }

        Island island = islandManager.getIslandByPosition(new GridPosition(gridX, gridZ));

        if (island == null) {
            sender.sendMessage(Component.text("No island exists at that grid position.", NamedTextColor.RED));
            return;
        }

        if (islandDeletionService.isDeleting(island)) {
            sender.sendMessage(Component.text("That island is currently being deleted.", NamedTextColor.YELLOW));
            return;
        }

        island.setProgress(progress);

        islandStorage.saveIsland(island);
        oneBlockBossBar.update(island);

        sender.sendMessage(Component.text("Set island (" + gridX + ", " + gridZ + ") progress to " + progress + ".", NamedTextColor.GREEN));
    }

    @Override
    public String getPermission() {
        return PERMISSION;
    }
}
