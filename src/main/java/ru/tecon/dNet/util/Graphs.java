package ru.tecon.dNet.util;

public class Graphs {

    public static final String CO = "ЦО";
    public static final String GVS = "ГВС";
    public static final String VENT = "ВЕНТ";
    public static final String TC = "ТС";

    public static String getSumNamePrefix(String name) {
        switch (name) {
            case CO:
            case VENT: return "G";
            case GVS: return "V";
            default: return "";
        }
    }
}
