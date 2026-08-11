package eu.linuxfox.oneblock.progression;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class StageManager {

    private final JavaPlugin plugin;

    private final Map<Integer, Stage> stages = new HashMap<>();

    private int baseRequired;
    private double multiplier;

    public StageManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadStages() {

        stages.clear();

        baseRequired = plugin.getConfig().getInt(
                "progression.base-required",
                250
        );

        multiplier = plugin.getConfig().getDouble(
                "progression.multiplier",
                1.5
        );

        ConfigurationSection stagesSection =
                plugin.getConfig().getConfigurationSection("stages");

        if (stagesSection == null) {
            plugin.getLogger().severe(
                    "No stages found in config.yml!"
            );
            return;
        }

        for (String stageKey : stagesSection.getKeys(false)) {

            int stageNumber;

            try {
                stageNumber = Integer.parseInt(stageKey);
            } catch (NumberFormatException exception) {
                plugin.getLogger().warning(
                        "Invalid stage number in config.yml: " + stageKey
                );
                continue;
            }

            String path = "stages." + stageKey;

            String name = plugin.getConfig().getString(
                    path + ".name",
                    "Stage " + stageNumber
            );

            ConfigurationSection blocksSection =
                    plugin.getConfig().getConfigurationSection(
                            path + ".blocks"
                    );

            if (blocksSection == null) {
                plugin.getLogger().warning(
                        "Stage " + stageNumber + " has no blocks!"
                );
                continue;
            }

            Map<Material, Integer> blockWeights =
                    new LinkedHashMap<>();

            for (String materialName : blocksSection.getKeys(false)) {

                Material material = Material.matchMaterial(materialName);

                if (material == null || !material.isBlock()) {
                    plugin.getLogger().warning(
                            "Invalid block '" +
                                    materialName +
                                    "' in stage " +
                                    stageNumber
                    );
                    continue;
                }

                int weight =
                        blocksSection.getInt(materialName);

                if (weight <= 0) {
                    plugin.getLogger().warning(
                            "Block '" +
                                    materialName +
                                    "' in stage " +
                                    stageNumber +
                                    " has an invalid weight."
                    );
                    continue;
                }

                blockWeights.put(material, weight);
            }

            if (blockWeights.isEmpty()) {
                plugin.getLogger().warning(
                        "Stage " +
                                stageNumber +
                                " contains no valid blocks!"
                );
                continue;
            }

            int requiredBlocks =
                    calculateRequiredBlocks(stageNumber);

            Stage stage = new Stage(
                    stageNumber,
                    name,
                    requiredBlocks,
                    blockWeights
            );

            stages.put(stageNumber, stage);
        }

        plugin.getLogger().info(
                "Loaded " + stages.size() + " stage(s)."
        );
    }

    private int calculateRequiredBlocks(int stageNumber) {

        double required =
                baseRequired *
                        Math.pow(multiplier, stageNumber - 1);

        return (int) Math.round(required);
    }

    public Stage getStage(int stageNumber) {
        return stages.get(stageNumber);
    }

    public boolean hasStage(int stageNumber) {
        return stages.containsKey(stageNumber);
    }

    public int getStageCount() {
        return stages.size();
    }
}
