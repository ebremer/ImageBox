/*
 * Software by Erich Bremer
 * ALL RIGHTS RESERVED
 */

package com.ebremer.imagebox;

import com.ebremer.imagebox.Enums.ImageFormat;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Erich Bremer
 */
public class IIIF {
    private static final Pattern PATTERN1 = Pattern.compile("/iiif/(.*)?/(\\d*),(\\d*),(\\d*),(\\d*)/(\\d*),/(\\d*)/default.(jpg|png)");
    private static final Pattern PATTERN2 = Pattern.compile("/iiif/(.*)?/full/(\\d*),/(\\d*)/default.(jpg|png)");
    private static final Pattern INFO = Pattern.compile("/iiif/(.*)?/info.js");
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
    public ImageFormat imageformat;

    IIIF(String url) throws URISyntaxException {
        matcher = PATTERN1.matcher(url);
        if (matcher.find()) {
            tilerequest = true;
            uri = new URI(matcher.group(1));
            x = Integer.parseInt(matcher.group(2));
            y = Integer.parseInt(matcher.group(3));
            w = Integer.parseInt(matcher.group(4));
            h = Integer.parseInt(matcher.group(5));
            tx = Integer.parseInt(matcher.group(6));
            rotation = Integer.parseInt(matcher.group(7));
            if ("jpg".equals(matcher.group(8))) {
                imageformat = ImageFormat.JPG;
            } else if ("png".equals(matcher.group(8))) {
                imageformat = ImageFormat.PNG;
            }
        } else {
            matcher = INFO.matcher(url);
            if (matcher.find()) {
                //System.out.println("matched here");
                inforequest = true;
                //System.out.println("ya ya "+matcher.group(1));
                uri = new URI(matcher.group(1));
                //System.out.println("ya ya after : "+uri);
            } else {
                matcher = PATTERN2.matcher(url);
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