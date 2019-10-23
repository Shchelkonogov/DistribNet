package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.StringJoiner;

public class Problem implements Serializable {

    private String name;
    private String color;
    private boolean display;
    private Integer problemId;

    public Problem(String name, String color, boolean display, Integer problemId) {
        this.name = name;
        this.color = color;
        this.display = display;
        this.problemId = problemId;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public boolean isDisplay() {
        return display;
    }

    public Integer getProblemId() {
        return problemId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Problem.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("color='" + color + "'")
                .add("display=" + display)
                .add("problemId=" + problemId)
                .toString();
    }
}
