package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.StringJoiner;

public class ConnectorValue implements Serializable {

    private String value;
    private String color;

    public ConnectorValue(String value, String color) {
        this.value = value;
        this.color = color;
    }

    public String getValue() {
        return value;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConnectorValue.class.getSimpleName() + "[", "]")
                .add("value='" + value + "'")
                .add("color='" + color + "'")
                .toString();
    }
}
