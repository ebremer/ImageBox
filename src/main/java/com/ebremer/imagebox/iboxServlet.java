package com.ebremer.imagebox;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author erich
 */
public class iboxServlet extends HttpServlet {
    final ConcurrentHashMap uris = new ConcurrentHashMap();
    final ConcurrentHashMap transfers = new ConcurrentHashMap();
    final ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
    String turi = null;
    long imageaquire;
    long imagetiled;
    long imageencoding;
    long minfreespace = 500;
    long maxqueuesize = 10;

    @Override
    protected void doPost( HttpServletRequest request,HttpServletResponse response ) throws ServletException,IOException {
         //       } else if (req.startsWith("/upload")) {
            System.out.println("UPLOADER...");
            System.out.println(request.getParameter("UserFile"));
    }
    
    @Override
    protected void doGet( HttpServletRequest request,HttpServletResponse response ) throws ServletException,IOException {
        HttpSession session = request.getSession();
        imageaquire=0;
        turi = "blank";
        imagetiled=0;
        imageencoding=0;
        String req = request.getRequestURI();
        //System.out.println("start request URI=" + req);
        if (req.compareTo("/imagebox")==0) {
            turi = request.getParameter("url");
            System.out.println("TURI : "+turi);
            this.imageaquire = System.currentTimeMillis();
            System.out.println("QUEUE SIZE IS "+queue.size());
            File file = null;
            if (!uris.containsKey(turi)) {
                synchronized (this) {
                    long left = (long) new File(".").getFreeSpace()/1024/1024;
                    System.out.println("Free Disk : "+left);
                    System.out.println("queue size : "+queue.size());
                    if (queue.size()>=maxqueuesize) {
                            System.out.println("Too many files in queue!");
                    }
                    if (left<=minfreespace) {
                            System.out.println("Below minimum free space!");
                    }
                    while ((queue.size()>0)&&(queue.size()>=maxqueuesize)&&(left<=minfreespace)) {
                        System.out.println("Deleting oldest file. "+queue.size());
                        String kill = (String) queue.poll();
                        if (kill!=null) {
                            file = (File)uris.remove(kill);
                            queue.remove(kill);
                            transfers.remove(kill);
                            if (file.exists()) {
                                System.out.println("located oldest file..."+file.toString()+". Deleting...");
                                System.out.println("STATUS : "+file.delete());
                                System.out.println("does file exist after deleting? "+file.exists());
                            } else {
                                System.out.println("Did not find file : "+file.toString());
                            }
                        }
                    }
                }
                GetAnImage gai = new GetAnImage();
                try {
                    file = new File(gai.grab(turi));
                    queue.add(turi);
                    uris.put(turi, file);
                } catch (Exception ex) {
                    //Logger.getLogger(iBoxHandler.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                file = (File)uris.get(turi);
            }
            this.imageaquire = System.currentTimeMillis()-this.imageaquire;
            if (!transfers.containsKey(turi)) {
                try {
                    JSONObject t = new JSONObject();
                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();
                    System.out.println(dateFormat.format(date));
                    t.put("url", turi);
                    t.put("time", this.imageaquire);
                    t.put("datetime", dateFormat.format(date));
                    t.put("filesize",file.length());
                    System.out.println(t.toString());
                    transfers.put(turi, t);
                } catch (JSONException ex) {
                    Logger.getLogger(iboxServlet.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else if (req.compareTo("/favicon.ico")==0) {
            // give them something here, at somepoint, for favicon.ico thing
        } else if (req.startsWith("/iiif/")) {
            turi = request.getParameter("iri");
            //System.out.println("turi = "+turi);
            String[] parts = turi.split("/");
            String[] dim = parts[5].split(",");
            int rx = Integer.parseInt(dim[0]);
            int ry = Integer.parseInt(dim[1]);
            int rw = Integer.parseInt(dim[2]);
            int rh = Integer.parseInt(dim[3]);
            String[] ts = parts[6].split(",");
            //System.out.println("tile parts "+ts.length);
            int rtx;
            int rty;
            if (ts.length==2) {
                rtx = Integer.parseInt(ts[0]);
                rty = Integer.parseInt(ts[1]);
            } else {
                rtx = Integer.parseInt(ts[0]);
                rty = rtx;
            }
            int rot = Integer.parseInt(parts[7]);
            String imagetype = parts[6];
            String iri = "";
            for (int j=1; j<parts.length-4;j++) {
                iri = iri + "/" + parts[j];
            }
            //System.out.println("IRI : "+iri);
            NeoTiler nt;
            synchronized(this) {
                if (this.getServletConfig().getServletContext().getAttribute("neo")==null) {
                    NeoTiler xa = new NeoTiler(iri,rx,ry,rw,rh,rtx,rty);
                    this.getServletConfig().getServletContext().setAttribute("neo", xa);
                }
                nt = (NeoTiler) this.getServletConfig().getServletContext().getAttribute("neo");
            }
            //if (session.isNew()) {
//                System.out.println(session.isNew());
//                nt = new NeoTiler(iri,rx,ry,rw,rh,rtx,rty);
//                session.setAttribute("neo", nt);
//            } else {
//                nt = (NeoTiler) session.getAttribute("neo");
//            }
            BufferedImage originalImage;
            originalImage = nt.FetchImage(rx, ry, rw, rh, rtx, rty);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //System.out.println("Generating JPG...");
            ImageIO.write( originalImage, "jpg", baos );
            baos.flush();
            byte[] imageInByte = baos.toByteArray();
            baos.close();
            response.setContentType("image/jpg");
            response.setContentLength(imageInByte.length);
            response.getOutputStream().write(imageInByte);
            //baseRequest.setHandled(true);
        } else if (req.startsWith("/bog/")) {
            req = "http://localhost:8888"+req;
            IIIF i = new IIIF(req);
            String image = null;
            if (i.uri.startsWith("file://")) {
                image = i.uri.substring(6);
            } else if (i.uri.startsWith("file:/")) {
                image = i.uri.substring(5);
            }
            NeoTiler nt = null;
            synchronized(this) {
                String lastimage = (String) this.getServletConfig().getServletContext().getAttribute("image");
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
            if (i.tilerequest) {
                BufferedImage originalImage;
                originalImage = nt.FetchImage(i.x, i.y, i.w, i.h, i.tx, i.tx);
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
        } else {
            System.out.println("NO IDEA WHAT YOU WANT FROM ME "+req);
        }
    }
}


