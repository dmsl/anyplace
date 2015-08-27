/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Panagiotis Irakleous
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

using Microsoft.Phone.Maps.Controls;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AnyPlace.ApiClient
{
    public class GoogleWMSTiles : TileSource
    {

        public GoogleWMSTiles()
        {
            UriFormat = @"http://mt{0}.google.com/vt/lyrs={1}&z={2}&x={3}&y={4}";
            TileSourceType = GoogleTileSourceType.Street;
        }
        private int _servernr;
        private char _mapMode;

        private int Server
        {
            get
            {
                return _servernr = (_servernr + 1) % 4;
            }
        }

        private GoogleTileSourceType _tileSourceType;
        public GoogleTileSourceType TileSourceType
        {
            get { return _tileSourceType; }
            set
            {
                _tileSourceType = value;
                _mapMode = TypeToMapMode(value);
            }
        }

        public override Uri GetUri(int x, int y, int zoomLevel)
        {
            if (zoomLevel > 0)
            {
                var url = string.Format(UriFormat, Server, _mapMode, zoomLevel, x, y);
                return new Uri(url);
            }

            return null;
        }

        private static char TypeToMapMode(GoogleTileSourceType tileSourceType)
        {
            switch (tileSourceType)
            {
                case GoogleTileSourceType.Hybrid:
                    return 'y';
                case GoogleTileSourceType.Satellite:
                    return 's';
                case GoogleTileSourceType.Street:
                    return 'm';
                case GoogleTileSourceType.Physical:
                    return 't';
                case GoogleTileSourceType.PhysicalHybrid:
                    return 'p';
                case GoogleTileSourceType.StreetOverlay:
                    return 'h';
                case GoogleTileSourceType.WaterOverlay:
                    return 'r';
            } return ' ';
        }

        public enum GoogleTileSourceType
        {
            Street,
            Hybrid,
            Satellite,
            Physical,
            PhysicalHybrid,
            StreetOverlay,
            WaterOverlay
        }
    }
}
