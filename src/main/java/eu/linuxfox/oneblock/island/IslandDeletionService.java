package eu.linuxfox.oneblock.island;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.storage.IslandStorage;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class IslandDeletionService {

    /*
     * Maximum amount of deletion/scanning work done per tick.
     *
     * This is deliberately based on operations, NOT only blocks
     * that were actually changed.
     */
    private static final int OPERATIONS_PER_TICK = 10000;

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final OneBlockBossBar oneBlockBossBar;
    private final World oneBlockWorld;

    /*
     * Track deleting GRID POSITIONS, not owner UUIDs.
     *
     * An owner may later create a new island, and we don't want
     * an old deletion job to affect that new island.
     */
    private final Set<GridPosition> deletingIslands =
            new HashSet<>();

    public IslandDeletionService(
            JavaPlugin plugin,
            IslandManager islandManager,
            IslandStorage islandStorage,
            OneBlockBossBar oneBlockBossBar,
            World oneBlockWorld
    ) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.oneBlockBossBar = oneBlockBossBar;
        this.oneBlockWorld = oneBlockWorld;
    }

    public boolean isDeleting(Island island) {

        return deletingIslands.contains(
                getPosition(island)
        );
    }

    public void deleteIsland(
            Island island,
            Runnable whenFinished
    ) {

        GridPosition islandPosition =
                getPosition(island);

        // Prevent duplicate deletion jobs
        if (!deletingIslands.add(islandPosition)) {
            return;
        }

        int centerX =
                island.getGridX()
                        * IslandAllocator.ISLAND_SPACING;

        int centerZ =
                island.getGridZ()
                        * IslandAllocator.ISLAND_SPACING;

        int halfSize =
                IslandAllocator.ISLAND_SPACING / 2;

        int minX =
                centerX - halfSize;

        int maxX =
                centerX + halfSize - 1;

        int minZ =
                centerZ - halfSize;

        int maxZ =
                centerZ + halfSize - 1;


        // ---------------------------------------------------------
        // PREPARE DELETION
        // ---------------------------------------------------------

        oneBlockBossBar.hide(island);

        movePlayersOut(
                minX,
                maxX,
                minZ,
                maxZ
        );


        // ---------------------------------------------------------
        // BATCHED DELETION TASK
        // ---------------------------------------------------------

        new BukkitRunnable() {

            private int currentX = minX;
            private int currentZ = minZ;

            private int currentY;
            private boolean columnInitialized = false;

            @Override
            public void run() {

                int operations = 0;

                while (operations < OPERATIONS_PER_TICK) {

                    // ---------------------------------------------
                    // Entire island is finished
                    // ---------------------------------------------

                    if (currentX > maxX) {

                        finishDeletion();

                        cancel();
                        return;
                    }


                    // ---------------------------------------------
                    // Start a new X/Z column
                    // ---------------------------------------------

                    if (!columnInitialized) {

                        currentY =
                                oneBlockWorld.getHighestBlockYAt(
                                        currentX,
                                        currentZ
                                );

                        operations++;

                        /*
                         * Empty void columns can return a height
                         * where the block is still air.
                         *
                         * If so, skip the whole column immediately.
                         */
                        Block highestBlock =
                                oneBlockWorld.getBlockAt(
                                        currentX,
                                        currentY,
                                        currentZ
                                );

                        if (highestBlock.getType().isAir()) {

                            advanceColumn();
                            continue;
                        }

                        columnInitialized = true;
                    }


                    // ---------------------------------------------
                    // Process one block in the current column
                    // ---------------------------------------------

                    Block block =
                            oneBlockWorld.getBlockAt(
                                    currentX,
                                    currentY,
                                    currentZ
                            );

                    operations++;

                    if (!block.getType().isAir()) {

                        block.setType(
                                Material.AIR,
                                false
                        );
                    }

                    currentY--;


                    // ---------------------------------------------
                    // Reached bottom of this column
                    // ---------------------------------------------

                    if (currentY
                            < oneBlockWorld.getMinHeight()) {

                        advanceColumn();
                    }
                }
            }


            private void advanceColumn() {

                columnInitialized = false;

                currentZ++;

                if (currentZ > maxZ) {

                    currentZ = minZ;
                    currentX++;
                }
            }


            private void finishDeletion() {

                /*
                 * Remove exactly THIS island.
                 *
                 * If some unexpected stale object exists, this
                 * cannot remove a newer island owned by the
                 * same player.
                 */
                islandManager.removeIsland(island);

                /*
                 * IslandStorage performs the same coordinate
                 * verification before removing YAML data.
                 */
                islandStorage.deleteIsland(island);

                deletingIslands.remove(
                        islandPosition
                );

                if (whenFinished != null) {
                    whenFinished.run();
                }
            }

        }.runTaskTimer(
                plugin,
                1L,
                1L
        );
    }

    private GridPosition getPosition(
            Island island
    ) {

        return new GridPosition(
                island.getGridX(),
                island.getGridZ()
        );
    }

    private void movePlayersOut(
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {

        World fallbackWorld =
                Bukkit.getWorlds()
                        .stream()
                        .filter(world ->
                                !world.equals(oneBlockWorld)
                        )
                        .findFirst()
                        .orElse(null);

        /*
         * Normally the server will have its ordinary overworld.
         * If for some reason it doesn't, don't blindly teleport
         * players somewhere unsafe.
         */
        if (fallbackWorld == null) {

            plugin.getLogger().warning(
                    "Could not find another world to teleport players to during island deletion."
            );

            return;
        }

        Location fallback =
                fallbackWorld.getSpawnLocation();

        for (Player player :
                oneBlockWorld.getPlayers()) {

            Location location =
                    player.getLocation();

            int x =
                    location.getBlockX();

            int z =
                    location.getBlockZ();

            if (x >= minX
                    && x <= maxX
                    && z >= minZ
                    && z <= maxZ) {

                player.teleport(fallback);

                player.sendMessage(
                        "The island you were on is being deleted."
                );
            }
        }
    }
}