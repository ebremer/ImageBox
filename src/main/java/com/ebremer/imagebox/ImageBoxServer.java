/*
 * Software by Erich Bremer
 * ALL RIGHTS RESERVED
 */

package com.ebremer.imagebox;

import java.net.BindException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;

/**
 *
 * @author Erich Bremer
 */
public class ImageBoxServer extends Thread {
    
    String webfiles;
    
    public ImageBoxServer() {
        webfiles = "files/webfiles";
        System.out.println("Initializing ImageBoxServer with default webfiles path : "+webfiles);
    }
    
    public ImageBoxServer(String path) {
        System.out.println("Initializing ImageBoxServer with webfiles path : "+path);
        webfiles = path;
    }
      
    @Override
    public void run() {
        System.out.println("Starting ImageBox Version 1.1.0");
        W3Cmf srv = null;
        try {
            srv = new W3Cmf(webfiles);
        } catch (BindException ex) {
            final JFrame parent = new JFrame();
            JButton button = new JButton();
            button.setText("Other instance detected!  Shutting down...");
            parent.add(button);
            parent.pack();
            parent.setVisible(true);
            button.addActionListener((java.awt.event.ActionEvent evt) -> {
                System.exit(0);
            });
        } catch (Exception ex) {
            Logger.getLogger(ImageBoxServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                srv.server.stop();
            } catch (Exception ex) {
                Logger.getLogger(ImageBoxServer.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
    }
}
