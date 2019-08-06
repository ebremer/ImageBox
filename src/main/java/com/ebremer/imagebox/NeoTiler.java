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
import loci.common.DebugTools;
import loci.common.Location;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.MetadataTools;
import loci.formats.gui.AWTImageTools;
import loci.formats.in.SVSReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLService;
import ome.xml.meta.OMEXMLMetadataRoot;
import ome.xml.model.primitives.PositiveInteger;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;

/**
 *
 * @author erich
 */
public class NeoTiler {
    private IFormatReader warp;
    private static final File f = new File("tmp");
    //private Memoizer reader;
    private SVSReader reader;
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
        //System.out.println("NeoTiler : "+f);
        String getthis = null;
        if (f.startsWith("http")) {
            HTTPIRandomAccess3 bbb = new HTTPIRandomAccess3(f);
            Location.mapFile("charm", bbb);
            getthis = "charm";
        } else {
            getthis = f;
        }
        File cache = new File("cache");
        if (!cache.exists()) {
            cache.mkdir();
        }
        //warp = new SVSReader();
        reader = new SVSReader();
        //reader = new Memoizer(warp, 0L, new File("cache"));
        reader.setGroupFiles(true);
        reader.setMetadataFiltered(true);
        reader.setOriginalMetadataPopulated(true);
        try {
            factory = new ServiceFactory();
            service = factory.getInstance(OMEXMLService.class);
            reader.setMetadataStore(service.createOMEXMLMetadata(null, null));
            reader.setId(getthis);
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
            if (getthis.endsWith(".vsi")) {
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
                if (off>0.01) {
                    px[offset] = 1;
                }
            }
            SortImages();
            upperbound = lowerbound + 1;
            while ((upperbound<numi)&&(px[upperbound]>1024)) {
                upperbound++;
            }
            for (int j=0;j<numi;j++) {
                pr[j] = px[0]/px[j];
                //System.out.println(j+" >>> "+pi[j]+" "+pr[j]+"  "+px[j]+","+py[j]);
            }
            numi = upperbound - lowerbound;
            //System.out.println("lower bound : "+lowerbound);
            //System.out.println("upper bound : "+upperbound);
            reader.setSeries(lowerbound);
            iWidth = reader.getSizeX();
            iHeight = reader.getSizeY();
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

//    public int MaxImage(Memoizer reader) {
    public int MaxImage(SVSReader reader) {
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
        Model m = ModelFactory.createDefaultModel();
        if (borked) {
            m.addLiteral(m.createResource("http://www.ebremer.com/a"), m.createProperty("http://www.w3.org/2003/12/exif/ns#status"), status);
        } else {
            m.addLiteral(m.createResource("http://www.ebremer.com/a"), m.createProperty("http://www.w3.org/2003/12/exif/ns#height"), iHeight);
            m.addLiteral(m.createResource("http://www.ebremer.com/a"), m.createProperty("http://www.w3.org/2003/12/exif/ns#width"), iWidth);
            m.addLiteral(m.createResource("http://www.ebremer.com/a"), m.createProperty("http://www.w3.org/2003/12/exif/ns#xResolution"), Math.round(10000/mppx));
            m.addLiteral(m.createResource("http://www.ebremer.com/a"), m.createProperty("http://www.w3.org/2003/12/exif/ns#yResolution"), Math.round(10000/mppy));
            m.addLiteral(m.createResource("http://www.ebremer.com/a"), m.createProperty("http://www.w3.org/2003/12/exif/ns#resolutionUnit"), 3);   
        }
        StringWriter out = new StringWriter();
        JsonLDWriteContext ctx = new JsonLDWriteContext();
        ctx.setJsonLDContext(null);
        ctx.setJsonLDContextSubstitution("\"http://iiif.io/api/image/2/context.json\"");
        RDFWriter w =
            RDFWriter.create()
            .format(RDFFormat.JSONLD_COMPACT_PRETTY)
            .source(DatasetFactory.wrap(m).asDatasetGraph())
            .context(ctx)
            .build();
        w.output(out);
        return out.toString();
    }
    
    public BufferedImage FetchImage(int x, int y, int w, int h, int tx, int ty) {
        //System.out.println("FetchImage : "+x+" "+y+" "+w+" "+h+" "+tx+" "+ty);
        int iratio = w/tx;
        int jj = 0;
        //System.out.println("numi : "+numi+" "+iratio);
        while ((jj<numi)&&(iratio>pr[jj])) {
            //System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>> "+jj+"  "+pi[jj]+" "+pr[jj]+"   "+numi+"  "+iratio);
            jj++;
        }
        //System.out.println("setting series to : "+pi[jj]);
        reader.setSeries(pi[jj]);
        double rr = ((double) reader.getSizeX())/((double) iWidth);
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