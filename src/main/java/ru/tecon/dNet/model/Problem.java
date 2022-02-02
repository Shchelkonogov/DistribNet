package ru.tecon.dNet.model;

import java.io.Serializable;
import java.util.Objects;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Problem problem = (Problem) o;
        return display == problem.display &&
                Objects.equals(name, problem.name) &&
                Objects.equals(color, problem.color) &&
                Objects.equals(problemId, problem.problemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, color, display, problemId);
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
