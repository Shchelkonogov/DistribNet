package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.StringJoiner;

public class Tooltip implements Serializable {

    private String id;
    private String value;

    public Tooltip(String id, String value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Tooltip.class.getSimpleName() + "[", "]")
                .add("id='" + id + "'")
                .add("value='" + value + "'")
                .toString();
    }
}
