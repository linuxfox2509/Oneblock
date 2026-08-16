package eu.linuxfox.oneblock.command.subcommand.admin;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.island.GridPosition;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.progression.StageManager;
import eu.linuxfox.oneblock.storage.IslandStorage;
import eu.linuxfox.oneblock.command.CommandUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;

public class AdminSetStageCommand implements AdminSubCommand {
    private static final String PERMISSION = "oneblock.admin.set-stage";

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final StageManager stageManager;
    private final OneBlockBossBar oneBlockBossBar;
    private final IslandDeletionService islandDeletionService;

    public AdminSetStageCommand(
            IslandManager islandManager,
            IslandStorage islandStorage,
            StageManager stageManager,
            OneBlockBossBar oneBlockBossBar,
            IslandDeletionService islandDeletionService
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.stageManager = stageManager;
        this.oneBlockBossBar = oneBlockBossBar;
        this.islandDeletionService = islandDeletionService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(Component.text("Usage: /oneblock admin set-stage <X> <Z> <stage>", NamedTextColor.RED));
            return;
        }

        Integer gridX = CommandUtils.parseInteger(args[2]);
        Integer gridZ = CommandUtils.parseInteger(args[3]);
        Integer stageNumber = CommandUtils.parseInteger(args[4]);

        if (gridX == null || gridZ == null || stageNumber == null) {
            sender.sendMessage(Component.text("X, Z and stage must be valid integers", NamedTextColor.RED));
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

        if (!stageManager.hasStage(stageNumber)) {
            sender.sendMessage(Component.text("That stage does not exist.", NamedTextColor.RED));
            return;
        }

        island.setStage(stageNumber);
        island.setProgress(0);

        islandStorage.saveIsland(island);
        oneBlockBossBar.update(island);

        sender.sendMessage(Component.text("Set island (" + gridX + ", " + gridZ + ") to Stage " + stageNumber + ".", NamedTextColor.GREEN));
    }

    @Override
    public String getPermission() {
        return PERMISSION;
    }
}
