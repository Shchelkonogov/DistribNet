package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.StringJoiner;

public class Problem implements Serializable {

    private String name;
    private String color;

    public Problem(String name, String color) {
        this.name = name;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Problem.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("color='" + color + "'")
                .toString();
    }
}
