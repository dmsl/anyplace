using System.Windows.Controls;
using System.Device.Location;

namespace AnyPlace
{
    public partial class BuildingPois : UserControl
    {
        private GeoCoordinate _coordinate;
        private bool _entrance;
        public BuildingPois()
        {
            InitializeComponent();
        }

        public bool Entrance
        {
            get { return _entrance; }
            set { _entrance = value; }
        }

        public GeoCoordinate Coordinate
        {
            get { return _coordinate; }
            set { _coordinate = value; }
        }


    }
}
