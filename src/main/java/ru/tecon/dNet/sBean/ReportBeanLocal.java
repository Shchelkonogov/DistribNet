package ru.tecon.dNet.sBean;

import ru.tecon.dNet.report.model.ConsumerModel;
import ru.tecon.dNet.report.model.DataModel;

import javax.ejb.Local;
import java.util.List;

/**
 * local интерфейс для получения данных для excel отчета
 */
@Local
public interface ReportBeanLocal {

    /**
     * Получение имени ЦТП
     * @param object id объекта
     * @return имя ЦТП
     */
    String getCTP(int object);

    /**
     * Получение схемы подключения
     * @param object id объекта
     * @return схема подключения
     */
    String getConnectSchema(int object);

    /**
     * Получение имени филиала
     * @param object id объекта
     * @return имя филиала
     */
    String getFilial(int object);

    /**
     * Полечение имени организации
     * @param object id объекта
     * @return имя организации
     */
    String getCompany(int object);

    /**
     * Получение источника
     * @param object id объекта
     * @return источник
     */
    String getSource(int object);

    /**
     * Получение географического адреса объекта
     * @param objectID id объекта
     * @return адрес
     */
    String getAddress(int objectID);

    /**
     * Получение списка подключенных объектов с их id
     * @param object id объекта
     * @return список подключенных объектов
     */
    List<ConsumerModel> getObjectNames(int object);

    /**
     * Получение списка входный параметров
     * @param object id объекта
     * @param date дата в формате dd.MM.yyyy
     * @return список входных параметров
     */
    List<DataModel> getInParameters(int object, String date);

    /**
     * Получение списка выходных параметров
     * @param object id объекта
     * @param date дата в формате dd.MM.yyyy
     * @return список выходных параметров
     */
    List<DataModel> getOutParameters(int object, String date);

    /**
     * Получние значений по парамтру
     * @param parentID id цтп
     * @param object id объекта
     * @param id id параметра
     * @param statId id стат агрегата
     * @param date дата в формате dd.MM.yyyy
     * @return значение параметра
     */
    String getValue(int parentID, int object, int id, int statId, String date);
}
