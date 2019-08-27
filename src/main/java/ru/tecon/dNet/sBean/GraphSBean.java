package ru.tecon.dNet.sBean;

import ru.tecon.dNet.exception.GraphLoadException;
import ru.tecon.dNet.model.Connector;
import ru.tecon.dNet.model.ConnectorValue;
import ru.tecon.dNet.model.GraphElement;

import javax.annotation.Resource;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Stateless bean для загрузки информации для построения мнемосхемы
 */
@Startup
@Stateless
public class GraphSBean {

    private final static Logger LOG = Logger.getLogger(GraphSBean.class.getName());

    private static final String SQL_ALTER = "alter session set NLS_NUMERIC_CHARACTERS = '.,'";

    private static final String SQL_CONSUMERS = "select distinct(obj_id2) as obj_id, " +
            "(select obj_name from obj_object where obj_id = obj_id2) as obj_name " +
            "from (select x.dev_agr_type, x.obj_id1, x.dev_agr_id2, x.obj_id2 " +
            "from dev_agr_link x, obj_object xx, dev_agr xxx " +
            "where xx.obj_id = x.obj_id2 and x.obj_id1 = ? and x.dev_agr_type = 514 " +
            "and x.dev_agr_id2 = xxx.agr_id) " +
            "order by obj_name";
    private static final String SQL_PRODUCER = "select obj_name || 'title=' || (select GET_OBJ_ADDRESS(?) from dual) " +
            "from obj_object where obj_id = ?";
    private static final String SQL_INIT_PARAMS = "select n1 as time, n2 as tech_proc, " +
            "n3||'='||n4 as direct_left, n5 as direct_left_color, " +
            "n9||'='||n10 as direct_center, n11 as direct_center_color, " +
            "n15||'='||n16 as direct_right, n17 as direct_right_color, " +
            "n6||'='||n7 as reverse_left, n8 as reverse_left_color, " +
            "n12||'='||n13 as reverse_center, n14 as reverse_center_Color, " +
            "n18||'='||n19 as reverse_right, n20 as reverse_right_color, " +
            "n21||'='||n22||' '||n24||'='||n25||' '||n26 as q, " +
            "n27||'='||n28||' '||n30||'='||n31||' '||n33||'='||n34||' '||n36||'='||n37||' '||n39||'='||n40 as k, " +
            "n29||' '||n32||' '||n35||' '||n38||' '||n41 as k_color from table (mnemo.get_Rnet_CTP_hist_data(?, to_date(?, 'dd-mm-yyyy')))";
    private static final String SQL_CONNECTIONS = "select n1||' '||n20||'='||n21 as name, " +
            "n2||'='||n3 as direct_left, n4 as direct_left_color, " +
            "n8||'='||n9 as direct_center, n10 as direct_center_color, " +
            "n14||'='||n15 as direct_right, n16 as direct_right_color, " +
            "n5||'='||n6 as reverse_left, n7 as reverse_left_color, " +
            "n11||'='||n12 as reverse_center, n13 as reverse_center_Color, " +
            "n17||'='||n18 as reverse_right, n19 as reverse_right_color," +
            "nvl2(n23, n23||'='||n24, null) as k0, n25 as k0_color, " +
            "nvl2(n26, n26||'='||n27, null) as k1, n28 as k1_color, " +
            "nvl2(n29, n29||'='||n30, null) as k2, n31 as k2_color " +
            "from table (mnemo.get_Rnet_UU_hist_data(?, ?, to_date(?, 'dd-mm-yyyy')))";
    public static final String SQL_CHECK_SUMMER = "select decode(season, 'LETO', '1', '0') " +
            "from (select season from sys_season_log " +
            "where updated_when < to_date(?, 'dd-mm-yyyy') " +
            "order by updated_when desc) " +
            "where rownum = 1";
    private static final String SQL_PROBLEM_IDS = "select trim(case when p1 = 0 then '1' end || " +
            "case when p2 = 0 then ' 2' end || " +
            "case when p3 = 0 then ' 3' end || " +
            "case when p4 = 0 then ' 4' end || " +
            "case when p5 = 0 then ' 5' end || " +
            "case when p6 = 0 then ' 6' end || " +
            "case when p7 = 0 then ' 7' end || " +
            "case when p8 = 0 then ' 8' end || " +
            "case when p9 = 0 then ' 9' end || " +
            "case when p10 = 0 then ' 10' end || " +
            "case when p11 = 0 then ' 11' end || " +
            "case when p12 = 0 then ' 12' end || " +
            "case when p13 = 0 then ' 13' end || " +
            "case when p14 = 0 then ' 14' end || " +
            "case when p15 = 0 then ' 15' end || " +
            "case when p16 = 0 then ' 16' end || " +
            "case when p17 = 0 then ' 17' end || " +
            "case when p18 = 0 then ' 18' end || " +
            "case when p19 = 0 then ' 19' end || " +
            "case when p20 = 0 then ' 20' end || " +
            "case when p21 = 0 then ' 21' end || " +
            "case when p22 = 0 then ' 22' end || " +
            "case when p23 = 0 then ' 23' end) as ids " +
            "from table(dsp_0090t.sel_matrix_ctp_detail(?, to_date(?, 'dd-mm-yyyy')))";
    private static final String SQL_PROBLEMS = "select techproc, problem_name from dz_rs_problem where problem_id in (?)";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    /**
     * Метод загружает начальные данные для мнемосхемы (входные данные в источник)
     * @param objectId id объекта
     * @return данные мнемосхемы
     * @throws GraphLoadException если запросы вернули не корректные данные
     */
    public GraphElement loadInitData(int objectId, String date) throws GraphLoadException {
        LOG.info("loadInitData start");
        GraphElement init = null;
        try (Connection connect = ds.getConnection();
             PreparedStatement stmAlter = connect.prepareStatement(SQL_ALTER);
             PreparedStatement stm = connect.prepareStatement(SQL_INIT_PARAMS)) {
            stmAlter.executeQuery();
            stm.setInt(1, objectId);
            stm.setString(2, date);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                init = new GraphElement(0, null, date);

                Connector connector = new Connector(res.getString(15));
                connector.getIn()[0] = new ConnectorValue(res.getString(3), res.getString(4));
                connector.getIn()[1] = new ConnectorValue(res.getString(5), res.getString(6));
                connector.getIn()[2] = new ConnectorValue(res.getString(7), res.getString(8));
                connector.getOut()[0] = new ConnectorValue(res.getString(9), res.getString(10));
                connector.getOut()[1] = new ConnectorValue(res.getString(11), res.getString(12));
                connector.getOut()[2] = new ConnectorValue(res.getString(13), res.getString(14));
                connector.getCenter()[0] = new ConnectorValue(res.getString("k"), res.getString("k_color"));
                init.addConnect(connector);

                if (init.getDate() == null) {
                    LOG.warning("loadInitData Источник ни разу не выходил на связь!");
                    throw new GraphLoadException("Источник ни разу не выходил на связь!");
                }
            }
        } catch (SQLException e) {
            LOG.warning("loadInitData "+ e.getMessage());
        }

