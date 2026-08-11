package eu.linuxfox.oneblock.progression;

import eu.linuxfox.oneblock.island.Island;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MilestoneManager {

    private final JavaPlugin plugin;
    private final Random random = new Random();

    private final Map<Integer, Integer> intervals = new HashMap<>();
    private final Map<Integer, Integer> rolls = new HashMap<>();
    private final Map<Integer, List<LootEntry>> lootTables = new HashMap<>();

    public MilestoneManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadMilestones() {

        intervals.clear();
        rolls.clear();
        lootTables.clear();

        ConfigurationSection stagesSection =
                plugin.getConfig().getConfigurationSection("stages");

        if (stagesSection == null) {
            return;
        }

        for (String stageKey : stagesSection.getKeys(false)) {

            int stageNumber;

            try {
                stageNumber = Integer.parseInt(stageKey);
            } catch (NumberFormatException exception) {
                continue;
            }

            String chestPath = "stages." + stageKey + ".chest";

            int interval = plugin.getConfig().getInt(
                    chestPath + ".interval",
                    50
            );

            int stageRolls = plugin.getConfig().getInt(
                    chestPath + ".rolls",
                    3
            );

            intervals.put(stageNumber, interval);
            rolls.put(stageNumber, stageRolls);

            ConfigurationSection lootSection =
                    plugin.getConfig().getConfigurationSection(
                            chestPath + ".loot"
                    );

            List<LootEntry> entries = new ArrayList<>();

            if (lootSection != null) {

                for (String itemName : lootSection.getKeys(false)) {

                    Material material = Material.matchMaterial(itemName);

                    if (material == null || !material.isItem()) {
                        plugin.getLogger().warning(
                                "Invalid chest loot item '" +
                                        itemName +
                                        "' in Stage " +
                                        stageNumber
                        );
                        continue;
                    }

                    int weight = lootSection.getInt(
                            itemName + ".weight",
                            1
                    );

                    int min = lootSection.getInt(
                            itemName + ".min",
                            1
                    );

                    int max = lootSection.getInt(
                            itemName + ".max",
                            min
                    );

                    if (weight <= 0 || min <= 0 || max < min) {
                        plugin.getLogger().warning(
                                "Invalid loot settings for '" +
                                        itemName +
                                        "' in Stage " +
                                        stageNumber
                        );
                        continue;
                    }

                    entries.add(
                            new LootEntry(
                                    material,
                                    weight,
                                    min,
                                    max
                            )
                    );
                }
            }

            lootTables.put(stageNumber, entries);
        }

        plugin.getLogger().info(
                "Loaded milestone settings for " +
                        lootTables.size() +
                        " stage(s)."
        );
    }

    public boolean shouldGenerateChest(Island island) {

        int interval = intervals.getOrDefault(
                island.getStage(),
                50
        );

        if (interval <= 0) {
            return false;
        }

        return island.getProgress() > 0
                && island.getProgress() % interval == 0;
    }

    public void fillChest(Island island, Chest chest) {

        List<LootEntry> entries =
                lootTables.get(island.getStage());

        if (entries == null || entries.isEmpty()) {
            return;
        }

        int stageRolls = rolls.getOrDefault(
                island.getStage(),
                3
        );

        for (int i = 0; i < stageRolls; i++) {

            LootEntry selected = selectRandomEntry(entries);

            if (selected == null) {
                continue;
            }

            int amount = random.nextInt(
                    selected.getMaxAmount()
                            - selected.getMinAmount()
                            + 1
            ) + selected.getMinAmount();

            ItemStack item = new ItemStack(
                    selected.getMaterial(),
                    amount
            );

            chest.getBlockInventory().addItem(item);
        }
    }

    private LootEntry selectRandomEntry(List<LootEntry> entries) {

        int totalWeight = 0;

        for (LootEntry entry : entries) {
            totalWeight += entry.getWeight();
        }

        if (totalWeight <= 0) {
            return null;
        }

        int value = random.nextInt(totalWeight);

        int currentWeight = 0;

        for (LootEntry entry : entries) {

            currentWeight += entry.getWeight();

            if (value < currentWeight) {
                return entry;
            }
        }

        return null;
    }
}