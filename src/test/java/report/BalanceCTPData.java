package report;

import ru.tecon.dNet.report.model.ConsumerModel;
import ru.tecon.dNet.report.model.DataModel;
import ru.tecon.dNet.sBean.ReportBeanLocal;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BalanceCTPData implements ReportBeanLocal {
    
    public String getCTP(int object) {
        return "01-01-01";
    }

    public String getConnectSchema(int object) {
        return "схема подключения";
    }

    public String getFilial(int object) {
        return "Филиал 1";
    }

    public String getCompany(int object) {
        return "Предприятие 1";
    }

    public String getSource(int object) {
        return "Источник 1";
    }

    @Override
    public String getAddress(int objectID) {
        return "ADDRESS";
    }

    public List<ConsumerModel> getObjectNames(int object) {
        return Arrays.asList(new ConsumerModel("дом 1", 1), new ConsumerModel("дом 2", 2),
                new ConsumerModel("дом 3", 3), new ConsumerModel("дом 4", 4));
    }

    @Override
    public List<DataModel> getInParameters(int object, String date) {
        return Arrays.asList(new DataModel("Q", String.valueOf(ThreadLocalRandom.current().nextInt(-1000, 1000)), 1, 1), new DataModel("Имя 2", "Значение 2", 2, 2),
                new DataModel("Имя 3", "Значение 3", 3, 3));
    }

    @Override
    public List<DataModel> getOutParameters(int object, String date) {
        return Arrays.asList(new DataModel("Имя 1", "Значение 1", 1, 1), new DataModel("Q", String.valueOf(ThreadLocalRandom.current().nextInt(-1000, 1000)), 2, 2),
                new DataModel("Имя 3", "Значение 3", 3, 3), new DataModel("Имя 4", "Значение 4", 4, 4));
    }

    @Override
    public String getValue(int parentID, int object, int id, int statId, String date) {
        return String.valueOf(ThreadLocalRandom.current().nextInt(-1000, 1000));
    }
}
