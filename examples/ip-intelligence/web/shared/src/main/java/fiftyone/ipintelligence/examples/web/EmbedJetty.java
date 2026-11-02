/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2026 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.ipintelligence.examples.web;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import javax.servlet.Servlet;
import java.util.Map;
import java.util.Scanner;

public class EmbedJetty {
    public static void runWebApp(String resourceBase, int port) throws Exception {
        Server server = startWebApp(resourceBase, port);
        System.out.format("Browse to http://localhost:%d using a 'private' window in your browser\n" +
                "Hit enter to stop server:", port);
        new Scanner(System.in).nextLine();
        server.stop();
    }

    public static Server startWebApp(String resourceBase, int port) throws Exception {
        Server server = new Server(port);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        // Create a WebAppContext.
        WebAppContext context = new WebAppContext();
        // Configure the path of the packaged web application (file or directory).
        context.setResourceBase(resourceBase);
        context.setDescriptor("WEB-INF/web.xml");

        // Link the context to the server.
        server.setHandler(context);

        // Ensure the connector socket is released promptly and gracefully when
        // the server is stopped, so a subsequent bind on the same port (e.g.
        // when tests run back-to-back) does not hit "Address already in use".
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);

        server.start();

        // Wait for the server to be fully started and ready to accept connections
        // This ensures all servlets and filters are initialized before returning
        waitForServerReady(server, 30000); // 30 second timeout

        return server;
    }

    /**
     * Returns the local TCP port the server's first connector is actually
     * listening on. When the server is started with port {@code 0} the OS
     * assigns a free ephemeral port; tests should read it back via this method
     * rather than assuming a fixed port.
     *
     * @param server a started Jetty server
     * @return the bound local port
     */
    public static int boundPort(Server server) {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    /**
     * Stops the server and blocks until it has fully terminated, ensuring the
     * listening socket is released before the method returns.
     *
     * @param server the server to stop (ignored if null)
     */
    public static void stopAndJoin(Server server) throws Exception {
        if (server != null) {
            server.stop();
            server.join();
        }
    }

    /**
     * Wait for the server to be fully started and ready to accept connections.
     * This includes waiting for all connectors to be started and the context to be available.
     *
     * @param server the Jetty server to wait for
     * @param timeoutMs maximum time to wait in milliseconds
     * @throws Exception if server doesn't become ready within timeout
     */
    private static void waitForServerReady(Server server, long timeoutMs) throws Exception {
        long startTime = System.currentTimeMillis();
        long timeout = startTime + timeoutMs;

        while (System.currentTimeMillis() < timeout) {
            // Check if server is started
            if (!server.isStarted()) {
                Thread.sleep(50);
                continue;
            }

            // Check if all connectors are started
            boolean allConnectorsStarted = true;
            for (Connector connector : server.getConnectors()) {
                if (!connector.isStarted()) {
                    allConnectorsStarted = false;
                    break;
                }
            }

            if (!allConnectorsStarted) {
                Thread.sleep(50);
                continue;
            }

            // Check if handler (context) is started
            if (server.getHandler() != null && !server.getHandler().isStarted()) {
                Thread.sleep(50);
                continue;
            }

            // Server is ready
            return;
        }

        throw new Exception("Server did not become ready within " + timeoutMs + "ms");
    }

    public static void runServlet(String contextPath, int port, Class<? extends Servlet> servlet,
                                  Map<String, String> initParams) throws Exception {
        Server server = startServlet(contextPath, port, servlet, initParams);
        System.out.format("Browse to http://localhost:%d using a 'private' window in your browser\n" +
                "Hit enter to stop server:", port);
        new Scanner(System.in).nextLine();
        server.stop();
    }

    public static Server startServlet(String contextPath, int port,
                                      Class<? extends Servlet> servlet,
                                      Map<String, String> initParams) throws Exception {
        Server server = new Server(port);
        Connector connector = new ServerConnector(server);
        server.addConnector(connector);

        // Create a ServletContextHandler with contextPath.
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setSessionHandler(new SessionHandler());
        context.setContextPath("/");

        // Add the Servlet to the context.
        ServletHolder servletHolder = context.addServlet(servlet, contextPath);
        // Configure the Servlet with init-parameters.
        servletHolder.setInitParameters(initParams);

        // Link the context to the server.
        server.setHandler(context);

        // Release the connector socket promptly and gracefully on stop (see
        // startWebApp for rationale).
        server.setStopAtShutdown(true);
        server.setStopTimeout(5000);

        server.start();

        // Wait for the server to be fully started and ready to accept connections
        waitForServerReady(server, 30000); // 30 second timeout

        return server;
    }
}
