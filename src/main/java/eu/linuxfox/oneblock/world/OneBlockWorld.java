package eu.linuxfox.oneblock.world;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class OneBlockWorld {

    private final String worldName;

    public OneBlockWorld(String worldName) {
        this.worldName = worldName;
    }

    public World createOrLoad() {

        WorldCreator creator = new WorldCreator(worldName);

        creator.environment(World.Environment.NORMAL);
        creator.generator(new VoidGenerator());
        creator.biomeProvider(new PlainsBiomeProvider());

        return creator.createWorld();
    }

    private static class VoidGenerator extends ChunkGenerator {
    }

    private static class PlainsBiomeProvider extends BiomeProvider {

        @Override
        public @NotNull Biome getBiome(
                @NotNull WorldInfo worldInfo,
                int x,
                int y,
                int z
        ) {
            return Biome.PLAINS;
        }

        @Override
        public @NotNull List<Biome> getBiomes(
                @NotNull WorldInfo worldInfo
        ) {
            return List.of(Biome.PLAINS);
        }
    }
}