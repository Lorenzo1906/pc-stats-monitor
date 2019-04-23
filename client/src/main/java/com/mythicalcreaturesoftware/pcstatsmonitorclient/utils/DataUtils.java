package com.mythicalcreaturesoftware.pcstatsmonitorclient.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}
