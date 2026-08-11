package eu.linuxfox.oneblock.island;

import org.bukkit.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IslandManager {

    private final Map<UUID, Island> islandsByOwner = new HashMap<>();
    private final Map<GridPosition, Island> islandsByPosition = new HashMap<>();

    public boolean hasIsland(UUID playerUuid) {
        return islandsByOwner.containsKey(playerUuid);
    }

    public Island getIsland(UUID playerUuid) {
        return islandsByOwner.get(playerUuid);
    }

    public void addIsland(Island island) {

        islandsByOwner.put(
                island.getOwner(),
                island
        );

        GridPosition position = new GridPosition(
                island.getGridX(),
                island.getGridZ()
        );

        islandsByPosition.put(
                position,
                island
        );
    }

    /*
     * Removes one SPECIFIC island.
     *
     * Using Map.remove(key, value) is important:
     * it will only remove the mapping if it still points
     * to this exact Island object.
     */
    public void removeIsland(Island island) {

        islandsByOwner.remove(
                island.getOwner(),
                island
        );

        GridPosition position = new GridPosition(
                island.getGridX(),
                island.getGridZ()
        );

        islandsByPosition.remove(
                position,
                island
        );
    }

    /*
     * Convenience method if we ever need to remove by owner.
     * It delegates to the safe exact-island removal above.
     */
    public void removeIsland(UUID playerUuid) {

        Island island = islandsByOwner.get(playerUuid);

        if (island != null) {
            removeIsland(island);
        }
    }

    public boolean isPositionOccupied(GridPosition position) {
        return islandsByPosition.containsKey(position);
    }

    public Collection<Island> getAllIslands() {
        return islandsByOwner.values();
    }

    public Island getIslandByOneBlockLocation(Location location) {

        for (Island island : islandsByOwner.values()) {

            /*
             * Make sure we're looking in the same world.
             *
             * Otherwise a block at 0,100,0 in another world could
             * accidentally be mistaken for an island's OneBlock.
             */
            if (island.getHome().getWorld() == null
                    || location.getWorld() == null
                    || !island.getHome().getWorld().equals(location.getWorld())) {

                continue;
            }

            Location oneBlock = island.getOneBlockLocation(
                    island.getHome().getWorld()
            );

            if (oneBlock.getBlockX() == location.getBlockX()
                    && oneBlock.getBlockY() == location.getBlockY()
                    && oneBlock.getBlockZ() == location.getBlockZ()) {

                return island;
            }
        }

        return null;
    }

    public Island getIslandByPosition(GridPosition position) {
        return islandsByPosition.get(position);
    }

    public boolean hasIslandAtPosition(GridPosition position) {
        return islandsByPosition.containsKey(position);
    }

    public Island getIslandByOwnerName(String ownerName) {

        for (Island island : islandsByOwner.values()) {

            if (island.getOwnerName()
                    .equalsIgnoreCase(ownerName)) {

                return island;
            }
        }

        return null;
    }
}