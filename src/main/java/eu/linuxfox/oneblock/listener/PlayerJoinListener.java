package eu.linuxfox.oneblock.listener;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final OneBlockBossBar oneBlockBossBar;
    private final World oneBlockWorld;

    public PlayerJoinListener(
            IslandManager islandManager,
            IslandStorage islandStorage,
            OneBlockBossBar oneBlockBossBar,
            World oneBlockWorld
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.oneBlockBossBar = oneBlockBossBar;
        this.oneBlockWorld = oneBlockWorld;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Island island = islandManager.getIsland(event.getPlayer().getUniqueId());

        if (island != null) {
            updateOwnerName(event, island);
            oneBlockBossBar.update(island);
        }

        World fallbackWorld = findFallbackWorld();

        if (fallbackWorld != null) {
            event.getPlayer().teleport(fallbackWorld.getSpawnLocation());
        }
    }

    private void updateOwnerName(PlayerJoinEvent event, Island island) {
        String currentName = event.getPlayer().getName();

        if (currentName.equals(island.getOwnerName())) {
            return;
        }

        island.setOwnerName(currentName);
        islandStorage.saveIsland(island);
    }

    private World findFallbackWorld() {
        for (World world : Bukkit.getWorlds()) {
            if (!world.equals(oneBlockWorld)) {
                return world;
            }
        }

        return null;
    }
}