package com.ebremer.imagebox;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import static java.lang.Math.abs;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import loci.common.DebugTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.formats.Memoizer;
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
    private ImageReader warp;
    private static final File f = new File("tmp");
    private Memoizer reader;
    private ServiceFactory factory;
    private OMEXMLService service;
    private MetadataStore store;
    private IMetadata meta;
    private OMEXMLMetadataRoot newRoot;
    private int lowerbound = 0;
    private int upperbound = 0;
    private int numi;
    private final int iWidth;
    private final int iHeight;
    private final int[] px;
    private final int[] py;
    private final int[] pr;
    private final int[] pi;
    private final float[] pratio;
    private double mppx;
    private double mppy;
    private boolean borked = false;
    private String status = "";
    private long lastaccessed;
    
    public NeoTiler(String f) {
        DebugTools.enableLogging("ERROR");
        lastaccessed = System.nanoTime();
//        System.out.println("NeoTiler : "+f.getPath()+" : "+x+","+y+","+w+","+h+","+tx+","+ty);
        File cache = new File("cache");
        if (!cache.exists()) {
            cache.mkdir();
        }
        warp = new ImageReader();
        reader = new Memoizer(warp, 0L, new File("cache"));
        reader.setGroupFiles(true);
        reader.setMetadataFiltered(true);
        reader.setOriginalMetadataPopulated(true);
        try {
            factory = new ServiceFactory();
            service = factory.getInstance(OMEXMLService.class);
            reader.setMetadataStore(service.createOMEXMLMetadata(null, null));
            reader.setId(f);
            store = reader.getMetadataStore();
            MetadataTools.populatePixels(store, reader, false, false);
            reader.setSeries(0);
            String xml = service.getOMEXML(service.asRetrieve(store));
            meta = service.createOMEXMLMetadata(xml);
        } catch (DependencyException | ServiceException | IOException ex) {
            Logger.getLogger(NeoTiler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FormatException ex) {
            borked = true;
            status = ex.getMessage();
        }
        if (!borked) {
            newRoot = (OMEXMLMetadataRoot) meta.getRoot();
            numi = reader.getSeriesCount();
            if (f.endsWith(".vsi")) {
                lowerbound = MaxImage(reader);
            }
            Hashtable<String, Object> hh = reader.getSeriesMetadata();
            //Enumeration ee = hh.keys();
        //while (ee.hasMoreElements()) {
//            String ya = (String) ee.nextElement();
//            System.out.println("*****>>>>> "+ya);
//        }
  //          System.out.println(hh.get("MPP"));
            if (hh.containsKey("MPP")) {
                double mpp = Double.parseDouble((String) hh.get("MPP"));
                mppx = mpp;
                mppy = mpp;
            }
            numi = numi - lowerbound;
            px = new int[numi];
            py = new int[numi];
            pr = new int[numi];
            pi = new int[numi];
            pratio = new float[numi];
            CoreMetadata big;
            System.out.println("=============================================================");
            for (int j=0;j<reader.getSeriesCount();j++) {
                big = reader.getCoreMetadataList().get(j);
                System.out.println(j+" >>> "+big.sizeX+","+big.sizeY+" aspect ratio : "+(((float) big.sizeX)/((float)big.sizeY)));
            }
            System.out.println("=============================================================");
            big = reader.getCoreMetadataList().get(lowerbound);
            float ratio = ((float) big.sizeX)/((float) big.sizeY);
            for (int j=lowerbound;j<(numi+lowerbound);j++) {
                big = reader.getCoreMetadataList().get(j);
                int offset = j-lowerbound;
                px[offset] = big.sizeX;
                py[offset] = big.sizeY;
                pr[offset] = px[lowerbound]/px[offset];
                pi[offset] = j;
                float mi = (((float) big.sizeX)/((float)big.sizeY));
                float off = abs((mi-ratio)/ratio);
                System.out.println("OFFNESS : "+off+ " "+ratio);
                if (off>0.01) {
                    px[offset] = 1;
                    System.out.println("nullifying : "+j);
                }
                //System.out.println(offset+" >>> "+pi[offset]+" "+pr[offset]+"  "+px[offset]+","+py[offset]);
            }
            //System.out.println("=============================================================");
            SortImages();
            //lowerbound = 0;
            upperbound = lowerbound + 1;
            System.out.println(upperbound+" "+numi+" "+px[upperbound]);
            while ((upperbound<numi)&&(px[upperbound]>1000)) {
                System.out.println(px[upperbound]);
                upperbound++;
            }
            System.out.println(numi+" IIII : "+upperbound);
            for (int j=0;j<numi;j++) {
                pr[j] = px[0]/px[j];
                System.out.println(j+" >>> "+pi[j]+" "+pr[j]+"  "+px[j]+","+py[j]);
            }
            System.out.println(numi);
            //numi = upperbound - lowerbound;
            System.out.println(numi);
            System.out.println("lower bound : "+lowerbound);
            System.out.println("upper bound : "+upperbound);
            reader.setSeries(lowerbound);
            iWidth = reader.getSizeX();
            iHeight = reader.getSizeY();
    //        System.out.println(iWidth+":::"+iHeight);
        } else {
            iWidth = 0;
            iHeight = 0;            
            px = null;
            py = null;
            pr = null;
            pi = null;
            pratio = null;
        }
    }
    
    public int GetWidth() {
        return iWidth;
    }
    
    public int GetHeight() {
        return iHeight;
    }    
    
    public boolean isBorked() {
        return borked;
    }
    
    public String getStatus() {
        return status;
    }
    
    public long GetLastAccess() {
        return lastaccessed;
    }
    
    public void UpdateLastAccess() {
        lastaccessed = System.nanoTime();
    }
    
    public int MaxImage(Memoizer reader) {
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
        JsonObject model;
        if (borked) {
            model = Json.createObjectBuilder()
                .add("status", status).build();
        } else {
            model = Json.createObjectBuilder()
                .add("height", iHeight)
                .add("width", iWidth)
                .add("mpp-x", mppx)
                .add("mpp-y", mppy)
                .build();
        }
        StringWriter stWriter = new StringWriter();
        try (JsonWriter jsonWriter = Json.createWriter(stWriter)) {
            jsonWriter.writeObject(model);
        }
        return stWriter.toString();
    }
    
    public BufferedImage FetchImage(int x, int y, int w, int h, int tx, int ty) {
        System.out.println("FetchImage : "+x+" "+y+" "+w+" "+h+" "+tx+" "+ty);
        int iratio = w/tx;
        int jj = 0;
        System.out.println("numi : "+numi+" "+iratio);
        while ((jj<numi-1)&&(iratio>pr[jj])) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>> "+jj+"  "+pi[jj]+" "+pr[jj]+"   "+numi+"  "+iratio);
            jj++;
        }
        //System.out.println("J : "+jj);
        //System.out.println(iratio+" picked "+jj+" "+pi[jj]+" "+pr[jj]);
        //int oratio = pr[jj];
        reader.setSeries(pi[jj]);
        //System.out.println("pre ratio : "+reader.getSizeX()+ " "+iWidth);
        double rr = ((double) reader.getSizeX())/((double) iWidth);
        //System.out.println("Ratio : "+rr);
        int gx=(int) (x*rr);
        int gy=(int) (y*rr);
        int gw=(int) (w*rr);
        int gh=(int) (h*rr);
        BufferedImage bi = GrabImage(gx,gy,gw,gh);
        BufferedImage target;
        AffineTransform at = new AffineTransform();
        double scale = (((double) tx)/((double) bi.getWidth()));
        at.scale(scale,scale);
        AffineTransformOp scaleOp =  new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        target = new BufferedImage((int)(gw*scale),(int)(gh*scale),bi.getType());
        scaleOp.filter(bi, target);
        return target;
    }
    
    private BufferedImage GrabImage(int xpos, int ypos, int width, int height) {
        //System.out.println("grab image : "+xpos+ " "+ypos+" "+width+" "+height);
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