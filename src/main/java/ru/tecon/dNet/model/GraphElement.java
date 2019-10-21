package ru.tecon.dNet.model;

import ru.tecon.dNet.util.Graphs;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class GraphElement implements Serializable {

    private int objectId;
    private String name;
    private String tooltip;
    private List<Connector> connectors = new ArrayList<>();
    private List<GraphElement> children;
    private String date;
    private int trimSize;

    public GraphElement(int objectId, String name, int trimSize) {
        this.objectId = objectId;
        if (name != null) {
            this.trimSize = trimSize;
            this.tooltip = name;
            this.name = name.contains("title=") ? name.substring(0, name.indexOf("title=")) : name;
            if (this.name.length() > trimSize) {
                while (this.name.length() > trimSize) {
                    this.name = this.name.substring(0, this.name.lastIndexOf(' '));
                }
                this.name += "...";
            }
        }
    }

    public GraphElement(int objectId, String name, String date, int trimSize) {
        this(objectId, name, trimSize);
        this.date = date;
        this.trimSize = trimSize;
    }

    public GraphElement(int objectId, String name, String date) {
        this(objectId, name, date, 0);
    }

    public void addConnect(Connector connectors) {
        this.connectors.add(connectors);
    }

    public int getObjectId() {
        return objectId;
    }

    public String getName() {
        return name;
    }

    public String getTooltip() {
        return tooltip.replace("title=", ", ");
    }

    public String getFullTooltip() {
        return tooltip;
    }

    public List<Connector> getConnectors() {
        return connectors;
    }

    public List<GraphElement> getChildren() {
        return children;
    }

    public void addChildren(GraphElement children) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }

        this.children.add(children);
    }

    public String getDate() {
        return date;
    }

    public int getTrimSize() {
        return trimSize;
    }

    /**
     * Метод считает суммы энергий потребителей по каждой схеме
     * @return список с суммированными энергиями
     */
    public List<String> getConnectionDescriptionLine1() {
        return getConnectors().stream().map(e -> {
            if (e.getName() == null) return null;

            String name = e.getName().contains(" ") ? e.getName().substring(0, e.getName().indexOf(" ")) : e.getName();

            boolean isData = getChildren().stream()
                    .anyMatch(v -> v.getConnectors().stream()
                            .anyMatch(f -> f.getName().contains(name) && f.getEnergy() != null));

            if (!isData) return e.getName();

            double energySum = getChildren().stream().mapToDouble(v ->
                            v.getConnectors().stream()
                                    .filter(f -> f.getName().contains(name) && (f.getEnergy() != null))
                                    .findFirst()
                                    .map(connector -> new BigDecimal(connector.getEnergy()).doubleValue())
                                    .orElse(0.0))
                    .sum();

            return e.getName() + " ΣQ=" + new BigDecimal(energySum).setScale(2, RoundingMode.HALF_EVEN);
        }).collect(Collectors.toList());
    }

    /**
     * Метод считает суммы расходов потребителей по каждой схеме
     * @return список с суммированными энергиями
     */
    public List<String> getConnectionDescriptionLine2() {
        return getConnectors().stream().map(e -> {
            if (e.getName() == null) return null;

            String name = e.getName().contains(" ") ? e.getName().substring(0, e.getName().indexOf(" ")) : e.getName();
            String result = "";

            boolean isData = getChildren().stream()
                    .anyMatch(v -> v.getConnectors().stream()
                            .anyMatch(f -> f.getName().contains(name) && (f.getIn()[2] != null)));

            if (isData) {
                double energySum = getChildren().stream().mapToDouble(v ->
                                v.getConnectors().stream()
                                        .filter(f -> f.getName().contains(name) && (f.getIn()[2] != null))
                                        .findFirst()
                                        .map(connector -> {
                                            String value = connector.getIn()[2].getValue();
                                            return new BigDecimal(value.substring(value.indexOf("=") + 1)).doubleValue();
                                        })
                                        .orElse(0.0))
                        .sum();

                result += "Σ" + Graphs.getSumNamePrefix(name) + "под=" + new BigDecimal(energySum).setScale(2, RoundingMode.HALF_EVEN) + " ";
            }

            isData = getChildren().stream()
                    .anyMatch(v -> v.getConnectors().stream()
                            .anyMatch(f -> f.getName().contains(name) && (f.getOut()[2] != null)));

            if (isData) {
                double energySum = getChildren().stream().mapToDouble(v ->
                                v.getConnectors().stream()
                                        .filter(f -> f.getName().contains(name) && (f.getOut()[2] != null))
                                        .findFirst()
                                        .map(connector -> {
                                            String value = connector.getOut()[2].getValue();
                                            return new BigDecimal(value.substring(value.indexOf("=") + 1)).doubleValue();
                                        })
                                        .orElse(0.0))
                        .sum();

                result += "Σ" + Graphs.getSumNamePrefix(name) + "обр=" + new BigDecimal(energySum).setScale(2, RoundingMode.HALF_EVEN);
            }

            return result;
        }).collect(Collectors.toList());

    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GraphElement.class.getSimpleName() + "[", "]")
                .add("objectId=" + objectId)
                .add("name='" + name + "'")
                .add("tooltip='" + tooltip + "'")
                .add("connectors=" + connectors)
                .add("children=" + children)
                .add("date='" + date + "'")
                .toString();
    }
}
