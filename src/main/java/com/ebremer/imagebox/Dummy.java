package com.ebremer.imagebox;

import java.io.RandomAccessFile;

/**
 *
 * @author erich
 */
public class Dummy {
    public static void main(String args[]) throws Exception {
        //RandomAccessFile f = new RandomAccessFile("/svs/dummy.svs", "rw");
        //long filesize = Integer.MAX_VALUE;
        //filesize++;
        //f.setLength(filesize);
        byte k = -6;
        System.out.println(Integer.toHexString(0xf&k));
    }
}