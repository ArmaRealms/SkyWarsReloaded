package com.walrusone.skywarsreloaded.commands.player;

import com.walrusone.skywarsreloaded.SkyWarsReloaded;
import com.walrusone.skywarsreloaded.enums.MatchState;
import com.walrusone.skywarsreloaded.game.GameMap;
import com.walrusone.skywarsreloaded.managers.MatchManager;
import com.walrusone.skywarsreloaded.utilities.Messaging;
import com.walrusone.skywarsreloaded.utilities.Party;
import com.walrusone.skywarsreloaded.utilities.SWRServer;
import com.walrusone.skywarsreloaded.utilities.Util;
import me.gaagjescraft.network.team.skywarsreloaded.extension.SWExtension;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SWPlayCmd extends com.walrusone.skywarsreloaded.commands.BaseCmd {

    public SWPlayCmd(String t) {
        type = t;
        forcePlayer = true;
        cmdName = "play";
        alias = new String[]{"jogar"};
        argLength = 2;
    }

    @Override
    public boolean run(CommandSender sender, Player player, String[] args) {
        GameMap a = MatchManager.get().getPlayerMap(player);
        if (a != null) {
            if (Bukkit.getPluginManager().isPluginEnabled("Skywars-Extension")) {
                player.sendMessage(SWExtension.c(SWExtension.get().getConfig().getString("already_ingame")));
            } else {
                player.sendMessage(new Messaging.MessageFormatter().format("error.already-in-game"));
            }
            return true;
        }
        // Se não estivermos no lobby, e estivermos em modo bungee, utilizamos o metodo de join simples
        if (SkyWarsReloaded.getCfg().bungeeMode() && !SkyWarsReloaded.getCfg().isLobbyServer()) {
            SWRServer server = SWRServer.getAvailableServer();
            if (server != null) {
                server.setPlayerCount(server.getPlayerCount() + 1);
                server.updateSigns();
                SkyWarsReloaded.get().sendBungeeMsg(player, "Connect", server.getServerName());
            }
            return true;
        }

        String mapName = args[1];
        return joinSpecificMap(player, mapName);
    }

    /**
     * Tenta inserir o jogador (ou a party) em um mapa específico.
     */
    private boolean joinSpecificMap(Player player, String mapName) {
        GameMap gameMap = SkyWarsReloaded.getGameMapMgr().getMap(mapName);
        if (gameMap == null) {
            player.sendMessage(format("error.map-does-not-exist"));
            return true;
        }

        SWRServer server = null;
        MatchState matchState;

        // Se estivermos no lobby com bungeeMode, buscamos o servidor associado ao mapa
        if (SkyWarsReloaded.getCfg().bungeeMode() && SkyWarsReloaded.getCfg().isLobbyServer()) {
            server = SWRServer.getServer(mapName);
            if (server == null) {
                return true;
            }
            matchState = server.getMatchState();
        } else {
            matchState = gameMap.getMatchState();
        }

        // Somente permite join se o mapa estiver em um estado adequado
        if (matchState != MatchState.WAITINGSTART && matchState != MatchState.WAITINGLOBBY) {
            Util.get().playSound(player, player.getLocation(), SkyWarsReloaded.getCfg().getErrorSound(), 1, 1);
            player.sendMessage(format("error.could-not-join"));
            return true;
        }

        // Verifica permissão
        if (!player.hasPermission("sw.play")) {
            player.sendMessage(format("error.no-perm"));
            return true;
        }

        // Fecha o inventário do jogador para evitar conflitos visuais
        player.closeInventory();

        Party party = Party.getParty(player);
        boolean joined = false;

        if (party != null) {
            // Apenas o líder pode realizar o join para a party
            if (!party.getLeader().equals(player.getUniqueId())) {
                player.sendMessage(format("party.onlyleader"));
                return true;
            }
            if (gameMap.canAddParty(party)) {
                joined = gameMap.addPlayers(null, party);
            } else if (server != null && server.canAddParty(party)) {
                server.setPlayerCount(server.getPlayerCount() + party.getSize() - 1);
                server.updateSigns();
                // Para cada membro da party, envia a mensagem para conectar no servidor
                for (UUID memberId : party.getMembers()) {
                    Player member = Bukkit.getPlayer(memberId);
                    if (member != null) {
                        SkyWarsReloaded.get().sendBungeeMsg(member, "Connect", server.getServerName());
                    }
                }
                joined = true;
            }
        } else {
            // Caso seja um jogador individual
            if (gameMap.canAddPlayer(player)) {
                joined = gameMap.addPlayers(null, player);
            } else if (server != null && server.canAddPlayer()) {
                server.setPlayerCount(server.getPlayerCount() + 1);
                server.updateSigns();
                SkyWarsReloaded.get().sendBungeeMsg(player, "Connect", server.getServerName());
                joined = true;
            }
        }
        if (!joined) {
            player.sendMessage(format("error.could-not-join2"));
        }
        return true;
    }

    /**
     * Metodo auxiliar para formatar mensagens.
     */
    private String format(String key) {
        return new Messaging.MessageFormatter().format(key);
    }
}
