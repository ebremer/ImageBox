package com.ebremer.imagebox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author erich
 */
public class ImageReaderPool {
    private final ConcurrentHashMap<String, IRL> pool;
    private final Timer timer = new Timer();
    //private final File f = new File("cache");
    
    ImageReaderPool() {
        this.pool = new ConcurrentHashMap<>();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Iterator<String> i = pool.keySet().iterator();
                while (i.hasNext()) {
                    String tag = i.next();
                    IRL irl = pool.get(tag);
                    long age = ((System.nanoTime()-irl.getLastAccess())/1000000000);
                    if (age>Settings.MaxAgeReaderPool) {
                        //System.out.println("purging "+tag);
                        RemovePool(tag);
                    }
                    //System.out.println(tag + "  "+ age);
                }
            }
        }, Settings.ReaderPoolScanDelay, Settings.ReaderPoolScanRate);
    }
    
    private synchronized NeoTiler GetReaderFromPool(String id) {
        //System.out.println("GetReaderFromPool "+id);
        NeoTiler reader;
        if (pool.containsKey(id)) {
            //System.out.println("Found Reader in pool! : "+id);
            ArrayList<IRO> list = pool.get(id).getPool();
            IRO iro = list.remove(0);
            reader = iro.getNeoTiler();
            if (list.isEmpty())  {
                pool.remove(id);
            }
        } else {
            reader = new NeoTiler(id);
        }
        return reader;
    }
    
    public NeoTiler GetReader(String id) {
        //System.out.println("GetReader "+id);
        NeoTiler reader = GetReaderFromPool(id);
        if (reader==null) {
            reader = new NeoTiler(id);
        }
        return reader;
    }

    public synchronized void RemovePool(String id) {
        pool.remove(id);
    }

    public synchronized void ReturnReader(String id, NeoTiler reader) {
        //System.out.println("ReturnReader "+id);
        if (pool.containsKey(id)) {
            ArrayList<IRO> list = pool.get(id).getPool();
            list.add(new IRO(reader));
          //  System.out.println("pool size [e]: "+list.size());
        } else {
            //System.out.println("creating new list...");
            ArrayList<IRO> list = new ArrayList<>();
            list.add(new IRO(reader));
            pool.put(id, new IRL(list));
            //System.out.println("pool size [ne]: "+list.size());
        }        
    }
}