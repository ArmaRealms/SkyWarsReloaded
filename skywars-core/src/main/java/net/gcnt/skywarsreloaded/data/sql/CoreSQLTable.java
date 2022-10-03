package net.gcnt.skywarsreloaded.data.sql;

import net.gcnt.skywarsreloaded.wrapper.entity.SWPlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class CoreSQLTable<DataType> implements SQLTable<DataType> {

    protected final SQLStorage storage;
    protected final String table;

    public CoreSQLTable(SQLStorage storage, String table) {
        this.storage = storage;
        this.table = table;

        storage.addTable(this);
    }

    /**
     * @param statement     The {@link PreparedStatement} to supply with parameter data
     * @param paramPosition Number from 1 to n
     * @param value         Paramter data
     * @throws SQLException
     */
    @Override
    public void bindPropertyValue(PreparedStatement statement, int paramPosition, Object value) throws SQLException {
        if (statement == null) return;

        if (value instanceof Integer) statement.setInt(paramPosition, (Integer) value);
        else if (value instanceof Double) statement.setDouble(paramPosition, (Double) value);
        else if (value instanceof Boolean) statement.setBoolean(paramPosition, (Boolean) value);
        else if (value instanceof Float) statement.setFloat(paramPosition, (Float) value);
        else if (value instanceof SWPlayer) statement.setString(paramPosition, ((SWPlayer) value).getUuid().toString());
        else statement.setString(paramPosition, value.toString());
    }

    /**
     * Does nothing by default
     */
    @Override
    public void setProperty(String property, Object value, DataType data) {
        try (Connection conn = storage.getConnection()) {
            PreparedStatement ps = conn.prepareStatement("UPDATE `" + table + "` SET ?=? WHERE `uuid`=?");

            ps.setString(1, property);
            bindPropertyValue(ps, 2, value);
            bindPropertyValue(ps, 3, data);

            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
