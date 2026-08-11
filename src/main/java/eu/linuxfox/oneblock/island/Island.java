package eu.linuxfox.oneblock.island;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;

import java.util.UUID;

public class Island {

    private final UUID owner;
    private String ownerName;

    private final int gridX;
    private final int gridZ;

    private int stage;
    private int progress;

    private Location home;

    private boolean rewardChestActive;

    private Biome biome;


    // Used when creating a NEW island
    public Island(
            UUID owner,
            String ownerName,
            int gridX,
            int gridZ,
            World world
    ) {
        this.owner = owner;
        this.ownerName = ownerName;

        this.gridX = gridX;
        this.gridZ = gridZ;

        this.stage = 1;
        this.progress = 0;

        this.home = getOneBlockLocation(world)
                .clone()
                .add(0.5, 1, 0.5);

        this.rewardChestActive = false;

        // Every new island starts as Plains
        this.biome = Biome.PLAINS;
    }


    // Used when LOADING an existing island
    public Island(
            UUID owner,
            String ownerName,
            int gridX,
            int gridZ,
            int stage,
            int progress,
            Location home,
            boolean rewardChestActive,
            Biome biome
    ) {
        this.owner = owner;
        this.ownerName = ownerName;

        this.gridX = gridX;
        this.gridZ = gridZ;

        this.stage = stage;
        this.progress = progress;

        this.home = home;

        this.rewardChestActive = rewardChestActive;

        this.biome = biome;
    }


    public UUID getOwner() {
        return owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public int getGridX() {
        return gridX;
    }

    public int getGridZ() {
        return gridZ;
    }

    public int getStage() {
        return stage;
    }

    public int getProgress() {
        return progress;
    }

    public Location getHome() {
        return home;
    }

    public Biome getBiome() {
        return biome;
    }

    public void setBiome(Biome biome) {
        this.biome = biome;
    }


    public Location getOneBlockLocation(World world) {

        return new Location(
                world,
                gridX * 500,
                100,
                gridZ * 500
        );
    }


    public boolean containsLocation(Location location) {

        Location oneBlock =
                getOneBlockLocation(location.getWorld());

        int centerX = oneBlock.getBlockX();
        int centerZ = oneBlock.getBlockZ();

        int minX = centerX - 250;
        int maxX = centerX + 249;

        int minZ = centerZ - 250;
        int maxZ = centerZ + 249;

        int x = location.getBlockX();
        int z = location.getBlockZ();

        return x >= minX
                && x <= maxX
                && z >= minZ
                && z <= maxZ;
    }


    public void setHome(Location home) {
        this.home = home;
    }

    public void resetHome(World world) {

        this.home = getOneBlockLocation(world)
                .clone()
                .add(0.5, 1, 0.5);
    }

    public void incrementProgress() {
        progress++;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isRewardChestActive() {
        return rewardChestActive;
    }

    public void setRewardChestActive(
            boolean rewardChestActive
    ) {
        this.rewardChestActive = rewardChestActive;
    }
}