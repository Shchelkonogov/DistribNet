package ru.tecon.dNet.mBean;

import org.primefaces.PrimeFaces;
import org.primefaces.model.diagram.Connection;
import org.primefaces.model.diagram.DefaultDiagramModel;
import org.primefaces.model.diagram.Element;
import org.primefaces.model.diagram.connector.FlowChartConnector;
import org.primefaces.model.diagram.connector.StraightConnector;
import org.primefaces.model.diagram.endpoint.BlankEndPoint;
import org.primefaces.model.diagram.endpoint.EndPoint;
import org.primefaces.model.diagram.endpoint.EndPointAnchor;
import org.primefaces.model.diagram.overlay.ArrowOverlay;
import org.primefaces.model.diagram.overlay.LabelOverlay;
import ru.tecon.dNet.exception.GraphLoadException;
import ru.tecon.dNet.model.*;
import ru.tecon.dNet.sBean.GraphSBean;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ManagedBean(name = "graph")
@ViewScoped
public class GraphMBean implements Serializable {

    private static Logger log = Logger.getLogger(GraphMBean.class.getName());

    private int object;
    private LocalDate localDate;
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private GraphElement producerData;
    private GraphElement init = null;

    //Высота элементы мнемосхемы в пикселях
    private static final int BLOCK_HEIGHT = 100;

    //Модели для мнемосхемы
    private DefaultDiagramModel diagramModelLeft;
    private DefaultDiagramModel diagramModelRight;

    private StringBuilder styles = new StringBuilder();
    private List<String> checkStyleList = new ArrayList<>();

    private List<Tooltip> tooltips = new ArrayList<>();

    //Коэффициенты источника
    private ConnectorValue[] producerIndex;

    private String temperature;
    private String temperatureColor;

    private boolean co = true;
    private boolean gvs = true;
    private boolean vent = true;
    private boolean isVisibleGvs = false;
    private boolean isVisibleCo = false;
    private boolean isVisibleVent = false;
    private static final String CO = "ЦО";
    private static final String GVS = "ГВС";
    private static final String VENT = "ВЕНТ";
    private static final String TC = "ТС";

    private String error;

    private Map<String, List<Problem>> problems = new HashMap<>();

    private List<Integer> consumersId = new ArrayList<>();

    @EJB
    private GraphSBean bean;

