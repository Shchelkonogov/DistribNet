package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.StringJoiner;

public class Connector implements Serializable {

    private String name;
    private String energy;
    private ConnectorValue temperature;
    private ConnectorValue[] in = new ConnectorValue[3];
    private ConnectorValue[] out = new ConnectorValue[3];
    private ConnectorValue[] center = new ConnectorValue[3];

    public Connector(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ConnectorValue[] getIn() {
        return in;
    }

    public ConnectorValue[] getOut() {
        return out;
    }

    public ConnectorValue[] getCenter() {
        return center;
    }

    public String getEnergy() {
        return energy;
    }

    public void setEnergy(String energy) {
        this.energy = energy;
    }

    public ConnectorValue getTemperature() {
        return temperature;
    }

    public void setTemperature(ConnectorValue temperature) {
        this.temperature = temperature;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Connector.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("energy='" + energy + "'")
                .add("temperature=" + temperature)
                .add("in=" + Arrays.toString(in))
                .add("out=" + Arrays.toString(out))
                .add("center=" + Arrays.toString(center))
                .toString();
    }
}
