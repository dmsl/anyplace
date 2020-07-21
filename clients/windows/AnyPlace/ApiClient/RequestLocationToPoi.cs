using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace AnyPlace.ApiClient
{
    public class RequestLocationToPoi
    {
        public string access_token { get; set; }
        public string buid { get; set; }
        public string floor_number { get; set; }
        public string pois_to { get; set; }
        public string coordinates_lat { get; set; }
        public string coordinates_lon { get; set; }
    }
}
