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
                //System.out.println(diff+"  "+originalImage.getWidth()+","+originalImage.getHeight()+" "+req);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write( originalImage, "jpg", baos );
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