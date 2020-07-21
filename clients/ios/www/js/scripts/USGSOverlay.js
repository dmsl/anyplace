/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou, Kyriakos Georgiou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */

USGSOverlay.prototype = new google.maps.OverlayView();
/** @constructor */
function USGSOverlay(bounds, image, map) {

    // Now initialize all properties.
    this.bounds_ = bounds;
    this.image_ = image;
    this.image64 = image;
    this.map_ = map;

    // We define a property to hold the image's div. We'll
    // actually create this div upon receipt of the onAdd()
    // method so we'll leave it null for now.
    this.div_ = null;

    // Explicitly call setMap on this overlay
    this.setMap(map);
}

USGSOverlay.prototype.onAdd = function() {

    // Note: an overlay's receipt of onAdd() indicates that
    // the map's panes are now available for attaching
    // the overlay to the map via the DOM.

    // Create the DIV and set some basic attributes.
    var div = document.createElement('div');
    div.id="ground_overlay";
    div.style.borderStyle = 'none';
    div.style.borderWidth = '0px';
    div.style.position = 'absolute';

    // Create an IMG element and attach it to the DIV.
    var img = document.createElement('img');
    img.src = this.image64;
    img.style.width = '100%';
    img.style.height = '100%';
    img.style.position = 'absolute';
    div.appendChild(img);

    // Set the overlay's div_ property to this DIV
    this.div_ = div;

    // We add an overlay to a map via one of the map's panes.
    // We'll add this overlay to the overlayLayer pane.
    var panes = this.getPanes();
    panes.overlayLayer.appendChild(div);
};

USGSOverlay.prototype.draw = function() {

    // Size and position the overlay. We use a southwest and northeast
    // position of the overlay to peg it to the correct position and size.
    // We need to retrieve the projection from this overlay to do this.
    var overlayProjection = this.getProjection();

    // Retrieve the southwest and northeast coordinates of this overlay
    // in latlngs and convert them to pixels coordinates.
    // We'll use these coordinates to resize the DIV.
    var sw = overlayProjection.fromLatLngToDivPixel(this.bounds_.getSouthWest());
    var ne = overlayProjection.fromLatLngToDivPixel(this.bounds_.getNorthEast());

    // Resize the image's DIV to fit the indicated dimensions.
    var div = this.div_;
    div.style.left = sw.x + 'px';
    div.style.top = ne.y + 'px';
    div.style.width = (ne.x - sw.x) + 'px';
    div.style.height = (sw.y - ne.y) + 'px';
};

USGSOverlay.prototype.onRemove = function() {
    this.div_.parentNode.removeChild(this.div_);
    this.div_ = null;
};