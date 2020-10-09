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
using System.Globalization;

namespace AnyPlace
{
	public class GeoCoordinatesHelper
	{
		public double NeLat { get; private set; }
		public double NeLon { get; private set; }
		public double SwLat { get; private set; }
		public double SwnLon { get; private set; }

		public GeoCoordinatesHelper(double neLat, double neLon, double swLat, double swnLon)
		{
			NeLat = neLat;
			NeLon = neLon;
			SwLat = swLat;
			SwnLon = swnLon;
		}

		public GeoCoordinatesHelper(string neLat, string neLon, string swLat, string swnLon)
		{
			NeLat = double.Parse(neLat, CultureInfo.InvariantCulture);
			NeLon = double.Parse(neLon, CultureInfo.InvariantCulture);
			SwLat = double.Parse(swLat, CultureInfo.InvariantCulture);
			SwnLon = double.Parse(swnLon, CultureInfo.InvariantCulture);
		}

		public void IncreaseBoundingBox(int meters)
		{
			//const int r = 6335000; // in metres
			//var x = (meters * 180 / (Math.PI * r * Math.Cos(NeLat)));
			//NeLon = NeLon + x;
			//SwnLon = SwnLon - (meters * 180 / (Math.PI * r * Math.Cos(SwLat)));

			//NeLat = NeLat + (meters * 180 / (Math.PI * r));
			//SwLat = SwLat - (meters * 180 / (Math.PI * r));
			
			var k = 0.003;
			
			NeLat += k;
			NeLon += k;

			SwLat -= k;
			SwnLon -= k;
		}

	}
}
