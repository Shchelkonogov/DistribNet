package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;

public class DiagramElement implements Serializable {

    private String name;
    private List<String> values;
    private String className;

    public DiagramElement(String name, List<String> values, String className) {
        this.name = name;
        this.values = values;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public List<String> getValues() {
        return values;
    }

    public String getClassName() {
        return className;
    }

    public int getSize() {
        return values.size();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DiagramElement.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("values=" + values)
                .add("className='" + className + "'")
                .toString();
    }
}