    @PostConstruct
    public void init() {
        //Объявляем граф
        diagramModelLeft = new DefaultDiagramModel();
        diagramModelLeft.setMaxConnections(-1);
        diagramModelRight = new DefaultDiagramModel();
        diagramModelRight.setMaxConnections(-1);

        //Задаем тип соединителей
        StraightConnector connector = new StraightConnector();
        connector.setPaintStyle("{strokeStyle:'#404a4e', lineWidth:3}");
        diagramModelLeft.setDefaultConnector(connector);
        diagramModelRight.setDefaultConnector(connector);

        //Загрузка информации по графу
        if (producerData == null) {
            object = Integer.valueOf(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("object"));
            String date = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("date");
            localDate = LocalDate.parse(date, dtf);

            try {
                init = bean.loadInitData(object, date);
                producerData = bean.loadGraph(object, init.getDate());

                checkVisible(producerData);

                producerData.getChildren().forEach(this::checkVisible);

                if (bean.checkSummer(date)) {
                    co = false;
                    vent = false;
                }
            } catch (GraphLoadException e) {
                log.warning(e.getMessage());
                error = e.getMessage();
                return;
            }

            bean.getProblems(problems, object, date);
        } else {
            styles = new StringBuilder();
            checkStyleList.clear();
            tooltips.clear();
            error = "";
            producerIndex = null;
            temperature = "";
            temperatureColor = "";
        }

        GraphElement producer = clone(producerData);
        if (!gvs) {
            producer.getConnectors().removeIf(el -> el.getName().matches(GVS + ".*"));
            producer.getChildren().forEach(el -> el.getConnectors().removeIf(f -> f.getName().matches(GVS + ".*")));
        }
        if (!co) {
            producer.getConnectors().removeIf(el -> el.getName().matches(CO + ".*"));
            producer.getChildren().forEach(el -> el.getConnectors().removeIf(f -> f.getName().matches(CO + ".*")));
        }
        if (!vent) {
            producer.getConnectors().removeIf(el -> el.getName().matches(VENT + ".*"));
            producer.getChildren().forEach(el -> el.getConnectors().removeIf(f -> f.getName().matches(VENT + ".*")));
        }

        //Убираем элементы из графа если нету связей
        producer.getChildren().removeIf(consumer -> consumer.getConnectors().size() == 0);
        if ((producer.getChildren().size() == 0) || (producer.getConnectors().size() == 0)) {
            log.info("init: Выберите систему для отображения!");
            error = "Выберите систему для отображения!";
            return;
        }

        //Задаем значение для температуры и ее цвет
        temperature = init.getConnectors().get(0).getName().split(" ")[1];
        temperatureColor = init.getConnectors().get(0).getName().split(" ")[2];

        //Задаем значения для коэффикиентов источнка и задаем их стили
        List<String> values = new ArrayList<>(Arrays.asList(init.getConnectors().get(0).getCenter()[0].getValue().split(" ")));
        List<String> colors = new ArrayList<>(Arrays.asList(init.getConnectors().get(0).getCenter()[0].getColor().split(" ")));
        producerIndex = new ConnectorValue[Math.min(values.size(), colors.size())];
        for (int i = 0; i < producerIndex.length; i++) {
            producerIndex[i] = new ConnectorValue(values.get(i), colors.get(i));

            if (!checkStyleList.contains(producerIndex[i].getColor())) {
                checkStyleList.add(producerIndex[i].getColor());

                styles.append('.')
                        .append(producerIndex[i].getColor())
                        .append("{background: ")
                        .append(producerIndex[i].getColor())
                        .append("}");
            }
        }

        //Созадем связку элемент соединитель элемент для источника
        Element prodLeft = new Element(
                new DiagramElement(producer.getName(),
                        getCTPName(producer),
                        "right"),
                "18em", "9em");
        prodLeft.setDraggable(false);
        prodLeft.setId("idProd-objectIdLeft");

        Element prodRight = new Element(null, "55em", "9em");
        prodRight.setDraggable(false);
        prodRight.setId("idInvisible");

        diagramModelLeft.addElement(prodLeft);
        diagramModelLeft.addElement(prodRight);

        addStyle(producer, "right", "Left");
        styles.append("#left\\:diaLeft-idProd-objectIdLeft, #left\\:diaLeft-idInvisible")
                .append("{height: ")
                .append(BLOCK_HEIGHT * producer.getConnectors().size())
                .append("px;} ")
                .append("#left\\:diaLeft-idInvisible {visibility: hidden; width: 1em;}");

        initConnections(prodLeft, prodRight, producer, diagramModelLeft);
        tooltips.add(new Tooltip("left\\:diaLeft-idProd-objectIdLeft", producer.getTooltip()));

        //Создаем элемент и связь для входных данных в источник
        Element initElement = new Element(
                new DiagramElement(init.getConnectors().get(0).getName().split(" ")[0], null, null),
                "1em", "0em");
        initElement.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM_LEFT));
        initElement.addEndPoint(new BlankEndPoint(EndPointAnchor.BOTTOM_RIGHT));
        initElement.setStyleClass("init-node");
        initElement.setDraggable(false);

        diagramModelLeft.addElement(initElement);

        prodLeft.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_TOP));
        prodLeft.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_TOP));

        diagramModelLeft.connect(createConnection(initElement.getEndPoints().get(1),
                prodLeft.getEndPoints().get(prodLeft.getEndPoints().size() - 2),
                init.getConnectors().get(0).getIn(), false, true, getColor(TC)));
        diagramModelLeft.connect(createConnection(initElement.getEndPoints().get(0),
                prodLeft.getEndPoints().get(prodLeft.getEndPoints().size() - 1),
                init.getConnectors().get(0).getOut(), true, true, getColor(TC)));

        //Созадем связку элемент соединитель элемент для потребителей
        int yPos = 10;
        int index = 0;
        consumersId.clear();
        for (GraphElement el : producer.getChildren()) {
            consumersId.add(el.getObjectId());
            Element left = new Element(null, "0em", yPos + "px");
            left.setDraggable(false);
            left.setId("id" + yPos + "invisible");

            Element right = new Element(
                    new DiagramElement(el.getName(),
                            el.getConnectors().stream().map(Connector::getName).collect(Collectors.toList()),
                            "left"),
                    "24em", yPos + "px");
            right.setDraggable(false);
            right.setId("id" + yPos + "-objectIdRight" + index);
            right.setStyleClass("ui-diagram-element-right");

            diagramModelRight.addElement(left);
            diagramModelRight.addElement(right);

            addStyle(el, "left", "Right");
            styles.append("#right\\:diaRight-id")
                    .append(yPos)
                    .append("-objectIdRight")
                    .append(index)
                    .append(", #right\\:diaRight-id")
                    .append(yPos)
                    .append("invisible")
                    .append("{height: ")
                    .append(BLOCK_HEIGHT * el.getConnectors().size())
                    .append("px;} ")
                    .append("#right\\:diaRight-id")
                    .append(yPos)
                    .append("invisible")
                    .append("{visibility: hidden; width: 1em;}");

            initConnections(left, right, el, diagramModelRight);
            tooltips.add(new Tooltip("right\\:diaRight-id" + yPos + "-objectIdRight" + index, el.getTooltip()));

            yPos += 20 + (BLOCK_HEIGHT * el.getConnectors().size());
            index++;
        }

        //Добавляем объединяющий вертикальный элемент
        Element leftWrapper = new Element(null, "oem", "10px");
        leftWrapper.setDraggable(false);
        leftWrapper.setId("idLeftWrapper");
        leftWrapper.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));

        Element downWrapper = new Element(null, "0em", yPos - 20 + "px");
        downWrapper.setDraggable(false);
        downWrapper.setId("idDownWrapper");
        downWrapper.addEndPoint(new BlankEndPoint(EndPointAnchor.TOP));

        diagramModelRight.addElement(leftWrapper);
        diagramModelRight.addElement(downWrapper);

        styles.append("#right\\:diaRight-idLeftWrapper {height: ")
                .append(Math.max(yPos - 30, BLOCK_HEIGHT * producer.getConnectors().size() + 120))
                .append("px; width: 1em; box-shadow: none;}")
                .append("#right\\:diaRight-idDownWrapper{height: 1em; width: 1em; visibility: hidden;}");
    }

    private List<String> getCTPName(GraphElement producer) {
        return producer.getConnectors().stream().map(e -> {
            if (e.getName().contains(" ")) {
                String name = e.getName().split(" ")[0];

                boolean isData = producer.getChildren().stream()
                        .anyMatch(v -> v.getConnectors().stream().filter(f1 -> f1.getName().contains(name))
                                .anyMatch(f2 -> !f2.getEnergy().equals("н/д")));

                if (isData) {
                    double test = producer.getChildren().stream().mapToDouble(v -> {
                        Optional<Connector> value = v.getConnectors().stream().filter(f -> f.getName().contains(name)).findFirst();
                        if (value.isPresent()) {
                            try {
                                return new BigDecimal(value.get().getEnergy()).doubleValue();
                            } catch (NumberFormatException ex) {
                                return 0;
                            }
                        } else {
                            return 0;
                        }
                    }).sum();
                    return e.getName() + "; Σ=" + test;
                } else {
                    return e.getName() + "; Σ=" + "н/д";
                }
            } else {
                return e.getName();
            }
        }).collect(Collectors.toList());
    }

    private void initConnections(Element left, Element right, GraphElement el, DefaultDiagramModel model) {
        for (int i = 0; i < el.getConnectors().size(); i++) {
            left.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_RIGHT));
            left.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_RIGHT));
            left.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_RIGHT));

            right.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_LEFT));
            right.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_LEFT));
            right.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_LEFT));

            model.connect(createConnection(
                    left.getEndPoints().get(3 * i),
                    right.getEndPoints().get(3 * i),
                    el.getConnectors().get(i).getIn(), false, false, getColor(el.getConnectors().get(i).getName())));

            model.connect(createConnection(
                    left.getEndPoints().get(3 * i + 1),
                    right.getEndPoints().get(3 * i + 1), el.getConnectors().get(i).getCenter()));

            model.connect(createConnection(
                    left.getEndPoints().get(3 * i + 2),
                    right.getEndPoints().get(3 * i + 2),
                    el.getConnectors().get(i).getOut(), true, false, getColor(el.getConnectors().get(i).getName())));
        }
    }

    private String getColor(String name) {
        if (name.matches(TC + ".*")) {
            return "#FF0000";
        } else {
            if (name.matches(CO + ".*")) {
                return "#ff7f00";
            } else {
                if (name.matches(GVS + ".*")) {
                    return "#00FF00";
                } else {
                    if (name.matches(VENT + ".*")) {
                        return "#FFFF00";
                    } else {
                        return "#404a4e";
                    }
                }
            }
        }
    }

    /**
     * Создание соединителя с невидемой линией
     * @param from начальная точка
     * @param to конечная точка
     * @param label данные для отображания
     * @return элемент связи
     */
    private Connection createConnection(EndPoint from, EndPoint to, ConnectorValue[] label) {
        Connection connect = new Connection(from, to);

        StraightConnector connector = new StraightConnector();
        connector.setPaintStyle("{strokeStyle:'rgba(100, 100, 100, 0)', lineWidth:0}");

        connect.setConnector(connector);

        if(label != null) {
            for (int i = 0; i < label.length; i++) {
                if (label[i] != null) {
                    connect.getOverlays().add(new LabelOverlay(label[i].getValue(), "flow-label-k" + i + " " + label[i].getColor(), 0.5));
                    if (!checkStyleList.contains(label[i].getColor())) {
                        checkStyleList.add(label[i].getColor());

                        styles.append('.')
                                .append(label[i].getColor())
                                .append("{background: ")
                                .append(label[i].getColor())
                                .append("}");
                    }
                }
            }
        }

        return connect;
    }

    /**
     * Создание соединителя с видимой линией
     * @param from начальная точка
     * @param to конечная точка
     * @param label данные для отображения
     * @param isLeft положение стрелки (true стрелка слева)
     * @param isConnector вариант соединителя (true FlowChartConnector)
     * @return элемент связи
     */
    private Connection createConnection(EndPoint from, EndPoint to, ConnectorValue[] label, boolean isLeft,
                                        boolean isConnector, String color) {
        Connection conn = new Connection(from, to);

        if (isLeft) {
            conn.getOverlays().add(new ArrowOverlay(20, 20, 0, -1));
        } else {
            conn.getOverlays().add(new ArrowOverlay(20, 20, 1, 1));
        }

        org.primefaces.model.diagram.connector.Connector connector;
        if (isConnector) {
            connector = new FlowChartConnector();
        } else {
            connector = new StraightConnector();
        }
        connector.setPaintStyle("{strokeStyle:'" + color + "', lineWidth:3}");
        conn.setConnector(connector);

        if (label != null) {
            for (int i = 0; i < label.length; i++) {
                if (label[i] != null) {
                    conn.getOverlays().add(new LabelOverlay(label[i].getValue(), "flow-label" + i + " " + label[i].getColor(), 0.5));
                    if (!checkStyleList.contains(label[i].getColor())) {
                        checkStyleList.add(label[i].getColor());

                        styles.append('.')
                                .append(label[i].getColor())
                                .append("{background: ")
                                .append(label[i].getColor())
                                .append("}");
                    }
                }
            }
        }

        return conn;
    }

    private void addStyle(GraphElement element, String direction, String graph) {
        int size = element.getConnectors().size();
        if (!checkStyleList.contains(size + direction + graph)) {
            checkStyleList.add(size + direction + graph);

            for (int i = 0; i < size; i++) {
                double y = (double) (2 * i + 1) * (BLOCK_HEIGHT * size + 2) / (size * 2 + 1) - 35.2 +
                        (double) (BLOCK_HEIGHT * size + 2) / (2 * (size * 2 + 1));

                styles.append(".text")
                        .append(graph)
                        .append("Graph")
                        .append(i)
                        .append(".")
                        .append(direction)
                        .append(size)
                        .append(" {top: ")
                        .append(y)
                        .append("px;}");
            }
        }
    }

    private void checkVisible(GraphElement graphElement) {
        graphElement.getConnectors().forEach(el -> {
            if (!isVisibleGvs && el.getName().matches(GVS + ".*")) {
                isVisibleGvs = true;
            }
            if (!isVisibleCo && el.getName().matches(CO + ".*")) {
                isVisibleCo = true;
            }
            if (!isVisibleVent && el.getName().matches(VENT + ".*")) {
                isVisibleVent = true;
            }
        });
    }

    private GraphElement clone(GraphElement o2) {
        GraphElement o1 = new GraphElement(o2.getObjectId(), o2.getFullTooltip(), o2.getDate(), o2.getTrimSize());
        for (Connector el: o2.getConnectors()) {
            o1.addConnect(el);
        }
        for (GraphElement el: o2.getChildren()) {
            GraphElement o3 = new GraphElement(el.getObjectId(), el.getFullTooltip(), el.getDate(), el.getTrimSize());
            for (Connector item: el.getConnectors()) {
                o3.addConnect(item);
            }
            o1.addChildren(o3);
        }
        return o1;
    }

    public void changeButton() {
        init();
    }

    public void redirect() {
        String id = FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("objectId");

        String objectId = null;

        if (id.equals("Left")) {
            objectId = String.valueOf(object);
        } else {
            try {
                objectId = String.valueOf(consumersId.get(Integer.valueOf(id)));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            //Временно отключил ветку потребителей
            objectId = null;
        }

        if (objectId != null) {
            PrimeFaces.current().executeScript("window.open('" + bean.getRedirectUrl(objectId) + "'), '_blank'");
        }
    }

    public ConnectorValue[] getProducerIndex() {
        return producerIndex;
    }

    public String getError() {
        return error;
    }

    public DefaultDiagramModel getDiagramModelLeft() {
        return diagramModelLeft;
    }

    public DefaultDiagramModel getDiagramModelRight() {
        return diagramModelRight;
    }

    public List<Tooltip> getTooltips() {
        return tooltips;
    }

    public String getStyles() {
        return styles.toString();
    }

    public String getTemperature() {
        return temperature;
    }

    public String getTemperatureColor() {
        return temperatureColor;
    }

    public boolean isCo() {
        return co;
    }

    public void setCo(boolean co) {
        this.co = co;
    }

    public boolean isGvs() {
        return gvs;
    }

    public void setGvs(boolean gvs) {
        this.gvs = gvs;
    }

    public boolean isVent() {
        return vent;
    }

    public void setVent(boolean vent) {
        this.vent = vent;
    }

    public boolean isVisibleGvs() {
        return isVisibleGvs;
    }

    public boolean isVisibleCo() {
        return isVisibleCo;
    }

    public boolean isVisibleVent() {
        return isVisibleVent;
    }

    public int getObject() {
        return object;
    }

    public String getNextDate() {
        return localDate.plusDays(1).format(dtf);
    }

    public String getDate() {
        return localDate.format(dtf);
    }

    public String getBeforeDate() {
        return localDate.minusDays(1).format(dtf);
    }

    public List<String> getProblemsName() {
        return new ArrayList<>(problems.keySet());
    }

    public List<Problem> getProblemsValues(String key) {
        return problems.get(key);
    }
}
