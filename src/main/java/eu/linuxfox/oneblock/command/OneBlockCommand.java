package eu.linuxfox.oneblock.command;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.command.subcommand.*;
import eu.linuxfox.oneblock.island.IslandAllocator;
import eu.linuxfox.oneblock.island.IslandBiomeService;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.progression.StageManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class OneBlockCommand implements CommandExecutor, TabCompleter {

    private final IslandManager islandManager;
    private final IslandAllocator islandAllocator;
    private final World oneBlockWorld;
    private final IslandStorage islandStorage;
    private final OneBlockBossBar oneBlockBossBar;
    private final StageManager stageManager;
    private final IslandDeletionService islandDeletionService;
    private final IslandBiomeService islandBiomeService;
    private final long confirmTimeoutMs;

    private AdminCommand adminCommand;

    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();
    private final Map<String, SubCommand> subCommands = new HashMap<>();

    public OneBlockCommand(
            IslandManager islandManager,
            IslandAllocator islandAllocator,
            World oneBlockWorld,
            IslandStorage islandStorage,
            OneBlockBossBar oneBlockBossBar,
            StageManager stageManager,
            IslandDeletionService islandDeletionService,
            IslandBiomeService islandBiomeService,
            long confirmTimeoutMs
    ) {
        this.islandManager = islandManager;
        this.islandAllocator = islandAllocator;
        this.oneBlockWorld = oneBlockWorld;
        this.islandStorage = islandStorage;
        this.oneBlockBossBar = oneBlockBossBar;
        this.stageManager = stageManager;
        this.islandDeletionService = islandDeletionService;
        this.islandBiomeService = islandBiomeService;
        this.confirmTimeoutMs = confirmTimeoutMs;

        registerSubCommands();
    }

    private void registerSubCommands() {
        subCommands.put(
                "create",
                new CreateCommand(
                        islandManager,
                        islandStorage,
                        islandAllocator,
                        oneBlockBossBar,
                        oneBlockWorld
                )
        );

        subCommands.put(
                "delete",
                new DeleteCommand(
                        islandManager,
                        islandDeletionService,
                        pendingActions,
                        confirmTimeoutMs
                )
        );

        subCommands.put(
                "confirm",
                new ConfirmCommand(
                        islandManager,
                        islandDeletionService,
                        pendingActions
                )
        );

        subCommands.put(
                "home",
                new HomeCommand(islandManager)
        );

        subCommands.put(
                "visit",
                new VisitCommand(
                        islandManager,
                        islandDeletionService
                )
        );

        subCommands.put(
                "info",
                new InfoCommand(
                        islandManager,
                        oneBlockWorld
                )
        );

        subCommands.put(
                "set-home",
                new SetHomeCommand(
                        islandManager,
                        islandStorage,
                        oneBlockWorld
                )
        );

        subCommands.put(
                "reset-home",
                new ResetHomeCommand(
                        islandManager,
                        islandStorage,
                        oneBlockWorld
                )
        );

        subCommands.put(
                "biome",
                new BiomeCommand(
                        islandManager,
                        islandStorage,
                        islandBiomeService,
                        islandDeletionService
                )
        );

        adminCommand = new AdminCommand(
                islandManager,
                islandStorage,
                stageManager,
                oneBlockBossBar,
                islandDeletionService,
                oneBlockWorld,
                pendingActions,
                confirmTimeoutMs,
                islandAllocator
        );

        subCommands.put("admin", adminCommand);
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String subCommandName = args[0].toLowerCase(Locale.ROOT);
        SubCommand subCommand = subCommands.get(subCommandName);

        if (subCommand == null) {
            sender.sendMessage(Component.text(
                    "Unknown subcommand. Use /oneblock help.",
                    NamedTextColor.RED
            ));
            return true;
        }

        return subCommand.execute(sender, args);
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            String[] args
    ) {
        if (args.length == 1) {
            return getTopLevelSuggestions(sender, args[0]);
        }

        SubCommand subCommand = subCommands.get(
                args[0].toLowerCase(Locale.ROOT)
        );

        if (subCommand == null) {
            return List.of();
        }

        return subCommand.tabComplete(sender, args);
    }

    private List<String> getTopLevelSuggestions(
            CommandSender sender,
            String input
    ) {
        String lowerInput = input.toLowerCase(Locale.ROOT);

        List<String> suggestions = new java.util.ArrayList<>();

        suggestions.add("help");

        subCommands.keySet().stream()
                .filter(name -> isVisibleTo(sender, name))
                .forEach(suggestions::add);

        return suggestions.stream()
                .filter(name -> name.startsWith(lowerInput))
                .sorted()
                .toList();
    }

    private boolean isVisibleTo(CommandSender sender, String commandName) {
        return switch (commandName) {
            case "biome" -> sender.hasPermission("oneblock.biome");
            case "admin" -> adminCommand.hasAnyPermission(sender);
            default -> true;
        };
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text(
                "----- OneBlock Commands -----",
                NamedTextColor.GOLD
        ));

        sendHelpLine(sender, "/oneblock create - Create your island");
        sendHelpLine(sender, "/oneblock delete - Delete your island");
        sendHelpLine(sender, "/oneblock confirm - Confirm a pending action");
        sendHelpLine(sender, "/oneblock home - Teleport to your island home");
        sendHelpLine(sender, "/oneblock visit <player> - Visit another player's island");
        sendHelpLine(sender, "/oneblock set-home - Set your island home");
        sendHelpLine(sender, "/oneblock reset-home - Reset your island home");
        sendHelpLine(sender, "/oneblock info [player] - Show information about an island");

        if (sender.hasPermission("oneblock.biome")) {
            sendHelpLine(sender, "/oneblock biome - Show your island biome");
            sendHelpLine(sender, "/oneblock biome <biome> - Change your island biome");
            sendHelpLine(sender, "/oneblock biome reset - Reset your island biome to Plains");
        }

        if (adminCommand.hasAnyPermission(sender)) {
            sendHelpLine(sender, "/oneblock admin - OneBlock administration commands");
        }

        sendHelpLine(sender, "/oneblock help - Show this help");
    }

    private void sendHelpLine(CommandSender sender, String text) {
        sender.sendMessage(Component.text(
                text,
                NamedTextColor.GRAY
        ));
    }
}