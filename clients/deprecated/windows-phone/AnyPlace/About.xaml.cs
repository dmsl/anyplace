using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;
using GestureEventArgs = System.Windows.Input.GestureEventArgs;
using Windows.System;

namespace AnyPlace
{
    public partial class About : PhoneApplicationPage
    {
        public About()
        {
            InitializeComponent();
        }

        private async void Dmsl_OnTap(object sender, GestureEventArgs e)
        {
            await Launcher.LaunchUriAsync(new Uri("http://dmsl.cs.ucy.ac.cy/"));

        }

        private async void Kios_OnTap(object sender, GestureEventArgs e)
        {
            await Launcher.LaunchUriAsync(new Uri("http://www.kios.ucy.ac.cy/"));
        }

        private async void Ucy_OnTap(object sender, GestureEventArgs e)
        {
            await Launcher.LaunchUriAsync(new Uri("http://ucy.ac.cy/el"));
        }
    }
}