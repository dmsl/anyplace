using System.Globalization;
using System.Windows.Controls.Primitives;
using System.Windows.Shapes;
using AnyPlace.ApiClient;
using AnyPlace.Helpers;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Maps;
using Microsoft.Phone.Maps.Controls;
using Microsoft.Phone.Maps.Services;
using Microsoft.Phone.Shell;
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Device.Location;
using System.IO;
using System.IO.IsolatedStorage;
using System.Runtime.Serialization;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Media.Imaging;
using System.Threading.Tasks;
using GestureEventArgs = System.Windows.Input.GestureEventArgs;
using NetworkInterface = System.Net.NetworkInformation.NetworkInterface;
using AnyPlace.classes;
using Microsoft.Phone.Maps.Toolkit;


namespace AnyPlace
{
    public partial class MainPage : PhoneApplicationPage
    {
        #region Variables

        private bool isDownloading = false;

        public const int MinZoomLevel = 2;
        public const int MaxZoomLevel = 20;
        private GeoCoordinateWatcher _locationManager;
        private MapLayer mypositionlayer, poisByFloor_layer, routedetails_layer, mapHoldLayer;
        private MapOverlay mypositionoverlay, poisbyflooroverlay, mapHoldOverlay;
        PoisByFloor _y = new PoisByFloor();
        private bool _first = true;
        private double _myLatitude = 0.0;
        private double _myLongitude = 0.0;
        bool show = false;
        private GeocodeQuery _myGeocodeQuery;
        private List<GeoCoordinate> GetRoute = new List<GeoCoordinate>();
        private GeoCoordinate _tapLocation;
        TextBox _txtSearch = new TextBox();
        private GeocodeQuery _mygeocodequery;
        private MapRoute _myMapRoute;
        private RouteQuery MyQuery;

        // navigation route details
        private List<RouteDetails> routeNavDetails = new List<RouteDetails>();

        // dropdown search
        List<string> _searchItems = new List<string>();

        double _initialPosition;
        bool _viewMoved = false;

        // pois
        PoisControl _control;

        // downloadedstaffs 
        PoisByBuilding _poisByBuilding;
        AllBuildingFloors _allbuildingfloors;
        WorldBuilding _worldbuildings;

        MapLayer buildingsLayer;
        MapOverlay buildingsOverlay;

        // user settings variables
        bool _zoomCheck;
        private bool _downloadCkeck;
        private bool _trackmeCheck;
        private bool _tilesCheck;

        bool gps_on = false;
        bool ready = false;

        #endregion


        private readonly MapGestureBase _rotateGesture;
        public MainPage()
        {

            InitializeComponent();


            // rotation
            _rotateGesture = new MapRotationGesture(Mymap);


            //initialize settings
            GetSettings();

            _wmstileprovider = new WMSTiles();
            _wmstileprovider.setBuidAndFloor("", "");

            // tracking current position
            LocalizeGeoWatcher();

            _buildingSearch = "";

            // read data from storage
            IsolatedStorageDataRead();

            // set curren position to 0,0
            mypositionlayer = new MapLayer();
            mypositionoverlay = new MapOverlay
            {
                GeoCoordinate = new GeoCoordinate(0, 0),
                PositionOrigin = new Point(0.5, 1.0)
            };

            Mymap.Height = Application.Current.Host.Content.ActualHeight - 50;

            double width = Application.Current.Host.Content.ActualWidth;
            stack.MaxWidth = width;
            stack.Width = width;
            grid_search.Width = width;
            grid_search.MaxWidth = width;

            addBuildingsToMap(null);
            AddBuildingToSearch();
        }



        private void IsolatedStorageDataRead()
        {
            ReadWorldBuildingsFromIsolatedStorage();
            createBuildingPicker();
        }




        // locate user position
        private void LocalizeGeoWatcher()
        {
            loading.Visibility = Visibility.Visible;
            loading.IsIndeterminate = true;

            // create the location watcher
            _locationManager = new GeoCoordinateWatcher(GeoPositionAccuracy.High);

            // location change in meters
            _locationManager.MovementThreshold = 3;

            // when status changed
            _locationManager.StatusChanged += locationManager_getStatus;

            //when position changed
            _locationManager.PositionChanged += locationManager_getPosition;

            // start the watcher
            _locationManager.Start();
        }

        private void locationManager_getStatus(object sender, GeoPositionStatusChangedEventArgs e)
        {
            switch (e.Status)
            {
                case GeoPositionStatus.Initializing:
                    break;
                case GeoPositionStatus.Ready:
                    if (start_nav)
                        getNavigationOnStart(bid, pid);
                    break;
                case GeoPositionStatus.Disabled:
                    ready = false;
                    loading.Visibility = Visibility.Collapsed;
                    loading.IsIndeterminate = false;
                    gps_on = false;
                    if (Mymap.Layers.Contains(mypositionlayer))
                        Mymap.Layers.Remove(mypositionlayer);
                    MessageBox.Show("Location services are disabled. To enable them, Goto Settings - Location - Enable Location Services.", "Location services", MessageBoxButton.OK);
                    break;
                case GeoPositionStatus.NoData:
                    break;
            }
        }


        // get current position
        private void locationManager_getPosition(object sender, GeoPositionChangedEventArgs<GeoCoordinate> e)
        {
            if (!ready)
                ready = true;
            gps_on = true;
            _myLatitude = e.Position.Location.Latitude;
            _myLongitude = e.Position.Location.Longitude;

            positionChanged();
        }

        private void positionChanged()
        {
            if (_myLatitude == 0.0 || _myLongitude == 0.0)
                return;

            if (Mymap.Layers.Contains(mypositionPoi))
            {
                Mymap.Layers.Remove(mypositionPoi);
                mypositionPoi.Clear();
            }

            if (_trackmeCheck || _first)
            {
                DrawMyPosition(_myLatitude, _myLongitude);

                if (_first)
                {
                    // center map
                    Mymap.Center.Latitude = _myLatitude;
                    Mymap.Center.Longitude = _myLongitude;
                    Mymap.SetView(Mymap.Center, 19, MapAnimationKind.Parabolic);

                }

            }

            if (Mymap.Layers.Contains(detailPois))
            {
                Mymap.Layers.Remove(detailPois);
                detailPois.Clear();
            }

            // set map view
            if (_first)
            {
                if (ContentPanel.Visibility != Visibility.Visible && gd_result.Visibility != Visibility.Visible
                    && directions_grid.Visibility != Visibility.Visible)
                    ApplicationBar.IsVisible = true;

                if (!isDownloading)
                {
                    loading.Visibility = Visibility.Collapsed;
                    loading.IsIndeterminate = false;
                }

                //var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
                //if (!settings1.FileExists("WorldBuilding"))
                //{
                //    var result = MessageBox.Show("You have to download the required maps to use all the features. Download now?", "Download Required Maps", MessageBoxButton.OKCancel);

                //    if (result == MessageBoxResult.OK)
                //    {
                //        var isNetwork = NetworkInterface.GetIsNetworkAvailable();

                //        if (isNetwork)
                //        {
                //            ApplicationBar.IsVisible = false;
                //            downloading.Visibility = Visibility.Visible;
                //            DownloadRequireStaff();
                //        }
                //        else
                //            MessageBox.Show("No Internet Connection. Please check your internet connection and try again!");
                //    }
                //}


                _first = false;
            }

        }



        void addBuildingsToMap(string notBuid)
        {

            if (_worldbuildings == null)
                return;

            if (buildingsLayer != null)
                buildingsLayer.Clear();
            else
                buildingsLayer = new MapLayer();


            if (Mymap.Layers.Contains(buildingsLayer))
                Mymap.Layers.Remove(buildingsLayer);

            foreach (var building in _worldbuildings.buildings)
            {
                if (building.buid.Equals(notBuid))
                    continue;
                var lat = Double.Parse(building.coordinates_lat, CultureInfo.InvariantCulture);
                var lon = Double.Parse(building.coordinates_lon, CultureInfo.InvariantCulture);
                var poi = new BuildingPoi { Coordinate = new GeoCoordinate(lat, lon), Buid = building.buid };
                var imagePoiLocation = new Image
                {
                    Source = new BitmapImage(new Uri("/Assets/MapPin.png", UriKind.Relative)),
                    DataContext = poi
                };

                buildingsOverlay = new MapOverlay();

                imagePoiLocation.Tap += loadClickedBuilding;
                buildingsOverlay.Content = imagePoiLocation;
                buildingsOverlay.PositionOrigin = new Point(0.5, 0.5);
                buildingsOverlay.GeoCoordinate = new GeoCoordinate(lat, lon);
                buildingsLayer.Add(buildingsOverlay);
            }

            Mymap.Layers.Add(buildingsLayer);
        }

        private async void loadClickedBuilding(object sender, GestureEventArgs e)
        {

            if (isDownloading)
                return;
            removeSearchButton();
            var img = (Image)sender;
            var poi = img.DataContext as BuildingPoi;
            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.DirectoryExists(poi.Buid) && checkCompleted(poi.Buid))
            {
                // load staff to the map
                clear();
                ReadAllBuildingFloorsFromIsolatedStorage(poi.Buid);
                ReadPoisByBuildingFromIsolatedStorage(poi.Buid);

                addBuildingsToMap(poi.Buid);
                buildLoaded = true;
                loadBuidingToMap(poi.Buid);
                createAppBarMenuItem();
            }
            else
            {

                isDownloading = true;
                //ApplicationBar.IsVisible = false;
                //downloading.Visibility = Visibility.Visible;

                loading.Visibility = Visibility.Visible;
                loading.IsIndeterminate = true;

                clear();
                // download staff and show them to the map
                var fsdf = await DownloadPoisBuildStaff(poi.Buid);
                ReadAllBuildingFloorsFromIsolatedStorage(poi.Buid);
                ReadPoisByBuildingFromIsolatedStorage(poi.Buid);

                addBuildingsToMap(poi.Buid);
                buildLoaded = true;
                loadBuidingToMap(poi.Buid);

                loading.Visibility = Visibility.Collapsed;
                loading.IsIndeterminate = false;

                createAppBarMenuItem();
                isDownloading = false;
            }
            _searchBuilding = true;
        }

        private async Task<object> DownloadPoisBuildStaff(string buid)
        {

            //Dispatcher.BeginInvoke(() =>
            //{
            //    txt_download.Text = "Download building data..";
            //});

            var building = await CustomPushpinWp8APIClient.GetPoisByBuilding(buid);
            var bFloors = await CustomPushpinWp8APIClient.GetAllBuildingFloors(buid);

            int counterh = 0;

            foreach (var obj7 in bFloors.floors)
            {
                if (obj7.buid.Equals(buid))
                {
                    //Dispatcher.BeginInvoke(() =>
                    //{
                    //    txt_download.Text = "Download floor " + (counterh + 1) + " of " + bFloors.floors.Count;
                    //});
                    await CustomPushpinWp8APIClient.GetTiles(obj7.buid, obj7.floor_number);
                    counterh++;
                }

            }


            //Dispatcher.BeginInvoke(() =>
            //{
            //    txt_download.Text = "Saving data..";
            //});
            writePoisByBuildingToIsolatedStorage(building, buid);
            writeAllBuildingFloorsToIsolatedStorage(bFloors, buid);
            writeCompletedToIsolatedStorage(buid);
            ApplicationBar.IsVisible = true;
            return null;
        }

        private async void loadBuidingToMap(string buid)
        {
            //if (Mymap.MapElements.Contains(_polyline))
            //{
            //    if (_tileLoaded && !_sourcePoi && !_externalSource && !_searchBuilding)
            //    {
            //        Mymap.MapElements.Remove(_polyline);
            //        Mymap.MapElements.Clear();
            //        GetNavigationRoutesBuilding(_selectedBuild, _floorPoiTo, _selectedFloor, _myLatitude.ToString(CultureInfo.InvariantCulture),
            //            _myLongitude.ToString(CultureInfo.InvariantCulture));
            //    }
            //}

            GeoCoordinate geo = Mymap.Center; ;
            foreach (var obj in _worldbuildings.buildings)
            {
                if (obj.buid.Equals(buid))
                {
                    geo = new GeoCoordinate(double.Parse(obj.coordinates_lat, CultureInfo.InvariantCulture), double.Parse(obj.coordinates_lon, CultureInfo.InvariantCulture));
                    break;
                }

            }

            Mymap.SetView(geo, 20, MapAnimationKind.Parabolic);

            if (_allbuildingfloors != null)
            {
                List<string> floors = new List<string>();

                AddBuildingToSearch();

                foreach (var x in _allbuildingfloors.floors)
                {
                    _foundTile = false;
                    _selectedBuild = x.buid;
                    _selectedFloor = x.floor_number;

                    floors.Add(x.floor_number);
                }


                Dispatcher.BeginInvoke(() =>
                {
                    txt_cur.Text = _selectedFloor;
                });


                if (!_foundTile)
                {
                    _foundTile = false;
                    _tileLoaded = true;

                    if (_tilesCheck)
                    {
                        if (_tileLoaded)
                        {
                            try
                            {
                                _wmstileprovider.setBuidAndFloor(_selectedBuild, _selectedFloor);
                                if (!Mymap.TileSources.Contains(_wmstileprovider))
                                    Mymap.TileSources.Add(_wmstileprovider);
                            }
                            catch (Exception e)
                            {
                            }


                            ViewPoisByBuilding();
                        }
                    }
                    else
                    {
                        if (Mymap.TileSources.Contains(_wmstileprovider))
                            Mymap.TileSources.Remove(_wmstileprovider);
                    }
                    //floorButton.Visibility = Visibility.Visible;
                    _foundTile = true;
                }
            }
        }

        private async void DownloadRequireStaff()
        {
            try
            {
                //Dispatcher.BeginInvoke(() =>
                //{
                //    txt_download.Text = "Download building data..";
                //});

                var worldbuildings1 = await CustomPushpinWp8APIClient.GetWorldBuildings();

                //Dispatcher.BeginInvoke(() =>
                //{
                //    txt_download.Text = "Saving data..";
                //});

                writeWorldBuildingsToIsolatedStorage(worldbuildings1);

                IsolatedStorageDataRead();

                // add buildings to the map
                addBuildingsToMap(null);

                ReadWorldBuildingsFromIsolatedStorage();
                AddBuildingToSearch();
                
            }
            catch
            {

                MessageBox.Show("Failed to download maps. Check your internet connection and try again!");
            }

            isDownloading = false;
            loading.Visibility = Visibility.Collapsed;
            loading.IsIndeterminate = false;

            //deleteIsostorageFiles();
        }


