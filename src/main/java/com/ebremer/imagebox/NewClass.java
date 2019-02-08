/*
 * Software by Erich Bremer
 * ALL RIGHTS RESERVED
 */

package com.ebremer.imagebox;

import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLService;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Erich Bremer
 */
public class NewClass {
    
    public static void main(String[] args) {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)(org.slf4j.Logger)LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.OFF);
        IFormatReader reader = new ImageReader();
        reader.setGroupFiles(true);
        reader.setMetadataFiltered(true);
        reader.setOriginalMetadataPopulated(true);
        ServiceFactory factory;
        OMEXMLService service;
        MetadataStore store;
        IMetadata meta;
        try {
            factory = new ServiceFactory();
            service = factory.getInstance(OMEXMLService.class);
            reader.setMetadataStore(service.createOMEXMLMetadata(null, null));
            reader.setId("D:\\WSI\\20180504\\001738-000002_02_20180504.vsi");
            //reader.setId("D:\\WSI\\japan\\61618.svs");
            store = reader.getMetadataStore();
            MetadataTools.populatePixels(store, reader, false, false);
            reader.setSeries(0);
            String xml = service.getOMEXML(service.asRetrieve(store));
            meta = service.createOMEXMLMetadata(xml);
        } catch (DependencyException | ServiceException | FormatException | IOException ex) {
            Logger.getLogger(NeoTiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("hasFlattenedResolutions : "+reader.hasFlattenedResolutions());
        System.out.println("num resolutions : "+reader.getResolutionCount());
        reader.setFlattenedResolutions(true);
        
        System.out.println("series count : "+reader.getSeriesCount());

        Hashtable hh = reader.getSeriesMetadata();
        //Enumeration ee = hh.keys();
        //while (ee.hasMoreElements()) {
//            String ya = (String) ee.nextElement();
//            System.out.println("*****>>>>> "+ya);
//        }
        System.out.println(hh.get("MPP"));
        
        for (int j=0;j<reader.getSeriesCount();j++) {
            CoreMetadata big = reader.getCoreMetadataList().get(j);
            System.out.println(j+" >>> "+big.sizeX+" "+big.sizeY+"  "+reader.getResolutionCount());
        }
        //reader.setSeries(6);
        //System.out.println(reader.getResolutionCount());
    }
}
