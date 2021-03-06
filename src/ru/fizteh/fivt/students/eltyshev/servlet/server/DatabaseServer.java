package ru.fizteh.fivt.students.eltyshev.servlet.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import ru.fizteh.fivt.students.eltyshev.servlet.database.TransactionManager;

import java.io.IOException;

public class DatabaseServer {
    private static final int DEFAULT_PORT = 8080;
    private Server server;
    private TransactionManager manager;

    public DatabaseServer(TransactionManager manager) {
        this.manager = manager;
    }

    public void start(int port) throws Exception {
        if (server != null && server.isStarted()) {
            throw new IllegalStateException("already started");
        }

        if (port == -1) {
            port = DEFAULT_PORT;
        }

        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        context.addServlet(new ServletHolder(new BeginServlet(manager)), Constants.BEGIN);
        context.addServlet(new ServletHolder(new GetServlet(manager)), Constants.GET);
        context.addServlet(new ServletHolder(new PutServlet(manager)), Constants.PUT);
        context.addServlet(new ServletHolder(new CommitServlet(manager)), Constants.COMMIT);
        context.addServlet(new ServletHolder(new RollbackServlet(manager)), Constants.ROLLBACK);
        context.addServlet(new ServletHolder(new SizeServlet(manager)), Constants.SIZE);
        context.addServlet(new ServletHolder(new TestServlet()), Constants.TEST);

        server.setHandler(context);
        server.start();
    }

    public int getPort() {
        return server.getConnectors()[0].getPort();
    }

    public void stop() throws IOException {
        if (server == null || !server.isStarted()) {
            throw new IllegalStateException("not started");
        }
        try {
            server.stop();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}
