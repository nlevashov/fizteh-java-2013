package ru.fizteh.fivt.students.lizaignatyeva.database.httpserver;

import ru.fizteh.fivt.storage.structured.Storeable;
import ru.fizteh.fivt.students.lizaignatyeva.database.MyTable;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PutServlet extends HttpServlet {
    private Database database;

    public PutServlet(Database database) {
        this.database = database;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String transactionId = req.getParameter("tid");
        if (transactionId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No tid provided");
            return;
        }
        String key = req.getParameter("key");
        if (key == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No key provided");
            return;
        }
        String value = req.getParameter("value");
        if (value == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No value provided");
            return;
        }
        MyTable table = database.getTransaction(transactionId);
        if (table == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No such transaction");
            return;
        }
        Storeable realValue;
        try {
            realValue = database.tableProvider.deserialize(table, value);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect value provided");
            return;
        }
        Storeable oldValue;
        try {
            oldValue = table.put(key, realValue);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to put value: " + e.getMessage());
            return;
        }
        if (oldValue == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Old value was null");
            return;
        }
        String result;
        try {
            result = database.tableProvider.serialize(table, oldValue);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to deserialize: " + e.getMessage());
            return;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.setCharacterEncoding("UTF8");
        resp.getWriter().println(result);
    }
}
