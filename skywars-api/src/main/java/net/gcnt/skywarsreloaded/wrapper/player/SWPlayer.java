package net.gcnt.skywarsreloaded.wrapper.player;

import net.gcnt.skywarsreloaded.data.player.SWPlayerData;
import net.gcnt.skywarsreloaded.game.GameWorld;
import net.gcnt.skywarsreloaded.utils.Item;
import net.gcnt.skywarsreloaded.utils.SWCoord;
import net.gcnt.skywarsreloaded.wrapper.sender.SWCommandSender;

import java.util.concurrent.CompletableFuture;

/**
 * General data about a player that is independent of any running state of games or teams
 */
public interface SWPlayer extends SWCommandSender, SWOfflinePlayer {

    SWPlayerData getPlayerData();

    void setPlayerData(SWPlayerData playerData);

    GameWorld getGameWorld();

    void setGameWorld(GameWorld world);

    Item getItemInHand(boolean offHand);

    Item[] getInventory();

    void setSlot(int slot, Item item);

    Item getSlot(int slot);

    Item getHelmet();

    void setHelmet(Item helmet);

    Item getChestplate();

    void setChestplate(Item chestplate);

    Item getLeggings();

    void setLeggings(Item leggings);

    Item getBoots();

    void setBoots(Item boots);

    void clearInventory();

    SWCoord getLocation();

    void teleport(SWCoord coord);

    void teleport(String world, double x, double y, double z);

    void teleport(String world, double x, double y, double z, float yaw, float pitch);

    CompletableFuture<Boolean> teleportAsync(String world, double x, double y, double z);

    void sendTitle(String title, String subtitle);

    void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut);

    /**
     * Change the game mode of the player.
     *
     * @param gamemode <p>
     *                 0 - survival
     *                 1 - creative
     *                 2 - adventure
     *                 3 - spectate
     *                 </p>
     */
    void setGameMode(int gamemode);
}
