package com.alon.exchangetrackerserver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class ExchangeTrackerRatesThread extends Thread {

    private final String fixerApi = "http://api.fixer.io/latest?base=%s&symbols=%s";
    private final double INC = 0.01;


    private double version = 0.01;
    private volatile boolean needsUpdate = false;
    private static ExchangeTrackerRatesThread instance;

    static ExchangeTrackerRatesThread getInstance() {
        return instance == null ? new ExchangeTrackerRatesThread() : instance;
    }

    private ExchangeTrackerRatesThread() {
        instance = this;
        System.out.println("Rate Fetcher Thread started.");
    }

    @SuppressWarnings("InfiniteLoopStatement")
    @Override
    public void run() {
        while (true) {
            if (needsUpdate) {
                downloadRates();
                needsUpdate = false;
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void downloadRates() {
        HashMap<String, Set<String>> trackerForeignAndBases = getAllNeededCurrencies();
        boolean incVersion = false;
        for (Map.Entry<String, Set<String>> entry : trackerForeignAndBases.entrySet()) {
            boolean incV = downloadSpecificRates(entry.getKey(), entry.getValue());
            if (!incVersion)
                incVersion = incV;
        }
        if (incVersion)
            version += INC;
    }


    private HashMap<String, Set<String>> getAllNeededCurrencies() {
        HashMap<String, Set<String>> map = new HashMap<>();
        for (String base : ExchangeTrackerTrackers.getTackerBases())
            map.put(base, ExchangeTrackerTrackers.getForeignsForBase(base));
        return map;
    }

    private boolean downloadSpecificRates(String base, Set<String> symbols) {
        String actualURL = String.format(fixerApi, base, constructSymbols(symbols));
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(actualURL).openConnection();
            conn.connect();

            InputStream stream = conn.getInputStream();

            int aRead;
            byte[] buffer = new byte[1024];
            StringBuilder response = new StringBuilder();

            while ((aRead = stream.read(buffer)) != -1)
                response.append(new String(buffer, 0, aRead));

            stream.close();
            conn.disconnect();

            JSONObject currencies = new JSONObject(response.toString());
            JSONObject rates = currencies.getJSONObject("rates");
            return ExchangeTrackerRates.updateConversionRates(base, rates);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String constructSymbols(Set<String> symbols) {
        StringBuilder builder = new StringBuilder();
        for (String symbol : symbols)
            builder.append(symbol).append(",");
        builder.deleteCharAt(builder.lastIndexOf(","));
        return builder.toString();
    }

    double getVersion() {
        return version;
    }

    void needsUpdate() {
        needsUpdate = true;
    }
}