        private void DrawMyPosition(double lat, double lon)
        {
            mypositionlayer.Clear();
            if (Mymap.Layers.Count != 0)
                Mymap.Layers.Remove(mypositionlayer);
            var poi = new MyLocationPoi { cordinate = new GeoCoordinate(lat, lon) };
            //var imageMylocation = new Image
            //{
            //    Source = new BitmapImage(new Uri("/Assets/location.png", UriKind.Relative)),
            //    DataContext = poi
            //};
            var imageMylocation = new Ellipse
            {
                Fill = new SolidColorBrush(Colors.Red),
                Stroke = new SolidColorBrush(Colors.Black),
                Width = 25,
                Height = 25,
                DataContext = poi
            };
            imageMylocation.Tap += image_mylocation_Tap;
            mypositionoverlay.Content = imageMylocation;
            mypositionoverlay.PositionOrigin = new Point(0.5, 0.5);
            mypositionoverlay.GeoCoordinate.Latitude = lat;
            mypositionoverlay.GeoCoordinate.Longitude = lon;
            mypositionlayer.Add(mypositionoverlay);
            Mymap.Layers.Add(mypositionlayer);
        }




        bool buildLoaded;

        void AddBuildingToSearch()
        {
            _searchItems = new List<string>();
            if (_worldbuildings != null)
            {
                foreach (var obj in _worldbuildings.buildings)
                {
                    _searchItems.Add(obj.name);
                }

                autoCompleteBox.ItemsSource = _searchItems;
            }
            addSearchButton();
        }


        void addSearchButton()
        {

            if (buildLoaded)
            {
                _searchItems = new List<string>();
                foreach (var obj in _poisByBuilding.pois)
                {
                    _searchItems.Add(obj.name);
                    if (!obj.description.Equals("-") && !obj.description.Equals(obj.name))
                        _searchItems.Add(obj.description);
                }

                autoCompleteBox.ItemsSource = _searchItems;

                floorPanel.Visibility = Visibility.Visible;



                Dispatcher.BeginInvoke(() =>
                {
                    txt_msg.Text = findBuildName();
                });

                closeGrid.Visibility = Visibility.Visible;
            }

            //foreach (var button in ApplicationBar.Buttons)
            //{
            //    ApplicationBarIconButton y = (ApplicationBarIconButton)button;
            //    y.IsEnabled = true;
            //}


        }

        void removeSearchButton()
        {
            _searchItems = new List<string>();
            if (_worldbuildings != null)
            {
                foreach (var obj in _worldbuildings.buildings)
                {
                    _searchItems.Add(obj.name);
                }

                autoCompleteBox.ItemsSource = _searchItems;
            }

            //foreach (var button in ApplicationBar.Buttons)
            //{
            //    ApplicationBarIconButton y = (ApplicationBarIconButton)button;
            //    y.IsEnabled = false;
            //}
            floorPanel.Visibility = Visibility.Collapsed;
            closeGrid.Visibility = Visibility.Collapsed;
        }

        string findBuildName()
        {
            foreach (var obj in _worldbuildings.buildings)
            {
                if (obj.buid.Equals(_selectedBuild))
                {
                    return obj.name;
                }
            }
            return null;
        }

        List<string> _poisearch;
        private async void GetPoisByFloor(string build, string floor)
        {
            loading.Visibility = Visibility.Visible;
            loading.IsIndeterminate = true;
            poisByFloor_layer = new MapLayer();
            if (Mymap.Layers.Contains(poisByFloor_layer))
                Mymap.Layers.Remove(poisByFloor_layer);
            try
            {
                _y = await CustomPushpinWp8APIClient.GetPoisByFloor(build, floor);
                foreach (var obj in _y.pois)
                {
                    if (!obj.pois_type.Equals("None"))
                    {
                        var poi = new FloorPoisDetails
                        {
                            Coordinates =
                                new GeoCoordinate(double.Parse(obj.coordinates_lat, CultureInfo.InvariantCulture), double.Parse(obj.coordinates_lon, CultureInfo.InvariantCulture)),
                            Description = obj.name,
                            Information = obj.description,
                            poiid = obj.puid,
                            poiFloor = obj.floor_number
                        };
                        if (obj.is_building_entrance != null)
                            poi.Entrange = bool.Parse(obj.is_building_entrance);
                        else
                            poi.Entrange = false;

                        var imageFloorPois = new Image
                        {
                            Source = new BitmapImage(new Uri("/Assets/other_loc.png", UriKind.Relative)),
                            DataContext = poi
                        };
                        imageFloorPois.Tap += image_floor_pois_Tap;

                        poisbyflooroverlay = new MapOverlay
                        {
                            Content = imageFloorPois,
                            GeoCoordinate = poi.Coordinates,
                            PositionOrigin = new Point(0.5, 1.0)
                        };
                        poisByFloor_layer.Add(poisbyflooroverlay);
                    }
                }
                Mymap.Layers.Add(poisByFloor_layer);
            }
            catch
            {
                loading.Visibility = Visibility.Collapsed;
                loading.IsIndeterminate = false;
                downloading.Visibility = Visibility.Collapsed;
                MessageBox.Show("Can not load floor pois. Please check your connection and try again!");
            }
            loading.Visibility = Visibility.Collapsed;
            loading.IsIndeterminate = false;
            downloading.Visibility = Visibility.Collapsed;
        }


        #region IsolatedStorage

