package ru.tecon.dNet.sBean;

import ru.tecon.dNet.report.model.ConsumerModel;
import ru.tecon.dNet.report.model.DataModel;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Stateless(name = "report")
@Local(ReportBeanLocal.class)
public class ReportBean implements ReportBeanLocal {

    private static final Logger LOG = Logger.getLogger(ReportBean.class.getName());

    private static final String ALTER = "alter session set nls_numeric_characters = '.,'";

    private static final String SELECT_CTP = "select obj_name from obj_object where obj_id = ?";
    private static final String SELECT_CONNECT_SCHEMA = "select val from obj_object_properties " +
            "where obj_type_id = 1 and prop_id = 20 and obj_id = ?";
    private static final String SELECT_FILIAL = "select get_obj_filial(?) from dual";
    private static final String SELECT_COMPANY = "select get_obj_pred(?) from dual";
    private static final String SELECT_SOURCE = "select val from obj_object_properties " +
            "where obj_type_id = 1 and prop_id = 14 and obj_id = ?";

    private static final String SELECT_CONSUMERS = "select distinct(obj_id2) as obj_id, " +
            "(select obj_name from obj_object where obj_id = obj_id2) as obj_name " +
            "from (select x.dev_agr_type, x.obj_id1, x.dev_agr_id2, x.obj_id2 " +
            "from dev_agr_link x, obj_object xx, dev_agr xxx " +
            "where xx.obj_id = x.obj_id2 and x.obj_id1 = ? and x.dev_agr_type = 514 " +
            "and x.dev_agr_id2 = xxx.agr_id) " +
            "order by obj_name";

    private static final String SELECT_IN_PARAMETERS = "select * from table (mnemo.get_Rnet_CTP_otch_data(?, to_date(?, 'dd.mm.yyyy')))";
    private static final String SELECT_OUT_PARAMETERS ="select * from table (mnemo.get_Rnet_CTP_out_otch_data(?, to_date(?,'dd.mm.yyyy')))";

    private static final String SELECT_VALUE = "select par_value from dz_hist_day_data " +
            "where obj_id = ? and par_id = ? and stat_aggr = ? and time_stamp = to_date(?, 'dd.mm.yyyy')";

    @Resource(name = "jdbc/DataSource")
    private DataSource ds;

    @Override
    public String getCTP(int object) {
        return loadData(SELECT_CTP, object);
    }

    @Override
    public String getConnectSchema(int object) {
        return loadData(SELECT_CONNECT_SCHEMA, object);
    }

    @Override
    public String getFilial(int object) {
        return loadData(SELECT_FILIAL, object);
    }

    @Override
    public String getCompany(int object) {
        return loadData(SELECT_COMPANY, object);
    }

    @Override
    public String getSource(int object) {
        return loadData(SELECT_SOURCE, object);
    }

    @Override
    public List<ConsumerModel> getObjectNames(int object) {
        List<ConsumerModel> result = new ArrayList<>();
        try (Connection connection = ds.getConnection();
             PreparedStatement stm = connection.prepareStatement(SELECT_CONSUMERS)) {
            stm.setInt(1, object);
            ResultSet res = stm.executeQuery();

            while (res.next()) {
                result.add(new ConsumerModel(res.getString(2), res.getInt(1)));
            }
        } catch (SQLException e) {
            LOG.warning("error load consumers for object: " + object);
        }
        return result;
    }

    @Override
    public List<DataModel> getInParameters(int object, String date) {
        return loadParameters(object, date, SELECT_IN_PARAMETERS);
    }

    @Override
    public List<DataModel> getOutParameters(int object, String date) {
        return loadParameters(object, date, SELECT_OUT_PARAMETERS);
    }

    @Override
    public String getValue(int object, int id, int statId, String date) {
        try (Connection connection = ds.getConnection();
             PreparedStatement stm = connection.prepareStatement(SELECT_VALUE)) {
            stm.setInt(1, object);
            stm.setInt(2, id);
            stm.setInt(3, statId);
            stm.setString(4, date);
            ResultSet res = stm.executeQuery();

            if (res.next()) {
                try {
                    return new BigDecimal(res.getString(1).trim()).setScale(2, RoundingMode.HALF_EVEN).toString();
                } catch (Exception ignore) {
                }
            }
        } catch (SQLException e) {
            LOG.warning("error load data for object: " + object);
        }
        return "";
    }

    private List<DataModel> loadParameters(int object, String date, String select) {
        List<DataModel> result = new ArrayList<>();
        try (Connection connection = ds.getConnection();
             PreparedStatement stmAlter = connection.prepareStatement(ALTER);
             PreparedStatement stm = connection.prepareStatement(select)) {
            stmAlter.executeQuery();

            stm.setInt(1, object);
            stm.setString(2, date);
            ResultSet res = stm.executeQuery();

            while (res.next()) {
                result.add(new DataModel(res.getString(1), res.getString(4), res.getInt(2), res.getInt(3)));
            }
        } catch (SQLException e) {
            LOG.warning("error load data select: " + select + " for object: " + object + " date: " + date);
        }
        return result;
    }

    private String loadData(String select, int object) {
        try (Connection connection = ds.getConnection();
             PreparedStatement stm = connection.prepareStatement(select)) {
            stm.setInt(1, object);
            ResultSet res = stm.executeQuery();

            if (res.next()) {
                return res.getString(1);
            }
        } catch (SQLException e) {
            LOG.warning("error load data select: " + select + " for object: " + object);
        }
        return "";
    }
}
