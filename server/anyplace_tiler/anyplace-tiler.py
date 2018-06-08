#!/usr/bin/python3

import math
import sys
import subprocess
import re
import os

#####################################################################################
# Google Maps Functionality
#####################################################################################
class GoogleMaps:
    TILE_SIZE = 256
    def __init__(self):
        pass

class LatLng:
    def __init__(self,lati,lngi):
        self.lat = lati
        self.lng = lngi

class Point:
    def __init__(self,xi,yi):
        self.x = xi
        self.y = yi

# Given the LatLng coordinates we get a Point that represents World Coordinates
def fromLatLngToWorldCoordinates( latlng ):
    tiles = 2 ** 0
    circumference = 256 * tiles
    radius = circumference / (2 * math.pi)
    # calculate the X coordinate
    falseEasting = -1.0 * circumference / 2.0
    worldX = (radius * math.radians(latlng.lng)) - falseEasting
    # calculate the Y coordinate
    falseNorthing = circumference / 2.0
    worldY = ((radius / 2.0 * math.log((1.0 + math.sin(math.radians(latlng.lat))) / (1.0 - math.sin(math.radians(latlng.lat)))))-falseNorthing) * -1

    return Point(worldX, worldY)

# Given the World Coordinates we get a Point of the Pixel Coordinates
def fromWorldToPixelCoordinates( point, zoom ):
    tiles = int( 2 ** zoom )
    x = int( math.floor(point.x * tiles) )
    y = int( math.floor(point.y * tiles) )
    return Point(x, y)

# Given the Pixel coordinates we get the Google Maps Tile Coordinates that this 
# location belongs to
def fromPixelCoordsToTileCoordinates( pixelCoords ):
    tileX = math.floor(pixelCoords.x / GoogleMaps.TILE_SIZE)
    tileY = math.floor(pixelCoords.y / GoogleMaps.TILE_SIZE)
    return Point(tileX, tileY)

# Given the LatLng coordinates we get the Google Maps Tile Coordinates that this
# location belongs to
# this is a combined version for the use of the above two methods
# fromLatLngToWorld -> fromWroldToPixel -> from PixelToTile
def fromLatLngToTileCoordinates(latlng, zoom):
    tiles = 2 ** zoom
    tileX = math.floor(tiles *((latlng.lng+180)/360))
    tileY = math.floor( tiles * (1-(math.log(math.tan(math.radians(latlng.lat)) + (1/math.cos(math.radians(latlng.lat)))) / math.pi)) / 2)
    return Point(tileX, tileY)

# Given the Google Maps Tile coordinates we get the LatLng coordinates
def fromTileCoordsToLatLng(tileCoords, zoom):
    tiles = 2 ** zoom
    tile_lng = tileCoords.x / tiles * 360.0 - 180.0
    tile_lat = math.degrees(math.radians(math.atan(math.sinh(math.pi*(1-2*tileCoords.y/tiles)))))
    return LatLng(tile_lat,tile_lng)   

# Given the Google Maps Tile coordinates we get the Pixel Coordinates
def fromTileCoordsToPixelCoordinates(tileCoords):
    x = tileCoords.x * GoogleMaps.TILE_SIZE
    y = tileCoords.y * GoogleMaps.TILE_SIZE
    return Point(x,y)

#####################################################################################

#####################################################################################
# Image Processing Functionality
#####################################################################################

# Returns the content type and dimensions of an image. 
# Currently works only with PNG images.
def getImageInfo(data):

    print(data)

    data = str(data)
    size = len(data)
    height = -1
    width = -1
    contentType=''

    # PNG 2
    if (size >= 24) and data.startswith('\211PNG\r\n\032\n') and (data[12:16]=='IHDR'):
        contentType = 'image/png'
        w,h = struct.unpack(">LL", data[16:24])
        width = int(w)
        height = int(h)

    # maybe older PNG
    elif (size >= 16) and data.startswith('\211PNG\r\rn\032\n'):
        contentType = 'image/png'
        w,h = struct.unpack(">LL", data[8:16])
        width = int(w)
        height = int(h)

    return (contentType, width, height)

def getImageInfoFromFile(filename):
    f = open(filename, 'rb')
    return getImageInfo(f.read())

def getImageInfoFromFile2(filename):
    path = filename
    dim = subprocess.Popen(["identify","-format","\"%w,%h\"",path], stdout=subprocess.PIPE).communicate()[0]
    #print(str(dim))
    imageInfo = str(dim)[0:]
    print("image info as returned by identify[w,h]: "+ imageInfo)
    try:
        (width, height) = [ int(x) for x in re.sub('[\t\r\n"]', '', imageInfo).split(',') ]
    except:
        print('getImageInfoFromFile2:: Error while getting image info!')
        sys.exit(1)

    return (width,height)

# it pads the image, denoted by filename to the LEFT and TOP sides with transparency
# in order to match the newW:width and newH:height
# as a result the original image is place in the Bottom Right portion of the new image
def convertPaddedImage( filename, newW, newH, outputName ):
    path = filename
    dim = subprocess.Popen(["convert",path, "-background","none", "-gravity","SouthEast", "-extent", ( "%dX%d!"%(newW, newH) ), outputName], stdout=subprocess.PIPE).communicate()[0]


def resizeImage( srcImage, zoomOriginal, zoomCurrent, destImage ):
    rasRatio=100
    if(zoomOriginal!=zoomCurrent):
        resRatio = 100.0 / (2 ** (zoomOriginal-zoomCurrent) )
    print(resRatio)
    dim = subprocess.Popen(["convert", srcImage, "-resize", ('%d%%'%(resRatio)), destImage ], stdout=subprocess.PIPE).communicate()[0]

