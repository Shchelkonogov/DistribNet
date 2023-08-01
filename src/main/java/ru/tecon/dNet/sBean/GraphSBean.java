package ru.tecon.dNet.sBean;

import ru.tecon.dNet.exception.GraphLoadException;
import ru.tecon.dNet.model.Connector;
import ru.tecon.dNet.model.ConnectorValue;
import ru.tecon.dNet.model.GraphElement;
import ru.tecon.dNet.model.Problem;

import javax.annotation.Resource;
import javax.ejb.LocalBean;
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
@Stateless
@LocalBean
public class GraphSBean {

    private final static Logger LOG = Logger.getLogger(GraphSBean.class.getName());

    private static final String SQL_CONSUMERS = "select distinct constable.obj_id2 as obj_id, " +
            "(select obj_name from admin.obj_object where obj_id = constable.obj_id2) as obj_name " +
            "from (select x.dev_agr_type, x.obj_id1, x.dev_agr_id2, x.obj_id2 " +
            "from admin.dev_agr_link x, admin.obj_object xx, admin.dev_agr xxx " +
            "where xx.obj_id = x.obj_id2 and x.obj_id1 = ? " +
            "and x.dev_agr_id2 = xxx.agr_id) constable " +
            "order by obj_name";
    private static final String SQL_PRODUCER = "select obj_name || 'title=' || (select admin.GET_OBJ_ADDRESS(?)) " +
            "from admin.obj_object where obj_id = ?";
    private static final String SQL_INIT_PARAMS = "select n1 as time, n2 as tech_proc, " +
            "case WHEN n4 IS null THEN NULL WHEN n4 = '' THEN NULL ELSE n3 ||'=' ||n4 END direct_left, n5 as direct_left_color, " +
            "case WHEN n10 IS null THEN NULL WHEN n10 = '' THEN NULL ELSE n9 ||'=' ||n10 END direct_center, n11 as direct_center_color, " +
            "case WHEN n16 IS null THEN NULL WHEN n16 = '' THEN NULL ELSE n15 ||'=' ||n16 END direct_right, n17 as direct_right_color, " +
            "case WHEN n7 IS null THEN NULL WHEN n7 = '' THEN NULL ELSE n6 ||'=' ||n7 END reverse_left, n8 as reverse_left_color, " +
            "case WHEN n13 IS null THEN NULL WHEN n13 = '' THEN NULL ELSE n12 ||'=' ||n13 END reverse_center, n14 as reverse_center_Color, " +
            "case WHEN n19 IS null THEN NULL WHEN n19 = '' THEN NULL ELSE n18 ||'=' ||n19 END reverse_right, n20 as reverse_right_color, " +
            "case WHEN n22 IS null THEN NULL WHEN n22 = '' THEN NULL ELSE n21 ||'=' ||n22 END q, " +
            "case WHEN n44 IS null THEN NULL WHEN n44 = '' THEN NULL ELSE n43 ||'=' ||n44 END t, " +
            "case WHEN n25 IS null THEN NULL WHEN n25 = '' THEN NULL ELSE n24 ||'=' ||n25 END temperature, n26 as temperature_color, " +
            "case WHEN n28 IS null THEN NULL WHEN n28 = '' THEN NULL ELSE n27 ||'=' ||n28 END k0, n29 as k0_color, " +
            "case WHEN n31 IS null THEN NULL WHEN n31 = '' THEN NULL ELSE n30 ||'=' ||n31 END k1, n32 as k1_color, " +
            "case WHEN n34 IS null THEN NULL WHEN n34 = '' THEN NULL ELSE n33 ||'=' ||n34 END k2, n35 as k2_color, " +
            "case WHEN n37 IS null THEN NULL WHEN n37 = '' THEN NULL ELSE n36 ||'=' ||n37 END k3, n38 as k3_color, " +
            "case WHEN n40 IS null THEN NULL WHEN n40 = '' THEN NULL ELSE n39 ||'=' ||n40 END k4, n41 as k4_color " +
            "from mnemo.get_rnet_ctp_hist_data(?, to_date(?, 'dd-mm-yyyy'))";
    private static final String SQL_CONNECTIONS = "select case WHEN n21 IS null THEN n1 WHEN n21 = '' THEN n1 ELSE n1||' '||n20||'='||n21 END name, " +
            "case WHEN n3 IS null THEN NULL WHEN n3 = '' THEN NULL ELSE n2||'='||n3 END direct_left, n4 as direct_left_color, " +
            "case WHEN n9 IS null THEN NULL WHEN n9 = '' THEN NULL ELSE n8||'='||n9 END direct_center, n10 as direct_center_color, " +
            "case WHEN n15 IS null THEN NULL WHEN n15 = '' THEN NULL ELSE n14||'='||n15 END direct_right, n16 as direct_right_color, " +
            "case WHEN n6 IS null THEN NULL WHEN n6 = '' THEN NULL ELSE n5||'='||n6 END reverse_left, n7 as reverse_left_color, " +
            "case WHEN n12 IS null THEN NULL WHEN n12 = '' THEN NULL ELSE n11||'='||n12 END reverse_center, n13 as reverse_center_Color, " +
            "case WHEN n18 IS null THEN NULL WHEN n18 = '' THEN NULL ELSE n17||'='||n18 END reverse_right, n19 as reverse_right_color," +
            "case WHEN n24 IS null THEN NULL WHEN n24 = '' THEN NULL ELSE n23||'='||n24 END k0, n25 as k0_color, " +
            "case WHEN n27 IS null THEN NULL WHEN n27 = '' THEN NULL ELSE n26||'='||n27 END k1, n28 as k1_color, " +
            "case WHEN n30 IS null THEN NULL WHEN n30 = '' THEN NULL ELSE n29||'='||n30 END k2, n31 as k2_color, " +
            "n21 as energy, n42 as connectionAggregateId " +
            "from mnemo.get_rnet_uu_hist_data(?, ?, to_date(?, 'dd-mm-yyyy'))";
    private static final String SQL_REDIRECT = "select mnemo_ip, mnemo_port from admin.dz_sys_param";
    private static final String SELECT_REDIRECT_TD_URL = "select td_url from admin.dz_sys_param";
    private static final String SQL_CHECK_SUMMER = "select case WHEN season = 'LETO'::varchar THEN true ELSE false END CHECK_SUMMER " +
            "from (select season from admin.sys_season_log " +
            "where updated_when < to_date(?, 'dd-mm-yyyy') " +
            "order by updated_when desc) cstable " +
            "limit 1";
    private static final String SQL_PROBLEM_IDS = "select problem_id from dsp_0090t.sel_obj_problem_d(?, to_date(?, 'dd-mm-yyyy'))";
    private static final String SQL_PROBLEMS = "select techproc, main_problem_name, color, visible, a.main_problem_id from dz_rs_problem a, dz_rs_main_problem b " +
            "where b.main_problem_id in (?) and a.main_problem_id = b.main_problem_id";
    private static final String SQL_PROBLEMS_DESCRIPTION = "select mnemo.get_rnet_problem_data(?, ?, to_date(?, 'dd-mm-yyyy'))";
    private static final String BALANCE_AFFILIATION = "select val from admin.obj_type_prop_val_vie where obj_prop_name='На балансе' and obj_id= " +
            "(select obj_id from admin.obj_object where obj_name=?) limit 1";
    private static final String HOUSE_TYPE = "select val from admin.obj_type_prop_val_vie where obj_prop_name='Категория строения' and obj_id= " +
            "(select obj_id from admin.obj_object where obj_name=?) limit 1";

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
             PreparedStatement stm = connect.prepareStatement(SQL_INIT_PARAMS)) {

            stm.setInt(1, objectId);
            stm.setString(2, date);
            ResultSet res = stm.executeQuery();

            if (res.next()) {
                init = new GraphElement(0, null, date);

                Connector connector = new Connector((
                        (Objects.nonNull(res.getString("q")) ? res.getString("q") : "") +
                                " " +
                                (Objects.nonNull(res.getString("t")) ? res.getString("t") : "")
                ).trim());
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
             PreparedStatement stm = connect.prepareStatement(SQL_CONNECTIONS)) {
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
            connector.setConnectionAggregateId(res.getInt("connectionAggregateId"));
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
    public void getProblems(Map<String, Set<Problem>> problems, int id, String date) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_PROBLEM_IDS)) {
            stm.setInt(1, id);
            stm.setString(2, date);

            List<String> ids = new ArrayList<>();

            ResultSet res = stm.executeQuery();
            while (res.next()) {
                ids.add(res.getString(1));
            }

            if (!ids.isEmpty()) {
                StringBuilder builder = new StringBuilder();

                ids.forEach(e -> builder.append("?,"));

                String sql = SQL_PROBLEMS.replace("?", builder.deleteCharAt(builder.length() - 1));

                try (PreparedStatement stmProblems = connect.prepareStatement(sql)) {
                    for (int i = 0; i < ids.size(); i++) {
                        stmProblems.setString(i + 1, ids.get(i));
                    }

                    res = stmProblems.executeQuery();
                    while (res.next()) {

                        // Сказали зашиться на желтый цвет
//                        Problem problem = new Problem(res.getString(2), res.getString(3), res.getBoolean(4), res.getInt(5));
                        Problem problem = new Problem(res.getString(2), "yellow", res.getBoolean(4), res.getInt(5));

                        if (problems.containsKey(res.getString(1))) {
                            problems.get(res.getString(1)).add(problem);
                        } else {
                            problems.put(res.getString(1), new HashSet<>(Collections.singleton(problem)));
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
     * Метод возвращает описание проблем для каждого потребителя
     * @param problemId id проблемы
     * @param ids id потребителей
     * @param date дата в формате dd-MM-yyyy за которую смотрятся данные
     * @param result результат выполнения метода
     */
    public void getProblemDescription(Integer problemId, List<Integer> ids, String date, Map<Integer, String> result) {
        LOG.info("getProblemDescription: start");
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SQL_PROBLEMS_DESCRIPTION)) {
            ResultSet res;

            for (Integer id: ids) {
                stm.setInt(1, id);
                stm.setInt(2, problemId);
                stm.setString(3, date);

                res = stm.executeQuery();
                if (res.next() && res.getString(1) != null) {
                    result.put(id, res.getString(1));
                }
            }
        } catch (SQLException e) {
            LOG.warning("getProblemDescription: error " + e.getMessage());
        }
        LOG.info("getProblemDescription: end");
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

    /**
     * Метод возвращает url для перехода систему ТД
     * @return url адрес
     */
    public String getRedirectUrlTD() {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(SELECT_REDIRECT_TD_URL)) {
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * Метод возвращает балансовую принадлежность ЦТП
     * @return значение балансовой принадлежности
     */
    public String getBalanceAffiliation(String objName) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(BALANCE_AFFILIATION)) {
            stm.setString(1, objName);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Метод возвращает тип дома
     * @return тип дома
     */
    public String getHouseType(String objName) {
        try (Connection connect = ds.getConnection();
             PreparedStatement stm = connect.prepareStatement(HOUSE_TYPE)) {
            stm.setString(1, objName);
            ResultSet res = stm.executeQuery();
            if (res.next()) {
                return res.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
