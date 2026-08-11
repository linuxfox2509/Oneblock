package eu.linuxfox.oneblock.listener;

import eu.linuxfox.oneblock.bossbar.OneBlockBossBar;
import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.progression.MilestoneManager;
import eu.linuxfox.oneblock.progression.Stage;
import eu.linuxfox.oneblock.progression.StageManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

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
        Island island =
                islandManager.getIslandByOneBlockLocation(
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

        /*
         * Breaking a reward chest does NOT increase island progress.
         */
        if (island.isRewardChestActive()) {

            Location dropLocation =
                    getDropLocation(
                            event.getBlock()
                    );

            // Drop everything still inside the chest
            if (event.getBlock().getState() instanceof Chest chest) {

                Inventory inventory =
                        chest.getBlockInventory();

                for (ItemStack item :
                        inventory.getContents()) {

                    if (item == null
                            || item.getType().isAir()) {
                        continue;
                    }

                    dropControlledItem(
                            dropLocation,
                            item
                    );
                }

                // Prevent item duplication
                inventory.clear();
            }

            // Drop the chest itself
            dropControlledItem(
                    dropLocation,
                    new ItemStack(
                            Material.CHEST,
                            1
                    )
            );

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
                    stage.getRandomBlock(
                            random
                    );

            setOneBlockMaterial(
                    event.getBlock(),
                    nextBlock
            );

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


        // ---------------------------------------------------------
        // CALCULATE DROPS
        // ---------------------------------------------------------

        /*
         * Do this BEFORE damaging the tool so the current item is
         * still available for calculating block drops.
         */
        Collection<ItemStack> drops =
                event.getBlock().getDrops(
                        tool,
                        event.getPlayer()
                );

        Location dropLocation =
                getDropLocation(
                        event.getBlock()
                );

        for (ItemStack drop : drops) {

            dropControlledItem(
                    dropLocation,
                    drop
            );
        }


        // ---------------------------------------------------------
        // TOOL DURABILITY
        // ---------------------------------------------------------

        /*
         * BlockBreakEvent is cancelled, so vanilla Minecraft never
         * gets a chance to damage the player's tool.
         *
         * Paper's damageItemStack() handles durability-related
         * behavior such as enchantments, events and break animations.
         */
        if (!tool.getType().isAir()) {

            event.getPlayer().damageItemStack(
                    EquipmentSlot.HAND,
                    1
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

            if (stageManager.hasStage(
                    nextStageNumber
            )) {

                island.setStage(
                        nextStageNumber
                );

                island.setProgress(
                        0
                );

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
                && milestoneManager.shouldGenerateChest(
                island
        )) {

            event.getBlock()
                    .setType(
                            Material.CHEST
                    );

            if (event.getBlock().getState()
                    instanceof Chest chest) {

                milestoneManager.fillChest(
                        island,
                        chest
                );
            }

            island.setRewardChestActive(
                    true
            );

        } else {

            Material nextBlock =
                    stage.getRandomBlock(
                            random
                    );

            setOneBlockMaterial(
                    event.getBlock(),
                    nextBlock
            );
        }


        // ---------------------------------------------------------
        // SAVE + BOSSBAR
        // ---------------------------------------------------------

        islandStorage.saveIsland(
                island
        );

        oneBlockBossBar.update(
                island
        );
    }


    // -------------------------------------------------------------
    // GENERATE ONEBLOCK MATERIAL
    // -------------------------------------------------------------

    private void setOneBlockMaterial(
            Block block,
            Material material
    ) {

        block.setType(
                material
        );

        /*
         * Plugin-generated leaves would normally behave like
         * naturally generated leaves and may decay.
         *
         * Mark them persistent so the OneBlock cannot disappear.
         */
        if (block.getBlockData()
                instanceof Leaves leaves) {

            leaves.setPersistent(
                    true
            );

            block.setBlockData(
                    leaves
            );
        }
    }


    // -------------------------------------------------------------
    // ITEM DROPS
    // -------------------------------------------------------------

    private Location getDropLocation(
            Block block
    ) {

        return block.getLocation()
                .add(
                        0.5,
                        0.75,
                        0.5
                );
    }

    private void dropControlledItem(
            Location location,
            ItemStack itemStack
    ) {

        Item droppedItem =
                location.getWorld().dropItem(
                        location,
                        itemStack
                );

        /*
         * Prevent the normal random outward launch that makes
         * early-game items fall off the starter platform.
         */
        droppedItem.setVelocity(
                new Vector(
                        0,
                        0,
                        0
                )
        );
    }
}