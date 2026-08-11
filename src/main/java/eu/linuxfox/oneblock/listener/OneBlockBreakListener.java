package eu.linuxfox.oneblock.listener;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.progression.MilestoneManager;
import eu.linuxfox.oneblock.progression.Stage;
import eu.linuxfox.oneblock.progression.StageManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Random;

public class OneBlockBreakListener implements Listener {

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final Random random = new Random();
    private final StageManager stageManager;
    private final OneBlockBossBar oneBlockBossBar;
    private final MilestoneManager milestoneManager;
    private final IslandDeletionService islandDeletionService;

    public OneBlockBreakListener(
            IslandManager islandManager,
            IslandStorage islandStorage,
            StageManager stageManager,
            MilestoneManager milestoneManager,
            OneBlockBossBar oneBlockBossBar,
            IslandDeletionService islandDeletionService
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.stageManager = stageManager;
        this.milestoneManager = milestoneManager;
        this.oneBlockBossBar = oneBlockBossBar;
        this.islandDeletionService = islandDeletionService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        // Check whether the broken block is a registered OneBlock
        Island island = islandManager.getIslandByOneBlockLocation(
                event.getBlock().getLocation()
        );

        // Not a OneBlock -> normal Minecraft behavior
        if (island == null) {
            return;
        }

        // If this island is currently being deleted,
        // completely disable OneBlock behavior
        if (islandDeletionService.isDeleting(island)) {
            event.setCancelled(true);
            return;
        }

        // We handle OneBlock breaking ourselves
        event.setCancelled(true);


        // ---------------------------------------------------------
        // REWARD CHEST
        // ---------------------------------------------------------

        // Breaking a reward chest does NOT increase island progress
        if (island.isRewardChestActive()) {

            // Drop everything still inside the chest
            if (event.getBlock().getState() instanceof Chest chest) {

                Inventory inventory =
                        chest.getBlockInventory();

                for (ItemStack item : inventory.getContents()) {

                    if (item == null
                            || item.getType().isAir()) {
                        continue;
                    }

                    event.getBlock()
                            .getWorld()
                            .dropItemNaturally(
                                    event.getBlock().getLocation(),
                                    item
                            );
                }

                // Prevent item duplication
                inventory.clear();
            }

            island.setRewardChestActive(false);

            Stage stage =
                    stageManager.getStage(
                            island.getStage()
                    );

            if (stage == null) {
                event.getPlayer().sendMessage(
                        "Error: This island's stage does not exist."
                );
                return;
            }

            // Generate the next normal block
            Material nextBlock =
                    stage.getRandomBlock(random);

            event.getBlock().setType(nextBlock);

            islandStorage.saveIsland(island);
            oneBlockBossBar.update(island);

            return;
        }


        // ---------------------------------------------------------
        // NORMAL ONEBLOCK BREAK
        // ---------------------------------------------------------

        ItemStack tool =
                event.getPlayer()
                        .getInventory()
                        .getItemInMainHand();

        Collection<ItemStack> drops =
                event.getBlock().getDrops(
                        tool,
                        event.getPlayer()
                );

        for (ItemStack drop : drops) {

            event.getBlock()
                    .getWorld()
                    .dropItemNaturally(
                            event.getBlock().getLocation(),
                            drop
                    );
        }


        // ---------------------------------------------------------
        // PROGRESS
        // ---------------------------------------------------------

        island.incrementProgress();

        Stage stage =
                stageManager.getStage(
                        island.getStage()
                );

        if (stage == null) {
            event.getPlayer().sendMessage(
                    "Error: This island's stage does not exist."
            );
            return;
        }

        boolean stageChanged = false;


        // ---------------------------------------------------------
        // STAGE ADVANCEMENT
        // ---------------------------------------------------------

        if (island.getProgress()
                >= stage.getRequiredBlocks()) {

            int nextStageNumber =
                    island.getStage() + 1;

            if (stageManager.hasStage(nextStageNumber)) {

                island.setStage(nextStageNumber);
                island.setProgress(0);

                stage =
                        stageManager.getStage(
                                nextStageNumber
                        );

                stageChanged = true;

                event.getPlayer().playSound(
                        event.getPlayer().getLocation(),
                        Sound.ENTITY_PLAYER_LEVELUP,
                        1.0f,
                        0.7f
                );

                event.getPlayer().sendMessage(
                        "Island advanced to Stage "
                                + stage.getNumber()
                                + " - "
                                + stage.getName()
                                + "!"
                );
            }
        }


        // ---------------------------------------------------------
        // MILESTONE CHEST / NEXT BLOCK
        // ---------------------------------------------------------

        if (!stageChanged
                && milestoneManager.shouldGenerateChest(island)) {

            event.getBlock()
                    .setType(Material.CHEST);

            if (event.getBlock().getState()
                    instanceof Chest chest) {

                milestoneManager.fillChest(
                        island,
                        chest
                );
            }

            island.setRewardChestActive(true);

        } else {

            Material nextBlock =
                    stage.getRandomBlock(random);

            event.getBlock().setType(nextBlock);
        }


        // ---------------------------------------------------------
        // SAVE + BOSSBAR
        // ---------------------------------------------------------

        islandStorage.saveIsland(island);
        oneBlockBossBar.update(island);
    }
}