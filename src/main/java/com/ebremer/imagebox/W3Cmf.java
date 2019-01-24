package com.ebremer.imagebox;

import ch.qos.logback.classic.Level;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.NullSessionDataStore;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author erich
 */
public class W3Cmf {
    final Server server = new Server();
    int port = 8888;
    
    W3Cmf() throws Exception {
        startup();
    }
    
    private void startup() throws Exception {       
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);
     
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        SessionHandler sessions = context.getSessionHandler();
        SessionCache cache = new DefaultSessionCache(sessions);
        cache.setSessionDataStore(new NullSessionDataStore());
        sessions.setSessionCache(cache);

        context.addServlet(iboxServlet.class, "/");
        ServletHolder holderHome = new ServletHolder("static-home", DefaultServlet.class);
        holderHome.setInitParameter("resourceBase","files/webfiles");
        holderHome.setInitParameter("dirAllowed","true");
        holderHome.setInitParameter("pathInfoOnly","true");
        context.addServlet(holderHome,"/files/*");
        server.start();
        server.join();  
    }

    public static void main(String[] args) throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)(Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);
        W3Cmf engine = new W3Cmf();
    }
}

        /* sticking this here out of the way.....
        This chunk of code sets resource directory to jar file.  Saving for future use.  holderHome.setInitParameter("resourceBase",webDir);
        URL url = this.getClass().getClassLoader().getResource("webfiles");
        String webDir = null;
        if (url != null) {
            webDir = url.toExternalForm();
        } else {
            System.out.println("nothing found");
        }
        System.out.println(webDir);
        */