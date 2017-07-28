package com.alon.exchangetrackerserver;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by Alon on 6/25/2017.
 */
public class ExchangeTrackerDeploymentListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ExchangeTrackerRatesThread.getInstance().start();
        ExchangeTrackerRates.init(servletContextEvent.getServletContext().getRealPath(""));
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        ExchangeTrackerRatesThread.getInstance().interrupt();
    }
}
