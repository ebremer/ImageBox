package com.ebremer.imagebox;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
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
import loci.formats.ImageReader;
import loci.formats.MetadataTools;
import loci.formats.gui.AWTImageTools;
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
    private int x;
    private int y;
    private int w;
    private int h;
    private int tx;
    private int ty;
    private ImageReader reader;
    private ServiceFactory factory;
    private OMEXMLService service;
    private MetadataStore store;
    private IMetadata meta;
    private OMEXMLMetadataRoot newRoot;
    private int lowerbound = 0;
    private int numi;
    private final int iWidth;
    private final int iHeight;
    private final int[] px;
    private final int[] py;
    private final int[] pr;
    private final int[] pi;
    private double mppx;
    private double mppy;
    
    public NeoTiler(File f, int x, int y, int w, int h, int tx, int ty) {
        DebugTools.enableLogging("ERROR");
        System.out.println("NeoTiler : "+f.getPath()+" : "+x+","+y+","+w+","+h+","+tx+","+ty);
        //this.iri = iri;
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
            reader.setId(f.getPath());
            store = reader.getMetadataStore();
            MetadataTools.populatePixels(store, reader, false, false);
            reader.setSeries(0);
            String xml = service.getOMEXML(service.asRetrieve(store));
            meta = service.createOMEXMLMetadata(xml);
        } catch (DependencyException | ServiceException | FormatException | IOException ex) {
            Logger.getLogger(NeoTiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        newRoot = (OMEXMLMetadataRoot) meta.getRoot();
        numi = reader.getSeriesCount();
        if (f.getName().endsWith(".vsi")) {
            lowerbound = MaxImage(reader);
        }
        if (numi>2) {
            numi = numi-4;
        }
        //System.out.println("lower bound : "+lowerbound);
        //System.out.println("upper bound : "+(numi+lowerbound));
        //System.out.println("series count : "+numi);
        Hashtable hh = reader.getSeriesMetadata();
        Enumeration ee = hh.keys();
        //while (ee.hasMoreElements()) {
//            String ya = (String) ee.nextElement();
//            System.out.println("*****>>>>> "+ya);
//        }
        System.out.println(hh.get("MPP"));
        if (hh.containsKey("MPP")) {
            System.out.println("extracting mpp metadata...");
            double mpp = Double.parseDouble((String) hh.get("MPP"));
            mppx = mpp;
            mppy = mpp;
        }
        numi = numi - lowerbound;
        px = new int[numi];
        py = new int[numi];
        pr = new int[numi];
        pi = new int[numi];
        System.out.println("=============================================================");
        for (int j=0;j<reader.getSeriesCount();j++) {
            CoreMetadata big = reader.getCoreMetadataList().get(j);
            System.out.println(j+" >>> "+big.sizeX+","+big.sizeY+" aspect ratio : "+(((double) big.sizeX)/((double)big.sizeY)));
        }
        System.out.println("=============================================================");
        //System.out.println("lower bound : "+lowerbound);
        //System.out.println("upper bound : "+numi);
        for (int j=lowerbound;j<(numi+lowerbound);j++) {
            CoreMetadata big = reader.getCoreMetadataList().get(j);
            int offset = j-lowerbound;
            px[offset] = big.sizeX;
            py[offset] = big.sizeY;
            pr[offset] = px[0]/px[offset];
            pi[offset] = j;
            System.out.println(offset+" >>> "+pi[offset]+" "+pr[offset]+"  "+px[offset]+","+py[offset]);
        }
        SortImages();
        for (int j=0;j<numi;j++) {
            pr[j] = px[0]/px[j];
            System.out.println(j+" >>> "+pi[j]+" "+pr[j]+"  "+px[j]+","+py[j]);
        }
//        if (numi>2) {
//            numi = numi-4;
//        }
        reader.setSeries(lowerbound);
        iWidth = reader.getSizeX();
        iHeight = reader.getSizeY();
        System.out.println(iWidth+":::"+iHeight);
    }
    
    public int MaxImage(ImageReader reader) {
        int ii = 0;
        int maxseries = 0;
        int maxx = Integer.MIN_VALUE;
        for (CoreMetadata c : reader.getCoreMetadataList()) {
            if (c.sizeX>maxx) {
                maxseries = ii;
                maxx = c.sizeX;
            }
            ii++;
        }
    return maxseries;
  }
    
    public void SortImages() {
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int i = 0; i<numi-1; i++) {
                if (px[i]<px[i+1]) {
                    int temp = px[i];
                    px[i] = px[i+1];
                    px[i+1] = temp;
                    temp = py[i];
                    py[i] = py[i+1];
                    py[i+1] = temp;
                    temp = pi[i];
                    pi[i] = pi[i+1];
                    pi[i+1] = temp;
                    sorted = false;
                }
            }
        }
    }
    
    public String GetImageInfo() {
        String info = "{\"height\": "+iHeight+", \"width\": "+iWidth+", \"mpp-x\": "+mppx+", \"mpp-y\": "+mppy+"}";
        return info;
    }
    
    public synchronized BufferedImage FetchImage(int x, int y, int w, int h, int tx, int ty) {
        //System.out.println("FetchImage : "+x+" "+y+" "+w+" "+h+" "+tx+" "+ty);
        int iratio = w/tx;
        int jj = 0;
        while ((jj<numi-1)&&(iratio>pr[jj])) {
            //System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>> "+jj+"  "+pi[jj]+" "+pr[jj]+"   "+numi+"  "+iratio);
            jj++;
        }
        //System.out.println(iratio+" picked "+jj+" "+pi[jj]+" "+pr[jj]);
        //int oratio = pr[jj];
        reader.setSeries(pi[jj]);
        double rr = ((double) reader.getSizeX())/((double) iWidth);
        int gx=(int) (x*rr);
        int gy=(int) (y*rr);
        int gw=(int) (w*rr);
        int gh=(int) (h*rr);
        
        //System.out.println("B: "+gx+" "+gw+" "+reader.getSizeX()+" "+gy+" "+gh+" "+reader.getSizeY());
        //if ((gx+gw)>reader.getSizeX()) {
//            gw = (gx+gw)-reader.getSizeX();
  //      }
    //    if ((gy+gh)>reader.getSizeY()) {
      //      gh = (gy+gh)-reader.getSizeY();
        //}
        //System.out.println("A: "+gx+" "+gw+" "+reader.getSizeX()+" "+gy+" "+gh+" "+reader.getSizeY());
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
            bb = AWTImageTools.makeImage(buf, reader.isInterleaved(), meta, 0);
        } catch (FormatException | IOException ex) {
            Logger.getLogger(NeoTiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        return bb;
    }
}