#####################################################################################


#####################################################################################
# HELPER FUNCTIONS BELOW
#####################################################################################

def usage():
    return '\nUSAGE:::\n%s\n\n  %s\n\n  %s\n\n  %s\n\n  %s\n\n  %s\n' % ('python3 scriptname.py <TileScriptsDir> <ImageLatitude> <ImageLongitude> <OriginalZoom> <ToZoom> <ImageFileName>',
            '<ImageLatitude>: the latitude coordinates for the TopLeft corner of the image' ,
            '<ImageLongitude>: the longitude coordinates for the TopLeft corner of the image',
            '<OriginalZoom>: the zoom corresponding to the coordinates provided' ,
            '<ToZoom>: the zoom to which you want to have tiles, tiles will be provided for zoom levels in range [ToZoom, OriginalZoom]. ToZoom <= OriginalZoom' ,
            '<ImageFilename>: the filename of the image to use for tiles')

def googleTileCutter( googleTilerScript, zoom, tileCoords, imageFile ):
    dim = subprocess.Popen([ googleTilerScript, '-o', str(zoom), '-t', ('%d,%d' % (tileCoords.x, tileCoords.y)), '-z', str(zoom), imageFile ], stdout=subprocess.PIPE).communicate()[0]

def fixTileStructure( fixTileStructureScript, ImageFileName ):
    dim = subprocess.Popen([ fixTileStructureScript, ImageFileName ], stdout=subprocess.PIPE).communicate()[0]


#####################################################################################

###############################################################
# MAIN SCRIPT BELOW
##############################################################

# args:
#  1: TopLeft Latitude
#  2: TopLeft Longitude
#  3: 
def main( argv ):
    print( argv )
    
    if(8 != len(sys.argv)):
        print(usage())
        sys.exit(1)

    ScriptsDir = sys.argv[1]
    ImageLatitude = float(sys.argv[2])
    ImageLongitude = float(sys.argv[3])
    OriginalZoom = int(sys.argv[4])
    ToZoom = int(sys.argv[5])
    ImageFileName = sys.argv[6]
    UploadZoom = int(sys.argv[7])
    ImageDirName = os.path.dirname(os.path.realpath(ImageFileName))

    fixTileStructureScript = str(ScriptsDir + '/fix-tile-structure.sh')
    googleTilerScript = str(ScriptsDir + '/googletilecutter-0.11.sh')
    CURRENT_PADDED_IMAGE_NAME = ImageDirName + '/padded-image.png'
    CURRENT_ZOOM_IMAGE_NAME = ImageDirName + '/zoom-sized-image'

    # initializations
    
    # we will run the procedure for every zoom level in range [OriginalZoom..ToZoom]
    for currentZoom in range( OriginalZoom, (ToZoom-1), -1 ):
        currentImage=ImageFileName
        # call the command to resize the image according to the zoom level
        if( currentZoom == UploadZoom ):
            currentImage=ImageFileName
        else:
            newName=(('%s-z%d.png') % (CURRENT_ZOOM_IMAGE_NAME, currentZoom))
            resizeImage(currentImage, UploadZoom, currentZoom, newName)
            currentImage = newName

        # get the Image top left world coords
        topLeftWorldCoords = fromLatLngToWorldCoordinates(LatLng(ImageLatitude, ImageLongitude))
        print('Top Left World Coords: %.16f, %.16f' % (topLeftWorldCoords.x, topLeftWorldCoords.y)) 
        # get the image top left Pixel coords
        topLeftPixelCoords = fromWorldToPixelCoordinates(topLeftWorldCoords, currentZoom)
        print('Top Left Pixel Coords: %d, %d' % (topLeftPixelCoords.x, topLeftPixelCoords.y)) 
        # get the tile coordinates for the image
        tileCoords = fromPixelCoordsToTileCoordinates(topLeftPixelCoords)
        print('Tile Coords: %d, %d' % (tileCoords.x, tileCoords.y)) 
        # get the Pixel coords for the tile coordinates
        tilePixelCoords = fromTileCoordsToPixelCoordinates(tileCoords)
        print('Tile Pixel Coords: %d, %d' % (tilePixelCoords.x, tilePixelCoords.y)) 

        # calculate the adjustments that need to happen to the original image
        padW = topLeftPixelCoords.x - tilePixelCoords.x
        padH = topLeftPixelCoords.y - tilePixelCoords.y
        print('PadW[%d] PadH[%d]' % (padW, padH))

        # get the image information
        width, height = getImageInfoFromFile2(currentImage)
        print('Original W[%d] H[%d]' % (int(width), int(height)))

        # convert the image to the padded one
        convertPaddedImage(currentImage, width+padW, height+padH, CURRENT_PADDED_IMAGE_NAME )
        print('Padded W[%d] H[%d]' % (int(width)+padW, int(height)+padH))

        # NOW WE SHOULD CALL THE GOOGLETILER with the padded image as parameter
        googleTileCutter(googleTilerScript, currentZoom, tileCoords, CURRENT_PADDED_IMAGE_NAME)
        
    # NOW WE SHOULD CALL ANOTHER SCRIPT THAT WILL MOVE FILES INTO STRUCTURED FOLDERS
    fixTileStructure( fixTileStructureScript, ImageFileName )


if __name__ == "__main__":
    main(sys.argv)