        private void DeleteIsostorageFiles()
        {
            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists("PoisByBuilding"))
            {
                settings1.DeleteFile("PoisByBuilding");
                MessageBox.Show("Items PoisByBuilding removed successfully.");
            }
            if (settings1.FileExists("WorldBuildings"))
            {
                settings1.DeleteFile("WorldBuildings");
                MessageBox.Show("Items WorldBuildings removed successfully.");
            }
            if (settings1.FileExists("AllBuildingFloors"))
            {
                settings1.DeleteFile("AllBuildingFloors");
                MessageBox.Show("Items AllBuildingFloors removed successfully.");
            }
        }

        private void ReadPoisByBuildingFromIsolatedStorage(string directory)
        {

            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists(directory + "/PoisByBuilding"))
            {
                using (var fileStream = settings1.OpenFile(directory + "/PoisByBuilding", FileMode.Open))
                {
                    var serializer = new DataContractSerializer(typeof(PoisByBuilding));
                    _poisByBuilding = (PoisByBuilding)serializer.ReadObject(fileStream);
                }
            }
        }

        private void ReadWorldBuildingsFromIsolatedStorage()
        {

            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists("WorldBuilding"))
            {
                using (var fileStream = settings1.OpenFile("WorldBuilding", FileMode.Open))
                {
                    var serializer = new DataContractSerializer(typeof(WorldBuilding));
                    _worldbuildings = (WorldBuilding)serializer.ReadObject(fileStream);
                }
            }
        }

        private void ReadAllBuildingFloorsFromIsolatedStorage(string directory)
        {

            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists(directory + "/AllBuildingFloors"))
            {
                using (var fileStream = settings1.OpenFile(directory + "/AllBuildingFloors", FileMode.Open))
                {
                    var serializer = new DataContractSerializer(typeof(AllBuildingFloors));
                    _allbuildingfloors = (AllBuildingFloors)serializer.ReadObject(fileStream);
                }
            }
        }

        bool checkCompleted(string directory)
        {
            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists(directory + "/Completed"))
            {
                return true;
            }
            return false;
        }

        private void writeCompletedToIsolatedStorage(string directory)
        {
            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists(directory + "/Completed"))
            {
                settings1.DeleteFile(directory + "/Completed");
            }
            else
            {
                settings1.CreateDirectory(directory);
            }
            using (var fileStream = settings1.OpenFile(directory + "/Completed", FileMode.Create))
            {
                var serializer = new DataContractSerializer(typeof(String));
                serializer.WriteObject(fileStream, "done");
            }
        }

        private void writePoisByBuildingToIsolatedStorage(PoisByBuilding v, string directory)
        {
            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists(directory + "/PoisByBuilding"))
            {
                settings1.DeleteFile(directory + "/PoisByBuilding");
            }
            else
            {
                settings1.CreateDirectory(directory);
            }
            using (var fileStream = settings1.OpenFile(directory + "/PoisByBuilding", FileMode.Create))
            {
                var serializer = new DataContractSerializer(typeof(PoisByBuilding));
                serializer.WriteObject(fileStream, v);
            }
        }

        private void writeWorldBuildingsToIsolatedStorage(WorldBuilding v)
        {
            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists("WorldBuilding"))
            {
                settings1.DeleteFile("WorldBuilding");
            }
            using (var fileStream = settings1.OpenFile("WorldBuilding", FileMode.Create))
            {
                var serializer = new DataContractSerializer(typeof(WorldBuilding));
                serializer.WriteObject(fileStream, v);
            }
        }

        private void writeAllBuildingFloorsToIsolatedStorage(AllBuildingFloors v, string directory)
        {
            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (settings1.FileExists(directory + "/AllBuildingFloors"))
            {
                settings1.DeleteFile(directory + "/AllBuildingFloors");
            }
            else
            {
                settings1.CreateDirectory(directory);
            }
            using (var fileStream = settings1.OpenFile(directory + "/AllBuildingFloors", FileMode.Create))
            {
                var serializer = new DataContractSerializer(typeof(AllBuildingFloors));
                serializer.WriteObject(fileStream, v);
            }
        }

        #endregion

        #region Pois Tap and Add


        bool _isSearch;
        private void AddPoi(GeoCoordinate coordinates)
        {
            if (Mymap.Layers.Contains(mapHoldLayer))
                Mymap.Layers.Remove(mapHoldLayer);

            _control = new PoisControl
            {
                pb_procress = { Visibility = Visibility.Visible },
                txt_location = { Text = "Loading..", Visibility = Visibility.Visible },
                txt_distance = { Visibility = Visibility.Collapsed }
            };
            _control.btn_source.Click += btn_poiscontrol_Click;
            _control.btn_source.DataContext = coordinates;
            _control.btn_navigateHere.Click += btn_navigateHere_Click;
            _control.grid_details.Tap += grid_details_Tap;
            mapHoldOverlay = new MapOverlay
            {
                Content = _control,
                GeoCoordinate = coordinates,
                PositionOrigin = new Point(0.5, 0.95)
            };
            mapHoldLayer = new MapLayer { mapHoldOverlay };
            Mymap.Layers.Add(mapHoldLayer);
            if (!_isSearch)
                Mymap.SetView(_tapLocation, 19, MapAnimationKind.Parabolic);
            else
                Mymap.SetView(coordinates, 19, MapAnimationKind.Parabolic);
            LocationSearch(coordinates);
        }

        private MapOverlay _sourceOverlay;
        private MapLayer _sourceLayer;

        private void btn_poiscontrol_Click(object sender, RoutedEventArgs e)
        {
            var btn = sender as Button;
            var context = btn.DataContext as GeoCoordinate;

            if (Mymap.Layers.Contains(_sourceLayer))
                Mymap.Layers.Remove(_sourceLayer);

            _sourcePoi = false;
            _externalSource = true;
            _externalCoordinates = context;
            if (Mymap.MapElements.Contains(_polyline))
            {
                Mymap.MapElements.Remove(_polyline);
                NavigateToFloorPoi();
            }
            else
            {

                var imageMylocation = new Image
                {
                    Source = new BitmapImage(new Uri("/Assets/source.png", UriKind.Relative))
                };
                _sourceOverlay = new MapOverlay();
                _sourceLayer = new MapLayer();
                _sourceOverlay.Content = imageMylocation;
                _sourceOverlay.GeoCoordinate = context;
                _sourceOverlay.PositionOrigin = new Point(0.5, 1);
                _sourceLayer.Add(_sourceOverlay);
                Mymap.Layers.Add(_sourceLayer);
            }
        }

        void grid_details_Tap(object sender, GestureEventArgs e)
        {
            if (Mymap.Layers.Contains(mapHoldLayer))
            {
                Mymap.Layers.Remove(mapHoldLayer);
            }
            // MessageBox.Show("here");
        }

        void btn_navigateHere_Click(object sender, RoutedEventArgs e)
        {
            if (gd_result.Visibility == Visibility.Visible)
                gd_result.Visibility = Visibility.Collapsed;
            if (Mymap.Layers.Contains(_sourceLayer))
                Mymap.Layers.Remove(_sourceLayer);
            nav_here();
        }

        private void nav_here()
        {
            Mymap.Layers.Remove(mapHoldLayer);
            if (!_sourcePoi)
            {
                ApplicationBar.IsVisible = false;
                MoveViewWindow(-70);
                stackpanelOpen();
                directions_grid.Visibility = Visibility.Visible;
                calcProgrBar.Visibility = Visibility.Visible;
                time.Visibility = Visibility.Collapsed;
                distance.Visibility = Visibility.Collapsed;
                calcProgrBar.IsIndeterminate = true;
                GetNavigationRoute(_tapLocation);
                Mymap.SetView(_tapLocation, 20, MapAnimationKind.Parabolic);
            }
            else
            {
                MessageBox.Show("You can not get directions from building poi!");
            }

        }

        private void LocationSearch(GeoCoordinate coordinates)
        {
            GeoCoordinate g = new GeoCoordinate();

            if (_externalSource)
                g = _externalCoordinates;
            else
            {
                g = new GeoCoordinate(_myLatitude, _myLongitude);
            }

            _myGeocodeQuery = new GeocodeQuery
            {
                SearchTerm = coordinates.ToString(),
                GeoCoordinate = g
            };
            _myGeocodeQuery.QueryCompleted += SearchQueryCompleted;
            _myGeocodeQuery.QueryAsync();
        }
        private void SearchQueryCompleted(object sender, QueryCompletedEventArgs<IList<MapLocation>> e)
        {
            if (e.Error == null)
            {
                if (e.Result.Count > 0)
                {
                    foreach (var obj in e.Result)
                    {

                        if (obj.Information.Address.City == "")
                            Dispatcher.BeginInvoke(() =>
                            {
                                _control.txt_location.Text = "No Location Details";
                            });
                        else
                            Dispatcher.BeginInvoke(() =>
                            {
                                _control.txt_location.Text = obj.Information.Address.City;
                            });
                        // distance is in meters
                        getSearchLocation(_tapLocation);
                    }
                }
                else
                {
                    _control.pb_procress.Visibility = Visibility.Collapsed;
                    Dispatcher.BeginInvoke(() =>
                    {
                        _control.txt_location.Text = "No Location Details";
                    });
                }
            }
        }


        #endregion



        #region User Position



        MapLayer mypositionPoi = new MapLayer();
        void image_mylocation_Tap(object sender, GestureEventArgs e)
        {
            if (Mymap.Layers.Contains(mypositionPoi))
            {
                Mymap.Layers.Remove(mypositionPoi);
                mypositionPoi.Clear();
            }

            if (Mymap.Layers.Contains(detailPois))
            {
                Mymap.Layers.Remove(detailPois);
                detailPois.Clear();
            }

            var img = (Ellipse)sender;
            var poi = img.DataContext as MyLocationPoi;
            var over = new MapOverlay { GeoCoordinate = poi.cordinate };
            var control = new MyLocationPoiControl();
            control.grd_loc.Tap += grd_loc_Tap;
            if (!_sourcePoi && !_externalSource)
                control.btn_source.Visibility = Visibility.Collapsed;
            control.btn_source.Click += my_locabtnclick;
            over.Content = control;
            over.PositionOrigin = new Point(0.5, 1.0);
            mypositionPoi.Add(over);
            Mymap.Layers.Add(mypositionPoi);

        }

        private void my_locabtnclick(object sender, RoutedEventArgs e)
        {
            if (Mymap.Layers.Contains(_sourceLayer))
                Mymap.Layers.Remove(_sourceLayer);
            _externalSource = false;
            _sourcePoi = false;
            if (_routePoi != null)
            {
                if (_routePoi.pois != null)
                {
                    if (_routePoi.pois.Count > 0)
                    {
                        if (Mymap.Layers.Contains(indoorLayer))
                            Mymap.Layers.Remove(indoorLayer);
                        _routePoi.pois.Clear();
                        // clear directions
                        if (GetRoute.Count != 0)
                        {
                            GetRoute.Clear();
                            if (_myMapRoute != null)
                            {
                                routedetails_layer.Clear();
                                Mymap.RemoveRoute(_myMapRoute);
                                Mymap.Layers.Remove(routedetails_layer);
                                routeNavDetails.Clear();
                            }
                        }
                    }
                }

            }

            if (_floorPoiTo != null)
                if (Mymap.MapElements.Contains(_polyline))
                {
                    // if we are into the building and no source poi set get navigations route from location to poi
                    if (_tileLoaded && !_sourcePoi)
                    {
                        Mymap.MapElements.Clear();
                        GetNavigationRoutesBuilding(_selectedBuild, _floorPoiTo, _selectedFloor, _myLatitude.ToString(CultureInfo.InvariantCulture),
                            _myLongitude.ToString(CultureInfo.InvariantCulture));
                    }
                    else
                    {
                        // if we set source poi get navigations route from source poi else get all routes
                        if (_sourcePoi)
                            BuildingSourcePoi();
                        else
                        {
                            NavigateToFloorPoi();
                        }
                    }
                }

        }

        void grd_loc_Tap(object sender, GestureEventArgs e)
        {
            if (Mymap.Layers.Contains(mypositionPoi))
            {
                Mymap.Layers.Remove(mypositionPoi);
                mypositionPoi.Clear();
            }

            if (Mymap.Layers.Contains(detailPois))
            {
                Mymap.Layers.Remove(detailPois);
                detailPois.Clear();
            }
        }

        #endregion

        #region Map




        private void MapView_Loaded(object sender, RoutedEventArgs e)
        {
            // make transparent the notification bar then hide it
            SystemTray.Opacity = 0.0;
            SystemTray.IsVisible = false;

            if (!_loaded)
            {
                _loaded = true;
                // set the zoom level
                Mymap.ZoomLevel = 2;
            }

            _wmstileprovider = new WMSTiles();
            _wmstileprovider.setBuidAndFloor("", "");

            if (_tilesCheck)
                Mymap.TileSources.Add(_wmstileprovider);

            var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
            if (!settings1.FileExists("WorldBuilding"))
            {
                //var result = MessageBox.Show("You have to download the required maps to use all the features. Download now?", "Download Required Maps", MessageBoxButton.OKCancel);

                //if (result == MessageBoxResult.OK)
                //{
                    var isNetwork = NetworkInterface.GetIsNetworkAvailable();

                    if (isNetwork)
                    {
                        if (isDownloading)
                            return;
                        isDownloading = true;
                        loading.Visibility = Visibility.Visible;
                        loading.IsIndeterminate = true;
                        DownloadRequireStaff();
                        
                    }
                    //else
                    //    MessageBox.Show("No Internet Connection. Please check your internet connection and try again!");
                //}
            }
        }

        
        private void Map_Loaded(object sender, RoutedEventArgs e)
        {
            MapsSettings.ApplicationContext.ApplicationId = "<applicationid>";
            MapsSettings.ApplicationContext.AuthenticationToken = "<authenticationtoken>";
        }
       


        private void Mymap_Hold(object sender, GestureEventArgs e)
        {
            if (Mymap.Layers.Contains(floorPoisDetail))
                Mymap.Layers.Remove(floorPoisDetail);
            if (!_tileLoaded)
                RemoveIndoorStaff();
            var p = e.GetPosition(this.Mymap);
            _tapLocation = new GeoCoordinate();
            _tapLocation = Mymap.ConvertViewportPointToGeoCoordinate(p);
            _isSearch = false;
            AddPoi(_tapLocation);
        }

        private void Mymap_Tap(object sender, GestureEventArgs e)
        {
            if (Mymap.Layers.Contains(mapHoldLayer))
                Mymap.Layers.Remove((mapHoldLayer));
        }

        #endregion

        #region Application Bar Buttons

        // find current location on the map
        public void TrackMe(object sender, EventArgs e)
        {
            if (!gps_on)
                LocalizeGeoWatcher();
            // ToDo
            if (ready)
            {
                DrawMyPosition(_myLatitude, _myLongitude);
                Mymap.Center.Latitude = _myLatitude;
                Mymap.Center.Longitude = _myLongitude;
                Mymap.SetView(Mymap.Center, 19, MapAnimationKind.Parabolic);
            }

        }

        private void show_options(object sender, RoutedEventArgs e)
        {
            CanvasMenu.Visibility = Visibility.Collapsed;
            ApplicationBar.IsVisible = false;
            MapOptions.Visibility = Visibility.Visible;
        }

        //zoom in button press
        public void ZoomIn(object sender, EventArgs e)
        {
            Mymap.ZoomLevel = Math.Min(Mymap.ZoomLevel + 1, 20);
        }

        // zoom out button press
        public void ZoomOut(object sender, EventArgs e)
        {
            Mymap.ZoomLevel = Math.Max(Mymap.ZoomLevel - 1, 1);
        }


        private void Search(object sender, EventArgs e)
        {
            if (_searchItems == null)
            {
                MessageBox.Show("There are no buildings to search. Please download maps!");
                return;
            }


            if (buildLoaded)
            {
                closeGrid.Visibility = Visibility.Collapsed;
                grid_search.Visibility = Visibility.Visible;
                all_builds.Visibility = Visibility.Collapsed;
                Dispatcher.BeginInvoke(() =>
                {
                    txt_msg1.Text = findBuildName();
                });
            }
            else
            {
                grid_search.Visibility = Visibility.Collapsed;
                all_builds.Visibility = Visibility.Visible;
            }

            //_searchItems.Clear();
            autoCompleteBox.Text = string.Empty;
            //SystemTray.IsVisible = true;
            //SystemTray.Opacity = 1;
            ApplicationBar.IsVisible = false;
            autoCompleteBox.UpdateLayout();
            autoCompleteBox.Focus();
            autoCompleteBox.Visibility = Visibility.Visible;
            ContentPanel.Visibility = Visibility.Visible;

        }

        void autoCompleteBox_DropDownOpening(object sender, RoutedPropertyChangingEventArgs<bool> e)
        {
            if (!_autocompleteFocus)
                e.Cancel = true;
        }

        void autoCompleteBox_GotFocus(object sender, RoutedEventArgs e)
        {
            count++;
            _autocompleteFocus = true;
        }

        bool _autocompleteFocus;
        int count = 0;
        void autoCompleteBox_LostFocus(object sender, RoutedEventArgs e)
        {
            _autocompleteFocus = false;
        }


        private void road_click(object sender, RoutedEventArgs e)
        {
            Mymap.CartographicMode = MapCartographicMode.Road;
            ApplicationBar.IsVisible = true;
            MapOptions.Visibility = Visibility.Collapsed;
            CanvasMenu.Visibility = Visibility.Visible;
        }

        private void aerial_click(object sender, RoutedEventArgs e)
        {
            Mymap.CartographicMode = MapCartographicMode.Aerial;
            ApplicationBar.IsVisible = true;
            MapOptions.Visibility = Visibility.Collapsed;
            CanvasMenu.Visibility = Visibility.Visible;
        }

        private void hybrid_click(object sender, RoutedEventArgs e)
        {
            Mymap.CartographicMode = MapCartographicMode.Hybrid;
            ApplicationBar.IsVisible = true;
            MapOptions.Visibility = Visibility.Collapsed;
            CanvasMenu.Visibility = Visibility.Visible;
        }

        private void terrain_click(object sender, RoutedEventArgs e)
        {
            Mymap.CartographicMode = MapCartographicMode.Terrain;
            ApplicationBar.IsVisible = true;
            MapOptions.Visibility = Visibility.Collapsed;
            CanvasMenu.Visibility = Visibility.Visible;
        }

        private void light_click(object sender, RoutedEventArgs e)
        {
            Mymap.ColorMode = MapColorMode.Light;
            ApplicationBar.IsVisible = true;
            MapOptions.Visibility = Visibility.Collapsed;
            CanvasMenu.Visibility = Visibility.Visible;
        }

        private void dark_click(object sender, RoutedEventArgs e)
        {
            Mymap.ColorMode = MapColorMode.Dark;
            ApplicationBar.IsVisible = true;
            MapOptions.Visibility = Visibility.Collapsed;
            CanvasMenu.Visibility = Visibility.Visible;
        }

        protected override void OnNavigatedTo(System.Windows.Navigation.NavigationEventArgs e)
        {
            // Get a dictionary of query string keys and values. 
            IDictionary<string, string> queryStrings = this.NavigationContext.QueryString;

            // Ensure that the "CategoryID" key is present. 
            if (queryStrings.ContainsKey("BuildId"))
            {
                //Display Category ID 
                bid = queryStrings["BuildId"];

                if (!bid.Equals("none"))
                {
                    pid = queryStrings["PoiId"];

                    start_nav = true;
                }

            }

            if (PhoneApplicationService.Current.State.Count > 0)
            {
                var download = PhoneApplicationService.Current.State["downloaded"].ToString();

                GetSettings();
                _downloadCkeck = bool.Parse(download);

                if (_downloadCkeck)
                    IsolatedStorageDataRead();

                PhoneApplicationService.Current.State.Clear();

            }

            MoveViewWindow(30);
        }

        private async void getNavigationOnStart(string bid, string pid)
        {
            if (_worldbuildings == null)
            {
                MessageBox.Show("No buildings found. Please download maps");
            }
            else
            {
                bool f = await serchBuildStart(bid, pid);
                if (f)
                {
                    _searchBuilding = true;
                    // AddPoisToSearch();
                    _tileLoaded = true;
                    _foundTile = true;
                    //floorButton.Visibility = Visibility.Visible;
                    navigate();
                }
            }


        }

        private async Task<bool> serchBuildStart(string bid, string pid)
        {
            bool foundb = false;
            bool foundpoi = false;
            foreach (var obj in _worldbuildings.buildings)
            {
                if (obj.buid.Equals(bid))
                {
                    foundb = true;
                    var floor_num = "";
                    var lat = double.Parse(obj.coordinates_lat, CultureInfo.InvariantCulture);
                    var lon = double.Parse(obj.coordinates_lon, CultureInfo.InvariantCulture);

                    var settings1 = IsolatedStorageFile.GetUserStoreForApplication();

                    if (settings1.DirectoryExists(bid) && checkCompleted(bid))
                    {
                        // load staff to the map
                        clear();
                        ReadAllBuildingFloorsFromIsolatedStorage(bid);
                        ReadPoisByBuildingFromIsolatedStorage(bid);
                        addBuildingsToMap(bid);

                        buildLoaded = true;
                        loadBuidingToMap(bid);
                        createAppBarMenuItem();
                    }
                    else
                    {

                        isDownloading = true;
                        //ApplicationBar.IsVisible = false;
                        //downloading.Visibility = Visibility.Visible;

                        loading.Visibility = Visibility.Visible;
                        loading.IsIndeterminate = true;

                        clear();
                        // download staff and show them to the map
                        var fsdf = await DownloadPoisBuildStaff(bid);
                        ReadAllBuildingFloorsFromIsolatedStorage(bid);
                        ReadPoisByBuildingFromIsolatedStorage(bid);

                        addBuildingsToMap(bid);

                        buildLoaded = true;
                        loadBuidingToMap(bid);

                        loading.Visibility = Visibility.Collapsed;
                        loading.IsIndeterminate = false;

                        createAppBarMenuItem();

                        isDownloading = false;
                    }

                    if (_poisByBuilding != null)
                    {
                        foreach (var obj2 in _poisByBuilding.pois)
                        {
                            if (obj2.buid.Equals(bid))
                            {
                                if (pid.Equals("none"))
                                {
                                    if (obj2.is_building_entrance != null)
                                        if (obj2.is_building_entrance.Equals("true"))
                                        {
                                            entrance_location = new GeoCoordinate(lat, lon);
                                            floor_num = obj2.floor_number;
                                            pid = obj2.puid;
                                            to_entrance = true;
                                        }

                                }
                                else
                                    if (obj2.puid.Equals(pid))
                                    {
                                        floor_num = obj2.floor_number;
                                        to_entrance = false;
                                    }
                                foundpoi = true;
                            }

                        }
                    }


                    if (!foundpoi)
                        return false;

                    _floorPoiTo = pid;
                    _floorPoiToNumber = floor_num;
                    _floorPoiToLocation = new GeoCoordinate(lat, lon);

                    if (_tilesCheck)
                    {
                        _buildingSearch = obj.buid;
                        _selectedBuild = obj.buid;
                        _selectedFloor = floor_num;
                        _wmstileprovider.setBuidAndFloor(obj.buid, floor_num);
                        if (!Mymap.TileSources.Contains(_wmstileprovider))
                            Mymap.TileSources.Add(_wmstileprovider);
                        

                        Dispatcher.BeginInvoke(() =>
                        {
                            txt_cur.Text = _selectedFloor;
                        });

                        ViewPoisByBuilding();
                    }
                    else
                    {
                        Mymap.TileSources.Clear();

                        _selectedBuild = obj.buid;
                        _selectedFloor = floor_num;
                        Dispatcher.BeginInvoke(() =>
                        {
                            txt_cur.Text = _selectedFloor;
                        });

                        ViewPoisByBuilding();
                    }

                    ApplicationBar.IsVisible = true;
                    //Mymap.SetView(new GeoCoordinate(lat, lon), 20);

                    SystemTray.IsVisible = false;

                    _foundBuilding = true;
                    break;
                }
            }
            return true;
        }

        private void GetSettings()
        {
            _zoomCheck = App.Settings.ToggleButtonZoom;
            _trackmeCheck = App.Settings.ToogleButtonTrackme;
            _tilesCheck = App.Settings.ToogleButtonTiles;

            //_zoomCheck = bool.Parse(_settings[AppSettings.showzoomKeyName].ToString());
            //_trackmeCheck = bool.Parse(_settings[AppSettings.showtrackmeKeyName].ToString());
            //_optionsCheck = bool.Parse(_settings[AppSettings.showoptionsKeyName].ToString());
            //_tilesCheck = bool.Parse(_settings[AppSettings.showtilesKeyName].ToString());

            if (_zoomCheck)
                zoompanel.Visibility = Visibility.Visible;
            else
                zoompanel.Visibility = Visibility.Collapsed;


            if (_tilesCheck)
            {
                if (Mymap.TileSources.Count == 0)
                    if (_wmstileprovider != null)
                    {
                        if (Mymap.Layers.Contains(poisByFloor_layer))
                        {
                            _wmstileprovider.setBuidAndFloor(_selectedBuild, _selectedFloor);
                        }
                        Mymap.TileSources.Add(_wmstileprovider);
                    }
            }
            else
                Mymap.TileSources.Clear();
            //new MapRotationGesture(Mymap).rotation(enable_map_rotation);
        }

        protected override void OnBackKeyPress(CancelEventArgs e)
        {
            start_nav = false;

            if (downloading.Visibility == Visibility.Visible)
            {
                e.Cancel = true;
                return;
            }

            if (MapOptions.Visibility == Visibility.Visible)
            {
                e.Cancel = true;
                MapOptions.Visibility = Visibility.Collapsed;
                CanvasMenu.Visibility = Visibility.Visible;
                if (directions_grid.Visibility != Visibility.Visible)
                    ApplicationBar.IsVisible = true;
                return;
            }
            else
            {

                if (ApplicationBar.IsVisible == false)
                {
                    ApplicationBar.IsVisible = true;
                    MapOptions.Visibility = Visibility.Collapsed;
                }
            }

            //if (floorpicker.Visibility == Visibility.Visible)
            //{
            //    SystemTray.IsVisible = false;

            //    floorpicker.Visibility = Visibility.Collapsed;
            //    e.Cancel = true;
            //    if (show_grid)
            //    {
            //        MoveViewWindow(-70);
            //        ApplicationBar.IsVisible = false;
            //        directions_grid.Visibility = Visibility.Visible;
            //        show_grid = false;
            //    }
            //    else
            //    {
            //        MoveViewWindow(30);
            //        ApplicationBar.IsVisible = true;
            //    }

            //    return;
            //}

            if (Mymap.Layers.Contains(_sourceLayer))
                Mymap.Layers.Remove(_sourceLayer);

            if (_polyline != null)
                _polyline.Path.Clear();

            if (Mymap.MapElements.Count != 0)
                Mymap.MapElements.Clear();


            if (downloading.Visibility == Visibility.Visible)
            {
                e.Cancel = true;
                return;
            }

            if (ContentPanel.Visibility == Visibility.Visible)
            {
                SystemTray.IsVisible = false;
                //MoveViewWindow(0);
                ApplicationBar.IsVisible = true;
                ContentPanel.Visibility = Visibility.Collapsed;
                if (buildLoaded)
                {
                    closeGrid.Visibility = Visibility.Visible;
                }
                e.Cancel = true;
                return;
            }




            if (gd_result.Visibility == Visibility.Visible)
            {
                Dispatcher.BeginInvoke(() =>
                {
                    if (ls_search.ItemsSource != null)
                        ls_search.ItemsSource.Clear();
                });
                e.Cancel = true;
                gd_result.Visibility = Visibility.Collapsed;
                Mymap.Layers.Remove(mapHoldLayer);
                ApplicationBar.IsVisible = true;
                MoveViewWindow(30);
            }

            if (directions_grid.Visibility == Visibility.Visible)
            {
                if (Mymap.Layers.Contains(indoorLayer))
                    Mymap.Layers.Remove(indoorLayer);
                if (_routePoi != null)
                    _routePoi.pois.Clear();

                // clear directions
                if (GetRoute.Count != 0)
                {
                    GetRoute.Clear();
                    if (_myMapRoute != null)
                    {
                        routedetails_layer.Clear();
                        Mymap.RemoveRoute(_myMapRoute);
                        Mymap.Layers.Remove(routedetails_layer);
                        routeNavDetails.Clear();
                    }
                }
                if (Mymap.Layers.Contains(detailPois))
                {
                    Mymap.Layers.Remove(detailPois);
                    detailPois.Clear();
                }
                MoveViewWindow(30);
                Mymap.Layers.Remove(mapHoldLayer);
                Mymap.MapElements.Clear();
                directions_grid.Visibility = Visibility.Collapsed;
                stackpanelClose();
                e.Cancel = true;
            }

        }

        private void clear_map(object sender, EventArgs e)
        {
            clear();
        }

        void clear()
        {
            buildLoaded = false;
            removeSearchButton();
            deleteAppBarMenuItem();
            _searchBuilding = false;
            _buildingSearch = "";
            _externalSource = false;
            _sourcePoi = false;
            _tileLoaded = false;
            _foundTile = false;
            if (Mymap.Layers.Count > 1)
                Mymap.Layers.Clear();

            if (Mymap.Layers.Count == 0 && mypositionlayer != null)
                Mymap.Layers.Add(mypositionlayer);

            if (GetRoute.Count != 0 && _myMapRoute != null)
                Mymap.RemoveRoute(_myMapRoute);

            if (Mymap.TileSources.Count != 0)
                Mymap.TileSources.Clear();

            Mymap.MapElements.Clear();

            //floorButton.Visibility = Visibility.Collapsed;

            addBuildingsToMap(null);
        }

        private void settings_click(object sender, EventArgs e)
        {
            Helper.worldbuildings = _worldbuildings;
            Helper.allbuildingfloors = _allbuildingfloors;
            NavigationService.Navigate(new Uri("/Settings.xaml?", UriKind.Relative));
        }
        #endregion

        #region Navigation Route
        private async void GetNavigationRoutesBuilding(string buid, string puid, string floorNumber, string lat, string lon)
        {
            try
            {
                if (gd_result.Visibility == Visibility.Visible)
                    gd_result.Visibility = Visibility.Collapsed;
                if (Mymap.Layers.Contains(indoorLayer))
                    Mymap.Layers.Remove(indoorLayer);
                if (Mymap.Layers.Contains(floorPoisDetail))
                {
                    Mymap.Layers.Remove(floorPoisDetail);
                    floorPoisDetail.Clear();
                }
                Mymap.Layers.Remove(mapHoldLayer);
                ApplicationBar.IsVisible = false;
                MoveViewWindow(-70);
                stackpanelOpen();
                directions_grid.Visibility = Visibility.Visible;
                calcProgrBar.Visibility = Visibility.Visible;
                time.Visibility = Visibility.Collapsed;
                distance.Visibility = Visibility.Collapsed;
                calcProgrBar.IsIndeterminate = true;

                RouteLocationToPoi x;
                var isNetwork = NetworkInterface.GetIsNetworkAvailable();
                if (isNetwork)
                    x = await CustomPushpinWp8APIClient.GetLocationToPoiRoute(buid, puid, floorNumber, lat, lon);
                else
                {
                    MessageBox.Show("No internet connection found. Check your internet connection and try again!");
                    directions_grid.Visibility = Visibility.Collapsed;
                    stackpanelClose();
                    MoveViewWindow(30);
                    ApplicationBar.IsVisible = true;
                    return;
                }

                _polyline = new MapPolyline { StrokeColor = Colors.Blue, StrokeThickness = 5 };

                foreach (var obj in x.pois)
                {
                    _polyline.Path.Add(new GeoCoordinate(double.Parse(obj.lat, CultureInfo.InvariantCulture), double.Parse(obj.lon, CultureInfo.InvariantCulture)));
                }
                // clear route details
                if (Mymap.Layers.Contains(routedetails_layer))
                    Mymap.Layers.Remove(routedetails_layer);

                // clear directions
                if (GetRoute.Count != 0)
                {
                    GetRoute.Clear();
                    if (_myMapRoute != null)
                        Mymap.RemoveRoute(_myMapRoute);
                }

                // na dixnei ta results
                IndoorNavigationRoutesLocationToPoi();


                Mymap.MapElements.Add(_polyline);
            }
            catch
            {
                directions_grid.Visibility = Visibility.Collapsed;
                stackpanelClose();
                MoveViewWindow(30);
                ApplicationBar.IsVisible = true;
                MessageBox.Show("Can not get navigation route inside building. Please check your internet connection!");
            }

        }

        private void IndoorNavigationRoutesLocationToPoi()
        {
            indoorLayer = new MapLayer();
            routeNavDetails.Clear();
            var routeList = new List<DirectionsList>();
            time.Text = "Duration: Unknown";
            // total meters
            Dispatcher.BeginInvoke(() =>
            {
                distance.Text = "Distance: Unknown";
            });

            if (Mymap.Layers.Contains(indoorLayer))
            {
                Mymap.Layers.Remove(indoorLayer);
            }
            var direction = new DirectionsList
            {
                direction = "Your Location",
                geocoordinate = new GeoCoordinate(_myLatitude, _myLongitude),
                image = new BitmapImage(new Uri("/Assets/location.png", UriKind.Relative))
            };
            routeList.Add(direction);
            DirectionsList direction1 = new DirectionsList();
            if (_poisByBuilding != null)
            {
                foreach (var obj1 in _poisByBuilding.pois)
                {
                    if (obj1.puid.Equals(_floorPoiTo))
                    {
                        direction1 = new DirectionsList
                        {
                            direction = obj1.name,
                            geocoordinate = new GeoCoordinate(double.Parse(obj1.coordinates_lat, CultureInfo.InvariantCulture),
                                double.Parse(obj1.coordinates_lon, CultureInfo.InvariantCulture)),
                            image = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative))
                        };
                        routeList.Add(direction1);
                    }
                }
            }

            _overlayTo = new MapOverlay();
            var im = new Image
            {
                Source = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative))
            };
            _overlayTo.Content = im;
            _overlayTo.GeoCoordinate = direction1.geocoordinate;
            _overlayTo.PositionOrigin = new Point(1.0, 1.0);
            indoorLayer.Add(_overlayTo);
            if (Mymap.Layers.Contains(indoorLayer))
                Mymap.Layers.Remove(indoorLayer);
            Mymap.Layers.Add(indoorLayer);
            RouteLLS.ItemsSource = routeList;
            RouteLLS.SelectionChanged += RouteLLS_SelectionChanged;

            calcroutestext.Visibility = Visibility.Collapsed;
            calcProgrBar.Visibility = Visibility.Collapsed;
            time.Visibility = Visibility.Visible;
            distance.Visibility = Visibility.Visible;
        }

        RoutePoiToPoi _routePoi;
        private async void GetNavigationRoutesPoiToPoi(string poi_from, string poi_to, GeoCoordinate floorPoiLocation)
        {
            try
            {
                var isNetwork = NetworkInterface.GetIsNetworkAvailable();
                if (isNetwork)
                {
                    if (!poi_from.Equals(poi_to))
                        _routePoi = await CustomPushpinWp8APIClient.GetPoiToPoiRoute(poi_from, poi_to);
                }
                else
                {
                    MessageBox.Show("No internet connection found. Check your internet connection and try again!");
                    directions_grid.Visibility = Visibility.Collapsed;
                    stackpanelClose();
                    MoveViewWindow(30);
                    ApplicationBar.IsVisible = true;
                    return;
                }
                _getIndoornav = true;
                _selectedFloor = _floorPoiFromNumber;

                Dispatcher.BeginInvoke(() =>
                {
                    txt_cur.Text = _selectedFloor;
                });

                ChangeFloor();
                if (!poi_from.Equals(poi_to))
                    GetRouteOfTheCurrentFloor();
                // clear route details
                if (Mymap.Layers.Contains(routedetails_layer))
                    Mymap.Layers.Remove(routedetails_layer);

                // clear directions
                if (GetRoute.Count != 0)
                {
                    GetRoute.Clear();
                    if (_myMapRoute != null)
                        Mymap.RemoveRoute(_myMapRoute);
                }
                var g = new GeoCoordinate();
                // set phone current location
                if (!_externalSource)
                {
                    g.Latitude = _myLatitude;
                    g.Longitude = _myLongitude;
                }
                else
                {
                    g.Latitude = _externalCoordinates.Latitude;
                    g.Longitude = _externalCoordinates.Longitude;
                }

                GetRoute.Add(g);
                _mygeocodequery = new GeocodeQuery
                {
                    SearchTerm = floorPoiLocation.ToString(),
                    GeoCoordinate = g
                };
                _mygeocodequery.QueryCompleted += MygeocodequeryPoiSearch_QueryCompleted;
                _mygeocodequery.QueryAsync();

            }
            catch
            {
                directions_grid.Visibility = Visibility.Collapsed;
                stackpanelClose();
                MoveViewWindow(30);
                if (directions_grid.Visibility != Visibility.Visible)
                    ApplicationBar.IsVisible = true;
                if (_routePoi.message.Contains("same"))
                    MessageBox.Show(_routePoi.message);
                else
                    MessageBox.Show("Can not get navigation route inside building. Please check your internet connection!");
            }
        }

        MapPolyline _polyline;
        private void GetRouteOfTheCurrentFloor()
        {
            if (Mymap.MapElements.Contains(_polyline))
                Mymap.MapElements.Remove(_polyline);

            _polyline = new MapPolyline { StrokeColor = Colors.Blue, StrokeThickness = 5 };
            if (_routePoi != null)
                foreach (var obj in _routePoi.pois)
                {
                    if (obj.floor_number.Equals(_selectedFloor))
                        _polyline.Path.Add(new GeoCoordinate(double.Parse(obj.lat, CultureInfo.InvariantCulture), double.Parse(obj.lon, CultureInfo.InvariantCulture)));
                }
            Mymap.MapElements.Add(_polyline);
        }

        private void GetNavigationRoute(GeoCoordinate location)
        {
            // clear route details
            if (Mymap.Layers.Contains(routedetails_layer))
                Mymap.Layers.Remove(routedetails_layer);

            // clear directions
            if (GetRoute.Count != 0)
            {
                GetRoute.Clear();
                if (_myMapRoute != null)
                    Mymap.RemoveRoute(_myMapRoute);
            }

            GeoCoordinate g = new GeoCoordinate();
            if (_externalSource)
                g = _externalCoordinates;
            else
            {
                g = new GeoCoordinate(_myLatitude, _myLongitude);
            }
            // set phone current location
            GetRoute.Add(g);
            _mygeocodequery = new GeocodeQuery
            {
                SearchTerm = location.ToString(),
                GeoCoordinate = g
            };
            _mygeocodequery.QueryCompleted += Mygeocodequery_QueryCompleted;
            _mygeocodequery.QueryAsync();
        }

        void Mygeocodequery_QueryCompleted(object sender, QueryCompletedEventArgs<IList<MapLocation>> e)
        {
            if (e.Error == null)
            {
                MyQuery = new RouteQuery();
                GetRoute.Add(e.Result[0].GeoCoordinate);
                MyQuery.Waypoints = GetRoute;
                MyQuery.QueryCompleted += MyQuery_QueryCompleted;
                MyQuery.QueryAsync();
                _mygeocodequery.Dispose();
            }
        }

        void MyQuery_QueryCompleted(object sender, QueryCompletedEventArgs<Route> e)
        {
            if (e.Error == null)
            {
                var myRoute = e.Result;
                _myMapRoute = new MapRoute(myRoute);
                Mymap.AddRoute(_myMapRoute);
                // time to destination
                var time1 = myRoute.EstimatedDuration;
                time.Text = "Duration: " + time1.ToString();

                // total meters
                Dispatcher.BeginInvoke(() =>
                {
                    var meters = myRoute.LengthInMeters;
                    var kilometers = meters / 1000.0;
                    distance.Text = "Distance: " + kilometers.ToString(CultureInfo.InvariantCulture) + " Km";
                });
                routeNavDetails.Clear();
                var routeList = new List<DirectionsList>();
                foreach (var leg in myRoute.Legs)
                {
                    foreach (var maneuver in leg.Maneuvers)
                    {
                        var direction = new DirectionsList();
                        var det = new RouteDetails
                        {
                            instractions = maneuver.InstructionText,
                            coordinate =
                                new GeoCoordinate(maneuver.StartGeoCoordinate.Latitude,
                                    maneuver.StartGeoCoordinate.Longitude),
                            distance = maneuver.LengthInMeters
                        };
                        routeNavDetails.Add(det);
                        var dist = maneuver.LengthInMeters / 1000.0;
                        var str = maneuver.InstructionText.ToUpper();
                        if (str.Contains("LEFT"))
                            direction.image =
                                new BitmapImage(new Uri("/Assets/Navigation/turn-left.png", UriKind.Relative));
                        else if (str.Contains("RIGHT"))
                            direction.image =
                                new BitmapImage(new Uri("/Assets/Navigation/turn-right.png", UriKind.Relative));
                        else if (str.Contains("HEAD"))
                            direction.image = new BitmapImage(new Uri("/Assets/start.png", UriKind.Relative));
                        else if (str.Contains("YOU HAVE"))
                            direction.image = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative));
                        else if (str.Contains("TRAFFIC CIRCLE"))
                            direction.image =
                                new BitmapImage(new Uri("/Assets/Navigation/trafficcircle.png", UriKind.Relative));
                        direction.distance = dist.ToString(CultureInfo.InvariantCulture) + " Km";
                        direction.direction = maneuver.InstructionText;
                        direction.geocoordinate = det.coordinate;
                        routeList.Add(direction);
                    }
                }
                CreaterouteOverlays();
                RouteLLS.ItemsSource = routeList;
                RouteLLS.SelectionChanged += RouteLLS_SelectionChanged;
                MyQuery.Dispose();
            }
            else
            {
                ApplicationBar.IsVisible = true;
                MoveViewWindow(30);
                directions_grid.Visibility = Visibility.Collapsed;
                stackpanelClose();
                MessageBox.Show("Cannot get directions to the selected poi!");
            }

            calcroutestext.Visibility = Visibility.Collapsed;
            calcProgrBar.Visibility = Visibility.Collapsed;
            time.Visibility = Visibility.Visible;
            distance.Visibility = Visibility.Visible;

        }

        void RouteLLS_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            // If selected item is null (no selection) do nothing
            if (RouteLLS.SelectedItem == null)
                return;

            MoveViewWindow(-70);

            var direction = RouteLLS.SelectedItem as DirectionsList;

            if (direction != null) Mymap.SetView(direction.geocoordinate, 20, MapAnimationKind.Parabolic);

            // Reset selected item to null (no selection)
            RouteLLS.SelectedItem = null;
        }



        private void CreaterouteOverlays()
        {

            routedetails_layer = new MapLayer();
            routedetails_layer.Clear();
            MapOverlay overlay;
            if (routeNavDetails.Count > 1)
            {
                var imgwidth = 0;
                var imgheight = 0;
                double x = 0;
                double y = 0;
                for (int i = 0; i < routeNavDetails.Count; i++)
                {
                    var image1 = new Image();
                    if (i == 0)
                    {
                        image1.Source = new BitmapImage(new Uri("/Assets/start.png", UriKind.Relative));
                        imgwidth = 30;
                        imgheight = 30;
                        x = 0.0;
                        y = 0.9;
                    }
                    else if (i == routeNavDetails.Count - 1)
                    {
                        image1.Source = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative));
                        imgwidth = 30;
                        imgheight = 30;
                        x = 1;
                        y = 0.9;
                    }
                    else
                    {
                        image1.Source = new BitmapImage(new Uri("/Assets/details.png", UriKind.Relative));
                        imgwidth = 20;
                        imgheight = 20;
                        x = 0.5;
                        y = 0.5;
                    }
                    image1.Width = imgwidth;
                    image1.Height = imgheight;
                    var poi = new RouteDetailsPois
                    {
                        instractions = routeNavDetails[i].instractions,
                        distance = routeNavDetails[i].distance / 1000.0,
                        coordinate =
                            new GeoCoordinate(routeNavDetails[i].coordinate.Latitude,
                                routeNavDetails[i].coordinate.Longitude)
                    };
                    image1.DataContext = poi;
                    image1.Tap += image1_Tap;
                    overlay = new MapOverlay
                    {
                        Content = image1,
                        GeoCoordinate =
                            new GeoCoordinate(routeNavDetails[i].coordinate.Latitude,
                                routeNavDetails[i].coordinate.Longitude),
                        PositionOrigin = new Point(x, y)
                    };
                    routedetails_layer.Add(overlay);
                }
                Mymap.Layers.Add(routedetails_layer);
            }
        }

        MapLayer detailPois = new MapLayer();
        void image1_Tap(object sender, System.Windows.Input.GestureEventArgs e)
        {

            if (Mymap.Layers.Contains(mypositionPoi))
            {
                Mymap.Layers.Remove(mypositionPoi);
                mypositionPoi.Clear();
            }

            if (Mymap.Layers.Contains(detailPois))
            {
                Mymap.Layers.Remove(detailPois);
                detailPois.Clear();
            }

            var img = (Image)sender;
            var poi = img.DataContext as RouteDetailsPois;
            var over = new MapOverlay();
            if (poi != null)
            {
                var control = new RouteDetailsPoisControl
                {
                    txt_direction = { Text = poi.instractions },
                    txt_distance = { Text = "Distance: " + poi.distance.ToString(CultureInfo.InvariantCulture) + " Km" }
                };
                control.grd_content.Tap += grd_content_Tap;
                over.Content = control;
            }
            if (poi != null)
            {
                over.GeoCoordinate = poi.coordinate;
                over.PositionOrigin = new Point(0.5, 1.0);
                detailPois.Add(over);
                Mymap.Layers.Add(detailPois);
                Mymap.SetView(poi.coordinate, 19, MapAnimationKind.Parabolic);
            }
        }

        void grd_content_Tap(object sender, System.Windows.Input.GestureEventArgs e)
        {
            if (Mymap.Layers.Contains(mypositionPoi))
            {
                Mymap.Layers.Remove(mypositionPoi);
                mypositionPoi.Clear();
            }

            if (Mymap.Layers.Contains(detailPois))
            {
                Mymap.Layers.Remove(detailPois);
                detailPois.Clear();
            }
        }
        #endregion

        #region Slide Menu
        void MoveViewWindow(double top)
        {
            _viewMoved = true;
            ((Storyboard)canvas.Resources["moveAnimation"]).SkipToFill();
            ((DoubleAnimation)((Storyboard)canvas.Resources["moveAnimation"]).Children[0]).To = top;
            ((Storyboard)canvas.Resources["moveAnimation"]).Begin();

        }

        private void canvas_ManipulationDelta(object sender, ManipulationDeltaEventArgs e)
        {
            if (gd_result.Visibility == Visibility.Visible || directions_grid.Visibility == Visibility.Visible)
                if (e.DeltaManipulation.Translation.Y != 0)
                    Canvas.SetTop(CanvasMenu, Math.Min(Math.Max(-500, Canvas.GetTop(CanvasMenu) + e.DeltaManipulation.Translation.Y), 0));
        }


        private void canvas_ManipulationStarted(object sender, ManipulationStartedEventArgs e)
        {
            if (gd_result.Visibility == Visibility.Visible || directions_grid.Visibility == Visibility.Visible)
            {
                _viewMoved = false;
                _initialPosition = Canvas.GetTop(CanvasMenu);
            }

        }

        private void canvas_ManipulationCompleted(object sender, ManipulationCompletedEventArgs e)
        {
            if (gd_result.Visibility == Visibility.Visible || directions_grid.Visibility == Visibility.Visible)
            {
                var top = Canvas.GetTop(CanvasMenu);
                if (_viewMoved)
                    return;
                if (Math.Abs(_initialPosition - top) < 100)
                {
                    //bouncing back
                    MoveViewWindow(_initialPosition);
                    return;
                }


                //change of state
                if (_initialPosition - top > 0)
                {
                    //slide up
                    if (_initialPosition > -70)
                        MoveViewWindow(-70);
                    else
                        MoveViewWindow(-500);
                }
                else
                {
                    //slide down
                    if (_initialPosition < -320)
                        MoveViewWindow(-70);
                    else
                        MoveViewWindow(-70);
                }
            }
        }
        #endregion

        #region search staff


        private void autoCompleteBox_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            try
            {

                ApplicationBar.IsVisible = true;
                if (e.RemovedItems.Count > 0) return;
                if (e.AddedItems.Count > 0)
                {

                    ContentPanel.Visibility = Visibility.Collapsed;
                    RemoveIndoorStaff();
                    _foundBuilding = false;
                    Dispatcher.BeginInvoke(() => this.Focus());

                    if (autoCompleteBox.SelectedItem != null)
                    {
                        // if (_worldbuildings != null)
                        BuildingSearch(autoCompleteBox.SelectedItem.ToString());
                        if (_foundBuilding)
                        {
                            _searchBuilding = true;
                            _tileLoaded = true;
                            _foundTile = true;
                            //floorButton.Visibility = Visibility.Visible;
                           // ContentPanel.Visibility = Visibility.Collapsed;
                            //autoCompleteBox.Visibility = Visibility.Collapsed;
                            //MoveViewWindow(-5);
                            return;
                        }
                        else
                        {
                            _searchBuilding = false;
                            _tileLoaded = false;
                            _foundTile = false;
                            if (Mymap.TileSources.Contains(_wmstileprovider))
                                Mymap.TileSources.Remove(_wmstileprovider);
                        }
                    }
                    ContentPanel.Visibility = Visibility.Collapsed;
                    autoCompleteBox.Visibility = Visibility.Collapsed;
                    ApplicationBar.IsVisible = true;
                }

                //}

            }
            catch (Exception ex)
            {
                MessageBox.Show(ex.Message);
            }


        }

        private void RemoveIndoorStaff()
        {
            //floorButton.Visibility = Visibility.Collapsed;

            if (Mymap.Layers.Contains(floorPoisDetail))
            {
                Mymap.Layers.Remove(floorPoisDetail);
                floorPoisDetail.Clear();
            }
            if (Mymap.Layers.Contains(indoorLayer))
                Mymap.Layers.Remove(indoorLayer);
            if (Mymap.Layers.Contains(poisByFloor_layer))
                Mymap.Layers.Remove(poisByFloor_layer);
            if (Mymap.TileSources.Contains(_wmstileprovider))
                Mymap.TileSources.Remove(_wmstileprovider);
            // clear directions
            if (GetRoute.Count != 0)
            {
                GetRoute.Clear();
                if (_myMapRoute != null)
                {
                    routedetails_layer.Clear();
                    Mymap.RemoveRoute(_myMapRoute);
                    Mymap.Layers.Remove(routedetails_layer);
                    routeNavDetails.Clear();
                }
            }
            if (Mymap.MapElements.Contains(_polyline))
                Mymap.MapElements.Remove(_polyline);
        }


        private GeocodeQuery _mygeocodequerylocation;
        private RouteQuery _myQueryLocation;
        private WMSTiles _wmstileprovider;
        bool _foundBuilding = false;
        //private IsolatedStorageSettings _settings;
        private string _selectedFloor;
        private string _selectedBuild;
        private bool _loaded;

        private async void BuildingSearch(string name)
        {

            if (buildLoaded)
            {
                foreach (var obj in _poisByBuilding.pois)
                {
                    if (obj.name.Equals(name) || obj.description.Equals(name))
                    {
                        _selectedBuild = obj.buid;
                        _selectedFloor = obj.floor_number;

                        Dispatcher.BeginInvoke(() =>
                        {
                            txt_cur.Text = _selectedFloor;
                        });

                        _wmstileprovider = new WMSTiles();
                        _wmstileprovider.setBuidAndFloor(_selectedBuild, _selectedFloor);
                        if (Mymap.TileSources.Contains(_wmstileprovider))
                            Mymap.TileSources.Remove(_wmstileprovider);
                        Mymap.TileSources.Add(_wmstileprovider);

                        ChangeFloor();
                        if (directions_grid.Visibility == Visibility.Visible)
                            GetRouteOfTheCurrentFloor();
                        foreach (var o in poisByFloor_layer)
                        {
                            var x = o.Content as Image;

                            if (o.GeoCoordinate == new GeoCoordinate(double.Parse(obj.coordinates_lat, CultureInfo.InvariantCulture), double.Parse(obj.coordinates_lon, CultureInfo.InvariantCulture)))
                                buildingImg_tap(x.DataContext as FloorPoisDetails);
                        }

                        Mymap.SetView(new GeoCoordinate(double.Parse(obj.coordinates_lat, CultureInfo.InvariantCulture), double.Parse(obj.coordinates_lon, CultureInfo.InvariantCulture)), 20);

                        _foundBuilding = true;

                        break;
                    }
                }

            }



            foreach (var obj in _worldbuildings.buildings)
            {
                if (obj.name.Equals(name))
                {
                    var floor_num = "";
                    var lat = double.Parse(obj.coordinates_lat, CultureInfo.InvariantCulture);
                    var lon = double.Parse(obj.coordinates_lon, CultureInfo.InvariantCulture);

                    var settings1 = IsolatedStorageFile.GetUserStoreForApplication();
                    if (settings1.DirectoryExists(obj.buid) && checkCompleted(obj.buid))
                    {
                        // load staff to the map
                        clear();
                        ReadAllBuildingFloorsFromIsolatedStorage(obj.buid);
                        ReadPoisByBuildingFromIsolatedStorage(obj.buid);
                        addBuildingsToMap(obj.buid);

                        buildLoaded = true;
                        loadBuidingToMap(obj.buid);
                        createAppBarMenuItem();
                    }
                    else
                    {
                        isDownloading = true;
                        //ApplicationBar.IsVisible = false;
                        //downloading.Visibility = Visibility.Visible;

                        loading.Visibility = Visibility.Visible;
                        loading.IsIndeterminate = true;

                        clear();
                        // download staff and show them to the map
                        var fsdf = await DownloadPoisBuildStaff(obj.buid);
                        ReadAllBuildingFloorsFromIsolatedStorage(obj.buid);
                        ReadPoisByBuildingFromIsolatedStorage(obj.buid);

                        addBuildingsToMap(obj.buid);
                        buildLoaded = true;
                        loadBuidingToMap(obj.buid);

                        loading.Visibility = Visibility.Collapsed;
                        loading.IsIndeterminate = false;

                        createAppBarMenuItem();
                        isDownloading = false;
                    }


                    Mymap.SetView(new GeoCoordinate(lat, lon), 20, MapAnimationKind.Parabolic);

                    SystemTray.IsVisible = false;

                    _foundBuilding = true;
                    break;
                }
            }
        }

        ApplicationBarMenuItem menuitem;
        void createAppBarMenuItem()
        {
            if (ApplicationBar.MenuItems.Contains(menuitem))
                return;
            menuitem = new ApplicationBarMenuItem();
            ApplicationBar.MenuItems.Contains(menuitem);
            menuitem.Text = "refresh building";
            menuitem.Click += menuitem_Click;
            ApplicationBar.MenuItems.Add(menuitem);
        }

        void deleteAppBarMenuItem()
        {
            if (ApplicationBar.MenuItems.Contains(menuitem))
            {
                ApplicationBar.MenuItems.Remove(menuitem);
            }
        }

        async void menuitem_Click(object sender, EventArgs e)
        {
            isDownloading = true;
            buildLoaded = false;

            loading.Visibility = Visibility.Visible;
            loading.IsIndeterminate = true;

            clear();
            // download staff and show them to the map
            var fsdf = await DownloadPoisBuildStaff(_selectedBuild);
            ReadAllBuildingFloorsFromIsolatedStorage(_selectedBuild);
            ReadPoisByBuildingFromIsolatedStorage(_selectedBuild);

            addBuildingsToMap(_selectedBuild);
            buildLoaded = true;
            loadBuidingToMap(_selectedBuild);

            loading.Visibility = Visibility.Collapsed;
            loading.IsIndeterminate = false;

            createAppBarMenuItem();
            isDownloading = false;

            _searchBuilding = true;
        }

        private async Task<int> DownloadPoisAndTiles(List<string> floors, string buid)
        {
            if (floors.Count > 0)
            {

                Dispatcher.BeginInvoke(() =>
                {
                    txt_download.Text = "Download building data..";
                });

                var building = await CustomPushpinWp8APIClient.GetPoisByBuilding(buid);


                int counterh = 0;
                if (_allbuildingfloors != null)
                {
                    foreach (var obj7 in _allbuildingfloors.floors)
                    {
                        if (obj7.buid.Equals(buid))
                        {
                            Dispatcher.BeginInvoke(() =>
                            {
                                txt_download.Text = "Download floor " + (counterh + 1) + " of " + floors.Count;
                            });
                            await CustomPushpinWp8APIClient.GetTiles(obj7.buid, obj7.floor_number);
                            counterh++;
                        }

                    }
                }

                //Dispatcher.BeginInvoke(() =>
                //{
                //    txt_download.Text = "Saving data..";
                //});
                writePoisByBuildingToIsolatedStorage(building, buid);
                IsolatedStorageDataRead();

                downloading.Visibility = Visibility.Collapsed;
                ApplicationBar.IsVisible = true;
            }
            return 1;
        }




        private void ls_search_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            // If selected item is null, do nothing
            if (ls_search.SelectedItem == null)
                return;

            var searchloc = ls_search.SelectedItem as searchLocations;
            _isSearch = true;
            if (searchloc != null)
            {
                _tapLocation = searchloc.Coordinates;
                AddPoi(searchloc.Coordinates);
            }

            MoveViewWindow(-70);

            // Reset selected item to null
            ls_search.SelectedItem = null;
        }


        private void getSearchLocation(GeoCoordinate location)
        {
            _mygeocodequerylocation = new GeocodeQuery
            {
                SearchTerm = location.ToString(),
                GeoCoordinate = new GeoCoordinate(_myLatitude, _myLongitude)
            };
            _mygeocodequerylocation.QueryCompleted += MygeocodequerylocationSearch_QueryCompleted;
            _mygeocodequerylocation.QueryAsync();
        }

        void MygeocodequerylocationSearch_QueryCompleted(object sender, QueryCompletedEventArgs<IList<MapLocation>> e)
        {

            List<GeoCoordinate> r;
            if (e.Result.Count > 0)
            {
                _myQueryLocation = new RouteQuery();
                r = new List<GeoCoordinate> { new GeoCoordinate(_myLatitude, _myLongitude), e.Result[0].GeoCoordinate };
                _myQueryLocation.Waypoints = r;
                _myQueryLocation.QueryCompleted += MyQuerySearchLocation_QueryCompleted;
                _myQueryLocation.QueryAsync();
                //Mygeocodequerylocation.Dispose();
            }

        }

        void MyQuerySearchLocation_QueryCompleted(object sender, QueryCompletedEventArgs<Route> e)
        {
            if (e.Error == null)
            {
                // total meters
                var meters = e.Result.LengthInMeters;
                var kilometers = meters / 1000.0;

                Dispatcher.BeginInvoke(() =>
                {
                    _control.txt_distance.Text = "Distance: " + kilometers + " Km";
                });
                _control.pb_procress.Visibility = Visibility.Collapsed;
                _control.txt_distance.Visibility = Visibility.Visible;
                // MyQueryLocation.Dispose();
            }
            else
            {
                Dispatcher.BeginInvoke(() =>
                {
                    _control.txt_distance.Text = "Distance: " + "Can not be calculated.";
                });
                _control.pb_procress.Visibility = Visibility.Collapsed;
                _control.txt_distance.Visibility = Visibility.Visible;
            }

        }

        #endregion


        #region floor picker

        private void choose_floor(object sender, RoutedEventArgs e)
        {
            if (Mymap.Layers.Contains(mapHoldLayer))
                Mymap.Layers.Remove(mapHoldLayer);
            if (Mymap.Layers.Contains(floorPoisDetail))
            {
                Mymap.Layers.Remove(floorPoisDetail);
                floorPoisDetail.Clear();
            }

            if (directions_grid.Visibility == Visibility.Visible)
            {
                show_grid = true;
                directions_grid.Visibility = Visibility.Collapsed;
                stackpanelClose();
            }


            MoveViewWindow(30);
            autoCompleteBuilding.Text = string.Empty;
            //SystemTray.IsVisible = true;
            ApplicationBar.IsVisible = false;

        }

        private void Picker_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            if (_firsttimeSearch)
            {
                Mymap.TileSources.Clear();
                var a = Picker.SelectedItem as string;
                _selectedFloor = a;
                ChangeFloor();

                if (!_selectedFloor.Equals(_floorPoiToNumber))
                    Mymap.MapElements.Clear();
                else
                {
                    if (_polyline != null)
                        if (!Mymap.MapElements.Contains(_polyline))
                            Mymap.MapElements.Add(_polyline);
                }

                if (_routePoi != null)
                    if (_routePoi.pois != null)
                        if (_routePoi.pois.Count > 0)
                            GetRouteOfTheCurrentFloor();

                if (Mymap.Layers.Contains(indoorLayer))
                {
                    Mymap.Layers.Remove(indoorLayer);
                    indoorLayer.Clear();
                    if (_floorPoiFromNumber == _floorPoiToNumber)
                    {
                        if (_selectedFloor == _floorPoiToNumber)
                        {
                            indoorLayer.Add(_overlayFrom);
                            indoorLayer.Add(_overlayTo);
                        }

                    }
                    else
                    {
                        if (_selectedFloor == _floorPoiFromNumber)
                        {
                            indoorLayer.Add(_overlayFrom);
                        }
                        else
                            if (_selectedFloor == _floorPoiToNumber)
                                indoorLayer.Add(_overlayTo);
                    }
                    Mymap.Layers.Add(indoorLayer);
                }
            }
            _firsttimeSearch = true;
        }

        private void ChangeFloor()
        {
            if (_tilesCheck)
            {
                _wmstileprovider.setBuidAndFloor(_selectedBuild, _selectedFloor);
                if (!Mymap.TileSources.Contains(_wmstileprovider))
                    Mymap.TileSources.Add(_wmstileprovider);
            }

            ViewPoisByBuilding();
        }

        private void ViewPoisByBuilding()
        {
            if (Mymap.Layers.Contains(poisByFloor_layer))
                Mymap.Layers.Remove(poisByFloor_layer);
            poisByFloor_layer = new MapLayer();
            if (_poisByBuilding != null)
            {
                foreach (var obj1 in _poisByBuilding.pois)
                {
                    if (obj1.buid.Equals(_selectedBuild) && obj1.floor_number.Equals(_selectedFloor))
                    {
                        if (!obj1.pois_type.Equals("None"))
                        {
                            _selectedFloor = obj1.floor_number;

                            var poi = new FloorPoisDetails
                            {
                                Description = obj1.name,
                                Information = obj1.description,
                                poiid = obj1.puid,
                                poiFloor = obj1.floor_number
                            };
                            double lat = double.Parse(obj1.coordinates_lat, CultureInfo.InvariantCulture);
                            double lon = double.Parse(obj1.coordinates_lon, CultureInfo.InvariantCulture);
                            poi.Coordinates = new GeoCoordinate(lat, lon);
                            if (obj1.is_building_entrance != null)
                                poi.Entrange = bool.Parse(obj1.is_building_entrance);
                            else
                                poi.Entrange = false;

                            var imageFloorPois = new Image
                            {
                                Source = new BitmapImage(new Uri("/Assets/other_loc.png", UriKind.Relative)),
                                DataContext = poi
                            };
                            imageFloorPois.Tap += image_floor_pois_Tap;

                            poisbyflooroverlay = new MapOverlay
                            {
                                Content = imageFloorPois,
                                GeoCoordinate = poi.Coordinates,
                                PositionOrigin = new Point(0.5, 1.0)
                            };
                            poisByFloor_layer.Add(poisbyflooroverlay);
                        }
                    }
                }
            }
            Mymap.Layers.Add(poisByFloor_layer);
            downloading.Visibility = Visibility.Collapsed;
        }

        MapLayer floorPoisDetail = new MapLayer();
        private GeoCoordinate _floorPoiToLocation;
        private string _floorPoiTo;
        private string _floorPoiFrom;
        private bool _getIndoornav;
        private string _floorPoiFromNumber;
        private void image_floor_pois_Tap(object sender, GestureEventArgs e)
        {
            var img = (Image)sender;
            var poi = img.DataContext as FloorPoisDetails;
            buildingImg_tap(poi);
        }

        void buildingImg_tap(FloorPoisDetails poi)
        {
            if (Mymap.Layers.Contains(floorPoisDetail))
            {
                Mymap.Layers.Remove(floorPoisDetail);
                floorPoisDetail.Clear();
            }
            var over = new MapOverlay { GeoCoordinate = poi.Coordinates };

            var control = new BuildingPois { txt_description = { Text = poi.Description } };
            over.Content = control;

            var poitap = new PoiTap
            {
                poiFloor = poi.poiFloor,
                poiId = poi.poiid,
                coordinate = poi.Coordinates
            };

            //set button data context
            control.btn_navigateHere.DataContext = poitap;
            control.btn_source.DataContext = poitap;
            control.btn_poiInfo.DataContext = poi;

            // set info visibility
            control.btn_poiInfo.Visibility = poi.Information.Length > 2 ? Visibility.Visible : Visibility.Collapsed;

            control.btn_navigateHere.Click += floorPoiNavHere_Tap;
            control.btn_source.Click += btn_source_Click;
            control.btn_poiInfo.Click += BtnPoiInfoOnClick;
            control.grid_details.Tap += building_pois_tap;
            over.PositionOrigin = new Point(0.5, 1.0);
            floorPoisDetail.Add(over);
            Mymap.Layers.Add(floorPoisDetail);
            Mymap.SetView(poi.Coordinates, 20, MapAnimationKind.Parabolic);
        }

        private void BtnPoiInfoOnClick(object sender, RoutedEventArgs routedEventArgs)
        {
            var button = sender as Button;
            if (button == null) return;
            var context = button.DataContext as FloorPoisDetails;

            if (context != null) MessageBox.Show(context.Information, context.Description, MessageBoxButton.OK);
        }

        void btn_source_Click(object sender, RoutedEventArgs e)
        {
            _externalSource = false;
            if (Mymap.Layers.Contains(floorPoisDetail))
            {
                Mymap.Layers.Remove(floorPoisDetail);
                floorPoisDetail.Clear();
            }
            if (Mymap.Layers.Contains(indoorLayer))
                Mymap.Layers.Remove(indoorLayer);

            if (Mymap.Layers.Contains(_sourceLayer))
                Mymap.Layers.Remove(_sourceLayer);

            // clear directions
            if (GetRoute.Count != 0)
            {
                GetRoute.Clear();
                if (_myMapRoute != null)
                {
                    routedetails_layer.Clear();
                    Mymap.RemoveRoute(_myMapRoute);
                    Mymap.Layers.Remove(routedetails_layer);
                    routeNavDetails.Clear();
                }
            }


            MoveViewWindow(30);

            if (directions_grid.Visibility == Visibility.Visible)
            {
                directions_grid.Visibility = Visibility.Collapsed;
                stackpanelClose();
            }

            ApplicationBar.IsVisible = true;

            _sourcePoi = true;

            var button = sender as Button;
            var context = button.DataContext as PoiTap;
            _floorPoiFrom = context.poiId;
            _floorPoiFromNumber = context.poiFloor;

            if (Mymap.MapElements.Contains(_polyline))
            {
                Mymap.MapElements.Remove(_polyline);
                BuildingSourcePoi();
            }
            else
            {
                var imageMylocation = new Image
                {
                    Source = new BitmapImage(new Uri("/Assets/source.png", UriKind.Relative))
                };
                _sourceOverlay = new MapOverlay();
                _sourceLayer = new MapLayer();
                _sourceOverlay.Content = imageMylocation;
                _sourceOverlay.GeoCoordinate = context.coordinate;
                _sourceOverlay.PositionOrigin = new Point(0.5, 1);
                _sourceLayer.Add(_sourceOverlay);
                Mymap.Layers.Add(_sourceLayer);
            }

        }


        void BuildingSourcePoi()
        {
            if (gd_result.Visibility == Visibility.Visible)
                gd_result.Visibility = Visibility.Collapsed;
            if (Mymap.Layers.Contains(indoorLayer))
                Mymap.Layers.Remove(indoorLayer);

            Mymap.Layers.Remove(mapHoldLayer);
            ApplicationBar.IsVisible = false;
            MoveViewWindow(-70);
            stackpanelOpen();
            directions_grid.Visibility = Visibility.Visible;
            calcProgrBar.Visibility = Visibility.Visible;
            time.Visibility = Visibility.Collapsed;
            distance.Visibility = Visibility.Collapsed;
            calcProgrBar.IsIndeterminate = true;

            if (Mymap.Layers.Contains(floorPoisDetail))
            {
                floorPoisDetail.Clear();
                Mymap.Layers.Remove(floorPoisDetail);
            }

            IndoorNavigationRoutesPoiToPoi(_floorPoiFrom, _floorPoiTo);

        }

        private async void IndoorNavigationRoutesPoiToPoi(string poiFrom, string poiTo)
        {
            try
            {
                var isNetwork = NetworkInterface.GetIsNetworkAvailable();
                if (isNetwork)
                    _routePoi = await CustomPushpinWp8APIClient.GetPoiToPoiRoute(poiFrom, poiTo);
                else
                {
                    MessageBox.Show("No internet connection found. Check your internet connection and try again!");
                    directions_grid.Visibility = Visibility.Collapsed;
                    stackpanelClose();
                    MoveViewWindow(30);
                    ApplicationBar.IsVisible = true;
                    return;
                }
                _selectedFloor = _floorPoiFromNumber;
                Dispatcher.BeginInvoke(() =>
                {
                    txt_cur.Text = _selectedFloor;
                });

                ChangeFloor();
                GetRouteOfTheCurrentFloor();
                // clear route details
                if (Mymap.Layers.Contains(routedetails_layer))
                    Mymap.Layers.Remove(routedetails_layer);

                // clear directions
                if (GetRoute.Count != 0)
                {
                    GetRoute.Clear();
                    if (_myMapRoute != null)
                        Mymap.RemoveRoute(_myMapRoute);
                }

                ShowIndoorRoutesPoiToPoiResults();

            }
            catch
            {
                directions_grid.Visibility = Visibility.Collapsed;
                stackpanelClose();
                MoveViewWindow(30);
                ApplicationBar.IsVisible = true;
                if (_routePoi.message.Contains("same"))
                    MessageBox.Show(_routePoi.message);
                else
                    MessageBox.Show("Can not get navigation route inside building. Please check your internet connection!");
            }
        }

        private void ShowIndoorRoutesPoiToPoiResults()
        {


            time.Text = "Duration: Unknown";
            // total meters
            Dispatcher.BeginInvoke(() =>
            {
                distance.Text = "Distance: Unknown";
            });


            routeNavDetails.Clear();
            var routeList = new List<DirectionsList>();


            if (Mymap.Layers.Contains(indoorLayer))
            {
                Mymap.Layers.Remove(indoorLayer);
            }



            if (_poisByBuilding != null)
            {
                foreach (var obj1 in _poisByBuilding.pois)
                {
                    if (obj1.puid.Equals(_floorPoiFrom))
                    {
                        if (obj1.is_building_entrance != null && obj1.is_building_entrance.Equals("true"))
                        {
                            var direction = new DirectionsList
                            {
                                direction = "Get into Building",
                                geocoordinate =
                                    new GeoCoordinate(double.Parse(obj1.coordinates_lat, CultureInfo.InvariantCulture),
                                        double.Parse(obj1.coordinates_lon, CultureInfo.InvariantCulture)),
                                image =
                                    new BitmapImage(new Uri("/Assets/Navigation/entrance.png", UriKind.Relative))
                            };
                            routeList.Add(direction);

                            _overlayFrom = new MapOverlay();
                            var im = new Image
                            {
                                Source =
                                    new BitmapImage(new Uri("/Assets/Navigation/entrance.png", UriKind.Relative)),
                                Width = 64,
                                Height = 64
                            };
                            _overlayFrom.Content = im;
                            _overlayFrom.GeoCoordinate = direction.geocoordinate; ;
                            _overlayFrom.PositionOrigin = new Point(0.5, 1.0);
                        }
                        else
                        {
                            var direction = new DirectionsList
                            {
                                direction = obj1.name,
                                geocoordinate =
                                    new GeoCoordinate(double.Parse(obj1.coordinates_lat, CultureInfo.InvariantCulture),
                                        double.Parse(obj1.coordinates_lon, CultureInfo.InvariantCulture)),
                                image = new BitmapImage(new Uri("/Assets/start.png", UriKind.Relative))
                            };
                            routeList.Add(direction);

                            _overlayFrom = new MapOverlay();
                            var im = new Image
                            {
                                Source = new BitmapImage(new Uri("/Assets/start.png", UriKind.Relative)),
                                Width = 64,
                                Height = 64
                            };
                            _overlayFrom.Content = im;
                            _overlayFrom.GeoCoordinate = direction.geocoordinate; ;
                            _overlayFrom.PositionOrigin = new Point(0.0, 1.0);
                        }
                    }
                }
            }

            if (_poisByBuilding != null)
            {
                foreach (var obj1 in _poisByBuilding.pois)
                {
                    if (obj1.puid.Equals(_floorPoiTo))
                    {
                        var direction = new DirectionsList
                        {
                            direction = obj1.name,
                            geocoordinate =
                                new GeoCoordinate(double.Parse(obj1.coordinates_lat, CultureInfo.InvariantCulture),
                                    double.Parse(obj1.coordinates_lon, CultureInfo.InvariantCulture)),
                            image = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative))
                        };
                        routeList.Add(direction);

                        _overlayTo = new MapOverlay();
                        var im = new Image
                        {
                            Source = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative))
                        };
                        _overlayTo.Content = im;
                        _overlayTo.GeoCoordinate = direction.geocoordinate; ;
                        _overlayTo.PositionOrigin = new Point(1.0, 1.0);
                    }
                }
            }
            indoorLayer.Clear();

            if (Mymap.Layers.Contains(indoorLayer))
                Mymap.Layers.Remove(indoorLayer);
            if (_floorPoiFromNumber == _floorPoiToNumber)
            {
                if (_selectedFloor == _floorPoiToNumber)
                {
                    indoorLayer.Add(_overlayFrom);
                    indoorLayer.Add(_overlayTo);
                }
            }
            else
            {
                if (_selectedFloor == _floorPoiFromNumber)
                {
                    indoorLayer.Add(_overlayFrom);
                }
                else
                    if (_selectedFloor == _floorPoiToNumber)
                        indoorLayer.Add(_overlayTo);
            }
            Mymap.Layers.Add(indoorLayer);

            CreaterouteOverlays();
            RouteLLS.ItemsSource = routeList;
            RouteLLS.SelectionChanged += RouteLLS_SelectionChanged;


            calcroutestext.Visibility = Visibility.Collapsed;
            calcProgrBar.Visibility = Visibility.Collapsed;
            time.Visibility = Visibility.Visible;
            distance.Visibility = Visibility.Visible;
        }

        private void building_pois_tap(object sender, GestureEventArgs e)
        {
            if (Mymap.Layers.Contains(floorPoisDetail))
            {
                floorPoisDetail.Clear();
                Mymap.Layers.Remove(floorPoisDetail);
            }
        }

        private void floorPoiNavHere_Tap(object sender, RoutedEventArgs e)
        {
            var button = sender as Button;
            var context = button.DataContext as PoiTap;
            _floorPoiTo = context.poiId;
            _floorPoiToNumber = context.poiFloor;
            _floorPoiToLocation = context.coordinate;
            navigate();

        }

        private void navigate()
        {
            if (Mymap.Layers.Contains(floorPoisDetail))
            {
                floorPoisDetail.Clear();
                Mymap.Layers.Remove(floorPoisDetail);
            }

            if (Mymap.Layers.Contains(_sourceLayer))
                Mymap.Layers.Remove(_sourceLayer);

            if (_locationManager.Status != GeoPositionStatus.Ready && !_externalSource && !_sourcePoi)
            {
                //MessageBox.Show("No location services found. Please enable location services and try again!");
                if (Mymap.Layers.Contains(floorPoisDetail))
                    Mymap.Layers.Remove(floorPoisDetail);
            }
            else
            {
                if (to_entrance)
                {
                    Mymap.Layers.Remove(mapHoldLayer);

                    ApplicationBar.IsVisible = false;
                    MoveViewWindow(-70);
                    stackpanelOpen();
                    directions_grid.Visibility = Visibility.Visible;
                    calcProgrBar.Visibility = Visibility.Visible;
                    time.Visibility = Visibility.Collapsed;
                    distance.Visibility = Visibility.Collapsed;
                    calcProgrBar.IsIndeterminate = true;
                    GetNavigationRoute(entrance_location);
                    Mymap.SetView(entrance_location, 20, MapAnimationKind.Parabolic);
                    return;
                }
                else
                {
                    if (start_nav && intobuilding())
                    {
                        Mymap.MapElements.Clear();
                        GetNavigationRoutesBuilding(_selectedBuild, _floorPoiTo, _selectedFloor, _myLatitude.ToString(CultureInfo.InvariantCulture),
                        _myLongitude.ToString(CultureInfo.InvariantCulture));
                        return;
                    }

                }
                if (_searchBuilding && into_selectedbuiding())
                {
                    _searchBuilding = false;
                }


                // if we are into the building and no source poi set get navigations route from location to poi
                if (_tileLoaded && !_sourcePoi && !_externalSource && !_searchBuilding)
                {
                    Mymap.MapElements.Clear();
                    GetNavigationRoutesBuilding(_selectedBuild, _floorPoiTo, _selectedFloor, _myLatitude.ToString(CultureInfo.InvariantCulture),
                        _myLongitude.ToString(CultureInfo.InvariantCulture));

                }
                else
                {
                    // if we set source poi get navigations route from source poi else get all routes
                    if (_sourcePoi)
                        BuildingSourcePoi();
                    else
                    {
                        NavigateToFloorPoi();
                    }
                }
            }
        }

        private bool into_selectedbuiding()
        {
            if (_allbuildingfloors != null)
            {


                foreach (var obj1 in _allbuildingfloors.floors)
                {
                    if (obj1.buid.Equals(_selectedBuild))
                    {
                        var BBox = new GeoCoordinatesHelper(obj1.top_right_lat, obj1.top_right_lng, obj1.bottom_left_lat, obj1.bottom_left_lng);

                        //BBox.IncreaseBoundingBox(500);

                        var top_right_lat = BBox.NeLat;
                        var top_right_lon = BBox.NeLon;
                        var bottom_left_lat = BBox.SwLat;
                        var bottom_left_lon = BBox.SwnLon;

                        if (_myLatitude > bottom_left_lat && _myLatitude < top_right_lat &&
                      _myLongitude > bottom_left_lon &&
                      _myLongitude < top_right_lon)
                        {
                            return true;
                        }
                    }

                }
            }

            return false;
        }

        private bool intobuilding()
        {
            if (_allbuildingfloors != null)
            {

                foreach (var obj1 in _allbuildingfloors.floors)
                {
                    var BBox = new GeoCoordinatesHelper(obj1.top_right_lat, obj1.top_right_lng, obj1.bottom_left_lat, obj1.bottom_left_lng);

                    //BBox.IncreaseBoundingBox(500);

                    var top_right_lat = BBox.NeLat;
                    var top_right_lon = BBox.NeLon;
                    var bottom_left_lat = BBox.SwLat;
                    var bottom_left_lon = BBox.SwnLon;

                    if (_myLatitude > bottom_left_lat && _myLatitude < top_right_lat &&
                      _myLongitude > bottom_left_lon &&
                      _myLongitude < top_right_lon)
                    {
                        return true;
                    }
                }

            }
            return false;
        }

        private void NavigateToFloorPoi()
        {
            if (gd_result.Visibility == Visibility.Visible)
                gd_result.Visibility = Visibility.Collapsed;
            if (Mymap.Layers.Contains(indoorLayer))
                Mymap.Layers.Remove(indoorLayer);
            _getIndoornav = false;
            Mymap.Layers.Remove(mapHoldLayer);
            ApplicationBar.IsVisible = false;
            MoveViewWindow(-70);
            stackpanelOpen();
            directions_grid.Visibility = Visibility.Visible;
            calcProgrBar.Visibility = Visibility.Visible;
            time.Visibility = Visibility.Collapsed;
            distance.Visibility = Visibility.Collapsed;
            calcProgrBar.IsIndeterminate = true;

            if (_poisByBuilding != null)
            {
                foreach (var obj1 in _poisByBuilding.pois)
                {
                    if (obj1.buid.Equals(_selectedBuild))
                    {
                        if (obj1.is_building_entrance != null)
                        {
                            if (obj1.is_building_entrance.Equals("true"))
                            {
                                _floorPoiFrom = obj1.puid;
                                _floorPoiFromNumber = obj1.floor_number;
                            }
                        }
                    }

                }
            }
            set_coordinates = _floorPoiToLocation;
            GetNavigationRoutesPoiToPoi(_floorPoiFrom, _floorPoiTo, _floorPoiToLocation);

            if (Mymap.Layers.Contains(floorPoisDetail))
            {
                floorPoisDetail.Clear();
                Mymap.Layers.Remove(floorPoisDetail);
            }

        }

        void MygeocodequeryPoiSearch_QueryCompleted(object sender, QueryCompletedEventArgs<IList<MapLocation>> e)
        {
            if (e.Error == null)
            {
                MyQuery = new RouteQuery();
                GetRoute.Add(e.Result[0].GeoCoordinate);
                MyQuery.Waypoints = GetRoute;
                MyQuery.QueryCompleted += MyQueryPoiSearch_QueryCompleted;
                MyQuery.QueryAsync();
                _mygeocodequery.Dispose();
            }
        }

        MapLayer indoorLayer = new MapLayer();
        MapOverlay _overlayFrom;
        MapOverlay _overlayTo;
        private string _floorPoiToNumber;
        private bool _sourcePoi;
        private bool _firsttimeSearch;
        //private bool _trackmePress;
        private bool _tileLoaded;
        private bool _foundTile;
        private GeoCoordinate _externalCoordinates;
        private bool _externalSource;
        private string _buildingSearch;
        private bool _searchBuilding;
        private string bid = "";
        private string pid = "";
        private bool start_nav = false;
        private bool to_entrance;
        private GeoCoordinate entrance_location;
        private GeoCoordinate set_coordinates;
        private bool show_grid;

        void MyQueryPoiSearch_QueryCompleted(object sender, QueryCompletedEventArgs<Route> e)
        {
            if (e.Error == null)
            {
                var myRoute = e.Result;
                if (!_sourcePoi)
                {
                    _myMapRoute = new MapRoute(myRoute);
                    Mymap.AddRoute(_myMapRoute);
                    // time to destination
                    var time1 = myRoute.EstimatedDuration;
                    time.Text = "Duration: " + time1.ToString();

                    // total meters
                    Dispatcher.BeginInvoke(() =>
                    {
                        var meters = myRoute.LengthInMeters;
                        var kilometers = meters / 1000.0;
                        distance.Text = "Distance: " + kilometers.ToString(CultureInfo.InvariantCulture) + " Km";
                    });
                }
                else
                {
                    time.Text = "Duration: Unknown";
                    // total meters
                    Dispatcher.BeginInvoke(() =>
                    {
                        distance.Text = "Distance: Unknown";
                    });
                }


                routeNavDetails.Clear();
                var routeList = new List<DirectionsList>();
                if (!_sourcePoi)
                {
                    foreach (var leg in myRoute.Legs)
                    {
                        foreach (var maneuver in leg.Maneuvers)
                        {
                            var direction = new DirectionsList();
                            var det = new RouteDetails
                            {
                                instractions = maneuver.InstructionText,
                                coordinate =
                                    new GeoCoordinate(maneuver.StartGeoCoordinate.Latitude,
                                        maneuver.StartGeoCoordinate.Longitude),
                                distance = maneuver.LengthInMeters
                            };
                            routeNavDetails.Add(det);
                            var dist = maneuver.LengthInMeters / 1000.0;
                            var str = maneuver.InstructionText.ToUpper();
                            if (str.Contains("LEFT"))
                                direction.image = new BitmapImage(new Uri("/Assets/Navigation/turn-left.png", UriKind.Relative));
                            else if (str.Contains("RIGHT"))
                                direction.image = new BitmapImage(new Uri("/Assets/Navigation/turn-right.png", UriKind.Relative));
                            else if (str.Contains("HEAD"))
                                direction.image = new BitmapImage(new Uri("/Assets/start.png", UriKind.Relative));
                            else if (str.Contains("YOU HAVE"))
                                direction.image = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative));
                            else if (str.Contains("TRAFFIC CIRCLE"))
                                direction.image = new BitmapImage(new Uri("/Assets/Navigation/trafficcircle.png", UriKind.Relative));
                            direction.distance = dist.ToString(CultureInfo.InvariantCulture) + " Km";
                            direction.direction = maneuver.InstructionText;
                            direction.geocoordinate = det.coordinate;
                            routeList.Add(direction);
                        }
                    }
                }
                if (_getIndoornav)
                {
                    if (Mymap.Layers.Contains(indoorLayer))
                    {
                        Mymap.Layers.Remove(indoorLayer);
                    }



                    if (_poisByBuilding != null)
                    {
                        foreach (var obj1 in _poisByBuilding.pois)
                        {
                            if (obj1.puid.Equals(_floorPoiFrom))
                            {
                                if (obj1.is_building_entrance.Equals("true"))
                                {
                                    var direction = new DirectionsList
                                    {
                                        direction = "Get into Building",
                                        geocoordinate =
                                            new GeoCoordinate(double.Parse(obj1.coordinates_lat, CultureInfo.InvariantCulture),
                                                double.Parse(obj1.coordinates_lon, CultureInfo.InvariantCulture)),
                                        image =
                                            new BitmapImage(new Uri("/Assets/Navigation/entrance.png", UriKind.Relative))
                                    };
                                    routeList.Add(direction);

                                    _overlayFrom = new MapOverlay();
                                    var im = new Image
                                    {
                                        Source =
                                            new BitmapImage(new Uri("/Assets/Navigation/entrance.png", UriKind.Relative)),
                                        Width = 64,
                                        Height = 64
                                    };
                                    _overlayFrom.Content = im;
                                    _overlayFrom.GeoCoordinate = direction.geocoordinate; ;
                                    _overlayFrom.PositionOrigin = new Point(0.5, 1.0);
                                }
                                else
                                {
                                    var direction = new DirectionsList
                                    {
                                        direction = obj1.name,
                                        geocoordinate =
                                            new GeoCoordinate(double.Parse(obj1.coordinates_lat, CultureInfo.InvariantCulture),
                                                double.Parse(obj1.coordinates_lon, CultureInfo.InvariantCulture)),
                                        image = new BitmapImage(new Uri("/Assets/start.png", UriKind.Relative))
                                    };
                                    routeList.Add(direction);

                                    _overlayFrom = new MapOverlay();
                                    var im = new Image
                                    {
                                        Source = new BitmapImage(new Uri("/Assets/start.png", UriKind.Relative)),
                                        Width = 64,
                                        Height = 64
                                    };
                                    _overlayFrom.Content = im;
                                    _overlayFrom.GeoCoordinate = direction.geocoordinate; ;
                                    _overlayFrom.PositionOrigin = new Point(0.0, 1.0);
                                }
                            }
                        }

                        Mymap.SetView(set_coordinates, 20, MapAnimationKind.Parabolic);
                    }

                    if (_poisByBuilding != null)
                    {
                        foreach (var obj1 in _poisByBuilding.pois)
                        {
                            if (obj1.puid.Equals(_floorPoiTo))
                            {
                                var direction = new DirectionsList
                                {
                                    direction = obj1.name,
                                    geocoordinate =
                                        new GeoCoordinate(double.Parse(obj1.coordinates_lat, CultureInfo.InvariantCulture),
                                            double.Parse(obj1.coordinates_lon, CultureInfo.InvariantCulture)),
                                    image = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative))
                                };
                                routeList.Add(direction);

                                _overlayTo = new MapOverlay();
                                var im = new Image
                                {
                                    Source = new BitmapImage(new Uri("/Assets/finish.png", UriKind.Relative))
                                };
                                _overlayTo.Content = im;
                                _overlayTo.GeoCoordinate = direction.geocoordinate; ;
                                _overlayTo.PositionOrigin = new Point(1.0, 1.0);
                            }
                        }
                    }
                    indoorLayer.Clear();

                    if (Mymap.Layers.Contains(indoorLayer))
                        Mymap.Layers.Remove(indoorLayer);
                    if (_floorPoiFromNumber == _floorPoiToNumber)
                    {
                        if (_selectedFloor == _floorPoiToNumber)
                        {
                            indoorLayer.Add(_overlayFrom);
                            indoorLayer.Add(_overlayTo);
                        }
                    }
                    else
                    {
                        if (_selectedFloor == _floorPoiFromNumber)
                        {
                            indoorLayer.Add(_overlayFrom);
                        }
                        else
                            if (_selectedFloor == _floorPoiToNumber)
                                indoorLayer.Add(_overlayTo);
                    }
                    Mymap.Layers.Add(indoorLayer);
                }
                //_sourcePoi = false;
                CreaterouteOverlays();
                RouteLLS.ItemsSource = routeList;
                RouteLLS.SelectionChanged += RouteLLS_SelectionChanged;
                MyQuery.Dispose();
            }

            calcroutestext.Visibility = Visibility.Collapsed;
            calcProgrBar.Visibility = Visibility.Collapsed;
            time.Visibility = Visibility.Visible;
            distance.Visibility = Visibility.Visible;

        }


        #endregion

        private async void btn_builddown_Click(object sender, RoutedEventArgs e)
        {
            downloading.Visibility = Visibility.Visible;
            try
            {
                var worldbuildings1 = await CustomPushpinWp8APIClient.GetWorldBuildings();
                foreach (var obj in worldbuildings1.buildings)
                {
                    var building = await CustomPushpinWp8APIClient.GetPoisByBuilding(obj.buid);
                    //_poisByBuilding.Add(building);
                }
            }
            catch
            {
                downloading.Visibility = Visibility.Collapsed;
                MessageBox.Show("Can not load floor pois. Please check your connection and try again!");
            }
            downloading.Visibility = Visibility.Collapsed;
        }


        private void btn_floordown_Click(object sender, RoutedEventArgs e)
        {
            downloading.Visibility = Visibility.Visible;
            GetPoisByFloor(_selectedBuild, _selectedFloor);
        }

        private void download_click(object sender, EventArgs e)
        {
            var isNetwork = NetworkInterface.GetIsNetworkAvailable();

            if (isNetwork)
            {
                if (isDownloading)
                    return;
                isDownloading = true;
                loading.Visibility = Visibility.Visible;
                loading.IsIndeterminate = true;
                DownloadRequireStaff();
            }
            else
                MessageBox.Show("Error downloading maps. Please check your internet connection and try again!");

        }



        private void ContentPanel_Tap(object sender, System.Windows.Input.GestureEventArgs e)
        {
            //if (_autocompleteFocus || !_autocompleteFocus)
            //{
            //    SystemTray.IsVisible = false;
            //    ApplicationBar.IsVisible = true;
            //    ContentPanel.Visibility = Visibility.Collapsed;
            //    MoveViewWindow(30);
            //}
        }

        private void about_click(object sender, EventArgs e)
        {
            // NavigationService.Navigate(new Uri("/Settings.xaml?", UriKind.Relative));
            NavigationService.Navigate(new Uri("/About.xaml?", UriKind.Relative));
        }

        private void ContentPanel_OnHold(object sender, GestureEventArgs e)
        {
            Dispatcher.BeginInvoke(() => this.Focus());
            SystemTray.IsVisible = false;
            ApplicationBar.IsVisible = true;
            ContentPanel.Visibility = Visibility.Collapsed;
            MoveViewWindow(30);
        }



        #region building picker
        private void listpicker_selection_change(object sender, SelectionChangedEventArgs e)
        {

            if (isDownloading)
                return;
            if (!pickerOpen)
                return;

            pickerOpen = false;

            _foundBuilding = false;
            buildLoaded = false;
            removeSearchButton();
            if (BPicker.SelectedItem != null)
            {
                var selected = BPicker.SelectedItem.ToString();
                if (selected.Equals("None") || selected.Equals("No Buildings to show"))
                    return;
                if (Mymap.TileSources.Count > 0)
                    Mymap.TileSources.Clear();
                BuildingSearch(selected);
                if (_foundBuilding)
                {
                    _searchBuilding = true;
                    _tileLoaded = true;
                    _foundTile = true;
                }
                else
                {
                    _searchBuilding = false;
                    _tileLoaded = false;
                    _foundTile = false;
                    if (Mymap.TileSources.Contains(_wmstileprovider))
                        Mymap.TileSources.Remove(_wmstileprovider);
                }
                grid_search.Visibility = Visibility.Collapsed;
                ContentPanel.Visibility = Visibility.Collapsed;
                ApplicationBar.IsVisible = true;
            }

        }

        bool pickerOpen;
        private void choose_build(object sender, RoutedEventArgs e)
        {
            pickerOpen = true;
            RemoveIndoorStaff();
            BPicker.SelectedIndex = 0;
            BPicker.Open();
        }

        List<string> bpickerItems;
        void createBuildingPicker()
        {
            bpickerItems = new List<string>();
            if (_worldbuildings != null)
            {
                //bpickerItems.Add("None");
                foreach (var obj in _worldbuildings.buildings)
                {
                    bpickerItems.Add(obj.name);
                }
            }
            else
            {
                bpickerItems.Add("No Buildings to show");
            }
            bpickerItems.Sort();
            bpickerItems.Insert(0, "None");
            BPicker.ItemsSource = bpickerItems;
        }

        #endregion

        #region floor change (up,down)

        private void floorUp(object sender, RoutedEventArgs e)
        {
            if (!buildLoaded)
                return;

            if (mapHoldLayer != null)
            {
                mapHoldLayer.Clear();
                if (Mymap.Layers.Contains(mapHoldLayer))
                {
                    Mymap.Layers.Remove(mapHoldLayer);
                }
            }

            if (floorPoisDetail != null)
            {
                floorPoisDetail.Clear();
                if (Mymap.Layers.Contains(floorPoisDetail))
                    Mymap.Layers.Remove(floorPoisDetail);
            }


            var sel = txt_cur.Text;
            if (String.IsNullOrEmpty(sel))
                return;
            var x = int.Parse(sel);
            x = x + 1;
            bool correct = false;
            string floor = null;
            foreach (var obj in _allbuildingfloors.floors)
            {
                if (obj.floor_number.Equals(x.ToString()))
                {
                    correct = true;
                    floor = obj.floor_number;
                }
            }

            if (!correct)
                return;

            if (buildLoaded)
            {
                _selectedFloor = floor;

                Dispatcher.BeginInvoke(() =>
                {
                    txt_cur.Text = _selectedFloor;
                });

                Mymap.TileSources.Clear();
                ChangeFloor();

                if (!_selectedFloor.Equals(_floorPoiToNumber))
                    Mymap.MapElements.Clear();
                else
                {
                    if (_polyline != null)
                        if (!Mymap.MapElements.Contains(_polyline))
                            Mymap.MapElements.Add(_polyline);
                }

                if (_routePoi != null)
                    if (_routePoi.pois != null)
                        if (_routePoi.pois.Count > 0)
                            GetRouteOfTheCurrentFloor();

                if (Mymap.Layers.Contains(indoorLayer))
                {
                    Mymap.Layers.Remove(indoorLayer);
                    indoorLayer.Clear();
                    if (_floorPoiFromNumber == _floorPoiToNumber)
                    {
                        if (_selectedFloor == _floorPoiToNumber)
                        {
                            indoorLayer.Add(_overlayFrom);
                            indoorLayer.Add(_overlayTo);
                        }

                    }
                    else
                    {
                        if (_selectedFloor == _floorPoiFromNumber)
                        {
                            indoorLayer.Add(_overlayFrom);
                        }
                        else
                            if (_selectedFloor == _floorPoiToNumber)
                                indoorLayer.Add(_overlayTo);
                    }
                    Mymap.Layers.Add(indoorLayer);
                }
            }

        }

        private void floorDown(object sender, RoutedEventArgs e)
        {
            if (!buildLoaded)
                return;

            if (mapHoldLayer != null)
            {
                mapHoldLayer.Clear();
                if (Mymap.Layers.Contains(mapHoldLayer))
                {
                    Mymap.Layers.Remove(mapHoldLayer);
                }
            }

            if (floorPoisDetail != null)
            {
                floorPoisDetail.Clear();
                if (Mymap.Layers.Contains(floorPoisDetail))
                    Mymap.Layers.Remove(floorPoisDetail);
            }


            var sel = txt_cur.Text;
            if (String.IsNullOrEmpty(sel))
                return;
            var x = int.Parse(sel);
            x = x - 1;
            bool correct = false;
            string floor = null;
            foreach (var obj in _allbuildingfloors.floors)
            {
                if (obj.floor_number.Equals(x.ToString()))
                {
                    correct = true;
                    floor = obj.floor_number;
                }
            }

            if (!correct)
                return;

            if (buildLoaded)
            {
                _selectedFloor = floor;

                Dispatcher.BeginInvoke(() =>
                {
                    txt_cur.Text = _selectedFloor;
                });

                Mymap.TileSources.Clear();
                ChangeFloor();

                if (!_selectedFloor.Equals(_floorPoiToNumber))
                    Mymap.MapElements.Clear();
                else
                {
                    if (_polyline != null)
                        if (!Mymap.MapElements.Contains(_polyline))
                            Mymap.MapElements.Add(_polyline);
                }

                if (_routePoi != null)
                    if (_routePoi.pois != null)
                        if (_routePoi.pois.Count > 0)
                            GetRouteOfTheCurrentFloor();

                if (Mymap.Layers.Contains(indoorLayer))
                {
                    Mymap.Layers.Remove(indoorLayer);
                    indoorLayer.Clear();
                    if (_floorPoiFromNumber == _floorPoiToNumber)
                    {
                        if (_selectedFloor == _floorPoiToNumber)
                        {
                            indoorLayer.Add(_overlayFrom);
                            indoorLayer.Add(_overlayTo);
                        }

                    }
                    else
                    {
                        if (_selectedFloor == _floorPoiFromNumber)
                        {
                            indoorLayer.Add(_overlayFrom);
                        }
                        else
                            if (_selectedFloor == _floorPoiToNumber)
                                indoorLayer.Add(_overlayTo);
                    }
                    Mymap.Layers.Add(indoorLayer);
                }
            }
        }

        #endregion

        private void show_opt(object sender, EventArgs e)
        {
            CanvasMenu.Visibility = Visibility.Collapsed;
            ApplicationBar.IsVisible = false;
            MapOptions.Visibility = Visibility.Visible;
        }



        void stackpanelOpen()
        {
            stack.Margin = new Thickness(0, 110, 0, 0);
        }

        void stackpanelClose()
        {
            stack.Margin = new Thickness(0, 10, 0, 0);
        }

        private void clearMap(object sender, RoutedEventArgs e)
        {
            closeGrid.Visibility = Visibility.Collapsed;
            clear();
        }

        private void clearMap2(object sender, RoutedEventArgs e)
        {
            grid_search.Visibility = Visibility.Collapsed;
            all_builds.Visibility = Visibility.Visible;
            clear();
        }



    }
}

