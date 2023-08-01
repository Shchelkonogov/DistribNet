package ru.tecon.dNet.sBean;

import ru.tecon.dNet.report.model.CellValue;
import ru.tecon.dNet.report.model.ConsumerModel;
import ru.tecon.dNet.report.model.DataModel;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless(name = "report")
@Local(ReportBeanLocal.class)
public class ReportBean implements ReportBeanLocal {

    private static final Logger LOG = Logger.getLogger(ReportBean.class.getName());

    private static final String SELECT_CTP = "select obj_name from admin.obj_object where obj_id = ?";
    private static final String SELECT_CONNECT_SCHEMA = "select val from admin.obj_object_properties " +
            "where obj_type_id = 1 and prop_id = 20 and obj_id = ?";
    private static final String SELECT_FILIAL = "select get_obj_filial(?)";
    private static final String SELECT_COMPANY = "select get_obj_pred(?)";
    private static final String SELECT_SOURCE = "select val from admin.obj_object_properties " +
            "where obj_type_id = 1 and prop_id = 14 and obj_id = ?";
    private static final String SELECT_ADDRESS = "select admin.get_obj_address(?)";

    private static final String SELECT_CONSUMERS = "select distinct constable.obj_id2 as obj_id, " +
            "(select obj_name from admin.obj_object where obj_id = constable.obj_id2) as obj_name " +
            "from (select x.dev_agr_type, x.obj_id1, x.dev_agr_id2, x.obj_id2 " +
            "from admin.dev_agr_link x, admin.obj_object xx, admin.dev_agr xxx " +
            "where xx.obj_id = x.obj_id2 and x.obj_id1 = ? " +
//            "and x.dev_agr_type = 514 " +
            "and x.dev_agr_id2 = xxx.agr_id) constable " +
            "order by obj_name";

    private static final String SELECT_IN_PARAMETERS = "select * from dsp_0045t.get_rnet_ctp_otch_data(?, ?, ?)";
    private static final String SELECT_OUT_PARAMETERS = "select * from dsp_0045t.get_rnet_ctp_out_otch_data(?, ?, ?)";

    private static final String SELECT_VALUE = "call dsp_0045t.get_rnet_uu_otch_data(?, ?, ?, ?, ?, ?, ?)";

    private static final String SELECT_TOTAL_DATA = "select * from dsp_0045t.get_Rnet_CTP_otch_data_itog(?, ?, ?)";

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
    public String getAddress(int objectID) {
        return loadData(SELECT_ADDRESS, objectID);
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
    public List<DataModel> getInParameters(int object, LocalDate startDate, LocalDate endDate) {
        return loadParameters(object, startDate, endDate, SELECT_IN_PARAMETERS);
    }

    @Override
    public List<DataModel> getOutParameters(int object, LocalDate startDate, LocalDate endDate) {
        return loadParameters(object, startDate, endDate, SELECT_OUT_PARAMETERS);
    }

    @Override
    public CellValue getValue(int parentID, int object, int id, int statId, LocalDate startDate, LocalDate endDate) {
        try (Connection connection = ds.getConnection();
             CallableStatement cStm = connection.prepareCall(SELECT_VALUE)) {
            cStm.registerOutParameter(1, Types.VARCHAR);
            cStm.setInt(2, parentID);
            cStm.setInt(3, object);
            cStm.setInt(4, id);
            cStm.setInt(5, statId);
            cStm.setDate(6, Date.valueOf(startDate));
            cStm.setDate(7, Date.valueOf(endDate));
            cStm.registerOutParameter(8, Types.INTEGER);
            cStm.executeUpdate();

            try {
                return new CellValue(new BigDecimal(cStm.getString(1).trim()).setScale(2, RoundingMode.HALF_EVEN).toString(), cStm.getInt(8));
            } catch (Exception ignore) {
                return new CellValue("", 0);
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "error load data for object: " + object, e);
        }
        return null;
    }

    @Override
    public List<String> getTotalData(int objectID, LocalDate startDate, LocalDate endDate) {
        List<String> result = new ArrayList<>();
        try (Connection connection = ds.getConnection();
             PreparedStatement stm = connection.prepareStatement(SELECT_TOTAL_DATA)) {
            stm.setInt(1, objectID);
            stm.setDate(2, Date.valueOf(startDate));
            stm.setDate(3, Date.valueOf(endDate));

            ResultSet res = stm.executeQuery();

            if (res.next()) {
                for (int i = 1; i < 12; i++) {
                    result.add(res.getString("n" + i));
                    result.add(res.getString("n" + i + "_col"));
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "error load total data for object: " + objectID, e);
        }
        return result;
    }

    private List<DataModel> loadParameters(int object, LocalDate startDate, LocalDate endDate, String select) {
        List<DataModel> result = new ArrayList<>();
        try (Connection connection = ds.getConnection();
             PreparedStatement stm = connection.prepareStatement(select)) {

            stm.setInt(1, object);
            stm.setDate(2, Date.valueOf(startDate));
            stm.setDate(3, Date.valueOf(endDate));
            ResultSet res = stm.executeQuery();

            while (res.next()) {
                result.add(new DataModel(res.getString(1), res.getString(4), res.getInt(2), res.getInt(3), res.getInt(5)));
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "error load data select: " + select +
                    " for object: " + object + " startDate: " + startDate + " endDate: " + endDate, e);
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
