package com.mythicalcreaturesoftware.pcstatsmonitorserver.util;

import java.util.List;

public class Utils {

    private static String OS = null;

    public static double average(List<Double> values) {
        double sumTemp = 0d;
        for (Double value : values) {
            sumTemp += value;
        }

        return sumTemp / values.size();
    }

    public static String getOsName() {
        if(OS == null) { OS = System.getProperty("os.name"); }
        return OS;

    }

    public static int getOsValue(String name) {
        String[] cases = {"Windows", "Mac", "Linux"};

        for(int i = 0; i < cases.length; i++) {
            if(name.startsWith(cases[i])){
                return i;
            }
        }

        return -1;
    }
}
