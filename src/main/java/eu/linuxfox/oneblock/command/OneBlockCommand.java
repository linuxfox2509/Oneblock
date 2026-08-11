package eu.linuxfox.oneblock.command;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.island.GridPosition;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandAllocator;
import eu.linuxfox.oneblock.island.IslandBiomeService;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.progression.StageManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private final Map<UUID, PendingAction> pendingActions = new HashMap<>();

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
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {

        // ---------------------------------------------------------
        // /oneblock
        // /oneblock help
        // ---------------------------------------------------------

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {

            sender.sendMessage("----- OneBlock Commands -----");
            sender.sendMessage("/oneblock create - Create your island");
            sender.sendMessage("/oneblock delete - Delete your island");
            sender.sendMessage("/oneblock confirm - Confirm a pending action");
            sender.sendMessage("/oneblock home - Teleport to your island home");
            sender.sendMessage("/oneblock visit <player> - Visit another player's island");
            if (sender.isOp()) {
                sender.sendMessage("/oneblock biome - Show your island biome");
                sender.sendMessage("/oneblock biome <biome> - Change your island biome");
                sender.sendMessage("/oneblock biome reset - Reset your island biome to Plains");
            }
            sender.sendMessage("/oneblock set-home - Set your island home");
            sender.sendMessage("/oneblock reset-home - Reset your island home");
            sender.sendMessage("/oneblock info [player] - Show information about an island");
            sender.sendMessage("/oneblock help - Show this help");

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock create
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("create")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            if (islandManager.hasIsland(player.getUniqueId())) {
                player.sendMessage("You already have an island.");
                return true;
            }

            GridPosition position =
                    islandAllocator.findNextPosition();

            if (position == null) {
                player.sendMessage(
                        "Could not find a suitable location for your island."
                );
                return true;
            }

            Island island = new Island(
                    player.getUniqueId(),
                    player.getName(),
                    position.x(),
                    position.z(),
                    oneBlockWorld
            );

            Location oneBlockLocation =
                    island.getOneBlockLocation(oneBlockWorld);

            // Create 3x3 obsidian safety platform
            for (int xOffset = -1; xOffset <= 1; xOffset++) {
                for (int zOffset = -1; zOffset <= 1; zOffset++) {

                    Location platformLocation =
                            oneBlockLocation.clone().add(
                                    xOffset,
                                    -1,
                                    zOffset
                            );

                    platformLocation.getBlock()
                            .setType(Material.OBSIDIAN);
                }
            }

            // Bedrock directly underneath OneBlock
            oneBlockLocation.clone()
                    .add(0, -1, 0)
                    .getBlock()
                    .setType(Material.BEDROCK);

            // Initial OneBlock
            oneBlockLocation.getBlock()
                    .setType(Material.GRASS_BLOCK);

            islandManager.addIsland(island);
            islandStorage.saveIsland(island);

            oneBlockBossBar.update(island);

            player.teleport(island.getHome());

            player.sendMessage(
                    "Island created at grid (" +
                            position.x() + ", " +
                            position.z() + ")."
            );

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock info [player]
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("info")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage(
                        "This command can only be used by players."
                );
                return true;
            }

            Island island;

            if (args.length == 1) {

                island = islandManager.getIsland(
                        player.getUniqueId()
                );

            } else {

                island = islandManager.getIslandByOwnerName(
                        args[1]
                );
            }

            if (island == null) {

                player.sendMessage(
                        "No island found for that player."
                );

                return true;
            }

            Location oneBlockLocation =
                    island.getOneBlockLocation(oneBlockWorld);

            player.sendMessage(
                    "----- OneBlock Island -----"
            );

            player.sendMessage(
                    "Owner: " + island.getOwnerName()
            );

            player.sendMessage(
                    "Grid: (" +
                            island.getGridX() + ", " +
                            island.getGridZ() + ")"
            );

            player.sendMessage(
                    "OneBlock: (" +
                            oneBlockLocation.getBlockX() + ", " +
                            oneBlockLocation.getBlockY() + ", " +
                            oneBlockLocation.getBlockZ() + ")"
            );

            player.sendMessage(
                    "Stage: " + island.getStage()
            );

            player.sendMessage(
                    "Progress: " + island.getProgress()
            );

            player.sendMessage(
                    "Biome: " +
                            island.getBiome()
                                    .getKey()
                                    .getKey()
            );

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock visit <player>
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("visit")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage(
                        "This command can only be used by players."
                );
                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        "Usage: /oneblock visit <player>"
                );

                return true;
            }

            Island island =
                    islandManager.getIslandByOwnerName(
                            args[1]
                    );

            if (island == null) {

                player.sendMessage(
                        "No island found for that player."
                );

                return true;
            }

            if (islandDeletionService.isDeleting(island)) {

                player.sendMessage(
                        "That island is currently being deleted."
                );

                return true;
            }

            player.teleport(
                    island.getHome()
            );

            player.sendMessage(
                    "Teleported to " +
                            island.getOwnerName() +
                            "'s island."
            );

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock home
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("home")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            Island island =
                    islandManager.getIsland(
                            player.getUniqueId()
                    );

            if (island == null) {
                player.sendMessage("You don't have an island.");
                return true;
            }

            player.teleport(island.getHome());

            player.sendMessage(
                    "Teleported to your island."
            );

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock biome
        // /oneblock biome <biome>
        // /oneblock biome reset
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("biome")) {

            if (!sender.isOp()) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            if (!(sender instanceof Player player)) {

                sender.sendMessage(
                        "This command can only be used by players."
                );

                return true;
            }

            Island island =
                    islandManager.getIsland(
                            player.getUniqueId()
                    );

            if (island == null) {

                player.sendMessage(
                        "You don't have an island."
                );

                return true;
            }

            if (islandDeletionService.isDeleting(island)) {

                player.sendMessage(
                        "Your island is currently being deleted."
                );

                return true;
            }

            // -----------------------------------------------------
            // /oneblock biome
            // -----------------------------------------------------

            if (args.length == 1) {

                player.sendMessage(
                        "Current island biome: " +
                                island.getBiome()
                                        .getKey()
                                        .getKey()
                );

                return true;
            }

            if (islandBiomeService.isChangingBiome(island)) {

                player.sendMessage(
                        "Your island biome is already being changed."
                );

                return true;
            }

            Biome biome;


            // -----------------------------------------------------
            // /oneblock biome reset
            // -----------------------------------------------------

            if (args[1].equalsIgnoreCase("reset")) {

                biome = Biome.PLAINS;

            } else {

                // -------------------------------------------------
                // /oneblock biome <biome>
                // -------------------------------------------------

                String input =
                        args[1].toLowerCase();

                NamespacedKey key =
                        NamespacedKey.fromString(
                                input.contains(":")
                                        ? input
                                        : "minecraft:" + input
                        );

                if (key == null) {

                    player.sendMessage(
                            "Invalid biome."
                    );

                    return true;
                }

                Registry<Biome> biomeRegistry =
                        RegistryAccess.registryAccess()
                                .getRegistry(
                                        RegistryKey.BIOME
                                );

                biome =
                        biomeRegistry.get(key);

                if (biome == null) {

                    player.sendMessage(
                            "Unknown biome: " +
                                    args[1]
                    );

                    return true;
                }
            }


            // -----------------------------------------------------
            // Don't do unnecessary work
            // -----------------------------------------------------

            if (island.getBiome().equals(biome)) {

                player.sendMessage(
                        "Your island already uses the " +
                                biome.getKey().getKey() +
                                " biome."
                );

                return true;
            }


            // -----------------------------------------------------
            // Start biome change
            // -----------------------------------------------------

            player.sendMessage(
                    "Changing your island biome to " +
                            biome.getKey().getKey() +
                            "..."
            );

            Biome selectedBiome =
                    biome;

            boolean started =
                    islandBiomeService.setBiome(
                            island,
                            selectedBiome,
                            () -> {

                                islandStorage.saveIsland(
                                        island
                                );

                                player.sendMessage(
                                        "Island biome changed to " +
                                                selectedBiome
                                                        .getKey()
                                                        .getKey() +
                                                "."
                                );
                            }
                    );

            if (!started) {

                player.sendMessage(
                        "Your island biome is already being changed."
                );
            }

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock set-home
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("set-home")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            Island island =
                    islandManager.getIsland(
                            player.getUniqueId()
                    );

            if (island == null) {
                player.sendMessage("You don't have an island.");
                return true;
            }

            if (!player.getWorld().equals(oneBlockWorld)
                    || !island.containsLocation(player.getLocation())) {

                player.sendMessage(
                        "You must be on your own island to set your home."
                );

                return true;
            }

            Location playerLocation =
                    player.getLocation();

            if (playerLocation.clone()
                    .subtract(0, 1, 0)
                    .getBlock()
                    .getType()
                    .isAir()) {

                player.sendMessage(
                        "You cannot set your home while standing in the air."
                );

                return true;
            }

            island.setHome(
                    playerLocation.clone()
            );

            islandStorage.saveIsland(island);

            player.sendMessage(
                    "Island home updated."
            );

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock reset-home
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("reset-home")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            Island island =
                    islandManager.getIsland(
                            player.getUniqueId()
                    );

            if (island == null) {
                player.sendMessage("You don't have an island.");
                return true;
            }

            island.resetHome(oneBlockWorld);

            islandStorage.saveIsland(island);

            player.sendMessage(
                    "Island home reset to the OneBlock."
            );

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock delete
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("delete")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            Island island =
                    islandManager.getIsland(
                            player.getUniqueId()
                    );

            if (island == null) {
                player.sendMessage("You don't have an island.");
                return true;
            }

            if (islandDeletionService.isDeleting(island)) {
                player.sendMessage(
                        "Your island is already being deleted."
                );
                return true;
            }

            pendingActions.put(
                    player.getUniqueId(),
                    new PendingAction(
                            PendingAction.ActionType.DELETE_OWN_ISLAND,
                            island.getGridX(),
                            island.getGridZ(),
                            System.currentTimeMillis() + confirmTimeoutMs
                    )
            );

            player.sendMessage(
                    "Are you sure you want to delete your island?"
            );

            player.sendMessage(
                    "Everything inside your 500x500 island area will be PERMANENTLY deleted."
            );

            player.sendMessage(
                    "Use /oneblock confirm within " +
                            getConfirmTimeoutSeconds() +
                            " seconds to continue."
            );

            return true;
        }


        // ---------------------------------------------------------
        // /oneblock confirm
        // ---------------------------------------------------------

        if (args[0].equalsIgnoreCase("confirm")) {

            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by players.");
                return true;
            }

            PendingAction pendingAction =
                    pendingActions.get(
                            player.getUniqueId()
                    );

            if (pendingAction == null) {

                player.sendMessage(
                        "You don't have an action waiting for confirmation."
                );

                return true;
            }

            if (pendingAction.isExpired()) {

                pendingActions.remove(
                        player.getUniqueId()
                );

                player.sendMessage(
                        "Your pending action has expired. Please run the command again."
                );

                return true;
            }

            switch (pendingAction.type()) {

                case DELETE_OWN_ISLAND -> {

                    Island island =
                            islandManager.getIsland(
                                    player.getUniqueId()
                            );

                    if (island == null) {

                        pendingActions.remove(
                                player.getUniqueId()
                        );

                        player.sendMessage(
                                "You don't have an island or it no longer exists."
                        );

                        return true;
                    }

                    if (island.getGridX()
                            != pendingAction.gridX()
                            || island.getGridZ()
                            != pendingAction.gridZ()) {

                        pendingActions.remove(
                                player.getUniqueId()
                        );

                        player.sendMessage(
                                "The island has changed. Deletion cancelled."
                        );

                        return true;
                    }

                    if (islandDeletionService.isDeleting(island)) {

                        pendingActions.remove(
                                player.getUniqueId()
                        );

                        player.sendMessage(
                                "This island is already being deleted."
                        );

                        return true;
                    }

                    pendingActions.remove(
                            player.getUniqueId()
                    );

                    player.sendMessage(
                            "Deleting your island..."
                    );

                    islandDeletionService.deleteIsland(
                            island,
                            () -> player.sendMessage(
                                    "Your island has been deleted."
                            )
                    );

                    return true;
                }

                case ADMIN_DELETE_ISLAND -> {

                    Island island =
                            islandManager.getIslandByPosition(
                                    new GridPosition(
                                            pendingAction.gridX(),
                                            pendingAction.gridZ()
                                    )
                            );

                    if (island == null) {

                        pendingActions.remove(
                                player.getUniqueId()
                        );

                        player.sendMessage(
                                "That island no longer exists."
                        );

                        return true;
                    }

                    if (islandDeletionService.isDeleting(island)) {

                        pendingActions.remove(
                                player.getUniqueId()
                        );

                        player.sendMessage(
                                "That island is already being deleted."
                        );

                        return true;
                    }

                    int gridX =
                            island.getGridX();

                    int gridZ =
                            island.getGridZ();

                    pendingActions.remove(
                            player.getUniqueId()
                    );

                    player.sendMessage(
                            "Deleting island at grid (" +
                                    gridX + ", " +
                                    gridZ + ")..."
                    );

                    islandDeletionService.deleteIsland(
                            island,
                            () -> player.sendMessage(
                                    "Island at grid (" +
                                            gridX + ", " +
                                            gridZ +
                                            ") has been deleted."
                            )
                    );

                    return true;
                }
            }

            return true;
        }


        // =========================================================
        // ADMIN COMMANDS
        // =========================================================

        if (args[0].equalsIgnoreCase("admin")) {

            if (!sender.isOp()) {

                sender.sendMessage(
                        "You do not have permission to use admin commands."
                );

                return true;
            }

            if (args.length < 2) {

                sender.sendMessage(
                        "Usage: /oneblock admin <create|delete|set-home|reset-home|set-stage|set-progress> ..."
                );

                return true;
            }

            String adminAction =
                    args[1];


            // -----------------------------------------------------
            // /oneblock admin create <X> <Z> <player> [force]
            // -----------------------------------------------------

            if (adminAction.equalsIgnoreCase("create")) {

                if (args.length < 5) {

                    sender.sendMessage(
                            "Usage: /oneblock admin create <X> <Z> <player> [force]"
                    );

                    return true;
                }

                int gridX;
                int gridZ;

                try {

                    gridX =
                            Integer.parseInt(args[2]);

                    gridZ =
                            Integer.parseInt(args[3]);

                } catch (NumberFormatException exception) {

                    sender.sendMessage(
                            "X and Z must be valid integers."
                    );

                    return true;
                }

                GridPosition position =
                        new GridPosition(
                                gridX,
                                gridZ
                        );

                if (islandManager.hasIslandAtPosition(position)) {

                    sender.sendMessage(
                            "An island already exists at that grid position."
                    );

                    return true;
                }

                OfflinePlayer targetPlayer =
                        Bukkit.getOfflinePlayer(
                                args[4]
                        );

                if (!targetPlayer.hasPlayedBefore()
                        && !targetPlayer.isOnline()) {

                    sender.sendMessage(
                            "That player has never joined this server."
                    );

                    return true;
                }

                if (islandManager.hasIsland(
                        targetPlayer.getUniqueId()
                )) {

                    String existingName =
                            targetPlayer.getName() != null
                                    ? targetPlayer.getName()
                                    : args[4];

                    sender.sendMessage(
                            existingName +
                                    " already owns an island."
                    );

                    return true;
                }

                boolean force =
                        args.length >= 6
                                && args[5].equalsIgnoreCase("force");

                if (!force
                        && !islandAllocator.isAreaClear(position)) {

                    sender.sendMessage(
                            "The 200x200 clearance area is not empty. " +
                                    "Use 'force' to override this check."
                    );

                    return true;
                }

                String targetName =
                        targetPlayer.getName() != null
                                ? targetPlayer.getName()
                                : args[4];

                Island island =
                        new Island(
                                targetPlayer.getUniqueId(),
                                targetName,
                                gridX,
                                gridZ,
                                oneBlockWorld
                        );

                Location oneBlockLocation =
                        island.getOneBlockLocation(
                                oneBlockWorld
                        );

                for (int xOffset = -1;
                     xOffset <= 1;
                     xOffset++) {

                    for (int zOffset = -1;
                         zOffset <= 1;
                         zOffset++) {

                        Location platformLocation =
                                oneBlockLocation
                                        .clone()
                                        .add(
                                                xOffset,
                                                -1,
                                                zOffset
                                        );

                        platformLocation
                                .getBlock()
                                .setType(
                                        Material.OBSIDIAN
                                );
                    }
                }

                oneBlockLocation.clone()
                        .add(0, -1, 0)
                        .getBlock()
                        .setType(Material.BEDROCK);

                oneBlockLocation
                        .getBlock()
                        .setType(Material.GRASS_BLOCK);

                islandManager.addIsland(island);

                islandStorage.saveIsland(island);

                oneBlockBossBar.update(island);

                sender.sendMessage(
                        "Created island at grid (" +
                                gridX + ", " +
                                gridZ +
                                ") for " +
                                targetName + "."
                );

                return true;
            }


            // -----------------------------------------------------
            // /oneblock admin delete <X> <Z>
            // -----------------------------------------------------

            if (adminAction.equalsIgnoreCase("delete")) {

                if (args.length < 4) {

                    sender.sendMessage(
                            "Usage: /oneblock admin delete <X> <Z>"
                    );

                    return true;
                }

                if (!(sender instanceof Player player)) {

                    sender.sendMessage(
                            "This command must currently be confirmed by a player."
                    );

                    return true;
                }

                int gridX;
                int gridZ;

                try {

                    gridX =
                            Integer.parseInt(args[2]);

                    gridZ =
                            Integer.parseInt(args[3]);

                } catch (NumberFormatException exception) {

                    sender.sendMessage(
                            "X and Z must be valid integers."
                    );

                    return true;
                }

                Island island =
                        islandManager.getIslandByPosition(
                                new GridPosition(
                                        gridX,
                                        gridZ
                                )
                        );

                if (island == null) {

                    sender.sendMessage(
                            "No island exists at that grid position."
                    );

                    return true;
                }

                if (islandDeletionService.isDeleting(island)) {

                    sender.sendMessage(
                            "That island is already being deleted."
                    );

                    return true;
                }

                pendingActions.put(
                        player.getUniqueId(),
                        new PendingAction(
                                PendingAction.ActionType.ADMIN_DELETE_ISLAND,
                                gridX,
                                gridZ,
                                System.currentTimeMillis() + confirmTimeoutMs
                        )
                );

                sender.sendMessage(
                        "Are you sure you want to delete the island at grid (" +
                                gridX + ", " +
                                gridZ + ")?"
                );

                sender.sendMessage(
                        "Everything inside its 500x500 area will be PERMANENTLY deleted."
                );

                sender.sendMessage(
                        "Use /oneblock confirm within " +
                                getConfirmTimeoutSeconds() +
                                " seconds to continue."
                );

                return true;
            }


            // -----------------------------------------------------
            // /oneblock admin set-home <X> <Z>
            // -----------------------------------------------------

            if (adminAction.equalsIgnoreCase("set-home")) {

                if (args.length < 4) {

                    sender.sendMessage(
                            "Usage: /oneblock admin set-home <X> <Z>"
                    );

                    return true;
                }

                if (!(sender instanceof Player player)) {

                    sender.sendMessage(
                            "This command can only be used by a player."
                    );

                    return true;
                }

                int gridX;
                int gridZ;

                try {

                    gridX =
                            Integer.parseInt(args[2]);

                    gridZ =
                            Integer.parseInt(args[3]);

                } catch (NumberFormatException exception) {

                    sender.sendMessage(
                            "X and Z must be valid integers."
                    );

                    return true;
                }

                Island island =
                        islandManager.getIslandByPosition(
                                new GridPosition(
                                        gridX,
                                        gridZ
                                )
                        );

                if (island == null) {

                    sender.sendMessage(
                            "No island exists at that grid position."
                    );

                    return true;
                }

                if (islandDeletionService.isDeleting(island)) {

                    sender.sendMessage(
                            "That island is currently being deleted."
                    );

                    return true;
                }

                if (!player.getWorld().equals(oneBlockWorld)
                        || !island.containsLocation(player.getLocation())) {

                    sender.sendMessage(
                            "You must be standing inside that island's 500x500 area."
                    );

                    return true;
                }

                Location location =
                        player.getLocation();

                if (location.clone()
                        .subtract(0, 1, 0)
                        .getBlock()
                        .getType()
                        .isAir()) {

                    sender.sendMessage(
                            "You cannot set the island home while standing in the air."
                    );

                    return true;
                }

                island.setHome(
                        location.clone()
                );

                islandStorage.saveIsland(island);

                sender.sendMessage(
                        "Set the home for island (" +
                                gridX + ", " +
                                gridZ + ")."
                );

                return true;
            }


            // -----------------------------------------------------
            // /oneblock admin reset-home <X> <Z>
            // -----------------------------------------------------

            if (adminAction.equalsIgnoreCase("reset-home")) {

                if (args.length < 4) {

                    sender.sendMessage(
                            "Usage: /oneblock admin reset-home <X> <Z>"
                    );

                    return true;
                }

                int gridX;
                int gridZ;

                try {

                    gridX =
                            Integer.parseInt(args[2]);

                    gridZ =
                            Integer.parseInt(args[3]);

                } catch (NumberFormatException exception) {

                    sender.sendMessage(
                            "X and Z must be valid integers."
                    );

                    return true;
                }

                Island island =
                        islandManager.getIslandByPosition(
                                new GridPosition(
                                        gridX,
                                        gridZ
                                )
                        );

                if (island == null) {

                    sender.sendMessage(
                            "No island exists at that grid position."
                    );

                    return true;
                }

                if (islandDeletionService.isDeleting(island)) {

                    sender.sendMessage(
                            "That island is currently being deleted."
                    );

                    return true;
                }

                island.resetHome(oneBlockWorld);

                islandStorage.saveIsland(island);

                sender.sendMessage(
                        "Reset the home for island (" +
                                gridX + ", " +
                                gridZ +
                                ") to its OneBlock."
                );

                return true;
            }


            // -----------------------------------------------------
            // /oneblock admin set-stage <X> <Z> <stage>
            // -----------------------------------------------------

            if (adminAction.equalsIgnoreCase("set-stage")) {

                if (args.length < 5) {

                    sender.sendMessage(
                            "Usage: /oneblock admin set-stage <X> <Z> <stage>"
                    );

                    return true;
                }

                int gridX;
                int gridZ;
                int stageNumber;

                try {

                    gridX =
                            Integer.parseInt(args[2]);

                    gridZ =
                            Integer.parseInt(args[3]);

                    stageNumber =
                            Integer.parseInt(args[4]);

                } catch (NumberFormatException exception) {

                    sender.sendMessage(
                            "X, Z and stage must be valid integers."
                    );

                    return true;
                }

                Island island =
                        islandManager.getIslandByPosition(
                                new GridPosition(
                                        gridX,
                                        gridZ
                                )
                        );

                if (island == null) {

                    sender.sendMessage(
                            "No island exists at that grid position."
                    );

                    return true;
                }

                if (islandDeletionService.isDeleting(island)) {

                    sender.sendMessage(
                            "That island is currently being deleted."
                    );

                    return true;
                }

                if (!stageManager.hasStage(stageNumber)) {

                    sender.sendMessage(
                            "That stage does not exist."
                    );

                    return true;
                }

                island.setStage(stageNumber);
                island.setProgress(0);

                islandStorage.saveIsland(island);

                oneBlockBossBar.update(island);

                sender.sendMessage(
                        "Set island (" +
                                gridX + ", " +
                                gridZ +
                                ") to Stage " +
                                stageNumber + "."
                );

                return true;
            }


            // -----------------------------------------------------
            // /oneblock admin set-progress <X> <Z> <progress>
            // -----------------------------------------------------

            if (adminAction.equalsIgnoreCase("set-progress")) {

                if (args.length < 5) {

                    sender.sendMessage(
                            "Usage: /oneblock admin set-progress <X> <Z> <progress>"
                    );

                    return true;
                }

                int gridX;
                int gridZ;
                int progress;

                try {

                    gridX =
                            Integer.parseInt(args[2]);

                    gridZ =
                            Integer.parseInt(args[3]);

                    progress =
                            Integer.parseInt(args[4]);

                } catch (NumberFormatException exception) {

                    sender.sendMessage(
                            "X, Z and progress must be valid integers."
                    );

                    return true;
                }

                if (progress < 0) {

                    sender.sendMessage(
                            "Progress cannot be negative."
                    );

                    return true;
                }

                Island island =
                        islandManager.getIslandByPosition(
                                new GridPosition(
                                        gridX,
                                        gridZ
                                )
                        );

                if (island == null) {

                    sender.sendMessage(
                            "No island exists at that grid position."
                    );

                    return true;
                }

                if (islandDeletionService.isDeleting(island)) {

                    sender.sendMessage(
                            "That island is currently being deleted."
                    );

                    return true;
                }

                island.setProgress(progress);

                islandStorage.saveIsland(island);

                oneBlockBossBar.update(island);

                sender.sendMessage(
                        "Set island (" +
                                gridX + ", " +
                                gridZ +
                                ") progress to " +
                                progress + "."
                );

                return true;
            }


            sender.sendMessage(
                    "Unknown admin subcommand."
            );

            return true;
        }


        // ---------------------------------------------------------
        // UNKNOWN COMMAND
        // ---------------------------------------------------------

        sender.sendMessage(
                "Unknown subcommand. Use /oneblock help."
        );

        return true;
    }


    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {

        List<String> suggestions =
                new ArrayList<>();


        // ---------------------------------------------------------
        // /oneblock <...>
        // ---------------------------------------------------------

        if (args.length == 1) {

            suggestions.add("create");
            suggestions.add("delete");
            suggestions.add("confirm");
            suggestions.add("home");
            suggestions.add("visit");
            suggestions.add("set-home");
            suggestions.add("reset-home");
            suggestions.add("info");
            suggestions.add("help");

            if (sender.isOp()) {
                suggestions.add("admin");
                suggestions.add("biome");
            }

            return filterSuggestions(
                    suggestions,
                    args[0]
            );
        }


        // ---------------------------------------------------------
        // /oneblock info <player>
        // /oneblock visit <player>
        // ---------------------------------------------------------

        if (args.length == 2
                && (
                args[0].equalsIgnoreCase("info")
                        || args[0].equalsIgnoreCase("visit")
        )) {

            for (Island island :
                    islandManager.getAllIslands()) {

                suggestions.add(
                        island.getOwnerName()
                );
            }

            return filterSuggestions(
                    suggestions,
                    args[1]
            );
        }


        // ---------------------------------------------------------
        // /oneblock biome <biome|reset>
        // ---------------------------------------------------------

        if (sender.isOp()
                && args.length == 2
                && args[0].equalsIgnoreCase("biome")) {

            suggestions.add("reset");

            Registry<Biome> biomeRegistry =
                    RegistryAccess.registryAccess()
                            .getRegistry(
                                    RegistryKey.BIOME
                            );

            biomeRegistry.forEach(
                    biome -> {

                        NamespacedKey key =
                                biome.getKey();

                        if (key.getNamespace()
                                .equals("minecraft")) {

                            suggestions.add(
                                    key.getKey()
                            );
                        }
                    }
            );

            return filterSuggestions(
                    suggestions,
                    args[1]
            );
        }


        // ---------------------------------------------------------
        // Admin only from here
        // ---------------------------------------------------------

        if (!sender.isOp()) {
            return List.of();
        }


        // ---------------------------------------------------------
        // /oneblock admin <...>
        // ---------------------------------------------------------

        if (args.length == 2
                && args[0].equalsIgnoreCase("admin")) {

            suggestions.add("create");
            suggestions.add("delete");
            suggestions.add("set-home");
            suggestions.add("reset-home");
            suggestions.add("set-stage");
            suggestions.add("set-progress");

            return filterSuggestions(
                    suggestions,
                    args[1]
            );
        }


        // ---------------------------------------------------------
        // /oneblock admin create <X> <Z> <player>
        // ---------------------------------------------------------

        if (args.length == 5
                && args[0].equalsIgnoreCase("admin")
                && args[1].equalsIgnoreCase("create")) {

            for (Player player :
                    sender.getServer().getOnlinePlayers()) {

                suggestions.add(
                        player.getName()
                );
            }

            return filterSuggestions(
                    suggestions,
                    args[4]
            );
        }


        // ---------------------------------------------------------
        // /oneblock admin create <X> <Z> <player> force
        // ---------------------------------------------------------

        if (args.length == 6
                && args[0].equalsIgnoreCase("admin")
                && args[1].equalsIgnoreCase("create")) {

            suggestions.add("force");

            return filterSuggestions(
                    suggestions,
                    args[5]
            );
        }


        // Deliberately no coordinate suggestions
        return List.of();
    }


    private List<String> filterSuggestions(
            List<String> suggestions,
            String input
    ) {

        String lowerInput =
                input.toLowerCase();

        return suggestions.stream()
                .filter(
                        suggestion ->
                                suggestion
                                        .toLowerCase()
                                        .startsWith(lowerInput)
                )
                .toList();
    }


    private long getConfirmTimeoutSeconds() {
        return confirmTimeoutMs / 1000L;
    }
}