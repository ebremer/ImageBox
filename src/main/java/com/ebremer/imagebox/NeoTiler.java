package com.ebremer.imagebox;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.abs;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObjectBuilder;
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
//import loci.formats.in.NDPIReader;
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
    //private IFormatReader warp;
    IFormatReader uni;
    private static final File f = new File("tmp");
    //private Memoizer SReader;
    private SVSReader SReader;
//    private SVSReader reader;
    private NDPIReader NReader;
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
    private double mppx;
    private double mppy;
    private boolean borked = false;
    private String status = "";
    private long lastaccessed;
    private CoreMetadata big;
    private String url;
    private final float[] pratio;
    
    public NeoTiler(String f) {
        DebugTools.enableLogging("ERROR");
        lastaccessed = System.nanoTime();
        String getthis;
        if (f.startsWith("http")) {
            //System.out.println("RANGE REQUEST "+f);
            HTTPIRandomAccess4 bbb = new HTTPIRandomAccess4(f);
          
            Location.mapFile("charm", bbb);
            getthis = "charm";
        } else {
            System.out.println("nooooooooo RANGE REQUEST");
            getthis = f;
        }
        File cache = new File("cache");
        if (!cache.exists()) {
            cache.mkdir();
        }
        String fileType = f.substring(f.lastIndexOf('.') + 1);
        if(fileType.equals("svs")) {
            SReader = new SVSReader();
            this.uni = SReader;
            //SReader = new Memoizer(warp, 0L, new File("cache"));
            SReader.setGroupFiles(true);
            SReader.setMetadataFiltered(true);
            SReader.setOriginalMetadataPopulated(true);
            try {
                factory = new ServiceFactory();
                service = factory.getInstance(OMEXMLService.class);
                SReader.setMetadataStore(service.createOMEXMLMetadata(null, null));
                SReader.setId(getthis);
                store = SReader.getMetadataStore();
                MetadataTools.populatePixels(store, SReader, false, false);
                SReader.setSeries(0);
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
                numi = SReader.getSeriesCount();
                if (getthis.endsWith(".vsi")) {
                    lowerbound = MaxImage(SReader);
                }
                Hashtable<String, Object> hh = SReader.getSeriesMetadata();
                //Enumeration ee = hh.keys();
            //while (ee.hasMoreElements()) {
//                String ya = (String) ee.nextElement();
//                System.out.println("*****>>>>> "+ya);
//            }
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
                for (int j=0;j<SReader.getSeriesCount();j++) {
                    big = SReader.getCoreMetadataList().get(j);
                    System.out.println(j+" >>> "+big.sizeX+","+big.sizeY+" aspect ratio : "+(((float) big.sizeX)/((float)big.sizeY)));
                }
                System.out.println("=============================================================");
                big = SReader.getCoreMetadataList().get(lowerbound);
                float ratio = ((float) big.sizeX)/((float) big.sizeY);
                for (int j=lowerbound;j<(numi+lowerbound);j++) {
                    big = SReader.getCoreMetadataList().get(j);
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
                SReader.setSeries(lowerbound);
                iWidth = SReader.getSizeX();
                iHeight = SReader.getSizeY();
            } else {
                iWidth = 0;
                iHeight = 0;            
                px = null;
                py = null;
                pr = null;
                pi = null;
                pratio = null;
            }
        } else if (fileType.equals("ndpi")) {
        	NReader = new NDPIReader();
            uni = NReader;    
            //NReader = new Memoizer(warp, 0L, new File("cache"));
            //NReader.setGroupFiles(true);
            //NReader.setMetadataFiltered(true);
            //NReader.setOriginalMetadataPopulated(true);
            try {
                factory = new ServiceFactory();
                service = factory.getInstance(OMEXMLService.class);
                NReader.setMetadataStore(service.createOMEXMLMetadata(null, null));
                NReader.setId(getthis);
                store = NReader.getMetadataStore();
                MetadataTools.populatePixels(store, NReader, false, false);
                NReader.setSeries(0);
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
                numi = NReader.getSeriesCount();
                if (getthis.endsWith(".vsi")) {
                    lowerbound = MaxImage(NReader);
                }
                Hashtable<String, Object> hh = NReader.getSeriesMetadata();
                //Enumeration ee = hh.keys();
            //while (ee.hasMoreElements()) {
//                String ya = (String) ee.nextElement();
//                System.out.println("*****>>>>> "+ya);
//            }
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
                for (int j=0;j<NReader.getSeriesCount();j++) {
                    big = NReader.getCoreMetadataList().get(j);
                    System.out.println(j+" >>> "+big.sizeX+","+big.sizeY+" aspect ratio : "+(((float) big.sizeX)/((float)big.sizeY)));
                }
                System.out.println("=============================================================");
                big = NReader.getCoreMetadataList().get(lowerbound);
                float ratio = ((float) big.sizeX)/((float) big.sizeY);
                for (int j=lowerbound;j<(numi+lowerbound);j++) {
                    big = NReader.getCoreMetadataList().get(j);
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
                NReader.setSeries(lowerbound);
                iWidth = NReader.getSizeX();
                iHeight = NReader.getSizeY();
            } else {
                iWidth = 0;
                iHeight = 0;            
                px = null;
                py = null;
                pr = null;
                pi = null;
                pratio = null;
            }
        }  else {
            iWidth = 0;
            iHeight = 0;            
            px = null;
            py = null;
            pr = null;
            pi = null;
            pratio = null;
        }
        //warp = new SVSReader();

    }
    
    public void setURL(String r) {
        url = r;
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

//    public int MaxImage(Memoizer SReader) {
    public int MaxImage(SVSReader SReader) {
        int ii = 0;
        int maxseries = 0;
        int maxx = Integer.MIN_VALUE;
        for (CoreMetadata c : SReader.getCoreMetadataList()) {
            if (c.sizeX>maxx) {
                maxseries = ii;
                maxx = c.sizeX;
            }
            ii++;
        }
    return maxseries;
  }
    public int MaxImage(NDPIReader SReader) {
        int ii = 0;
        int maxseries = 0;
        int maxx = Integer.MIN_VALUE;
        for (CoreMetadata c : SReader.getCoreMetadataList()) {
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
/*    
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

            for (int j=0;j<px.length;j++) {
                System.out.println(j+" >>> "+px[j]+","+py[j]+" aspect ratio : "+pr[j]);
                m.add(m.createResource("http://www.ebremer.com/a"), m.createProperty("http://iiif.io/api/image/2#sizes"), m.createResource("http://www.ebremer.com/"+j));
                m.addLiteral(m.createResource(BlankNodeId.create("dummy-"+j).toString()), m.createProperty("http://www.w3.org/2003/12/exif/ns#height"), py[j]);
                m.addLiteral(m.createResource(BlankNodeId.create("dummy-"+j).toString()), m.createProperty("http://www.w3.org/2003/12/exif/ns#width"), px[j]);
            }
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
*/
        
    public String GetImageInfo() {
        JsonBuilderFactory jbf = Json.createBuilderFactory(null);
        JsonObjectBuilder value = jbf.createObjectBuilder()
                .add("@id", url.substring(0, url.length()-10))
                .add("@context", "http://iiif.io/api/image/2/context.json")
                .add("height", iHeight)
                .add("width", iWidth)
                .add("xResolution", Math.round(10000/mppx))
                .add("yResolution", Math.round(10000/mppx))
                .add("resolutionUnit", 3);
        
        JsonArrayBuilder sizes = jbf.createArrayBuilder();
        JsonArrayBuilder scalefactors = jbf.createArrayBuilder();
        /*
        int clip = px.length-3;
        for (int j=0;j<clip;j++) {
            scalefactors.add(pr[j]);
        }
        for (int j=clip-1;j>=0;j--) {
            sizes.add(jbf.createObjectBuilder().add("width", px[j]).add("height", py[j]));
        }*/
        
        int clip = Math.max(iWidth, iHeight);
        clip = (int) Math.ceil(Math.log(clip)/Math.log(2));
        clip = clip - (int) (Math.ceil(Math.log(Math.max(uni.getOptimalTileHeight(), uni.getOptimalTileWidth()))/Math.log(2)));
        for (int j=0;j<clip;j++) {
            int pow = (int) Math.pow(2, j);
            scalefactors.add(pow);
        }
        for (int j=clip-1;j>=0;j--) {
            int pow = (int) Math.pow(2, j);
            sizes.add(jbf.createObjectBuilder().add("width", iWidth/pow).add("height", iHeight/pow));
        }
        value.add("sizes", sizes);
        JsonArrayBuilder profile = jbf.createArrayBuilder();
        JsonArrayBuilder supports = jbf.createArrayBuilder();
        supports
                .add("canonicalLinkHeader")
                .add("profileLinkHeader")
                .add("mirroring")
                .add("rotationArbitrary")
                .add("sizeAboveFull")
                .add("regionSquare");
        JsonArrayBuilder qualities = jbf.createArrayBuilder();
        qualities
                .add("default")
                .add("bitonal")
                .add("gray")
                .add("color");
        JsonArrayBuilder formats = jbf.createArrayBuilder();
        formats
                .add("jpg")
                .add("png")
                .add("gray")
                .add("color");
        profile.add("http://iiif.io/api/image/2/level2.json")
               .add(jbf.createObjectBuilder()
                        .add("supports", supports)
                        .add("formats", formats)
                        .add("qualities", qualities));
        JsonArrayBuilder tiles = jbf.createArrayBuilder().add(jbf.createObjectBuilder()
                                                                .add("width", uni.getOptimalTileWidth())
                                                                .add("height", uni.getOptimalTileHeight())
                                                                .add("scaleFactors", scalefactors));
        value.add("protocol","http://iiif.io/api/image").add("profile", profile).add("tiles", tiles);
        return value.build().toString();
    }
    
    public BufferedImage FetchImage(int x, int y, int w, int h, int tx, int ty, String type) {
        //System.out.println("FetchImage : "+x+" "+y+" "+w+" "+h+" "+tx+" "+ty);
        int iratio = w/tx;
        int jj = 0;
        while ((jj<numi)&&(iratio>pr[jj])) {
//            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>> "+jj+"  "+pi[jj]+" "+pr[jj]+"   "+numi+"  "+iratio);
            jj++;
        }
        //System.out.println("setting series to : "+pi[jj]);
        double rr = 0;
        if (type.equals("svs")) {
        	SReader.setSeries(pi[jj]);
        	rr = ((double) SReader.getSizeX())/((double) iWidth);        	
        } else if (type.equals("ndpi")) {
        	NReader.setSeries(pi[jj]);
        	rr = ((double) NReader.getSizeX())/((double) iWidth);
        }
        int gx=(int) (x*rr);
        int gy=(int) (y*rr);
        int gw=(int) (w*rr);
        int gh=(int) (h*rr);
        System.out.println("gx:"+gx+", gy:"+gy+", gw:"+gw+", gh:"+gh+", size:"+rr);
        BufferedImage bi = GrabImage(gx,gy,gw,gh,type);
        BufferedImage target;
        AffineTransform at = new AffineTransform();
        double scale = (((double) tx)/((double) bi.getWidth()));
        at.scale(scale,scale);
        AffineTransformOp scaleOp =  new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        target = new BufferedImage((int)(gw*scale),(int)(gh*scale),bi.getType());
        scaleOp.filter(bi, target);
        return target;
    }
    
    private BufferedImage GrabImage(int xpos, int ypos, int width, int height, String type) {
        //System.out.println("grab image : "+xpos+ " "+ypos+" "+width+" "+height+"=== "+type);
        meta.setRoot(newRoot);
        meta.setPixelsSizeX(new PositiveInteger(width), 0);
        meta.setPixelsSizeY(new PositiveInteger(height), 0);
        byte[] buf;
        BufferedImage bb = null;
        try {
        	if (type.equals("svs")) {
        		buf = SReader.openBytes(0, xpos, ypos, width, height);
        		bb = AWTImageTools.makeImage(buf, SReader.isInterleaved(), meta, 0);        		
        	} else if (type.equals("ndpi")) {
                    System.out.println(NReader.getSizeX()+"x"+NReader.getSizeY());
                    buf = NReader.openBytes(0, xpos, ypos, width, height);
                    //buf = NReader.openBytes(0, 0, 0, 4096, 4096);
                    System.out.println("image is "+(buf==null));
                    bb = AWTImageTools.makeImage(buf, NReader.isInterleaved(), meta, 0);
        	}
        } catch (FormatException | IOException ex) {
            System.out.println("I'm dying....ERROR");
            Logger.getLogger(NeoTiler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return bb;
    }
}