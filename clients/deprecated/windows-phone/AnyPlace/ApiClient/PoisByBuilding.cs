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

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AnyPlace.ApiClient
{
  
    public class Geometry
    {
        public string type { get; set; }
        public List<string> coordinates { get; set; }
    }

    public class Pois
    {
        public string floor_number { get; set; }
        public string pois_type { get; set; }
        public string buid { get; set; }
        public string image { get; set; }
        public string coordinates_lon { get; set; }
        public string url { get; set; }
        public string coordinates_lat { get; set; }
        public string floor_name { get; set; }
        public string description { get; set; }
        public string name { get; set; }
        public string is_door { get; set; }
        public string is_published { get; set; }
        public string username_creator { get; set; }
        public string puid { get; set; }
        public Geometry geometry { get; set; }
        public string is_building_entrance { get; set; }
    }

    public class PoisByBuilding

    {
        public List<Pois> pois { get; set; }
        public string status { get; set; }
        public string message { get; set; }
        public int status_code { get; set; }
    }
}
