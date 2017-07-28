package com.alon.exchangetrackerserver;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivateKey;

import static com.alon.exchangetracker.commons.ExchangeTrackerConstants.LOG_INFO;
import static com.alon.exchangetrackerserver.ExchangeTrackerUIDList.addNewUID;
import static com.alon.exchangetrackerserver.ExchangeTrackerUtility.*;

/**
 * Created by Alon on 6/23/2017.
 */
public class ExchangeTrackerUIDFetcher extends HttpServlet {
    @Override
    public void init() throws ServletException {
        super.init();
        ExchangeTrackerUIDList.start();
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureOS(request.getHeader("User-Agent"))) {
            response.sendError(400);
            return;
        }
        if (!sendUID(response.getWriter()))
            response.sendError(500);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!ensureOS(request.getHeader("User-Agent"))) {
            response.sendError(400);
            return;
        }
        if (!sendUID(response.getWriter()))
            response.sendError(500);
    }

    private boolean sendUID(PrintWriter writer) {
        PrivateKey key = loadKey(getServletContext());
        if (key == null)
            return false;
        String newUID = addNewUID();
        Log(LOG_INFO, "Issued new UID:" + newUID);
        String encrypted = encrypt(newUID, key);
        if (encrypted == null)
            return false;
        writer.write(encrypted);
        return true;
    }
}
