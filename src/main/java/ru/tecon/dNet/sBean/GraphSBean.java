package ru.tecon.dNet.sBean;

import ru.tecon.dNet.exception.GraphLoadException;
import ru.tecon.dNet.model.Connector;
import ru.tecon.dNet.model.GraphElement;

import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Startup
@Stateless
public class GraphSBean {

    private static final String SQL_CONSUMERS = "select obj_id2 as obj_id, " +
            "(select obj_name from obj_object where obj_id = obj_id2) as obj_name " +
            "from obj_rel where obj_rel_type = 321 and obj_id1 = ? order by obj_name";
    private static final String SQL_PRODUCER = "select obj_name from obj_object where obj_id = ?";
    private static final String SQL_INIT_PARAMS = "select n1 as time, n2 as tech_proc, " +
            "n3||'='||n4||', '||n9||'='||n10||', '||n15||'='||n16 as direct, " +
            "n6||'='||n7||', '||n12||'='||n13||', '||n18||'='||n19 as reverse, " +
            "n21||'='||n22 as q from table (mnemo.get_Rnet_CTP_hist_data(?))";
    private static final String SQL_CONNECTIONS = "select n1||' '||n20||'='||n21 as name, " +
            "n2||'='||n3||', '||n8||'='||n9||', '||n14||'='||n15 as direct, " +
            "n5||'='||n6||', '||n11||'='||n12||', '||n17||'='||n18 as reverse " +
            "from table (mnemo.get_Rnet_UU_hist_data(?, to_date(?, 'dd-mm-yyyy hh24')))";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    public GraphElement loadGraph(int objectId, String date) throws GraphLoadException {
        GraphElement producer = null;
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_PRODUCER)) {
            stm.setInt(1, objectId);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                producer = new GraphElement(objectId, res.getString(1), date);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (producer != null) {
            loadConsumers(producer);
        }
        return producer;
    }

    public GraphElement loadInitData(int objectId) throws GraphLoadException {
        GraphElement init = null;
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_INIT_PARAMS)) {
            stm.setInt(1, objectId);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                init = new GraphElement(0, null, res.getString(1));
                init.addConnect(new Connector(res.getString(5), res.getString(3), res.getString(4)));

                if (init.getDate() == null) {
                    throw new GraphLoadException("Источник ни разу не выходил на связь!");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return init;
    }

    private void loadConsumers(GraphElement producer) throws GraphLoadException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_CONSUMERS)) {
            stm.setInt(1, producer.getObjectId());
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                producer.addChildren(new GraphElement(res.getInt(1), res.getString(2)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (producer.getChildren() == null) {
            throw new GraphLoadException("У источника нету потребителей!");
        }

        loadConnections(producer);
    }

    private void loadConnections(GraphElement producer) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_CONNECTIONS)) {
            doConnections(stm, producer, producer.getDate());

            for (GraphElement el: producer.getChildren()) {
                doConnections(stm, el, producer.getDate());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void doConnections(PreparedStatement stm, GraphElement producer, String date) throws SQLException {
        stm.setInt(1, producer.getObjectId());
        stm.setString(2, date);
        ResultSet res = stm.executeQuery();

        while (res.next()) {
            producer.addConnect(new Connector(res.getString(1), res.getString(2), res.getString(3)));
        }
    }
}
