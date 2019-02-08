/*
 * Software by Erich Bremer
 * ALL RIGHTS RESERVED
 */

package com.ebremer.imagebox;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.Memoizer;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Erich Bremer
 */
public class Performance {
    private long delta;
    private Memoizer m;
    private File f = new File("tmp");
    
    Performance() {
        long start = System.nanoTime();
        ImageReader m = new ImageReader();
        //m = new Memoizer(reader, 0L, f);
        try {
            m.setId("D:\\WSI\\20180504\\001738-000002_02_20180504.vsi");
        } catch (FormatException | IOException ex) {
            Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, null, ex);
        }
        delta = System.nanoTime()-start;
    }

    public void Turbo() {
        long start = System.nanoTime();
        ImageReader warp = new ImageReader();
        Memoizer m = new Memoizer(warp, 0L, f);
        try {
            m.setId("D:\\WSI\\20180504\\001738-000002_02_20180504.vsi");
        } catch (FormatException | IOException ex) {
            Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, null, ex);
        }
        delta = System.nanoTime()-start;
        try {
            m.close();
        } catch (IOException ex) {
            Logger.getLogger(Performance.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public long getDelta() {
        return delta;
    }
    
    public static void main(String[] args) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)(org.slf4j.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.OFF);
        new Performance().Turbo();
        long total = 0;
        for (int i=0; i<10000; i++) {
            Performance p = new Performance();
            total = total + p.getDelta();
        }
        Performance p = new Performance();
        System.out.println("time : "+total);
    }

}
    