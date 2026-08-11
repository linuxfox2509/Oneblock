package eu.linuxfox.oneblock;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.command.OneBlockCommand;
import eu.linuxfox.oneblock.island.IslandAllocator;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.listener.OneBlockBreakListener;
import eu.linuxfox.oneblock.listener.PlayerJoinListener;
import eu.linuxfox.oneblock.progression.MilestoneManager;
import eu.linuxfox.oneblock.progression.StageManager;
import eu.linuxfox.oneblock.storage.IslandStorage;
import eu.linuxfox.oneblock.world.OneBlockWorld;
import eu.linuxfox.oneblock.island.IslandBiomeService;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class OneBlockPlugin extends JavaPlugin {

    private IslandManager islandManager;
    private IslandAllocator islandAllocator;
    private World oneBlockWorld;
    private IslandStorage islandStorage;
    private StageManager stageManager;
    private OneBlockBossBar oneBlockBossBar;
    private MilestoneManager milestoneManager;
    private IslandDeletionService islandDeletionService;
    private IslandBiomeService islandBiomeService;

    @Override
    public void onEnable() {

        getLogger().info("OneBlock has been enabled!");

        // Create config.yml if it does not exist
        saveDefaultConfig();


        // ---------------------------------------------------------
        // WORLD
        // ---------------------------------------------------------

        OneBlockWorld worldManager =
                new OneBlockWorld("oneblock");

        oneBlockWorld =
                worldManager.createOrLoad();

        if (oneBlockWorld == null) {

            getLogger().severe(
                    "Failed to create or load the OneBlock world!"
            );

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);

            return;
        }


        // ---------------------------------------------------------
        // PROGRESSION
        // ---------------------------------------------------------

        stageManager =
                new StageManager(this);

        stageManager.loadStages();


        milestoneManager =
                new MilestoneManager(this);

        milestoneManager.loadMilestones();


        // ---------------------------------------------------------
        // BOSSBAR
        // ---------------------------------------------------------

        oneBlockBossBar =
                new OneBlockBossBar(stageManager);


        // ---------------------------------------------------------
        // ISLAND MANAGEMENT
        // ---------------------------------------------------------

        islandManager =
                new IslandManager();


        islandStorage =
                new IslandStorage(
                        this,
                        islandManager,
                        oneBlockWorld
                );

        islandStorage.loadIslands();


        islandAllocator =
                new IslandAllocator(
                        islandManager,
                        oneBlockWorld
                );


        // ---------------------------------------------------------
        // ISLAND DELETION
        // ---------------------------------------------------------

        islandDeletionService =
                new IslandDeletionService(
                        this,
                        islandManager,
                        islandStorage,
                        oneBlockBossBar,
                        oneBlockWorld
                );

        // ISLAND BIOMES
        islandBiomeService =
                new IslandBiomeService(
                        this,
                        oneBlockWorld
                );


        // ---------------------------------------------------------
        // COMMANDS
        // ---------------------------------------------------------

        long confirmTimeoutMs =
                getConfig().getLong(
                        "confirmation.timeout-seconds",
                        30
                ) * 1000L;

        OneBlockCommand oneBlockCommand =
                new OneBlockCommand(
                        islandManager,
                        islandAllocator,
                        oneBlockWorld,
                        islandStorage,
                        oneBlockBossBar,
                        stageManager,
                        islandDeletionService,
                        islandBiomeService,
                        confirmTimeoutMs
                );

        getCommand("oneblock")
                .setExecutor(oneBlockCommand);

        getCommand("oneblock")
                .setTabCompleter(oneBlockCommand);


        // ---------------------------------------------------------
        // EVENT LISTENERS
        // ---------------------------------------------------------

        getServer()
                .getPluginManager()
                .registerEvents(
                        new OneBlockBreakListener(
                                islandManager,
                                islandStorage,
                                stageManager,
                                milestoneManager,
                                oneBlockBossBar,
                                islandDeletionService
                        ),
                        this
                );


        getServer()
                .getPluginManager()
                .registerEvents(
                        new PlayerJoinListener(
                                islandManager,
                                islandStorage,
                                oneBlockBossBar
                        ),
                        this
                );
    }


    @Override
    public void onDisable() {

        if (islandStorage != null) {
            islandStorage.saveIslands();
        }

        getLogger().info(
                "OneBlock has been disabled!"
        );
    }
}