package com.ebremer.imagebox;

import ch.qos.logback.classic.Level;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author erich
 */
public class GetAnImage {

    public String grab(String url) throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)(Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);
        long time = System.nanoTime();
        SslContextFactory sslContextFactory = new SslContextFactory();
        HttpClient httpClient = new HttpClient(sslContextFactory);
        httpClient.setFollowRedirects(false);
        httpClient.start();
        //String url = "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/gbm/bcr/nationwidechildrens.org/tissue_images/slide_images/nationwidechildrens.org_GBM.tissue_images.Level_1.1.42.0/TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs";
        String mimetype = "application/octet-stream";
        String filename = "cache-"+UUID.randomUUID().toString()+".svs";
        FileOutputStream fos = new FileOutputStream(filename);
        System.out.println("Creating file "+filename);
        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(url).method(HttpMethod.GET).header("Accept", mimetype).send(listener);
        Response response = listener.get(240, TimeUnit.SECONDS);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        long counter = 0;
        if (response.getStatus() == 200) {
            try (InputStream responseContent = listener.getInputStream()) {
                DigestInputStream dis = new DigestInputStream(responseContent, md5);
                int read;
                byte[] bytes = new byte[1024];
                while ((read = dis.read(bytes)) != -1) {
                    counter = counter + bytes.length;
                    fos.write(bytes, 0, read);
                }
            } finally {
                fos.flush();
                fos.close();
                System.out.println("Created file "+filename);
            }
            byte[] hash = md5.digest();
            System.out.println("MD5      : "+generatehash(hash));
        } else {
            System.out.println("CRAP!");
        }
        double delta = (System.nanoTime()-time)/1000000000F;
        double mb = counter/1024F/1024F;
        double rate = mb/delta;
        System.out.println(url+"\nMBytes : "+mb+"\ntime : "+delta+"\n"+rate+" MB/sec...");
        httpClient.stop();
        return filename;
    }
    
    @SuppressWarnings("empty-statement")
    public static void main(String[] args) throws Exception {
        //ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger)(Logger)LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        //root.setLevel(Level.INFO);
        long time = System.nanoTime();
        SslContextFactory sslContextFactory = new SslContextFactory();
        HttpClient httpClient = new HttpClient(sslContextFactory);
        httpClient.setFollowRedirects(false);
        httpClient.start();
        String url = "https://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/gbm/bcr/nationwidechildrens.org/tissue_images/slide_images/nationwidechildrens.org_GBM.tissue_images.Level_1.1.42.0/TCGA-02-0001-01C-01-BS1.0cc8ca55-d024-440c-a4f0-01cf5b3af861.svs";
        String mimetype = "application/octet-stream";
        FileOutputStream fos = new FileOutputStream("img.svs");
        InputStreamResponseListener listener = new InputStreamResponseListener();
        httpClient.newRequest(url).method(HttpMethod.GET).header("Accept", mimetype).send(listener);
        Response response = listener.get(240, TimeUnit.SECONDS);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        long counter = 0;
        if (response.getStatus() == 200) {
            try (InputStream responseContent = listener.getInputStream()) {
                DigestInputStream dis = new DigestInputStream(responseContent, md5);
                int read = 0;
                byte[] bytes = new byte[1024];
                while ((read = dis.read(bytes)) != -1) {
                    counter = counter + bytes.length;
                    fos.write(bytes, 0, read);
                }
            } finally {
                fos.flush();
                fos.close();
            }
            byte[] hash = md5.digest();
            System.out.println("MD5      : "+generatehash(hash));
        } else {
            System.out.println("CRAP!");
        }
        double delta = (System.nanoTime()-time)/1000000000F;
        double mb = counter/1024F/1024F;
        double rate = mb/delta;
        System.out.println(url+"\nMBytes : "+mb+"\ntime : "+delta+"\n"+rate+" MB/sec...");
        httpClient.stop();
    }
    
    private static String generatehash(byte[] hash) throws Exception {
      	StringBuilder sb = new StringBuilder();        
        for (int i=0; i < hash.length; i++) {
           sb.append(Integer.toString( ( hash[i] & 0xff ) + 0x100, 16).substring(1));
        }
       return sb.toString();    
    }   
}