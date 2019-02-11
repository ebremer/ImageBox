/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebremer.imagebox;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author erich
 */
public class ImageReaderPool {
    private final HashMap pool = new HashMap();
    private final Timer timer = new Timer();
    private File f = new File("cache");
    
    ImageReaderPool() {
       
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //System.out.println("purging old objects...");
            }
        }, 60*60*1000, 60*60*1000);
    }
    
    private synchronized NeoTiler GetReaderFromPool(String id) {
        NeoTiler reader;
        if (pool.containsKey(id)) {
            ArrayList list = (ArrayList) pool.get(id);                    
            reader = (NeoTiler) list.remove(0);
            if (list.isEmpty())  {
                pool.remove(id);
            }
        } else {
            //System.out.println("creating new neotiler : "+id);
            reader = new NeoTiler(id);
        }
        return reader;
    }
    
    public NeoTiler GetReader(String id) {
        NeoTiler reader = GetReaderFromPool(id);
        if (reader==null) {
            reader = new NeoTiler(id);
        }
        return reader;
    }
    
    public synchronized void ReturnReader(String id, NeoTiler reader) {
        if (pool.containsKey(id)) {
            ArrayList list = (ArrayList) pool.get(id);            
            list.add(reader);
            //System.out.println("pool size : "+list.size());
        } else {
            ArrayList list = new ArrayList();
            list.add(reader);
            pool.put(id, list);
            //System.out.println("pool size : "+list.size());
        }        
    }
}
