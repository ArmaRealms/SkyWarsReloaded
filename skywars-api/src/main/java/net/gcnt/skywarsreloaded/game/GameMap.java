package net.gcnt.skywarsreloaded.game;

import net.gcnt.skywarsreloaded.wrapper.SWPlayer;

import java.util.List;

public interface GameMap {

    String getId();

    Game getGame();

    List<Team> getTeams();

    void addPlayers(SWPlayer... players);

    void removePlayer(SWPlayer player);

    List<? extends SWPlayer> getPlayers();

    List<? extends SWPlayer> getAlivePlayers();

    List<? extends SWPlayer> getSpectators();

    GameStatus getStatus();

    void setStatus(GameStatus status);

    int getTimer();

    void setTimer(int timer);

}
