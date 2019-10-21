package ru.tecon.dNet.sBean;

import ru.tecon.dNet.exception.GraphLoadException;
import ru.tecon.dNet.model.Connector;
import ru.tecon.dNet.model.ConnectorValue;
import ru.tecon.dNet.model.GraphElement;
import ru.tecon.dNet.model.Problem;

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
            "nvl2(n4, n3||'='||n4, null) as direct_left, n5 as direct_left_color, " +
            "nvl2(n10, n9||'='||n10, null) as direct_center, n11 as direct_center_color, " +
            "nvl2(n16, n15||'='||n16, null) as direct_right, n17 as direct_right_color, " +
            "nvl2(n7, n6||'='||n7, null) as reverse_left, n8 as reverse_left_color, " +
            "nvl2(n13, n12||'='||n13, null) as reverse_center, n14 as reverse_center_Color, " +
            "nvl2(n19, n18||'='||n19, null) as reverse_right, n20 as reverse_right_color, " +
            "nvl2(n22, n21||'='||n22, null) as q, " +
            "nvl2(n25, n24||'='||n25, null) as temperature, n26 as temperature_color, " +
            "nvl2(n28, n27||'='||n28, null) as k0, n29 as k0_color, " +
            "nvl2(n31, n30||'='||n31, null) as k1, n32 as k1_color, " +
            "nvl2(n34, n33||'='||n34, null) as k2, n35 as k2_color, " +
            "nvl2(n37, n36||'='||n37, null) as k3, n38 as k3_color, " +
            "nvl2(n40, n39||'='||n40, null) as k4, n41 as k4_color " +
            "from table (mnemo.get_Rnet_CTP_hist_data(?, to_date(?, 'dd-mm-yyyy')))";
    private static final String SQL_CONNECTIONS = "select nvl2(n21, n1||' '||n20||'='||n21, n1) as name, " +
            "nvl2(n3, n2||'='||n3, null) as direct_left, n4 as direct_left_color, " +
            "nvl2(n9, n8||'='||n9, null) as direct_center, n10 as direct_center_color, " +
            "nvl2(n15, n14||'='||n15, null) as direct_right, n16 as direct_right_color, " +
            "nvl2(n6, n5||'='||n6, null) as reverse_left, n7 as reverse_left_color, " +
            "nvl2(n12, n11||'='||n12, null) as reverse_center, n13 as reverse_center_Color, " +
            "nvl2(n18, n17||'='||n18, null) as reverse_right, n19 as reverse_right_color," +
            "nvl2(n24, n23||'='||n24, null) as k0, n25 as k0_color, " +
            "nvl2(n27, n26||'='||n27, null) as k1, n28 as k1_color, " +
            "nvl2(n30, n29||'='||n30, null) as k2, n31 as k2_color, " +
            "n21 as energy " +
            "from table (mnemo.get_Rnet_UU_hist_data(?, ?, to_date(?, 'dd-mm-yyyy')))";
    private static final String SQL_REDIRECT = "select mnemo_ip, mnemo_port from dz_sys_param";
    private static final String SQL_CHECK_SUMMER = "select decode(season, 'LETO', '1', '0') " +
            "from (select season from sys_season_log " +
            "where updated_when < to_date(?, 'dd-mm-yyyy') " +
            "order by updated_when desc) " +
            "where rownum = 1";
    private static final String SQL_PROBLEM_IDS = "select trim(case when p1 = 1 then '1' end || " +
            "case when p2 = 1 then ' 2' end || " +
            "case when p3 = 1 then ' 3' end || " +
            "case when p4 = 1 then ' 4' end || " +
            "case when p5 = 1 then ' 5' end || " +
            "case when p6 = 1 then ' 6' end || " +
            "case when p7 = 1 then ' 7' end || " +
            "case when p8 = 1 then ' 8' end || " +
            "case when p9 = 1 then ' 9' end || " +
            "case when p10 = 1 then ' 10' end || " +
            "case when p11 = 1 then ' 11' end || " +
            "case when p12 = 1 then ' 12' end || " +
            "case when p13 = 1 then ' 13' end || " +
            "case when p14 = 1 then ' 14' end || " +
            "case when p15 = 1 then ' 15' end || " +
            "case when p16 = 1 then ' 16' end || " +
            "case when p17 = 1 then ' 17' end || " +
            "case when p18 = 1 then ' 18' end || " +
            "case when p19 = 1 then ' 19' end || " +
            "case when p20 = 1 then ' 20' end || " +
            "case when p21 = 1 then ' 21' end || " +
            "case when p22 = 1 then ' 22' end || " +
            "case when p23 = 1 then ' 23' end || " +
            "case when p24 = 1 then ' 24' end || " +
            "case when p25 = 1 then ' 25' end || " +
            "case when p26 = 1 then ' 26' end || " +
            "case when p27 = 1 then ' 27' end || " +
            "case when p28 = 1 then ' 28' end || " +
            "case when p29 = 1 then ' 29' end || " +
            "case when p30 = 1 then ' 30' end || " +
            "case when p31 = 1 then ' 31' end) as ids " +
            "from table(dsp_0090t.sel_matrix_ctp_detail(?, to_date(?, 'dd-mm-yyyy')))";
    private static final String SQL_PROBLEMS = "select techproc, problem_name, color from dz_rs_problem where problem_id in (?)";

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

                Connector connector = new Connector(res.getString("q"));
                connector.setTemperature(new ConnectorValue(res.getString("temperature"), res.getString("temperature_color")));
                if (res.getString(3) != null) {
                    connector.getIn()[0] = new ConnectorValue(res.getString(3), res.getString(4));
                }
                if (res.getString(5) != null) {
                    connector.getIn()[1] = new ConnectorValue(res.getString(5), res.getString(6));
                }
                if (res.getString(7) != null) {
                    connector.getIn()[2] = new ConnectorValue(res.getString(7), res.getString(8));
                }
                if (res.getString(9) != null) {
                    connector.getOut()[0] = new ConnectorValue(res.getString(9), res.getString(10));
                }
                if (res.getString(11) != null) {
                    connector.getOut()[1] = new ConnectorValue(res.getString(11), res.getString(12));
                }
                if (res.getString(13) != null) {
                    connector.getOut()[2] = new ConnectorValue(res.getString(13), res.getString(14));
                }
                loadCoefficient(connector, res, 0);
                loadCoefficient(connector, res, 1);
                loadCoefficient(connector, res, 2);
                loadCoefficient(connector, res, 3);
                loadCoefficient(connector, res, 4);
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
     * Метод для получения коэффициентов
     * @param connector элемент в который кладем значения
     * @param res результаты выполнения запроса
     * @param index индекс коэффициента
     * @throws SQLException если есть ошибка при работе с бд
     */
    private void loadCoefficient(Connector connector, ResultSet res, int index) throws SQLException {
        if (res.getString("k" + index) != null) {
            connector.getCenter()[index] = new ConnectorValue(res.getString("k" + index), res.getString("k" + index + "_color"));
        }
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
                producer = new GraphElement(objectId, res.getString(1), date, 25);
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
                producer.addChildren(new GraphElement(res.getInt(1), res.getString(2), 34));
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
            connector.setEnergy(res.getString("energy"));
            if (res.getString(2) != null) {
                connector.getIn()[0] = new ConnectorValue(res.getString(2), res.getString(3));
            }
            if (res.getString(4) != null) {
                connector.getIn()[1] = new ConnectorValue(res.getString(4), res.getString(5));
            }
            if (res.getString(6) != null) {
                connector.getIn()[2] = new ConnectorValue(res.getString(6), res.getString(7));
            }
            if (res.getString(8) != null) {
                connector.getOut()[0] = new ConnectorValue(res.getString(8), res.getString(9));
            }
            if (res.getString(10) != null) {
                connector.getOut()[1] = new ConnectorValue(res.getString(10), res.getString(11));
            }
            if (res.getString(12) != null) {
                connector.getOut()[2] = new ConnectorValue(res.getString(12), res.getString(13));
            }
            loadCoefficient(connector, res, 0);
            loadCoefficient(connector, res, 1);
            loadCoefficient(connector, res, 2);
            producer.addConnect(connector);
        }
    }

    /**
     * Загрузка списка проблем возникшик на мнемосхеме
     * @param problems map в которой хранятся проблемы
     * @param id id объекта
     * @param date дата по которой смотрим проблемы (dd-mm-yyyy)
     */
    public void getProblems(Map<String, List<Problem>> problems, int id, String date) {
        try (Connection connect = ds.getConnection();
                PreparedStatement stm = connect.prepareStatement(SQL_PROBLEM_IDS)) {
            stm.setInt(1, id);
            stm.setString(2, date);

            List<String> ids = null;

            ResultSet res = stm.executeQuery();
            if (res.next() && res.getString(1) != null) {
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
                            problems.get(res.getString(1)).add(new Problem(res.getString(2), res.getString(3)));
                        } else {
                            problems.put(res.getString(1), new ArrayList<>(Collections.singletonList(
                                    new Problem(res.getString(2), res.getString(3)))));
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

    /**
     * Метод возвращает url для перехода на мнемосхему объектов
     * при нажатии на объект
     * @param object id объекта
     * @return url аддрес
     */
    public String getRedirectUrl(String object) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_REDIRECT)) {
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return "http://" + res.getString(1) + ":" + res.getString(2) + "/mnemo/?objectId=" + object;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
