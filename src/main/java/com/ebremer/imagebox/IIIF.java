/*
 * Software by Erich Bremer
 * ALL RIGHTS RESERVED
 */

package com.ebremer.imagebox;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Erich Bremer
 */
public class IIIF {
    private static final Pattern pattern = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?/bog/(.*)?/(\\d*),(\\d*),(\\d*),(\\d*)/(\\d*),/(\\d*)/default.jpg");
    private static final Pattern info = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?/bog/(.*)?/info.js");
    private Matcher matcher;
    public String protocol = null;
    public String domain = null;
    public String port = null;
    public URI uri = null;
    public int x;
    public int y;
    public int w;
    public int h;
    public int tx;
    public int ty;
    public int rotation;
    public boolean tilerequest = false;
    public boolean inforequest = false;

    IIIF(String url) throws URISyntaxException {
        matcher = pattern.matcher(url);
        if (matcher.find()) {
            tilerequest = true;
            protocol = matcher.group(1);
            domain = matcher.group(2);
            port = matcher.group(3);
            uri = new URI(matcher.group(4));
            x = Integer.parseInt(matcher.group(5));
            y = Integer.parseInt(matcher.group(6));
            w = Integer.parseInt(matcher.group(7));
            h = Integer.parseInt(matcher.group(8));
            tx = Integer.parseInt(matcher.group(9));
            rotation = Integer.parseInt(matcher.group(10));
        } else {
            matcher = info.matcher(url);
            if (matcher.find()) {
                //System.out.println("info request "+url);
                inforequest = true;
                protocol = matcher.group(1);
                domain = matcher.group(2);
                port = matcher.group(3);
                uri = new URI(matcher.group(4));
            }
        }
        //System.out.println("IIIF : ****"+url+"*****");
        //System.out.println("protocol: " + (protocol != null ? protocol : ""));
        //System.out.println("domain: " + (domain != null ? domain : ""));
        //System.out.println("port: " + (port != null ? port : ""));
        //System.out.println("uri: " + (uri != null ? uri : ""));
        //System.out.println(x+" "+y+" "+w+" "+h+"  "+rotation);
    }
}
