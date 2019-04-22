package com.mythicalcreaturesoftware.pcstatsmonitorclient.utils;

public class DataUtils {

    private static double KILOBYTE_SIZE = 1024;
    private static double PERCENTAGE_TOTAL= 100;

    public static Double kilobytesToMegabytes(String value) {
        double result;

        value = value.replace("kB", "").trim();
        result = Double.parseDouble(value);

        return result/KILOBYTE_SIZE;
    }

    public static Double calculatePercentage (double total, double amount) {
        return (amount/total)*PERCENTAGE_TOTAL;
    }
}
