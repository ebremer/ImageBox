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
    String webfiles = Settings.webfiles;
    
    public W3Cmf() throws Exception {
        startup();
    }
    
    public W3Cmf(String path) throws Exception {
        webfiles = path;
        startup();
    }
    
    public void SetWebFilesPath(String path) {
        System.out.println("SetWebFilesPath...");
        System.out.println("Current locations : "+webfiles);
        System.out.println("Attempting to set to : "+path);
        webfiles = path;
        System.out.println("new path is : "+webfiles);
    }
    
    private void startup() throws Exception {
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(Settings.port);
        server.addConnector(connector);    
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        SessionHandler sessions = context.getSessionHandler();
        SessionCache cache = new DefaultSessionCache(sessions);
        cache.setSessionDataStore(new NullSessionDataStore());
        sessions.setSessionCache(cache);
        context.addServlet(iboxServlet.class, "/iiif/*");
        ServletHolder holderHome = new ServletHolder("static-home", DefaultServlet.class);
        holderHome.setInitParameter("resourceBase",webfiles);
        holderHome.setInitParameter("dirAllowed","true");
        holderHome.setInitParameter("pathInfoOnly","true");
        context.addServlet(holderHome,"/*");
        server.start();
        server.join();  
    }

    public static void main(String[] args) throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)(Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.OFF);
        W3Cmf engine = new W3Cmf();
    }
}