package eu.linuxfox.oneblock.progression;

import org.bukkit.Material;

public class LootEntry {

    private final Material material;
    private final int weight;
    private final int minAmount;
    private final int maxAmount;

    public LootEntry(
            Material material,
            int weight,
            int minAmount,
            int maxAmount
    ) {
        this.material = material;
        this.weight = weight;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public Material getMaterial() {
        return material;
    }

    public int getWeight() {
        return weight;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }
}
