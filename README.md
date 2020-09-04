ImageBox Server 2.0

ImageBox is a java-based image tiling server that implements (partial) the IIIF (https://iiif.io/) interface.  It is built on the OME Bioformats image library (https://github.com/ome/bioformats) which enables it to provide tiles for a variety of image formats.

ImageBox has an unusual capability in that it can provide tiling over HTTP using HTTP range requests which is particularly useful when hosting images (such as whole slide images for pathology) on cloud services like Amazon S3.

Current demo server at http://imagebox.ebremer.com

RoadMap:
see https://github.com/ebremer/ImageBox/issues

Notes:

ImageBox has it's own embedded HTTP server based on Eclipse Jetty (https://www.eclipse.org/jetty/).  JSON-LD support is provided by Apache Jena (https://jena.apache.org/)

Quick Start:

1) Build Imagebox maven project to a jar file.  ImageBox was developing using the Apache Netbeans IDE (https://netbeans.apache.org/).
2) at a command line, execute "java -jar ImageBox-2.0.0.jar"
3) server will start and be listening on port 8899 (only changeable in code right now)
