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
    private static final Pattern pattern1 = Pattern.compile("/bog/(.*)?/(\\d*),(\\d*),(\\d*),(\\d*)/(\\d*),/(\\d*)/default.jpg");
    private static final Pattern pattern2 = Pattern.compile("/bog/(.*)?/full/(\\d*),/(\\d*)/default.jpg");
    private static final Pattern info = Pattern.compile("/bog/(.*)?/info.js");
    private Matcher matcher;
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
    public boolean fullrequest = false;

    IIIF(String url) throws URISyntaxException {
        //System.out.println("IIIF("+url+")");
        matcher = pattern1.matcher(url);
        if (matcher.find()) {
            //System.out.println("matched");
            tilerequest = true;
            //System.out.println("G1 : "+matcher.group(1));
            uri = new URI(matcher.group(1));
            x = Integer.parseInt(matcher.group(2));
            y = Integer.parseInt(matcher.group(3));
            w = Integer.parseInt(matcher.group(4));
            h = Integer.parseInt(matcher.group(5));
            tx = Integer.parseInt(matcher.group(6));
            rotation = Integer.parseInt(matcher.group(7));
        } else {
            matcher = info.matcher(url);
            if (matcher.find()) {
                //System.out.println("matched here");
                inforequest = true;
                //System.out.println("ya ya "+matcher.group(1));
                uri = new URI(matcher.group(1));
                //System.out.println("ya ya after : "+uri);
            } else {
                matcher = pattern2.matcher(url);
                if (matcher.find()) {
                    tilerequest = true;
                    uri = new URI(matcher.group(1));
                    x = 0;
                    y = 0;
                    w = Integer.MAX_VALUE;
                    h = Integer.MAX_VALUE;
                    tx = Integer.parseInt(matcher.group(2));
                    rotation = Integer.parseInt(matcher.group(3));
                    fullrequest = true;
                }
            }
        }
    }
}