package ru.tecon.dNet.report.model;

import java.util.StringJoiner;

public class ConsumerModel {

    private final String name;
    private final int id;

    public ConsumerModel(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConsumerModel.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("id=" + id)
                .toString();
    }
}
