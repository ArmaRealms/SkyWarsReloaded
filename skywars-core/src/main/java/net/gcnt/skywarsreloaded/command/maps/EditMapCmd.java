package net.gcnt.skywarsreloaded.command.maps;

import net.gcnt.skywarsreloaded.SkyWarsReloaded;
import net.gcnt.skywarsreloaded.command.Cmd;
import net.gcnt.skywarsreloaded.game.GameTemplate;
import net.gcnt.skywarsreloaded.game.GameWorld;
import net.gcnt.skywarsreloaded.game.types.GameStatus;
import net.gcnt.skywarsreloaded.utils.properties.InternalProperties;
import net.gcnt.skywarsreloaded.utils.properties.MessageProperties;
import net.gcnt.skywarsreloaded.wrapper.player.SWPlayer;
import net.gcnt.skywarsreloaded.wrapper.sender.SWCommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class EditMapCmd extends Cmd {

    public EditMapCmd(SkyWarsReloaded plugin) {
        super(plugin, "skywarsmap", "edit", "skywars.command.map.edit", true, "<name>", "Edit a map template.", "e");
    }

    @Override
    public boolean run(SWCommandSender sender, String[] args) {
        if (args.length == 0) {
            plugin.getMessages().getMessage(MessageProperties.MAPS_ENTER_NAME.toString()).send(sender);
            return true;
        }

        if (plugin.getDataConfig().getCoord("lobby") == null) {
            plugin.getMessages().getMessage(MessageProperties.ERROR_LOBBY_SPAWN_NOT_SET.toString()).send(sender);
            return true;
        }

        final String templateName = args[0];
        GameTemplate template = plugin.getGameManager().getGameTemplateByName(templateName);
        if (template == null) {
            plugin.getMessages().getMessage(MessageProperties.MAPS_DOESNT_EXIST.toString()).send(sender);
            return true;
        }
        final SWPlayer player = (SWPlayer) sender;

        List<GameWorld> worlds = plugin.getGameManager().getGameWorlds(template);
        for (GameWorld world : worlds) {
            if (world.isEditing()) {
                plugin.getMessages().getMessage(MessageProperties.MAPS_EDIT_EXISTING_WORLD.toString()).replace("%template%", template.getName()).send(sender);
                world.readyForEditing();
                player.teleport(world.getWorldName(), 0, 51, 0);
                return true;
            } else if (world.getStatus() != GameStatus.DISABLED) {
                plugin.getMessages().getMessage(MessageProperties.ERROR_CANNOT_SET_LOBBYSPAWN_IN_GAMEWORLD.toString()).replace("%template%", template.getName()).send(sender);
                return true;
            }
        }


        plugin.getMessages().getMessage(MessageProperties.MAPS_GENERATING_WORLD.toString()).replace("%template%", template.getName()).send(sender);
        plugin.getMessages().getMessage(MessageProperties.TITLES_MAPS_GENERATING_WORLD.toString()).replace("%template%", template.getName()).sendTitle(20, 600, 20, sender);
        GameWorld world = plugin.getGameManager().createGameWorld(template);
        world.setEditing(true);

        // Create instance of the world given the template data, or create a new one if it doesn't exist.
        CompletableFuture<Boolean> templateExistsFuture;
        try {
            templateExistsFuture = plugin.getWorldLoader().generateWorldInstance(world);
        } catch (IllegalArgumentException | IllegalStateException e) {
            plugin.getMessages().getMessage(MessageProperties.MAPS_GENERATING_WORLD_FAIL.toString()).replace("%template%", template.getName()).send(sender);
            return true;
        }

        templateExistsFuture.thenAccept(templateExists -> {
            // Handle the initialization of a world if this was the creation of the template
            if (!templateExists) {
                plugin.getWorldLoader().createBasePlatform(world);
            }

            world.readyForEditing();

            player.teleport(world.getWorldName(),
                    InternalProperties.MAP_CREATE_PLATFORM_X,
                    InternalProperties.MAP_CREATE_PLATFORM_Y + 1,
                    InternalProperties.MAP_CREATE_PLATFORM_Z);

            plugin.getMessages().getMessage(MessageProperties.TITLES_MAPS_GENERATED_WORLD.toString()).replace("%template%", template.getName()).sendTitle(0, 100, 0, sender);
            plugin.getMessages().getMessage(MessageProperties.MAPS_GENERATED_WORLD.toString()).replace("%template%", template.getName()).send(sender);
            template.checkToDoList(sender);
            });
        return true;
    }

    @Override
    public List<String> onTabCompletion(SWCommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> maps = new ArrayList<>();
            plugin.getGameManager().getGameTemplates().forEach(template -> maps.add(template.getName()));
            return maps;
        }
        return new ArrayList<>();
    }
}
