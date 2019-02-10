/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebremer.imagebox;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author erich
 */
public class ImageReaderPool {
    private final HashMap pool = new HashMap();
    
    public synchronized NeoTiler GetReader(String id) {
        NeoTiler reader;
        if (pool.containsKey(id)) {
            ArrayList list = (ArrayList) pool.get(id);                    
            reader = (NeoTiler) list.remove(0);
            if (list.isEmpty())  {
                pool.remove(id);
            }
        } else {
            reader = new NeoTiler(id);
        }
        return reader;
    }
    
    public synchronized void ReturnReader(String id, NeoTiler reader) {
        if (pool.containsKey(id)) {
            ArrayList list = (ArrayList) pool.get(id);            
            list.add(reader);
            System.out.println("pool size : "+list.size());
        } else {
            ArrayList list = new ArrayList();
            list.add(reader);
            pool.put(id, list);
            System.out.println("pool size : "+list.size());
        }        
    }
}
