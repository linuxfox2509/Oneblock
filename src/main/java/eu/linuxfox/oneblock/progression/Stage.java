package eu.linuxfox.oneblock.progression;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class Stage {

    private final int number;
    private final String name;
    private final int requiredBlocks;

    private final Map<Material, Integer> blockWeights;

    public Stage(
            int number,
            String name,
            int requiredBlocks,
            Map<Material, Integer> blockWeights
    ) {
        this.number = number;
        this.name = name;
        this.requiredBlocks = requiredBlocks;
        this.blockWeights = new LinkedHashMap<>(blockWeights);
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getRequiredBlocks() {
        return requiredBlocks;
    }

    public Material getRandomBlock(Random random) {

        int totalWeight = 0;

        for (int weight : blockWeights.values()) {
            totalWeight += weight;
        }

        int randomValue = random.nextInt(totalWeight);

        int currentWeight = 0;

        for (Map.Entry<Material, Integer> entry : blockWeights.entrySet()) {

            currentWeight += entry.getValue();

            if (randomValue < currentWeight) {
                return entry.getKey();
            }
        }

        // Happens if config invalid
        return Material.STONE;
    }
}