        LOG.info("loadInitData end");
        return init;
    }

    /**
     * Метода загружает основные данные мнемосхемы
     * @param objectId id объекта
     * @param date дата за которую грузить данные
     * @return данные мнемосхемы
     * @throws GraphLoadException если запросы вернули не корректные данные
     */
    public GraphElement loadGraph(int objectId, String date) throws GraphLoadException {
        LOG.info("loadGraph start");
        GraphElement producer = null;
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_PRODUCER)) {
            stm.setInt(1, objectId);
            stm.setInt(2, objectId);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                producer = new GraphElement(objectId, res.getString(1), date);
            }
        } catch (SQLException e) {
            LOG.warning("loadGraph " + e.getMessage());
        }

        // Загрузка данных присоединенных потребителей
        if (producer != null) {
            loadConsumers(producer);
        }

        LOG.info("loadGraph end");
        return producer;
    }

    /**
     * Метод который позволяет определить какой режим в системе
     * (лето или зима)
     * @param date дата
     * @return true если лето, false если зима
     * @throws GraphLoadException если произошла ошибка в sql
     */
    public boolean checkSummer(String date) throws GraphLoadException {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_CHECK_SUMMER)) {
            stm.setString(1, date);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getBoolean(1);
            }
        } catch (SQLException e) {
            LOG.warning("checkSummer " + e.getMessage());
            throw new GraphLoadException("Ошибка сервера");
        }
        return false;
    }

    /**
     * Загрузка данных по потребителям
     * @param producer данные мнемосхемы
     * @throws GraphLoadException если запросы вернули не корректные данные
     */
    private void loadConsumers(GraphElement producer) throws GraphLoadException {
        LOG.info("loadConsumers start");
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_CONSUMERS)) {
            stm.setInt(1, producer.getObjectId());
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                producer.addChildren(new GraphElement(res.getInt(1), res.getString(2)));
            }
        } catch (SQLException e) {
            LOG.warning("loadConsumers " + e.getMessage());
        }

        if (producer.getChildren() == null) {
            LOG.warning("loadConsumers У источника нету потребителей!");
            throw new GraphLoadException("У источника нету потребителей!");
        }

        // Загрузка данных по связам для каждого объекта мнемосхемы
        loadConnections(producer);

        LOG.info("loadConsumers end");
    }

    /**
     * Загрузка данных по связям для каждого элемента мнемосхемы
     * @param producer данные мнемосхемы
     */
    private void loadConnections(GraphElement producer) throws GraphLoadException {
        LOG.info("loadConnections start");
        try (Connection connect = ds.getConnection();
             PreparedStatement stmAlter = connect.prepareStatement(SQL_ALTER);
             PreparedStatement stm = connect.prepareStatement(SQL_CONNECTIONS)) {
            stmAlter.executeQuery();
            doConnections(stm, producer, producer.getDate(), producer.getObjectId());

            for (GraphElement el: producer.getChildren()) {
                doConnections(stm, el, producer.getDate(), producer.getObjectId());
            }

            if (producer.getConnectors().size() == 0) {
                LOG.warning("loadConnections: Источник не слинкован!");
                throw new GraphLoadException("Источник не слинкован!");
            }
            producer.getChildren().removeIf(consumer -> consumer.getConnectors().size() == 0);
            if (producer.getChildren().size() == 0) {
                LOG.warning("loadConnections: Потребители источника не слинкованы!");
                throw new GraphLoadException("Потребители источника не слинкованы!");
            }
        } catch (SQLException e) {
            LOG.warning("loadConnections " + e.getMessage());
        }
        LOG.info("loadConnections end");
    }

    private void doConnections(PreparedStatement stm, GraphElement producer, String date, int id) throws SQLException {
        stm.setInt(1, id);
        stm.setInt(2, producer.getObjectId());
        stm.setString(3, date);
        ResultSet res = stm.executeQuery();

        while (res.next()) {
            Connector connector = new Connector(res.getString(1));
            connector.getIn()[0] = new ConnectorValue(res.getString(2), res.getString(3));
            connector.getIn()[1] = new ConnectorValue(res.getString(4), res.getString(5));
            connector.getIn()[2] = new ConnectorValue(res.getString(6), res.getString(7));
            connector.getOut()[0] = new ConnectorValue(res.getString(8), res.getString(9));
            connector.getOut()[1] = new ConnectorValue(res.getString(10), res.getString(11));
            connector.getOut()[2] = new ConnectorValue(res.getString(12), res.getString(13));
            if (res.getString("k0") != null) {
                connector.getCenter()[0] = new ConnectorValue(res.getString("k0"), res.getString("k0_color"));
            }
            if (res.getString("k1") != null) {
                connector.getCenter()[1] = new ConnectorValue(res.getString("k1"), res.getString("k1_color"));
            }
            if (res.getString("k2") != null) {
                connector.getCenter()[2] = new ConnectorValue(res.getString("k2"), res.getString("k2_color"));
            }
            producer.addConnect(connector);
        }
    }

    public void getProblems(Map<String, List<String>> problems, int id, String date) {
        try (Connection connect = ds.getConnection();
                PreparedStatement stm = connect.prepareStatement(SQL_PROBLEM_IDS)) {
            stm.setInt(1, id);
            stm.setString(2, date);

            List<String> ids = null;

            ResultSet res = stm.executeQuery();
            if (res.next()) {
                ids = new ArrayList<>(Arrays.asList(res.getString(1).split(" ")));
            }

            if (ids != null) {
                StringBuilder builder = new StringBuilder();

                ids.forEach(e -> builder.append("?,"));

                String sql = SQL_PROBLEMS.replace("?", builder.deleteCharAt(builder.length() - 1));

                try (PreparedStatement stmProblems = connect.prepareStatement(sql)) {
                    for (int i = 0; i < ids.size(); i++) {
                        stmProblems.setString(i + 1, ids.get(i));
                    }

                    res = stmProblems.executeQuery();
                    while (res.next()) {
                        if (problems.containsKey(res.getString(1))) {
                            problems.get(res.getString(1)).add(res.getString(2));
                        } else {
                            problems.put(res.getString(1), new ArrayList<>(Collections.singletonList(res.getString(2))));
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
