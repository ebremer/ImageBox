package com.ebremer.imagebox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author erich
 */
public class iboxServlet extends HttpServlet {
    static final ImageReaderPool pool = new ImageReaderPool();
    Path fpath = Paths.get(System.getProperty("user.dir")+"/"+Settings.webfiles);
    
    @Override
    protected void doGet( HttpServletRequest request,HttpServletResponse response ) throws ServletException,IOException {
        HttpSession session = request.getSession();
        String req = request.getRequestURI();
        if (req.compareTo("/iiif/")==0) {

        } else if (req.compareTo("/favicon.ico")==0) {
            // give them something here, at somepoint, for favicon.ico thing
        } else if (req.startsWith("/bog/")) {
            //System.out.println("XREQ : "+req);
            IIIF i = null;
            try {
                i = new IIIF(req);
            } catch (URISyntaxException ex) {
                Logger.getLogger(iboxServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
            NeoTiler nt = null;
            //ImageReaderPool pool;
            String target = null;
            /*
            if (session.isNew()) {
                pool = new ImageReaderPool();
                session.setAttribute("pool", pool);
            } else {
                pool = (ImageReaderPool) session.getAttribute("pool");
            } */
            if (i.uri.getScheme()==null) {
                File image = Paths.get(fpath+"/"+i.uri.getPath()).toFile();
                target = image.getPath();
                nt = pool.GetReader(target);                
            } else if (i.uri.getScheme().startsWith("http")) {
                target = i.uri.toString();
                nt = pool.GetReader(target);                
            } else if (i.uri.getScheme().startsWith("file")) {
                File image = FileSystems.getDefault().provider().getPath(i.uri).toAbsolutePath().toFile();
                target = image.getPath();
                nt = pool.GetReader(target);
            } else {
                System.out.println("I'm so confused as what I am looking at....");
            }
            if (nt.isBorked()) {
                response.setContentType("application/json");
                response.setStatus(500);
                PrintWriter writer=response.getWriter();
                writer.append(nt.GetImageInfo());
                writer.flush();                
            } else if (i.tilerequest) {
                BufferedImage originalImage;
                if (i.fullrequest) {
                    i.x = 0;
                    i.y = 0;
                    i.w = nt.GetWidth();
                    i.h = nt.GetHeight();
                } else {
                    if ((i.x+i.w)>nt.GetWidth()) {
                        i.w = nt.GetWidth()-i.x;
                    }
                    if ((i.y+i.h)>nt.GetHeight()) {
                        i.h = nt.GetHeight()-i.y;
                    }                 
                }
                originalImage = nt.FetchImage(i.x, i.y, i.w, i.h, i.tx, i.tx);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                JPEGImageWriteParam jpegParams = new JPEGImageWriteParam(null);
                jpegParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpegParams.setCompressionQuality(1.0f);
                ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageOutputStream imageOut=ImageIO.createImageOutputStream(baos);
                writer.setOutput(imageOut);
                writer.write(null,new IIOImage(originalImage,null,null),jpegParams);                
                //ImageIO.write( originalImage, "jpg", baos );
                baos.flush();
                byte[] imageInByte = baos.toByteArray();
                baos.close();
                response.setContentType("image/jpg");
                response.setContentLength(imageInByte.length);
                response.getOutputStream().write(imageInByte);
            } else if (i.inforequest) {
                response.setContentType("application/json");
                PrintWriter writer=response.getWriter();
                writer.append(nt.GetImageInfo());
                writer.flush();
            } else {
                System.out.println("unknown IIIF request");
            }
            pool.ReturnReader(target, nt);
        } else {
            System.out.println("NO IDEA WHAT YOU WANT FROM ME "+req);
        }
    }
}
