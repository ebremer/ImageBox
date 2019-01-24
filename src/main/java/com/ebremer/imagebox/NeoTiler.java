package com.ebremer.imagebox;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import loci.common.DebugTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.gui.AWTImageTools;
import loci.formats.in.OMETiffReader;
import loci.formats.in.PyramidTiffReader;
import loci.formats.in.SVSReader;
import loci.formats.in.TiffReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLService;
import ome.xml.meta.OMEXMLMetadataRoot;
import ome.xml.model.primitives.PositiveInteger;

/**
 *
 * @author erich
 */
public class NeoTiler {
    private String iri;
    private int x;
    private int y;
    private int w;
    private int h;
    private int tx;
    private int ty;
    private IFormatReader reader;
    private ServiceFactory factory;
    private OMEXMLService service;
    private MetadataStore store;
    private IMetadata meta;
    private OMEXMLMetadataRoot newRoot;
    private int numi;
    private int iWidth;
    private int iHeight;
    private int[] px;
    private int[] py;
    private long[] pa;
    private int[] pr;
    private double mppx;
    private double mppy;
    
    public NeoTiler(String iri, int x, int y, int w, int h, int tx, int ty) {
        DebugTools.enableLogging("ERROR");
        System.out.println("NeoTiler : "+iri+" : "+x+","+y+","+w+","+h+","+tx+","+ty);
        this.iri = iri;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.tx = tx;
        this.ty = ty;
        reader = new ImageReader();
        reader.setGroupFiles(true);
        reader.setMetadataFiltered(true);
        reader.setOriginalMetadataPopulated(true);
        try {
            factory = new ServiceFactory();
            service = factory.getInstance(OMEXMLService.class);
            reader.setMetadataStore(service.createOMEXMLMetadata(null, null));
            reader.setId(iri);
            store = reader.getMetadataStore();
            MetadataTools.populatePixels(store, reader, false, false);
            reader.setSeries(0);
            String xml = service.getOMEXML(service.asRetrieve(store));
            meta = service.createOMEXMLMetadata(xml);
        } catch (DependencyException | ServiceException | FormatException | IOException ex) {
            Logger.getLogger(NeoTiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        newRoot = (OMEXMLMetadataRoot) meta.getRoot();
        iWidth = reader.getSizeX();
        iHeight = reader.getSizeY();
        numi = reader.getCoreMetadataList().size();
        Hashtable hh = reader.getSeriesMetadata();
        
        Enumeration ee = hh.keys();
        while (ee.hasMoreElements()) {
            String ya = (String) ee.nextElement();
            System.out.println("*****>>>>> "+ya);
        }
        System.out.println(hh.get("MPP"));
        if (hh.containsKey("MPP")) {
            System.out.println("extracting mpp metadata...");
            double mpp = Double.parseDouble((String) hh.get("MPP"));
            mppx = mpp;
            mppy = mpp;
        }
        px = new int[numi];
        py = new int[numi];
        pa = new long[numi];
        pr = new int[numi];
        System.out.println("series count : "+reader.getSeriesCount());
        for (int j=0;j<=reader.getSeriesCount()-1;j++) {
            CoreMetadata big = reader.getCoreMetadataList().get(j);
            px[j] = big.sizeX;
            py[j] = big.sizeY;
            pr[j] = px[0]/px[j];
            System.out.println(j+" >>> "+pr[j]+"  "+px[j]+","+py[j]);
        }

    }
    
    public String GetImageInfo() {
        String info = "{\"height\": "+iHeight+", \"width\": "+iWidth+", \"mpp-x\": "+mppx+", \"mpp-y\": "+mppy+"}";
        //System.out.println(info);
        return info;
    }
    
    public synchronized BufferedImage FetchImage(int x, int y, int w, int h, int tx, int ty) {
        //System.out.println(x+" "+y+" "+w+" "+h+" "+tx+" "+ty);
        int iratio = w/tx;
        int jj = 0;
        while ((jj<4)&&(iratio>pr[jj])) {
            jj++;
        }
        //System.out.println(iratio+" picked "+jj+" "+pr[jj]);
        int oratio = pr[jj];
        reader.setSeries(jj);
        int gx=x/oratio;
        int gy=y/oratio;
        int gw=w/oratio;
        int gh=h/oratio;
        if ((gx+gw)>px[jj]) {
            gw = (gx+gw)-px[jj];
        }
        if ((gy+gh)>py[jj]) {
            gh = (gy+gh)-py[jj];
        }        
        BufferedImage bi = GrabImage(gx,gy,gw,gh);
        BufferedImage target;
        AffineTransform at = new AffineTransform();
        double scale = (((double) tx)/((double) bi.getWidth()));
        at.scale(scale,scale);
        AffineTransformOp scaleOp =  new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        target = new BufferedImage(tx,ty,bi.getType());
        scaleOp.filter(bi, target);
        return target;
    }
    
    private BufferedImage GrabImage(int xpos, int ypos, int width, int height) {
        //System.out.println("GrabImage "+xpos+" "+ypos+" "+width+" "+height);
        meta.setRoot(newRoot);
        meta.setPixelsSizeX(new PositiveInteger(width), 0);
        meta.setPixelsSizeY(new PositiveInteger(height), 0);
        byte[] buf = null;
        BufferedImage bb = null;
        try {
            buf = reader.openBytes(0, xpos, ypos, width, height);
            bb = AWTImageTools.makeImage(buf, false, meta, 0);
        } catch (FormatException | IOException ex) {
            Logger.getLogger(NeoTiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return bb;
    }
}