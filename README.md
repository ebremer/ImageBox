ImageBox Server 2.0

ImageBox is a image tiling server that implements (partial) the IIIF (https://iiif.io/) interface.  It is built on the OME Bioformats image library (https://github.com/ome/bioformats) which enables it to provide tiles for a variety of image formats.

ImageBox has an unusual capability in that it can provide tiling over HTTP using HTTP range requests which is particularly useful when hosting images (such as whole slide images for pathology) on cloud services like Amazon S3.

Current demo server at http://imagebox.ebremer.com

TODO:
1) Finish core IIIF API implementation.  As original target images were whole slide images (WSI), the tile size was fixed square.  Need to support non-square tiles for IIIF.
2) JSON-LD - expose layer and other meta data as JSON-LD for ../info.json IIIF calls
3) Generalize setup for ports other than 8888.
4) Generalize setup for alternate local storage areas
5) oAuth2 authentication support
6) Add DICOM web API to augment IIIF API
7) add cleanup functions for ImageReaderPool
8) Documentation
