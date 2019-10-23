package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.List;
import java.util.StringJoiner;

public class DiagramElement implements Serializable {

    private String name;
    private List<String> values;
    private List<String> sumValues;
    private String className;
    private Integer objectId;
    private String problemDesc;

    public DiagramElement(String name) {
        this.name = name;
    }

    public DiagramElement(String name, List<String> values, String className, Integer objectId,
                          String problemDesc) {
        this(name);
        this.values = values;
        this.className = className;
        this.objectId = objectId;
        this.problemDesc = problemDesc;
    }

    public DiagramElement(String name, List<String> values, List<String> sumValues, String className) {
        this(name, values, className, null, null);
        this.sumValues = sumValues;
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

    public List<String> getSumValues() {
        return sumValues;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public String getProblemDesc() {
        return problemDesc;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DiagramElement.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("values=" + values)
                .add("sumValues=" + sumValues)
                .add("className='" + className + "'")
                .add("objectId=" + objectId)
                .add("problemDesc='" + problemDesc + "'")
                .toString();
    }
}
