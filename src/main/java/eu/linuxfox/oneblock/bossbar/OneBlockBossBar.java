package eu.linuxfox.oneblock.bossbar;

import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.progression.Stage;
import eu.linuxfox.oneblock.progression.StageManager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OneBlockBossBar {

    private final StageManager stageManager;

    private final Map<UUID, BossBar> bossBars = new HashMap<>();

    public OneBlockBossBar(StageManager stageManager) {
        this.stageManager = stageManager;
    }

    public void update(Island island) {

        Stage stage = stageManager.getStage(island.getStage());

        if (stage == null) {
            return;
        }

        BossBar bossBar = bossBars.computeIfAbsent(
                island.getOwner(),
                uuid -> BossBar.bossBar(
                        Component.empty(),
                        0.0f,
                        BossBar.Color.GREEN,
                        BossBar.Overlay.PROGRESS
                )
        );

        float progress = (float) island.getProgress()
                / stage.getRequiredBlocks();

        progress = Math.max(0.0f, Math.min(1.0f, progress));

        bossBar.name(
                Component.text(
                        getOwnerName(island) +
                                " - Stage " +
                                island.getStage()
                )
        );

        bossBar.progress(progress);

        Player owner = Bukkit.getPlayer(island.getOwner());

        if (owner != null) {
            owner.showBossBar(bossBar);
        }
    }

    private String getOwnerName(Island island) {

        Player owner = Bukkit.getPlayer(island.getOwner());

        if (owner != null) {
            return owner.getName();
        }

        return "Player";
    }

    public void hide(Island island) {
        BossBar bossBar = bossBars.get(island.getOwner());

        if (bossBar == null) {
            return;
        }

        Player owner = Bukkit.getPlayer(island.getOwner());

        if (owner != null) {
            owner.hideBossBar(bossBar);
        }
    }
}
