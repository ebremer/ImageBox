/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ebremer.imagebox;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.formats.FormatException;

/**
 *
 * @author erich
 */
public class bug843434 {
    
    public static void main(String[] args) {
        try {
            NDPIReader reader = new NDPIReader();
            reader.setId("/svs/Slide-0027572_2267-CST00574-07.ndpi");
            System.out.println(reader.getSizeX()+"x"+reader.getSizeY());
            reader.setSeries(0);
            byte[] buf = reader.openBytes(0, 0, 0, 4096, 4096);
            System.out.println(buf.length);
        } catch (FormatException ex) {
            Logger.getLogger(bug843434.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(bug843434.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
