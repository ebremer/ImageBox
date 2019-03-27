package com.ebremer.imagebox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
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
    //final ConcurrentHashMap uris = new ConcurrentHashMap();
    //final ConcurrentHashMap transfers = new ConcurrentHashMap();
    //final ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();

    @Override
    protected void doPost( HttpServletRequest request,HttpServletResponse response ) throws ServletException,IOException {
         //       } else if (req.startsWith("/upload")) {
            System.out.println("UPLOADER...");
            System.out.println(request.getParameter("UserFile"));
    }
    
    @Override
    protected void doGet( HttpServletRequest request,HttpServletResponse response ) throws ServletException,IOException {
        HttpSession session = request.getSession();
        String req = request.getRequestURI();
        if (req.compareTo("/iiif/")==0) {

        } else if (req.compareTo("/favicon.ico")==0) {
            // give them something here, at somepoint, for favicon.ico thing
        } else if (req.startsWith("/bog/")) {
            //System.out.println("REQ : "+req);
            req = "http://localhost:8888"+req;
            IIIF i = null;
            try {
                i = new IIIF(req);
            } catch (URISyntaxException ex) {
                Logger.getLogger(iboxServlet.class.getName()).log(Level.SEVERE, null, ex);
            }
            File image = FileSystems.getDefault().provider().getPath(i.uri).toAbsolutePath().toFile();
            NeoTiler nt;
            ImageReaderPool pool;
            if (session.isNew()) {
                //System.out.println("New session.  Creating pool...");
                pool = new ImageReaderPool();
                session.setAttribute("pool", pool);
            } else {
                pool = (ImageReaderPool) session.getAttribute("pool");
            }
            nt = pool.GetReader(image.getPath());
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
            pool.ReturnReader(image.getPath(), nt);
        } else {
            System.out.println("NO IDEA WHAT YOU WANT FROM ME "+req);
        }
    }
}


/*
            synchronized(this) {
                File lastimage = (File) this.getServletConfig().getServletContext().getAttribute("image");
                if (lastimage==null) {
                    nt = new NeoTiler(image,i.x,i.y,i.w,i.h,i.tx,i.tx);
                    this.getServletConfig().getServletContext().setAttribute("neo", nt);
                    this.getServletConfig().getServletContext().setAttribute("image", image);
                } else if (lastimage.equals(image)) {
                    nt = (NeoTiler) this.getServletConfig().getServletContext().getAttribute("neo");
                } else {
                    nt = new NeoTiler(image,i.x,i.y,i.w,i.h,i.tx,i.tx);
                    this.getServletConfig().getServletContext().setAttribute("neo", nt);
                    this.getServletConfig().getServletContext().setAttribute("image", image);
                }
            } 
*/
