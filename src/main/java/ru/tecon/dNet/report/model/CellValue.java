package ru.tecon.dNet.report.model;

import java.util.StringJoiner;

/**
 * Класс описывающий значения
 * @author Maksim Shchelkonogov
 */
public class CellValue {

    private String value;
    private int colorIndex;

    public CellValue(String value, int colorIndex) {
        this.value = value;
        this.colorIndex = colorIndex;
    }

    public String getValue() {
        return value;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CellValue.class.getSimpleName() + "[", "]")
                .add("value='" + value + "'")
                .add("colorIndex=" + colorIndex)
                .toString();
    }
}
