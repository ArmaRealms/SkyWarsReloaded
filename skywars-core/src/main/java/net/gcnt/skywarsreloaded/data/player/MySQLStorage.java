package net.gcnt.skywarsreloaded.data.player;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.gcnt.skywarsreloaded.AbstractSkyWarsReloaded;
import net.gcnt.skywarsreloaded.data.config.AbstractYAMLConfig;
import net.gcnt.skywarsreloaded.utils.properties.ConfigProperties;
import net.gcnt.skywarsreloaded.wrapper.player.SWPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLStorage implements Storage {

    private final AbstractSkyWarsReloaded plugin;
    private HikariDataSource ds;

    public MySQLStorage(AbstractSkyWarsReloaded plugin) {
        this.plugin = plugin;
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    @Override
    public void setup() {
        AbstractYAMLConfig yamlConfig = (AbstractYAMLConfig) plugin.getYAMLManager().getConfig("config");
        if (yamlConfig == null)
            throw new NullPointerException("Cannot set up database connection because config file is null. Unable to retrieve database info from config.yml.");

        String hostname = yamlConfig.getString(ConfigProperties.STORAGE_HOSTNAME.toString());
        String database = yamlConfig.getString(ConfigProperties.STORAGE_DATABASE.toString());
        int port = 3306;
        if (hostname.contains(":")) {
            String[] split = hostname.split(":");
            hostname = split[0];
            port = Integer.parseInt(split[1]);
        }

        HikariConfig config = new HikariConfig();
        String uri = "jdbc:mysql://" + hostname + ":" + port + "/" + database + "?useSSL=" + (yamlConfig.getBoolean(ConfigProperties.STORAGE_USE_SSL.toString()));
        config.setJdbcUrl(uri);
        config.setUsername(yamlConfig.getString(ConfigProperties.STORAGE_USERNAME.toString()));
        config.setPassword(yamlConfig.getString(ConfigProperties.STORAGE_DATABASE.toString()));
        config.setMinimumIdle(1);
        config.setMaximumPoolSize(50);
        config.setConnectionTimeout(4000);
        ds = new HikariDataSource(config);

        try (Connection connection = getConnection()) {
            connection.createStatement().executeUpdate("CREATE DATABASE IF NOT EXISTS " + database);
            connection.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS `sw_player_data` (" +
                    "`uuid`   VARCHAR(255)  NOT NULL UNIQUE," +
                    "`solo_wins`  INT(6) DEFAULT 0," +
                    "`solo_kills` INT(10) DEFAULT 0," +
                    "`solo_games` INT(10) DEFAULT 0," +
                    "`team_wins` INT(10) DEFAULT 0," +
                    "`team_kills` INT(10) DEFAULT 0," +
                    "`team_games` INT(10) DEFAULT 0," +
                    "`selected_solo_cage` VARCHAR(100) DEFAULT NULL," +
                    "`selected_team_cage` VARCHAR(100) DEFAULT NULL," +
                    "`selected_particle` VARCHAR(100) DEFAULT NULL," +
                    "`selected_kill_effect` VARCHAR(100) DEFAULT NULL," +
                    "`selected_win_effect` VARCHAR(100) DEFAULT NULL," +
                    "`selected_projectile_effect` VARCHAR(100) DEFAULT NULL," +
                    "`selected_kill_messages_theme` VARCHAR(100) DEFAULT NULL," +
                    "KEY  (`uuid`))");
        } catch (SQLException e) {
            plugin.getLogger().error("SkyWarsReloaded failed to connect to the MySQL database with the following hostname: '" + hostname + ":" + port + "'. Do you have access?");
            plugin.getLogger().error("Here's the mysql URI we used: " + uri);
            plugin.getLogger().error("Disabling the plugin to prevent further complications...");
            e.printStackTrace();
            plugin.disableSkyWars();
        }
    }

    private void createDefault(SWPlayer player, Connection conn) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("INSERT INTO `sw_player_data`(`uuid`) VALUES (?)");
        ps.setString(1, player.getUuid().toString());
        ps.executeUpdate();

        SWPlayerData swpd = this.plugin.getPlayerDataManager().createSWPlayerDataInstance();
        SWPlayerStats swps = this.plugin.getPlayerDataManager().createSWPlayerStatsInstance();
        swps.initData(0, 0, 0, 0, 0, 0);
        swpd.initData(swps, null, null, null, null, null, null, null);
        player.setPlayerData(swpd);
    }

    @Override
    public void loadData(SWPlayer player) {
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM `sw_player_data` WHERE `uuid`=?");
            ps.setString(1, player.getUuid().toString());
            ResultSet res = ps.executeQuery();

            if (!res.next()) {
                createDefault(player, conn);
                return;
            }

            SWPlayerData swpd = this.plugin.getPlayerDataManager().createSWPlayerDataInstance();
            SWPlayerStats swps = this.plugin.getPlayerDataManager().createSWPlayerStatsInstance();
            swps.initData(
                    res.getInt("solo_wins"),
                    res.getInt("solo_kills"),
                    res.getInt("solo_games"),
                    res.getInt("team_wins"),
                    res.getInt("team_kills"),
                    res.getInt("team_games")
            );
            swpd.initData(swps,
                    res.getString("selected_solo_cage"),
                    res.getString("selected_team_cage"),
                    res.getString("selected_particle"),
                    res.getString("selected_kill_effect"),
                    res.getString("selected_win_effect"),
                    res.getString("selected_projectile_effect"),
                    res.getString("selected_kill_messages_theme")
            );
            player.setPlayerData(swpd);
            res.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveData(SWPlayer player) {
        SWPlayerData swpd = player.getPlayerData();
        SWPlayerStats swps = swpd.getStats();

        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE `sw_player_data` SET solo_wins=?, solo_kills=?, solo_games=?, team_wins=?, team_kills=?, team_games=? WHERE `uuid`=?");
            ps.setInt(1, swps.getSoloWins());
            ps.setInt(2, swps.getSoloKills());
            ps.setInt(3, swps.getSoloGamesPlayed());
            ps.setInt(4, swps.getTeamKills());
            ps.setInt(5, swps.getTeamWins());
            ps.setInt(6, swps.getTeamGamesPlayed());

            ps.setString(7, player.getUuid().toString());

            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setProperty(String property, Object value, SWPlayer player) {
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE `sw_player_data` SET ?=? WHERE `uuid`=?");
            ps.setString(1, property);

            if (value instanceof Integer) ps.setInt(2, (Integer) value);
            else if (value instanceof Double) ps.setDouble(2, (Double) value);
            else if (value instanceof Boolean) ps.setBoolean(2, (Boolean) value);
            else if (value instanceof Float) ps.setFloat(2, (Float) value);
            else ps.setString(2, value.toString());

            ps.setString(3, player.getUuid().toString());

            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
