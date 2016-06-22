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

using AnyPlace.classes;
using SharpGIS;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.IO.IsolatedStorage;
using System.Net;
using System.Net.Http;
using System.Threading.Tasks;

namespace AnyPlace.ApiClient
{
    public class CustomPushpinWp8APIClient
    {
        private static readonly HttpClient AnyPlaceClient;

        public static Uri ServerBaseUri
        {
            //#error Replace with your IP address (the port is OK; it's part of the project)
            get { return new Uri("https://anyplace.rayzit.com/"); }
        }

        public static Boolean IsDirty { get; private set; }


        static CustomPushpinWp8APIClient()
        {
            IsDirty = true;

            AnyPlaceClient = new HttpClient(new HttpClientHandler{AutomaticDecompression =  DecompressionMethods.GZip | DecompressionMethods.Deflate});
        }

        public static async Task<PoisByFloor> GetPoisByFloor(string buid, string floor_number)
        {
            var r = new RequestPoisByFloor { access_token = "api_tester", buid = buid, floor_number = floor_number };
            var response = await AnyPlaceClient.PostAsJsonAsync(new Uri(ServerBaseUri, "anyplace/mapping/pois/all_floor"), r);
         
            var buildings = await response.Content.ReadAsAsync<PoisByFloor>();
            IsDirty = true;

            return buildings;
        }

        public static async Task<RouteLocationToPoi> GetLocationToPoiRoute(string buid, string puid, string floor_number, string lat, string lon)
        {
            RequestLocationToPoi r = new RequestLocationToPoi();
            r.access_token = "api_tester";
            r.buid = buid;
            r.pois_to = puid;
            r.floor_number = floor_number;
            r.coordinates_lat = lat;
            r.coordinates_lon = lon;
            var response = await AnyPlaceClient.PostAsJsonAsync(new Uri(ServerBaseUri, "anyplace/navigation/route_xy"), r);

            var buildings = await response.Content.ReadAsAsync<RouteLocationToPoi>();
            IsDirty = true;

            return buildings;
        }

        public static async Task<RoutePoiToPoi> GetPoiToPoiRoute(string poi_from,string poi_to)
        {
            RequestPoiToPoi r = new RequestPoiToPoi();
            r.access_token = "api_tester";
            r.pois_from = poi_from;
            r.pois_to = poi_to;

            var response = await AnyPlaceClient.PostAsJsonAsync(new Uri(ServerBaseUri, "anyplace/navigation/route"), r);

            var buildings = await response.Content.ReadAsAsync<RoutePoiToPoi>();
            IsDirty = true;

            return buildings;
        }

        public static async Task<WorldBuilding> GetWorldBuildings()
        {
            RequestWorldBuildings r = new RequestWorldBuildings();
            r.access_token = "api_tester";
            var response = await AnyPlaceClient.PostAsJsonAsync(new Uri(ServerBaseUri, "anyplace/mapping/building/all"), r);

            var buildings = await response.Content.ReadAsAsync<WorldBuilding>();
            IsDirty = true;

            return buildings;
        }

        public static async Task<PoisByBuilding> GetPoisByBuilding(string buid)
        {
            RequestPoisByBuilding r = new RequestPoisByBuilding();
            r.access_token = "api_tester";
            r.buid = buid;
            var response = await AnyPlaceClient.PostAsJsonAsync(new Uri(ServerBaseUri, "anyplace/mapping/pois/all_building"), r);

            var buildings = await response.Content.ReadAsAsync<PoisByBuilding>();
            IsDirty = true;

            return buildings;
        }

        public static async Task<AllBuildingFloors> GetAllBuildingFloors(string buid)
        {
            RequestAllBuildingFloors r = new RequestAllBuildingFloors();
            r.access_token = "api_tester";
            r.buid = buid;
            var response = await AnyPlaceClient.PostAsJsonAsync(new Uri(ServerBaseUri, "anyplace/mapping/floor/all"), r);

            var buildings = await response.Content.ReadAsAsync<AllBuildingFloors>();
            IsDirty = true;

            return buildings;
        }

        public static async Task<HttpResponseMessage> GetTiles(string buid, string floor_number)
        {
            var r = new RequestPoisByFloor
            {
                access_token = "api_tester",
                buid = buid,
                floor_number = floor_number,
                username = "",
                password = ""
            };

            try
            {
                var response = await AnyPlaceClient.PostAsJsonAsync(new Uri(ServerBaseUri, "/anyplace/floortiles/zip/" + buid + "/" + floor_number), r);

                if (!response.IsSuccessStatusCode)
                {
                    return null;
                }

                var x = await response.Content.ReadAsStreamAsync();

                using (var isoStore = IsolatedStorageFile.GetUserStoreForApplication())
                {
                    using (var zipStream = new UnZipper(x))
                    {
                        isoStore.CreateDirectory(buid);
                        isoStore.CreateDirectory(buid + "/" + floor_number);

                        foreach (var file in zipStream.FileNamesInZip)
                        {
                            string fileName = Path.GetFileName(file);

                            if (!string.IsNullOrEmpty(fileName))
                            {
                                Debug.WriteLine(fileName);

                                //save file entry to storage
                                using (var streamWriter =
                                    new BinaryWriter(new IsolatedStorageFileStream(buid + "/" + floor_number + "/" + fileName,
                                                                                   FileMode.Create,
                                                                                   FileAccess.Write, FileShare.Write,
                                                                                   isoStore)))
                                {
                                    Stream fileStream = zipStream.GetFileStream(file);

                                    var buffer = new byte[2048];
                                    int size;
                                    while ((size = fileStream.Read(buffer, 0, buffer.Length)) > 0)
                                    {
                                        streamWriter.Write(buffer, 0, size);
                                    }
                                }
                            }
                        }
                    }
                }

            }
            catch (Exception e) {
                var x = e.Message;
            }
            return null;
        }
    }
}
