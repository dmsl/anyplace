using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;
using System.Device.Location;

namespace AnyPlace
{
    public partial class UCCustomToolTip : UserControl
    {
        private string _description;

        private bool _entrance;

        private GeoCoordinate _coordinate;

        public string Description
        {
            get { return _description; }
            set { _description = value; }
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
    
        public UCCustomToolTip()
        {
            InitializeComponent();
            
        }

        
      
    }
}
