package ru.tecon.dNet.report.model;

import java.util.StringJoiner;

public class DataModel {

    private String name;
    private String value;
    private int id;
    private int statId;
    private int colorIndex;

    public DataModel(String name, String value, int id, int statId, int colorIndex) {
        this.name = name;
        this.value = value;
        this.id = id;
        this.statId = statId;
        this.colorIndex = colorIndex;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public int getId() {
        return id;
    }

    public int getStatId() {
        return statId;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DataModel.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("value='" + value + "'")
                .add("id=" + id)
                .add("statId=" + statId)
                .add("colorIndex=" + colorIndex)
                .toString();
    }
}
