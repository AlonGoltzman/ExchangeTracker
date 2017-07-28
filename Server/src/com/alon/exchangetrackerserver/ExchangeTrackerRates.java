package com.alon.exchangetrackerserver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.HashMap;

/**
 * Created by Alon on 6/25/2017.
 */
final class ExchangeTrackerRates {
    private static File conversionBase;

    static void init(String base) {
        conversionBase = new File(base);
    }

    static boolean updateConversionRates(String base, JSONObject rates) {
        if (conversionBase == null) {
            System.err.println("ExchangeTrackerRates not initialized.");
            throw new IllegalStateException();
        }
        File jsonLocation = new File(conversionBase, base);
        JSONObject currentRates = loadConversionRates(base);
        //If it's the same no reason to update the rates.
        if (currentRates != null)
            if (rates.toString().toLowerCase().trim().equals(currentRates.toString().toLowerCase().trim()))
                return false;
        try {
            OutputStream stream = new FileOutputStream(jsonLocation);
            stream.write(rates.toString().getBytes());
            stream.flush();
            stream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    private static JSONObject loadConversionRates(String base) {
        if (conversionBase == null) {
            System.err.println("ExchangeTrackerRates not initialized.");
            throw new IllegalStateException();
        }
        File jsonLocation = new File(conversionBase, base);
        if (!jsonLocation.exists())
            return null;
        try {
            InputStream stream = new FileInputStream(jsonLocation);

            int aRead;
            byte[] buffer = new byte[1024];
            StringBuilder builder = new StringBuilder();

            while ((aRead = stream.read(buffer)) != -1)
                builder.append(new String(buffer, 0, aRead));

            stream.close();
            return new JSONObject(builder.toString());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;

    }

    static String rates(String base, String foreign) {
        JSONObject rates = loadConversionRates(base);
        if (rates != null)
            if (rates.has(foreign)) {
                try {
                    return String.valueOf(rates.getDouble(foreign));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        return "-1";
    }
}
