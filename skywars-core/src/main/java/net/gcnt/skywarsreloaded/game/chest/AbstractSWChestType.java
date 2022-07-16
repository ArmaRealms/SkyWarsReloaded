package net.gcnt.skywarsreloaded.game.chest;

import net.gcnt.skywarsreloaded.SkyWarsReloaded;
import net.gcnt.skywarsreloaded.data.config.YAMLConfig;
import net.gcnt.skywarsreloaded.game.types.GameDifficulty;
import net.gcnt.skywarsreloaded.utils.Item;
import net.gcnt.skywarsreloaded.utils.properties.ChestProperties;
import net.gcnt.skywarsreloaded.utils.properties.ConfigProperties;
import net.gcnt.skywarsreloaded.utils.properties.FolderProperties;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public abstract class AbstractSWChestType implements SWChestType {

    private static final List<Integer> RANDOM_SLOTS;
    private static final List<Integer> RANDOM_SLOTS_DOUBLE;
    private static final Random RANDOM;

    static {
        RANDOM_SLOTS = new ArrayList<>();
        RANDOM_SLOTS_DOUBLE = new ArrayList<>();
        for (int i = 0; i < 54; i++) {
            RANDOM_SLOTS_DOUBLE.add(i);
            if (i < 27) RANDOM_SLOTS.add(i);
        }

        RANDOM = ThreadLocalRandom.current();
    }

    private final SkyWarsReloaded plugin;
    private final String name;
    private final YAMLConfig config;
    private final HashMap<GameDifficulty, HashMap<Integer, List<Item>>> inventoryContents;

    private String displayName;


    public AbstractSWChestType(SkyWarsReloaded plugin, String nameIn) {
        this.plugin = plugin;
        this.name = nameIn;
        this.inventoryContents = new HashMap<>();

        this.config = plugin.getYAMLManager().loadConfig(
                "chest-" + nameIn,
                FolderProperties.CHEST_TYPES_FOLDER.toString(),
                nameIn + ".yml",
                "/chests/default.yml");
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public HashMap<GameDifficulty, HashMap<Integer, List<Item>>> getAllContents() {
        return inventoryContents;
    }

    @Override
    public HashMap<Integer, List<Item>> getContents(GameDifficulty difficulty) {
        inventoryContents.forEach((key, value) -> System.out.println("difficulty found: " + key.getId()));
        return inventoryContents.get(difficulty);
    }

    @Override
    public Item[] generateChestLoot(GameDifficulty difficulty, boolean doubleChest) {
        // hashmap with a list of items per chance of occurrence
        int maxItems = plugin.getConfig().getInt(doubleChest ? ConfigProperties.GAME_CHESTS_MAX_ITEMS_DOUBLE.toString() : ConfigProperties.GAME_CHESTS_MAX_ITEMS.toString());

        List<Integer> slots = doubleChest ? RANDOM_SLOTS_DOUBLE : RANDOM_SLOTS;
        Collections.shuffle(slots);

        HashMap<Integer, List<Item>> items = getContents(difficulty);
        Item[] loot = new Item[slots.size()];
        if (items == null) return loot;

        int added = 0;
        adding:
        for (int chance : items.keySet()) {
            for (Item item : items.get(chance)) {
                if (item == null) continue;

                if (RANDOM.nextInt(100) + 1 <= chance) {
                    loot[slots.get(added)] = item;
                    added++;
                    if (added >= maxItems || added >= slots.size() - 1) break adding;
                }
            }
        }

        return loot;
    }

    @Override
    public synchronized void loadData() {
        // Basic chest type info init
        this.displayName = config.getString(ChestProperties.DISPLAY_NAME.toString(), name);

        try {
            config.getKeys(ChestProperties.DIFFICULTIES.toString()).stream().map(GameDifficulty::getById)
                    .forEach(this::loadDataFromGameType);

        } catch (Exception e) {
            plugin.getLogger().error(String.format("Failed to load chest type with id %s. Ignoring it. (%s)", name, e.getClass().getName() + ": " + e.getLocalizedMessage()));
        }
    }

    private void loadDataFromGameType(GameDifficulty gameDifficulty) {
        System.out.println("Loading chest type data for difficulty: " + gameDifficulty.getId());
        // Init data
        String gameTypeConfigSectionName = gameDifficulty.getId();
        String configPath = gameTypeConfigSectionName + ChestProperties.CONTENTS;

        // Load inventory content
        HashMap<Integer, List<Item>> gameTypeItems = new HashMap<>();

        if (config.isSet(configPath)) {
            config.getKeys(configPath).forEach(chance -> {
                try {
                    int number = Integer.parseInt(chance);
                    final List<Item> items = config.getItemList(configPath + "." + chance);
                    System.out.println("- Found " + number + " items for chance " + chance + ".");
                    gameTypeItems.put(number, items);
                } catch (Exception e) {
                    plugin.getLogger().error(
                            String.format("Failed to load percentage '%s' under game type '%s' for chest type '%s'. Ignoring it. (%s)",
                                    chance, gameTypeConfigSectionName, name, e.getClass().getName() + ": " + e.getLocalizedMessage()));
                }
            });
        }

        // Apply it to the data map
        inventoryContents.put(gameDifficulty, gameTypeItems);
    }

    @Override
    public void saveData() {
        config.set(ChestProperties.DISPLAY_NAME.toString(), displayName);

        getAllContents().forEach(
                (gameDifficulty, contents) -> {
                    String gameTypeConfigSectionName = gameDifficulty.getId();
                    contents.forEach((slot, item) ->
                            config.set(gameTypeConfigSectionName + "." + ChestProperties.CONTENTS + "." + slot, item));
                }
        );

        config.save();
    }
}
