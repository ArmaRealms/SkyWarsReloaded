package net.gcnt.skywarsreloaded.command.maps;

import net.gcnt.skywarsreloaded.SkyWarsReloaded;
import net.gcnt.skywarsreloaded.command.Cmd;
import net.gcnt.skywarsreloaded.game.GameTemplate;
import net.gcnt.skywarsreloaded.game.GameWorld;
import net.gcnt.skywarsreloaded.wrapper.player.SWPlayer;
import net.gcnt.skywarsreloaded.wrapper.scheduler.CoreSWRunnable;
import net.gcnt.skywarsreloaded.wrapper.sender.SWCommandSender;

public class CreateMapCmd extends Cmd {

    public CreateMapCmd(SkyWarsReloaded plugin) {
        super(plugin, "skywarsmap", "create", "skywars.command.map.create", true, "<name>", "Create a new map template.");
    }

    @Override
    public boolean run(SWCommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getUtils().colorize("&cPlease enter a game template name."));
            return false; // todo not sure if I should return false/true here to show the usage too or not.
        }

        final String templateName = args[0];
        GameTemplate template = plugin.getGameManager().createGameTemplate(templateName);
        if (template == null) {
            sender.sendMessage(plugin.getUtils().colorize("&cThere already is a game template with that name."));
            return true;
        }
        final SWPlayer player = (SWPlayer) sender;

        sender.sendMessage(plugin.getUtils().colorize("&aA new game template with the name &e" + templateName + "&a has successfully been created."));
        sender.sendMessage(plugin.getUtils().colorize("&7Please hold while we generate the template world..."));
        player.sendTitle(plugin.getUtils().colorize("&6Generating World..."), plugin.getUtils().colorize("&7Please hold while we generate the template world"), 20, Integer.MAX_VALUE, 20);
        plugin.getScheduler().runSyncLater(new CoreSWRunnable() {
            @Override
            public void run() {
                GameWorld world = plugin.getGameManager().createGameWorld(template);
                world.setEditing(true);

                plugin.getWorldLoader().createEmptyWorld(world);
                plugin.getWorldLoader().createBasePlatform(world);
                plugin.getWorldLoader().updateWorldBorder(world);

                player.teleport(world.getWorldName(), 0, 51, 0);
                player.sendTitle(plugin.getUtils().colorize("&aGenerated World!"), plugin.getUtils().colorize("&7We completed generating the template world"), 0, 100, 20);
                sender.sendMessage(plugin.getUtils().colorize("&aWe finished generating the template world! &7You can now start building within the world borders. We will automatically convert the world to a schematic file when you're done."));
            }
        }, 1);
        return true;
    }
}
