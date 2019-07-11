package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class GraphElement implements Serializable {

    private int objectId;
    private String name;
    private String tooltip;
    private List<Connector> connectors = new ArrayList<>();
    private List<GraphElement> children;
    private String date;

    public GraphElement(int objectId, String name) {
        this.objectId = objectId;
        if (name != null) {
            this.tooltip = name;
            this.name = name.contains("title=") ? name.substring(0, name.indexOf("title=")) : name;
            if (this.name.length() > 25) {
                while (this.name.length() > 25) {
                    this.name = this.name.substring(0, this.name.lastIndexOf(' '));
                }
                this.name += "...";
            }
        }
    }

    public GraphElement(int objectId, String name, String date) {
        this(objectId, name);
        this.date = date;
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
