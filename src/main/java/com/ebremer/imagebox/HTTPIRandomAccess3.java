/*
 * Software by Erich Bremer
 * ALL RIGHTS RESERVED
 */

package com.ebremer.imagebox;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import loci.common.ByteArrayHandle;
import loci.common.IRandomAccess;
import loci.common.Location;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.CoreMetadata;
import loci.formats.FormatException;
import loci.formats.IFormatReader;
import loci.formats.MissingLibraryException;
import loci.formats.gui.AWTImageTools;
import loci.formats.in.SVSReader;
import loci.formats.meta.IMetadata;
import loci.formats.meta.MetadataStore;
import loci.formats.services.OMEXMLService;
import loci.formats.services.OMEXMLServiceImpl;
import ome.xml.meta.OMEXMLMetadataRoot;
import ome.xml.model.Image;
import ome.xml.model.primitives.PositiveInteger;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 *
 * @author erich
 */
public class HTTPIRandomAccess3 implements IRandomAccess {
    private String url = null;
    private HttpClient httpClient = null;
    private long length = -1;
    private long bufferstart = Integer.MAX_VALUE;
    public long chunksize = (long) Math.pow(2,15);
    private long pos;
    private ByteArrayHandle bah;
    private ByteOrder order;
    private TreeMap<Long,ByteArrayHandle> tm;
    public long calls = 0;
    private long numreadByte = 0;
    private long numreadShort = 0;
    private long numreadByteRange = 0;
    private long minrange = Long.MAX_VALUE;
    private long maxrange = Long.MIN_VALUE;
    private long avgrange = 0;
    private long numreadUnsignedShort = 0;
    private long numreadInt = 0;
    private String uuid = UUID.randomUUID().toString();
	private int numreadLong;
	private int numreadFloat;
    
