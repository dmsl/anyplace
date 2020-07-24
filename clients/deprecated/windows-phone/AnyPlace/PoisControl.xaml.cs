using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;

namespace AnyPlace
{
    public partial class PoisControl : UserControl
    {
        private string _location;
        private string _distance;
        public PoisControl()
        {
            InitializeComponent();
        }

        public string Location
        {
            get { return _location; }
            set { _location = value; }
        }

        public string Distance
        {
            get { return _distance; }
            set { _distance = value; }
        }
    }
}
