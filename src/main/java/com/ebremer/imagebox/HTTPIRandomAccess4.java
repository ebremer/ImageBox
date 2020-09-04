/*
 * Software by Erich Bremer
 * ALL RIGHTS RESERVED
 */

package com.ebremer.imagebox;

import java.awt.image.BufferedImage;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
public class HTTPIRandomAccess4 implements IRandomAccess {
    private String url = null;
    private HttpClient httpClient = null;
    private long length = -1;
    public long chunksize = (long) Math.pow(2,17);
    private long pos;
    private ByteArrayHandle bah;
    private TreeMap<Integer,ByteArrayHandle> tm;
    private final String uuid = UUID.randomUUID().toString();
    private int numreadFloat;
    private int CurrentChunk = -1;
    private ByteOrder endianess = ByteOrder.LITTLE_ENDIAN;
    
    HTTPIRandomAccess4(String url) {
        //System.out.println("HTTPIRandomAccess4 " +url);
        tm = new TreeMap<>();
        this.url = url;
        if (httpClient == null) {
            SslContextFactory sslContextFactory = new SslContextFactory();
            httpClient = new HttpClient(sslContextFactory);
            httpClient.setFollowRedirects(true);   
            try {
                httpClient.start();
            } catch (Exception ex) {
                Logger.getLogger(HTTPIRandomAccess4.class.getName()).log(Level.SEVERE, null, ex);
            }
            String mimetype = "application/octet-stream";
            InputStreamResponseListener listener = new InputStreamResponseListener();
            //System.out.println("xURL : "+this.url);
            httpClient.newRequest(this.url).method(HttpMethod.HEAD).header("Accept", mimetype).send(listener);
            Response response = null;
            try {
                response = listener.get(240, TimeUnit.SECONDS);
            } catch (InterruptedException | TimeoutException | ExecutionException ex) {
                Logger.getLogger(HTTPIRandomAccess4.class.getName()).log(Level.SEVERE, null, ex);
            }
            //System.out.println("xxxResponse : "+response.getStatus());
            if (response.getStatus() == 200) {
                this.length = response.getHeaders().getField(HttpHeader.CONTENT_LENGTH).getLongValue();
                //this.length = 4085552795l;
                try {
                    seek(0L);
                    endianess = bah.getOrder();
                } catch (IOException ex) {
                    Logger.getLogger(HTTPIRandomAccess4.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                System.out.println("zamError detected on accessing!!! : ("+response.getStatus()+") : "+url);
            }
        }
        //System.out.println("Done init of HTTPIRandomAccess4...");
    }
    
    private int WhatChunkIs(long address) {
        return (int) (address/chunksize);
    }
    
    private void GrabChunk(int chunk) {
        //System.out.println("Grabbing chunk : "+chunk);
        if (tm.containsKey(chunk)) {
            //System.out.println("CACHE HIT! "+chunk);
            bah = tm.get(chunk);
        } else {
            FillBuffer(chunk*chunksize,chunksize);
            tm.put(chunk, bah);
        }
        CurrentChunk = chunk;
    }
    
    private boolean InChunk(long address) {
        return (CurrentChunk==WhatChunkIs(address));
    }
    
    private void FillBuffer(long start, long len) {
        //System.out.println("xFillBuffer   start "+Long.toHexString(start)+ " end "+Long.toHexString(start+len));
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
            Logger.getLogger(HTTPIRandomAccess4.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (response.getStatus() == 206) {
            try (InputStream responseContent = listener.getInputStream()) {
                bytes = new byte[(int)len];
                for (int z=0; z<len;z++) {
                    bytes[z] = (byte) responseContent.read();
                }
            } catch (IOException ex) {
                Logger.getLogger(HTTPIRandomAccess4.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
  
            }
        } else {
            System.out.println(this.uuid+"   dahaError detected on accessing : ("+response.getStatus()+") : "+url);
        }
        bah = new ByteArrayHandle(ByteBuffer.wrap(bytes));
        bah.setOrder(endianess);
        //bufferstart = start;
        pos = start;
    }

    @Override
    public void close() throws IOException {
        //throw new UnsupportedOperationException("Why am I closing"); 
        System.out.println("close() I'm a gonna do nothing....");
    }
    	

    @Override
    public long getFilePointer() throws IOException {
        //System.out.println("getFilePointer()");
        return this.pos;
    }

    @Override
    public long length() throws IOException {
        //System.out.println("length() = "+this.length);
        if (length<0) {
            throw new IOException("length is negative!!!");
        }
        return this.length;
    }

    @Override
    public ByteOrder getOrder() {
        //System.out.println("getOrder : "+bah2.getOrder());
        //return bah.getOrder();
        return this.endianess;
    }

    @Override
    public void setOrder(ByteOrder order) {
    //    System.out.println("setOrder() = "+order);
        this.endianess = order;
    }

    @Override
    public int read(byte[] b) throws IOException {
        //System.out.println("read(byte[] b) LEN : "+b.length);
        //numreadByteRange++;
        //avgrange = avgrange + b.length;
        //if (b.length>maxrange) {
//            maxrange = b.length;
//        }
//        if (b.length<minrange) {
//            minrange = b.length;
//        }
  //      if (b.length>(bah2.length()-bah2.getFilePointer())) {
//            seek(pos);
  //      }
        for (int i=0;i<b.length;i++) {
            b[i] = this.readByte();
        }
        return b.length;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {      
        //System.out.println(pos +" "+length+" READ "+b.length+" "+off+" "+len);
        //numreadByte++;
        int i = 0;
        while (i<len) {
            b[i+off] = this.readByte();
            i++;
        }
        return i;
    }

    public void Validate(long pos) throws IOException {
        //System.out.println("> Validate("+Long.toHexString(pos)+")");
        if ((pos > length)&&(pos<0)) { throw new IOException("Outside of valid range"); }
        if (!InChunk(pos)) {
            GrabChunk(WhatChunkIs(pos));
        }
        this.pos = pos;
        bah.seek(pos%chunksize);
    }
    
    
    @Override
    public void seek(long pos) throws IOException {
        //System.out.println("> seek("+Long.toHexString(pos)+")");
        if ((pos > length)&&(pos<0)) { throw new IOException("Outside of valid range"); }
        if (!InChunk(pos)) {
            GrabChunk(WhatChunkIs(pos));
        }
        this.pos = pos;
        bah.seek(pos%chunksize);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        //System.out.println("skipBytes(int n) = "+n);
        seek(pos+n);
        return n;
    }
    
    @Override
    public long skipBytes(long l) throws IOException {
        //System.out.println("skipBytes(long n) = "+l);
        seek(pos+l);
        return l;
    }  

    @Override
    public byte readByte() throws IOException {
        int ch = this.read0();
        //if (ch < 0) throw new EOFException();
        return (byte)(ch);
    }
    
    public int read0() {
        if (this.pos<this.length) {
            try {
                Validate(this.pos);
            } catch (IOException ex) {
                return -1;
            }
            byte b;
            try {
                b = bah.readByte();
                this.pos++;
            } catch (IOException ex) {
                return -1;
            }
            //int wow = b;
            //return b;
            //System.out.println(Byte.valueOf(b).toString()+"  "+Integer.toHexString(wow)+"  "+Integer.toHexString(0x00ff&b));
            return 0x00ff&b;
        }
        return -1;
    }    

    @Override
    public short readShort() throws IOException {
        //System.out.println("readShort() " + this.getOrder());
        int ch1 = this.read0();
        int ch2 = this.read0();
        if ((ch1 | ch2) < 0) throw new EOFException();
        if (this.getOrder() == ByteOrder.LITTLE_ENDIAN) {
            return (short)((ch2 << 8) + ch1);
        } else {
            return (short)((ch1 << 8) + ch2);
        }
    }

    @Override
    public int readUnsignedShort() throws IOException {
        //numreadUnsignedShort++;
        //System.out.println("readUnsignedShort() = ");
        int ch1 = this.read0();
        int ch2 = this.read0();
        if ((ch1 | ch2) < 0) throw new EOFException();
        if (this.getOrder() == ByteOrder.LITTLE_ENDIAN) {
            return (ch2 << 8) + ch1;
        } else {
            return (ch1 << 8) + ch2;
        }
    }

    @Override
    public int readInt() throws IOException {
        int ch1 = this.read0();
        int ch2 = this.read0();
        int ch3 = this.read0();
        int ch4 = this.read0();
        if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
        if (this.getOrder() == ByteOrder.LITTLE_ENDIAN) {
            return ((ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1);
        } else {
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
        }
    }

    @Override
    public float readFloat() throws IOException {
        //System.out.println("readFloat()");
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public long readLong() throws IOException {
        int a = this.readInt();
        int b = this.readInt();
        if (this.getOrder() == ByteOrder.LITTLE_ENDIAN) {
            return ((long)(b << 32) + (a & 0xFFFFFFFFL));
        } else {
            return ((long)(a << 32) + (b & 0xFFFFFFFFL));
        }
    }
    
    @Override
    public void readFully(byte[] b) throws IOException {
        int i = 0;
        while (i<b.length) {
            b[i] = this.readByte();
            i++;
        }
    }
    
    @Override
    public boolean exists() throws IOException {
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
    public boolean readBoolean() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
    public void readFully(byte[] b, int off, int len) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public char readChar() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public int readUnsignedByte() throws IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
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
        //HTTPIRandomAccess4 bbb = new HTTPIRandomAccess4("http://www.ebremer.com/TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs");
        //HTTPIRandomAccess4 bbb = new HTTPIRandomAccess4("http://vinculum.bmi.stonybrookmedicine.edu/TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs");
        HTTPIRandomAccess4 bbb = new HTTPIRandomAccess4("https://s3.amazonaws.com/ebremeribox/TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs");
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
        } catch (DependencyException de) {
            throw new MissingLibraryException(OMEXMLServiceImpl.NO_OME_XML_MSG, de);
        } finally {
            try {
               // httpClient.stop();
            } catch (Exception ex) {
                Logger.getLogger(HTTPIRandomAccess4.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
     } 
}