    HTTPIRandomAccess3(String url) {
//        System.out.println("HTTPIRandomAccess3 initializing..."+uuid+" "+url);
        tm = new TreeMap<>();
        this.url = url;
        if (httpClient == null) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            httpClient = new HttpClient(sslContextFactory);
            httpClient.setFollowRedirects(true);   
            try {
                httpClient.start();
            } catch (Exception ex) {
                Logger.getLogger(HTTPIRandomAccess3.class.getName()).log(Level.SEVERE, null, ex);
            }
            String mimetype = "application/octet-stream";
            InputStreamResponseListener listener = new InputStreamResponseListener();
            System.out.println("URL : "+this.url);
            httpClient.newRequest(this.url).method(HttpMethod.HEAD).header("Accept", mimetype).send(listener);
            Response response = null;
            try {
                response = listener.get(240, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException ex) {
                Logger.getLogger(HTTPIRandomAccess3.class.getName()).log(Level.SEVERE, null, ex);
            }
            //System.out.println("xxxResponse : "+response.getStatus());
            if (response.getStatus() == 200) {
                this.length = response.getHeaders().getField(HttpHeader.CONTENT_LENGTH).getLongValue();
                //System.out.println("Content Length : "+length);
                try {
                    seek(0L);
                } catch (IOException ex) {
                    Logger.getLogger(HTTPIRandomAccess3.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                System.out.println("zamError detected on accessing!!! : ("+response.getStatus()+") : "+url);
            }
        }
        //System.out.println("init complete : "+this.length);
    }

    private void FillBuffer(long start, long len) {
        calls++;
//        System.out.println("FillBuffer   start "+Long.toHexString(start)+ " end "+Long.toHexString(start+len));
        if (len>this.length) {
            System.out.println("FillBuffer   start "+Long.toHexString(start)+ " end "+Long.toHexString(start+len));
            System.out.println("clipping request..."+len+" to "+this.length);
            len = this.length;
        }
        byte[] bytes = null;
        String mimetype = "application/octet-stream";
        InputStreamResponseListener listener = new InputStreamResponseListener();
        long b = start+len-1;
        if (b>this.length) {
            b = this.length;
        }
        httpClient.newRequest(this.url).method(HttpMethod.GET).header("Accept", mimetype).header(HttpHeader.RANGE, "bytes="+start+"-"+b).send(listener);
        Response response = null;
        try {
            response = listener.get(240, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException | ExecutionException ex) {
            Logger.getLogger(HTTPIRandomAccess3.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (response.getStatus() == 206) {
            try (InputStream responseContent = listener.getInputStream()) {
                bytes = new byte[(int)len];
                for (int z=0; z<len;z++) {
                    bytes[z] = (byte) responseContent.read();
                }
            } catch (IOException ex) {
                Logger.getLogger(HTTPIRandomAccess3.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
  
            }
        } else {
            System.out.println(this.uuid+"   dahaError detected on accessing : ("+response.getStatus()+") : "+url);
        }
        bah = new ByteArrayHandle(ByteBuffer.wrap(bytes));
        bah.setOrder(order);
        bufferstart = start;
        pos = start;
    }

    @Override
    public void close() throws IOException {
//    	System.out.println("CLOSING HERE");
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    	

    @Override
    public long getFilePointer() throws IOException {
        return this.pos;
    }

    @Override
    public long length() throws IOException {
        return this.length;
    }

    @Override
    public ByteOrder getOrder() {
        //System.out.println("getOrder");
        return bah.getOrder();
    }

    @Override
    public void setOrder(ByteOrder order) {
        this.order = order;
    }

    @Override
    public int read(byte[] b) throws IOException {
        numreadByteRange++;
        avgrange = avgrange + b.length;
        if (b.length>maxrange) {
            maxrange = b.length;
        }
        if (b.length<minrange) {
            minrange = b.length;
        }
        //System.out.println("read(byte[] b)="+b.length);
        if (b.length>(bah.length()-bah.getFilePointer())) {
            seek(pos);
        }
        for (int i=0;i<b.length;i++) {
            b[i] = readByte();
        }
        //System.out.println(Integer.toHexString(b));
        return b.length;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int read(ByteBuffer buffer, int offset, int len) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void seek(long pos) throws IOException {
        //System.out.println("seek("+Long.toHexString(pos)+")");
        if ((pos<bufferstart) || (pos>bufferstart+bah.length())) {
            if (bah!=null) {
                //System.out.println("saving "+Long.toHexString(bufferstart)+" into cache...");
                if (!tm.containsKey((Long) bufferstart)) { 
                    tm.put((Long) bufferstart, bah);
                }
            }
            Entry entry = tm.floorEntry((Long)pos);
            //System.out.println("SIZE : "+tm.size());
            if (entry==null) {
                //System.out.println(Long.toHexString(pos)+" not in cache...");
                FillBuffer(pos,chunksize);
                //FillBuffer(0,315219369L);
            } else {
                ByteArrayHandle bh = (ByteArrayHandle) entry.getValue();
                bufferstart = (long) entry.getKey();
                //System.out.println("Loading the following saved cache : "+Long.toHexString(bufferstart));
                bah = bh;
                bah.setOrder(order);
                this.pos = pos;
                if ((pos<bufferstart) || (pos>bufferstart+bah.length())) {
                    //System.out.println(bufferstart+" not good enough, need to load new data...");
                    FillBuffer(pos,chunksize);
                } else {
                    bah.seek(pos-bufferstart);
                }  
            }
        } else {
            bah.seek(pos-bufferstart);
            this.pos = pos;
        }
    }

    @Override
    public void write(ByteBuffer buf) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(ByteBuffer buf, int off, int len) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void readFully(byte[] b) {
        System.out.println("readFully");
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void readFully(byte[] b, int off, int len) {
        System.out.println("readFully "+off+" "+len);
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int skipBytes(int n) throws IOException {
        //System.out.println("skipBytes("+n+")");
        if (n>(bah.length()-bah.getFilePointer())) {
            seek(pos+n);
        } else {
            pos=pos+n;
            bah.skipBytes(n);
        }
        return n;
    }

    @Override
    public boolean readBoolean() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public byte readByte() throws IOException {
        numreadByte++;
        //System.out.print("readByte() = ");
        byte b = 0;
        if (bah.getFilePointer()==bah.length()) {
            //System.out.println("untested 2 - "+pos+" "+chunksize);
            FillBuffer(pos,chunksize);
        }
        b = bah.readByte();
        pos++;
        //System.out.println(Integer.toHexString(b));
        return b;
    }

    @Override
    public int readUnsignedByte() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public short readShort() throws IOException {
        numreadShort++;
        //System.out.print("readShort() = ");
        if ((bah.length()-bah.getFilePointer())==0) {
            System.out.println("untested");
            FillBuffer(pos,chunksize);
        }
        short b = bah.readShort();
        pos=pos+2;
        //System.out.println(Integer.toHexString(b));
        return b;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        numreadUnsignedShort++;
        //System.out.print("readUnsignedShort() = ");
        int s = readShort() & 0xffff;
        return s;
    }

    @Override
    public char readChar() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int readInt() throws IOException {
        numreadInt++;
        //System.out.print("readInt() = ");
        int b = 0;
        if ((bah.length()-bah.getFilePointer())<4) {
            //System.out.println("untested 3");
            FillBuffer(pos,chunksize);
        }
        b = bah.readInt();
        pos=pos+4;
        //System.out.println(Integer.toHexString(b));
        return b;
    }

    @Override
    public long readLong() throws IOException {
    	numreadLong++;
        long b = 0;
        if ((bah.length()-bah.getFilePointer())<8) {
            FillBuffer(pos,chunksize);
        }
        b = bah.readLong();
        pos=pos+8;
        return b;
    }

    @Override
    public float readFloat() throws IOException {
    	numreadFloat++;
        //System.out.print("readInt() = ");
        float b = 0;
        if ((bah.length()-bah.getFilePointer())<4) {
            FillBuffer(pos,chunksize);
        }
        b = bah.readFloat();
        pos=pos+4;
        return b;
    }

    @Override
    public double readDouble() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String readUTF() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(int b) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(byte[] b) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeByte(int v) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeShort(int v) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeChar(int v) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeInt(int v) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeLong(long v) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeFloat(float v) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeDouble(double v) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeBytes(String s) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeChars(String s) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void writeUTF(String s) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
     public static void main(String[] args) throws MissingLibraryException, ServiceException, FormatException, IOException {
        loci.common.DebugTools.setRootLevel("WARN");
        //HTTPIRandomAccess3 bbb = new HTTPIRandomAccess3("http://www.ebremer.com/TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs");
        //HTTPIRandomAccess3 bbb = new HTTPIRandomAccess3("http://vinculum.bmi.stonybrookmedicine.edu/TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs");
        HTTPIRandomAccess3 bbb = new HTTPIRandomAccess3("https://s3.amazonaws.com/ebremeribox/TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs");
        IFormatReader reader = new SVSReader();
        reader.setOriginalMetadataPopulated(true);
        OMEXMLService service;
        //File in = new File("C:\\data\\TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs");
        //NIOFileHandle fh = new NIOFileHandle(in,"r");
        Location.mapFile("charm", bbb);
        //Location.mapFile("local", fh);
        try {
            ServiceFactory factory = new ServiceFactory();
            service = factory.getInstance(OMEXMLService.class);
            reader.setMetadataStore(service.createOMEXMLMetadata(null, null));
            reader.setId("charm");
            //reader.setId("local");
            System.out.println(reader.getSizeX()+" "+reader.getSizeY());
            System.out.println("layers " +reader.getCoreMetadataList().size());
            int ii = 0;
            for (CoreMetadata x : reader.getCoreMetadataList()) {
                System.out.println(ii+"  "+x.sizeX+","+x.sizeY);
                ii++;
            }
            MetadataStore store = reader.getMetadataStore();
            String xml = service.getOMEXML(service.asRetrieve(store));
            IMetadata meta = service.createOMEXMLMetadata(xml);
            OMEXMLMetadataRoot newRoot = (OMEXMLMetadataRoot) meta.getRoot();
            meta.setRoot(newRoot);
            OMEXMLMetadataRoot root = (OMEXMLMetadataRoot) store.getRoot();
            Image EI = root.getImage(0);
            newRoot.addImage(EI);
            meta.setRoot(newRoot);
            int height = 256;
            int width = 256;
            meta.setPixelsSizeX(new PositiveInteger(width), 0);
            meta.setPixelsSizeY(new PositiveInteger(height), 0);
            System.out.println("executing benchmarks...");
            int tilex = 256;
            int tiley = 256;
            long starttime = System.nanoTime();
            Random rand = new Random(); 
            int numruns = 100;
            for (int i=0; i<numruns; i++) {
                int scale = rand.nextInt(5);
                reader.setSeries(scale);
                int offx = rand.nextInt(reader.getSizeX()-tilex);
                int offy = rand.nextInt(reader.getSizeY()-tiley);
                byte[] buf = reader.openBytes(0, offx, offy, tilex, tiley);
                BufferedImage bb = AWTImageTools.makeImage(buf, false, meta, 0);
                File outputfile = new File("neosaved.jpg");
                ImageIO.write(bb, "jpg", outputfile);
            }
            long endtime = System.nanoTime();
            long totaltime = (endtime - starttime)/1000000;
            double time = ((double) totaltime)/numruns;
            System.out.println("Average time is : "+time);
            System.out.println("http calls           : "+bbb.calls);
            System.out.println("numreadByte          : "+bbb.numreadByte);
            System.out.println("numreadShort         : "+bbb.numreadShort);
            System.out.println("numreadByteRange     : "+bbb.numreadByteRange);
            System.out.println("maxrange             : "+bbb.maxrange);
            System.out.println("minrange             : "+bbb.minrange);
            System.out.println("avgrange             : "+((double) bbb.avgrange)/((double) bbb.numreadByteRange));
            System.out.println("numreadUnsignedShort : "+bbb.numreadUnsignedShort);
            System.out.println("numreadInt           : "+bbb.numreadInt);
    
        } catch (DependencyException de) {
            throw new MissingLibraryException(OMEXMLServiceImpl.NO_OME_XML_MSG, de);
        } finally {
            try {
               // httpClient.stop();
            } catch (Exception ex) {
                Logger.getLogger(HTTPIRandomAccess3.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
     }

    @Override
    public boolean exists() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public long skipBytes(long l) throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }   
}