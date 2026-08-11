package eu.linuxfox.oneblock.island;

import org.bukkit.World;

public class IslandAllocator {

    private final IslandManager islandManager;
    private final World oneBlockWorld;

    public static final int ISLAND_SPACING = 500;
    public static final int CLEARANCE_SIZE = 200;

    public IslandAllocator(
            IslandManager islandManager,
            World oneBlockWorld
    ) {
        this.islandManager = islandManager;
        this.oneBlockWorld = oneBlockWorld;
    }

    public GridPosition findNextPosition() {

        GridPosition origin = new GridPosition(0, 0);

        if (!islandManager.isPositionOccupied(origin)
                && isAreaClear(origin)) {
            return origin;
        }

        int x = 0;
        int z = 0;

        int directionX = 1;
        int directionZ = 0;

        int segmentLength = 1;
        int segmentProgress = 0;
        int segmentsCompleted = 0;

        while (true) {
            x += directionX;
            z += directionZ;

            GridPosition position = new GridPosition(x, z);

            if (!islandManager.isPositionOccupied(position)
                    && isAreaClear(position)) {
                return position;
            }

            segmentProgress++;

            if (segmentProgress == segmentLength) {
                segmentProgress = 0;

                int oldDirectionX = directionX;

                directionX = -directionZ;
                directionZ = oldDirectionX;

                segmentsCompleted++;

                if (segmentsCompleted % 2 == 0) {
                    segmentLength++;
                }
            }
        }
    }

    public boolean isAreaClear(GridPosition position) {

        int centerX = position.x() * ISLAND_SPACING;
        int centerZ = position.z() * ISLAND_SPACING;

        int halfSize = CLEARANCE_SIZE / 2;

        int minX = centerX - halfSize;
        int maxX = centerX + halfSize - 1;

        int minZ = centerZ - halfSize;
        int maxZ = centerZ + halfSize - 1;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {

                int highestY = oneBlockWorld.getHighestBlockYAt(x, z);

                if (highestY >= oneBlockWorld.getMinHeight()) {
                    return false;
                }
            }
        }

        return true;
    }
}
