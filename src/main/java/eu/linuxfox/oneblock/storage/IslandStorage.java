package eu.linuxfox.oneblock.storage;

import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;

public class IslandStorage {

    private final JavaPlugin plugin;
    private final IslandManager islandManager;
    private final World oneBlockWorld;

    private final File file;
    private final YamlConfiguration config;


    public IslandStorage(
            JavaPlugin plugin,
            IslandManager islandManager,
            World oneBlockWorld
    ) {
        this.plugin = plugin;
        this.islandManager = islandManager;
        this.oneBlockWorld = oneBlockWorld;

        this.file = new File(
                plugin.getDataFolder(),
                "islands.yml"
        );

        this.config =
                YamlConfiguration.loadConfiguration(file);
    }


    public void loadIslands() {

        ConfigurationSection islandsSection =
                config.getConfigurationSection("islands");

        if (islandsSection == null) {
            return;
        }

        Registry<Biome> biomeRegistry =
                RegistryAccess.registryAccess()
                        .getRegistry(RegistryKey.BIOME);

        int loaded = 0;

        for (String uuidString :
                islandsSection.getKeys(false)) {

            try {

                UUID owner =
                        UUID.fromString(uuidString);

                String path =
                        "islands." + uuidString;

                String ownerName =
                        config.getString(
                                path + ".owner-name",
                                "Unknown"
                        );

                int gridX =
                        config.getInt(
                                path + ".grid-x"
                        );

                int gridZ =
                        config.getInt(
                                path + ".grid-z"
                        );

                int stage =
                        config.getInt(
                                path + ".stage",
                                1
                        );

                int progress =
                        config.getInt(
                                path + ".progress",
                                0
                        );

                boolean rewardChestActive =
                        config.getBoolean(
                                path + ".reward-chest-active",
                                false
                        );


                // -------------------------------------------------
                // BIOME
                // -------------------------------------------------

                String biomeName =
                        config.getString(
                                path + ".biome",
                                "minecraft:plains"
                        );

                NamespacedKey biomeKey =
                        NamespacedKey.fromString(
                                biomeName
                        );

                Biome biome = null;

                if (biomeKey != null) {
                    biome =
                            biomeRegistry.get(
                                    biomeKey
                            );
                }

                if (biome == null) {

                    plugin.getLogger().warning(
                            "Invalid biome '" +
                                    biomeName +
                                    "' for island " +
                                    uuidString +
                                    ". Falling back to minecraft:plains."
                    );

                    biome = Biome.PLAINS;
                }


                // -------------------------------------------------
                // HOME
                // -------------------------------------------------

                double homeX =
                        config.getDouble(
                                path + ".home.x"
                        );

                double homeY =
                        config.getDouble(
                                path + ".home.y"
                        );

                double homeZ =
                        config.getDouble(
                                path + ".home.z"
                        );

                float homeYaw =
                        (float) config.getDouble(
                                path + ".home.yaw"
                        );

                float homePitch =
                        (float) config.getDouble(
                                path + ".home.pitch"
                        );

                Location home =
                        new Location(
                                oneBlockWorld,
                                homeX,
                                homeY,
                                homeZ,
                                homeYaw,
                                homePitch
                        );


                Island island =
                        new Island(
                                owner,
                                ownerName,
                                gridX,
                                gridZ,
                                stage,
                                progress,
                                home,
                                rewardChestActive,
                                biome
                        );

                islandManager.addIsland(island);

                loaded++;

            } catch (IllegalArgumentException exception) {

                plugin.getLogger().warning(
                        "Invalid UUID in islands.yml: "
                                + uuidString
                );
            }
        }

        plugin.getLogger().info(
                "Loaded " +
                        loaded +
                        " island(s)."
        );
    }


    public void saveIslands() {

        for (Island island :
                islandManager.getAllIslands()) {

            saveIslandData(island);
        }

        saveFile();
    }


    public void saveIsland(Island island) {

        saveIslandData(island);
        saveFile();
    }


    private void saveIslandData(
            Island island
    ) {

        String path =
                "islands." +
                        island.getOwner();


        // ---------------------------------------------------------
        // OWNER
        // ---------------------------------------------------------

        config.set(
                path + ".owner-name",
                island.getOwnerName()
        );


        // ---------------------------------------------------------
        // GRID POSITION
        // ---------------------------------------------------------

        config.set(
                path + ".grid-x",
                island.getGridX()
        );

        config.set(
                path + ".grid-z",
                island.getGridZ()
        );


        // ---------------------------------------------------------
        // BIOME
        // ---------------------------------------------------------

        config.set(
                path + ".biome",
                island.getBiome()
                        .getKey()
                        .asString()
        );


        // ---------------------------------------------------------
        // PROGRESSION
        // ---------------------------------------------------------

        config.set(
                path + ".stage",
                island.getStage()
        );

        config.set(
                path + ".progress",
                island.getProgress()
        );

        config.set(
                path + ".reward-chest-active",
                island.isRewardChestActive()
        );


        // ---------------------------------------------------------
        // HOME
        // ---------------------------------------------------------

        Location home =
                island.getHome();

        config.set(
                path + ".home.x",
                home.getX()
        );

        config.set(
                path + ".home.y",
                home.getY()
        );

        config.set(
                path + ".home.z",
                home.getZ()
        );

        config.set(
                path + ".home.yaw",
                home.getYaw()
        );

        config.set(
                path + ".home.pitch",
                home.getPitch()
        );
    }


    public void deleteIsland(
            Island island
    ) {

        String path =
                "islands." +
                        island.getOwner();

        /*
         * Only remove the data if the stored entry still points
         * to this exact grid position.
         */
        if (!config.contains(path)) {
            return;
        }

        int storedGridX =
                config.getInt(
                        path + ".grid-x"
                );

        int storedGridZ =
                config.getInt(
                        path + ".grid-z"
                );

        if (storedGridX
                != island.getGridX()
                || storedGridZ
                != island.getGridZ()) {

            plugin.getLogger().warning(
                    "Skipped deleting stale island data for "
                            + island.getOwner()
                            + " at grid ("
                            + island.getGridX()
                            + ", "
                            + island.getGridZ()
                            + ") because islands.yml now points to another island."
            );

            return;
        }

        config.set(
                path,
                null
        );

        saveFile();
    }


    private void saveFile() {

        try {

            config.save(file);

        }  catch (IOException exception) {

            plugin.getLogger().log(
                    Level.SEVERE,
                    "Could not save islands.yml!",
                    exception
            );
        }
    }
}