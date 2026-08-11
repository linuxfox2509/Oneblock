package eu.linuxfox.oneblock.island;

import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class IslandBiomeService {

    private static final int OPERATIONS_PER_TICK = 5000;
    private static final int BIOME_STEP = 4;

    private final JavaPlugin plugin;
    private final World oneBlockWorld;

    private final Set<GridPosition> changingBiomes =
            new HashSet<>();

    public IslandBiomeService(
            JavaPlugin plugin,
            World oneBlockWorld
    ) {
        this.plugin = plugin;
        this.oneBlockWorld = oneBlockWorld;
    }

    public boolean isChangingBiome(Island island) {

        return changingBiomes.contains(
                new GridPosition(
                        island.getGridX(),
                        island.getGridZ()
                )
        );
    }

    public boolean setBiome(
            Island island,
            Biome biome,
            Runnable whenFinished
    ) {

        GridPosition position =
                new GridPosition(
                        island.getGridX(),
                        island.getGridZ()
                );

        if (!changingBiomes.add(position)) {
            return false;
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

        /*
         * Align the starting coordinates to biome quart boundaries.
         */
        int startX =
                Math.floorDiv(minX, BIOME_STEP)
                        * BIOME_STEP;

        int startZ =
                Math.floorDiv(minZ, BIOME_STEP)
                        * BIOME_STEP;

        int startY =
                Math.floorDiv(
                        oneBlockWorld.getMinHeight(),
                        BIOME_STEP
                ) * BIOME_STEP;

        new BukkitRunnable() {

            private int currentX = startX;
            private int currentY = startY;
            private int currentZ = startZ;

            @Override
            public void run() {

                int operations = 0;

                while (operations < OPERATIONS_PER_TICK) {

                    if (currentX > maxX) {

                        finish();
                        cancel();
                        return;
                    }

                    /*
                     * Only write biome quarts that actually overlap
                     * this island's 500x500 area.
                     */
                    if (currentX >= minX
                            && currentX <= maxX
                            && currentZ >= minZ
                            && currentZ <= maxZ
                            && currentY >= oneBlockWorld.getMinHeight()
                            && currentY < oneBlockWorld.getMaxHeight()) {

                        oneBlockWorld.setBiome(
                                currentX,
                                currentY,
                                currentZ,
                                biome
                        );

                        operations++;
                    }

                    advance();
                }
            }

            private void advance() {

                currentY += BIOME_STEP;

                if (currentY >= oneBlockWorld.getMaxHeight()) {

                    currentY = startY;
                    currentZ += BIOME_STEP;

                    if (currentZ > maxZ) {

                        currentZ = startZ;
                        currentX += BIOME_STEP;
                    }
                }
            }

            private void finish() {

                island.setBiome(biome);

                refreshIslandChunks(
                        minX,
                        maxX,
                        minZ,
                        maxZ
                );

                changingBiomes.remove(position);

                if (whenFinished != null) {
                    whenFinished.run();
                }
            }

        }.runTaskTimer(
                plugin,
                1L,
                1L
        );

        return true;
    }

    private void refreshIslandChunks(
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {

        int minChunkX =
                Math.floorDiv(minX, 16);

        int maxChunkX =
                Math.floorDiv(maxX, 16);

        int minChunkZ =
                Math.floorDiv(minZ, 16);

        int maxChunkZ =
                Math.floorDiv(maxZ, 16);

        for (int chunkX = minChunkX;
             chunkX <= maxChunkX;
             chunkX++) {

            for (int chunkZ = minChunkZ;
                 chunkZ <= maxChunkZ;
                 chunkZ++) {

                if (oneBlockWorld.isChunkLoaded(
                        chunkX,
                        chunkZ
                )) {

                    oneBlockWorld.refreshChunk(
                            chunkX,
                            chunkZ
                    );
                }
            }
        }
    }
}