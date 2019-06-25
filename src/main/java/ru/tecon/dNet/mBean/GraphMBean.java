package ru.tecon.dNet.mBean;

import org.primefaces.model.diagram.Connection;
import org.primefaces.model.diagram.DefaultDiagramModel;
import org.primefaces.model.diagram.Element;
import org.primefaces.model.diagram.connector.StraightConnector;
import org.primefaces.model.diagram.endpoint.BlankEndPoint;
import org.primefaces.model.diagram.endpoint.EndPoint;
import org.primefaces.model.diagram.endpoint.EndPointAnchor;
import org.primefaces.model.diagram.overlay.ArrowOverlay;
import org.primefaces.model.diagram.overlay.LabelOverlay;
import ru.tecon.dNet.exception.GraphLoadException;
import ru.tecon.dNet.model.Connector;
import ru.tecon.dNet.model.GraphElement;
import ru.tecon.dNet.model.DiagramElement;
import ru.tecon.dNet.model.Tooltip;
import ru.tecon.dNet.sBean.GraphSBean;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ManagedBean(name = "graph")
@RequestScoped
public class GraphMBean {

    private static Logger log = Logger.getLogger(GraphMBean.class.getName());

    private DefaultDiagramModel diagramModelLeft;
    private DefaultDiagramModel diagramModelRight;

    private StringBuilder styles = new StringBuilder();

    private String error;

    private List<Tooltip> tooltips = new ArrayList<>();

    @ManagedProperty("#{param.object}")
    private int object;

    private List<String> counts = new ArrayList<>();

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
        GraphElement init;
        GraphElement producer;
        try {
//            20867 7302
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

        //Добавляем стили для надписей между стрелками
        addStyle(init, "right", "Left");
        addStyle(producer, "right", "Left");
        for (GraphElement el: producer.getChildren()) {
            addStyle(el, "left", "Right");
        }

        //Созадем связку элемент соединитель элемент для входных параметров
        Element initLeft = new Element(
                new DiagramElement(init.getName(),
                        init.getConnectors().stream().map(Connector::getName).collect(Collectors.toList()),
                        "right"),
                "0em", "0px");
        initLeft.setStyleClass("start-node");
        initLeft.setDraggable(false);
        initLeft.setId("idInitProd");

        Element initRight = new Element(null, "20em", "0em");
        initRight.setDraggable(false);
        initRight.setId("idInitInvisible");

        diagramModelLeft.addElement(initLeft);
        diagramModelLeft.addElement(initRight);

        styles.append("#diaLeft-idInitProd, #diaLeft-idInitInvisible")
                .append("{height: ")
                .append(90 * init.getConnectors().size())
                .append("px;} ")
                .append("#diaLeft-idInitInvisible {visibility: hidden; width: 1em;}");

        initConnections(initLeft, initRight, init, diagramModelLeft);

        //Созадем связку элемент соединитель элемент для источника
        Element prodLeft = new Element(
                new DiagramElement(producer.getName(),
                        producer.getConnectors().stream().map(Connector::getName).collect(Collectors.toList()),
                        "right"),
                "20em", "0px");
        prodLeft.setDraggable(false);
        prodLeft.setId("idProd");

        Element prodRight = new Element(null, "50em", "0em");
        prodRight.setDraggable(false);
        prodRight.setId("idInvisible");

        diagramModelLeft.addElement(prodLeft);
        diagramModelLeft.addElement(prodRight);

        styles.append("#diaLeft-idProd, #diaLeft-idInvisible")
                .append("{height: ")
                .append(90 * producer.getConnectors().size())
                .append("px;} ")
                .append("#diaLeft-idInvisible {visibility: hidden; width: 1em;}");

        initConnections(prodLeft, prodRight, producer, diagramModelLeft);
//        tooltips.add(new Tooltip("diaLeft-idProd", producer.getName()));

        //Созадем связку элемент соединитель элемент для потребителей
        int yPos = 0;
        for (GraphElement el : producer.getChildren()) {
            Element left = new Element(null, "0em", yPos + "px");
            left.setDraggable(false);
            left.setId("id" + yPos + "invisible");

            Element right = new Element(
                    new DiagramElement(el.getName(),
                            el.getConnectors().stream().map(Connector::getName).collect(Collectors.toList()),
                            "left"),
                    "17em", yPos + "px");
            right.setDraggable(false);
            right.setId("id" + yPos);

            diagramModelRight.addElement(left);
            diagramModelRight.addElement(right);

            styles.append("#diaRight-id")
                    .append(yPos)
                    .append(", #diaRight-id")
                    .append(yPos)
                    .append("invisible")
                    .append("{height: ")
                    .append(90 * el.getConnectors().size())
                    .append("px;} ")
                    .append("#diaRight-id")
                    .append(yPos)
                    .append("invisible")
                    .append("{visibility: hidden; width: 1em;}");

            initConnections(left, right, el, diagramModelRight);
            tooltips.add(new Tooltip("diaRight-id" + yPos, el.getName()));

            yPos += 20 + (90 * el.getConnectors().size());
        }

        //Добавляем объединяющий вертикальный элемент
        Element leftWrapper = new Element(null, "0em", "0em");
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
                .append(yPos - 20)
                .append("px; width: 1em; box-shadow: none;}")
                .append("#diaRight-idDownWrapper{height: 1em; width: 1em; visibility: hidden;}");
    }

    private void initConnections(Element left, Element right, GraphElement el, DefaultDiagramModel model) {
        for (int i = 0; i < el.getConnectors().size(); i++) {
            left.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_RIGHT));
            left.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_RIGHT));

            right.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_LEFT));
            right.addEndPoint(new BlankEndPoint(EndPointAnchor.CONTINUOUS_LEFT));

            model.connect(createConnection(
                    left.getEndPoints().get(2 * i),
                    right.getEndPoints().get(2 * i),
                    el.getConnectors().get(i).getInValue(), false));

            model.connect(createConnection(
                    left.getEndPoints().get(2 * i + 1),
                    right.getEndPoints().get(2 * i + 1),
                    el.getConnectors().get(i).getOutValue(), true));
        }
    }

    private Connection createConnection(EndPoint from, EndPoint to, String label, boolean isLeft) {
        Connection conn = new Connection(from, to);

        if (isLeft) {
            conn.getOverlays().add(new ArrowOverlay(20, 20, 0, -1));
        } else {
            conn.getOverlays().add(new ArrowOverlay(20, 20, 1, 1));
        }

        if (label != null) {
            conn.getOverlays().add(new LabelOverlay(label, "flow-label", 0.5));
        }

        return conn;
    }

    private void addStyle(GraphElement element, String direction, String graph) {
        int size = element.getConnectors().size();
        if (!counts.contains(size + direction + graph)) {
            counts.add(size + direction + graph);

            for (int i = 0; i < size; i++) {
                double y = (double) (2 * i + 1) * (90 * size + 2) / (size * 2 + 1) - 35.2 +
                        (double) (90 * size + 2) / (2 * (size * 2 + 1));

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
}
