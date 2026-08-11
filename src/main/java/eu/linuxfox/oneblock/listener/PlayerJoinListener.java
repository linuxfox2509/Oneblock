package eu.linuxfox.oneblock.listener;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final OneBlockBossBar oneBlockBossBar;

    public PlayerJoinListener(
            IslandManager islandManager,
            IslandStorage islandStorage,
            OneBlockBossBar oneBlockBossBar
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.oneBlockBossBar = oneBlockBossBar;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {

        Island island = islandManager.getIsland(
                event.getPlayer().getUniqueId()
        );

        if (island == null) {
            return;
        }

        String currentName = event.getPlayer().getName();

        // Update the stored owner name if the player changed their username
        if (!currentName.equals(island.getOwnerName())) {

            island.setOwnerName(currentName);

            islandStorage.saveIsland(island);
        }

        oneBlockBossBar.update(island);
    }
}