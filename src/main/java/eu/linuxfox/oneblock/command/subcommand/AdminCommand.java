package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.command.subcommand.admin.*;

import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.progression.StageManager;
import eu.linuxfox.oneblock.storage.IslandStorage;
import eu.linuxfox.oneblock.command.PendingAction;
import eu.linuxfox.oneblock.island.IslandAllocator;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.World;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

public class AdminCommand implements SubCommand {
    private final Map<String, AdminSubCommand> subCommands = new HashMap<>();
    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final StageManager stageManager;
    private final OneBlockBossBar oneBlockBossBar;
    private final IslandDeletionService islandDeletionService;
    private final World oneBlockWorld;
    private final Map<UUID, PendingAction> pendingActions;
    private final long confirmTimeoutMs;
    private final IslandAllocator islandAllocator;

    public AdminCommand(
            IslandManager islandManager,
            IslandStorage islandStorage,
            StageManager stageManager,
            OneBlockBossBar oneBlockBossBar,
            IslandDeletionService islandDeletionService,
            World oneBlockWorld,
            Map<UUID, PendingAction> pendingActions,
            long confirmTimeoutMs,
            IslandAllocator islandAllocator
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.stageManager = stageManager;
        this.oneBlockBossBar = oneBlockBossBar;
        this.islandDeletionService = islandDeletionService;
        this.oneBlockWorld = oneBlockWorld;
        this.pendingActions = pendingActions;
        this.confirmTimeoutMs = confirmTimeoutMs;
        this.islandAllocator = islandAllocator;

        registerSubCommands();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return true;
        }

        String commandName = args[1].toLowerCase(Locale.ROOT);
        AdminSubCommand subCommand = subCommands.get(commandName);

        if (subCommand == null) {
            sender.sendMessage(Component.text("Unknown admin subcommand",  NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(Component.text("You don't have permission.",  NamedTextColor.RED));
            return true;
        }

        subCommand.execute(sender, args);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);

            return subCommands.entrySet().stream()
                    .filter(entry -> sender.hasPermission(entry.getValue().getPermission()))
                    .map(Map.Entry::getKey)
                    .filter(name -> name.startsWith(input))
                    .sorted()
                    .toList();
        }

        if (args.length > 2) {
            AdminSubCommand subCommand = subCommands.get(args[1].toLowerCase(Locale.ROOT));

            if (subCommand == null || !sender.hasPermission(subCommand.getPermission())) {
                return List.of();
            }

            return subCommand.tabComplete(sender, args);
        }

        return List.of();
    }

    private void registerSubCommands() {
        subCommands.put("set-stage", new AdminSetStageCommand(islandManager, islandStorage, stageManager, oneBlockBossBar, islandDeletionService));
        subCommands.put("set-progress", new AdminSetProgressCommand(islandManager, islandStorage, oneBlockBossBar, islandDeletionService));
        subCommands.put("set-home", new AdminSetHomeCommand(islandManager, islandStorage, islandDeletionService, oneBlockWorld));
        subCommands.put("reset-home", new AdminResetHomeCommand(islandManager, islandStorage, islandDeletionService, oneBlockWorld));
        subCommands.put("delete", new AdminDeleteCommand(islandManager, islandDeletionService, pendingActions, confirmTimeoutMs));
        subCommands.put("create", new AdminCreateCommand(islandManager, islandAllocator, islandStorage, oneBlockBossBar, oneBlockWorld));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Usage: /oneblock admin <create|delete|set-home|reset-home|set-stage|set-progress>", NamedTextColor.YELLOW));
    }

    public boolean hasAnyPermission(CommandSender sender) {
        return subCommands.values().stream()
                .anyMatch(subCommand ->
                        sender.hasPermission(subCommand.getPermission())
                );
    }
}
