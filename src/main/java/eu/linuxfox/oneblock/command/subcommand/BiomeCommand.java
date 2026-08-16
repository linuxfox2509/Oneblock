package eu.linuxfox.oneblock.command.subcommand;

import eu.linuxfox.oneblock.island.Island;
import eu.linuxfox.oneblock.island.IslandBiomeService;
import eu.linuxfox.oneblock.island.IslandDeletionService;
import eu.linuxfox.oneblock.island.IslandManager;
import eu.linuxfox.oneblock.storage.IslandStorage;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class BiomeCommand implements SubCommand {
    private static final String PERMISSION = "oneblock.biome";

    private final IslandManager islandManager;
    private final IslandStorage islandStorage;
    private final IslandBiomeService islandBiomeService;
    private final IslandDeletionService islandDeletionService;

    public BiomeCommand(
            IslandManager islandManager,
            IslandStorage islandStorage,
            IslandBiomeService islandBiomeService,
            IslandDeletionService islandDeletionService
    ) {
        this.islandManager = islandManager;
        this.islandStorage = islandStorage;
        this.islandBiomeService = islandBiomeService;
        this.islandDeletionService = islandDeletionService;
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command.", NamedTextColor.RED));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return true;
        }

        Island island = islandManager.getIsland(player.getUniqueId());

        if (island == null) {
            player.sendMessage(Component.text("You don't have an island.", NamedTextColor.RED));
            return true;
        }

        if (islandDeletionService.isDeleting(island)) {
            player.sendMessage(Component.text("Your island is currently being deleted.", NamedTextColor.YELLOW));
            return true;
        }

        if (args.length == 1) {
            showCurrentBiome(player, island);
            return true;
        }

        if (islandBiomeService.isChangingBiome(island)) {
            player.sendMessage(Component.text("Your island biome is already being changed.", NamedTextColor.YELLOW));
            return true;
        }

        Biome biome = getRequestedBiome(args[1]);

        if (biome == null) {
            player.sendMessage(Component.text("Unknown biome: " + args[1], NamedTextColor.RED));
            return true;
        }

        if (island.getBiome().equals(biome)) {
            player.sendMessage(Component.text("Your island already uses " + getBiomeName(biome), NamedTextColor.YELLOW));
            return true;
        }

        changeBiome(player, island, biome);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERMISSION) || args.length != 2) {
            return List.of();
        }

        String input = args[1].toLowerCase(Locale.ROOT);
        List<String> suggestions = new ArrayList<>();

        suggestions.add("reset");

        Registry<Biome> biomeRegistry = getBiomeRegistry();

        biomeRegistry.forEach(biome -> {
            NamespacedKey key = biome.getKey();

            if (!key.getNamespace().equals("minecraft")) {
                return;
            }

            String biomeName = key.getKey();

            if (biomeName.startsWith(input)) {
                suggestions.add(biomeName);
            }
        });

        return suggestions;
    }

    private void showCurrentBiome(Player player, Island island) {
        player.sendMessage(Component.text("Current island biome: " + getBiomeName(island.getBiome()), NamedTextColor.GRAY));
    }

    private Biome getRequestedBiome(String input) {
        if (input.equalsIgnoreCase("reset")) {
            return Biome.PLAINS;
        }

        String biomeName = input.toLowerCase(Locale.ROOT);

        NamespacedKey key = NamespacedKey.fromString(
                biomeName.contains(":")
                ? biomeName
                        : "minecraft:" + biomeName
        );

        if (key == null) {
            return null;
        }

        return getBiomeRegistry().get(key);
    }

    private void changeBiome(Player player, Island island, Biome biome) {
        player.sendMessage(Component.text("Changing your island biome to " + getBiomeName(biome) + "...", NamedTextColor.YELLOW));

        boolean started = islandBiomeService.setBiome(island, biome, () -> finishBiomeChange(player, island, biome));

        if (!started) {
            player.sendMessage(Component.text("Your island biome is already being changed.", NamedTextColor.YELLOW));
        }
    }

    private void finishBiomeChange(Player player, Island island, Biome biome) {
        islandStorage.saveIsland(island);

        player.sendMessage(Component.text("Island biome changed to " + getBiomeName(biome) + ".", NamedTextColor.GREEN));
    }

    private Registry<Biome> getBiomeRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
    }

    private String getBiomeName(Biome biome) {
        return biome.getKey().getKey();
    }
}
