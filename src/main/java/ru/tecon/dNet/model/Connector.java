package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.StringJoiner;

public class Connector implements Serializable {

    private String name;
    private String inValue;
    private String outValue;

    public Connector(String name, String inValue, String outValue) {
        this.name = name;
        this.inValue = inValue;
        this.outValue = outValue;
    }

    public String getName() {
        return name;
    }

    public String getInValue() {
        return inValue;
    }

    public String getOutValue() {
        return outValue;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Connector.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("inValue='" + inValue + "'")
                .add("outValue='" + outValue + "'")
                .toString();
    }
}
