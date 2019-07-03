package ru.tecon.dNet.mBean;

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
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ManagedBean(name = "graph")
@RequestScoped
public class GraphMBean {

    private static Logger log = Logger.getLogger(GraphMBean.class.getName());

    @ManagedProperty("#{param.object}")
    private int object;

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

    private String error;

    @EJB
    private GraphSBean bean;

    @PostConstruct
    public void init() {
        // TODO проверить работу ошибки и поместить ее в центр окна
//        if (true) {
//            error = "asdasdasd";
//            return;
//        }
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
        GraphElement init;
        GraphElement producer;
        try {
            //TODO объекты которые проверяю: 20867, 7302
            init = bean.loadInitData(object);
            producer = bean.loadGraph(object, init.getDate());
        } catch (GraphLoadException e) {
            log.warning(e.getMessage());
            error = e.getMessage();
            return;
        }

        //Убираем элементы из графа если нету связей
        producer.getChildren().removeIf(consumer -> consumer.getConnectors().size() == 0);
        if (producer.getChildren().size() == 0) {
            error = "Потребители источника не слинкованы!";
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
                        producer.getConnectors().stream().map(Connector::getName).collect(Collectors.toList()),
                        "right"),
                "18em", "9em");
        prodLeft.setDraggable(false);
        prodLeft.setId("idProd");

        Element prodRight = new Element(null, "55em", "9em");
        prodRight.setDraggable(false);
        prodRight.setId("idInvisible");

        diagramModelLeft.addElement(prodLeft);
        diagramModelLeft.addElement(prodRight);

        addStyle(producer, "right", "Left");
        styles.append("#diaLeft-idProd, #diaLeft-idInvisible")
                .append("{height: ")
                .append(BLOCK_HEIGHT * producer.getConnectors().size())
                .append("px;} ")
                .append("#diaLeft-idInvisible {visibility: hidden; width: 1em;}");

        initConnections(prodLeft, prodRight, producer, diagramModelLeft);
        tooltips.add(new Tooltip("diaLeft-idProd", producer.getTooltip()));

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
                init.getConnectors().get(0).getIn(), false, true));
        diagramModelLeft.connect(createConnection(initElement.getEndPoints().get(0),
                prodLeft.getEndPoints().get(prodLeft.getEndPoints().size() - 1),
                init.getConnectors().get(0).getOut(), true, true));

        //Созадем связку элемент соединитель элемент для потребителей
        int yPos = 10;
        for (GraphElement el : producer.getChildren()) {
            Element left = new Element(null, "0em", yPos + "px");
            left.setDraggable(false);
            left.setId("id" + yPos + "invisible");

            Element right = new Element(
                    new DiagramElement(el.getName(),
                            el.getConnectors().stream().map(Connector::getName).collect(Collectors.toList()),
                            "left"),
                    "24em", yPos + "px");
            right.setDraggable(false);
            right.setId("id" + yPos);

            diagramModelRight.addElement(left);
            diagramModelRight.addElement(right);

            addStyle(el, "left", "Right");
            styles.append("#diaRight-id")
                    .append(yPos)
                    .append(", #diaRight-id")
                    .append(yPos)
                    .append("invisible")
                    .append("{height: ")
                    .append(BLOCK_HEIGHT * el.getConnectors().size())
                    .append("px;} ")
                    .append("#diaRight-id")
                    .append(yPos)
                    .append("invisible")
                    .append("{visibility: hidden; width: 1em;}");

            initConnections(left, right, el, diagramModelRight);
            tooltips.add(new Tooltip("diaRight-id" + yPos, el.getTooltip()));

            yPos += 20 + (BLOCK_HEIGHT * el.getConnectors().size());
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

        styles.append("#diaRight-idLeftWrapper {height: ")
                .append(yPos - 30)
                .append("px; width: 1em; box-shadow: none;}")
                .append("#diaRight-idDownWrapper{height: 1em; width: 1em; visibility: hidden;}");
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
                    el.getConnectors().get(i).getIn(), false, false));

            model.connect(createConnection(
                    left.getEndPoints().get(3 * i + 1),
                    right.getEndPoints().get(3 * i + 1), el.getConnectors().get(i).getCenter()));

            model.connect(createConnection(
                    left.getEndPoints().get(3 * i + 2),
                    right.getEndPoints().get(3 * i + 2),
                    el.getConnectors().get(i).getOut(), true, false));
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
    private Connection createConnection(EndPoint from, EndPoint to, ConnectorValue[] label, boolean isLeft, boolean isConnector) {
        Connection conn = new Connection(from, to);

        if (isLeft) {
            conn.getOverlays().add(new ArrowOverlay(20, 20, 0, -1));
        } else {
            conn.getOverlays().add(new ArrowOverlay(20, 20, 1, 1));
        }

        if (isConnector) {
            FlowChartConnector connector = new FlowChartConnector();
            connector.setPaintStyle("{strokeStyle:'#404a4e', lineWidth:3}");

            conn.setConnector(connector);
        }

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

                styles.append("#repeat\\:")
                        .append(i)
                        .append("\\:text")
                        .append(graph)
                        .append("Graph.")
                        .append(direction)
                        .append(size)
                        .append(" {top: ")
                        .append(y).append("px;}");
            }
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

    public int getObject() {
        return object;
    }

    public void setObject(int object) {
        this.object = object;
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
}
