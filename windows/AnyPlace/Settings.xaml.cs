using System.Collections.Generic;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Navigation;
using Microsoft.Phone.Controls;
using Microsoft.Phone.Shell;
using AnyPlace.ApiClient;
using System.IO.IsolatedStorage;
using System.Runtime.Serialization;
using System.IO;
using System.Net.NetworkInformation;
using AnyPlace.classes;

namespace AnyPlace
{
	public partial class Settings : PhoneApplicationPage
	{

		List<string> listItems;

		private bool downloaded;


		public Settings()
		{
			InitializeComponent();
		}

		protected override void OnNavigatedTo(NavigationEventArgs e)
		{
			createListPickerItem();
			base.OnNavigatedTo(e);
		}

		protected override void OnBackKeyPress(System.ComponentModel.CancelEventArgs e)
		{
			if (downloading.Visibility == Visibility.Collapsed)
			{
				NavigationService.GoBack();
			}
			
		}

		protected override void OnNavigatedFrom(NavigationEventArgs e)
		{
			PhoneApplicationService.Current.State.Clear();
			PhoneApplicationService.Current.State.Add("downloaded", downloaded);
			PhoneApplicationService.Current.State.Add("showzoom_check", App.Settings.ToggleButtonZoom);
			PhoneApplicationService.Current.State.Add("showtiles_check", App.Settings.ToogleButtonTiles);
			PhoneApplicationService.Current.State.Add("showtrackme_check", App.Settings.ToogleButtonTrackme);

			base.OnNavigatedFrom(e);
		}


		#region building control
		private void createListPickerItem()
		{
			listItems = new List<string>();
			if (Helper.worldbuildings != null)
			{
				listItems.Add("None");
				foreach (var obj in Helper.worldbuildings.buildings)
				{
					listItems.Add(obj.name);
				}
			}
			else
			{
				listItems.Add("No Buildings to show");
			}
			listpicker.ItemsSource = listItems;
			
		}


        async void downloadData() 
        {
            WorldBuilding worldbuildings1 = await CustomPushpinWp8APIClient.GetWorldBuildings();
            Helper.worldbuildings = worldbuildings1;
            writeWorldBuildingsToIsolatedStorage(worldbuildings1);
            createListPickerItem();
            downloading.Visibility = Visibility.Collapsed;
        }

		private async void downloadAllBuildings()
		{
			try
			{
				Dispatcher.BeginInvoke(() =>
				{
					txt_download.Text = "Download building data..";
				});

				
				WorldBuilding worldbuildings1 = await CustomPushpinWp8APIClient.GetWorldBuildings();
				foreach (var obj in worldbuildings1.buildings)
				{
					PoisByBuilding building = await CustomPushpinWp8APIClient.GetPoisByBuilding(obj.buid);

                    AllBuildingFloors allbuildingfloors = await CustomPushpinWp8APIClient.GetAllBuildingFloors(obj.buid);

                    writePoisByBuildingToIsolatedStorage(building,obj.buid);
                    writeAllBuildingFloorsToIsolatedStorage(allbuildingfloors, obj.buid);

                    for (int i = 0; i < allbuildingfloors.floors.Count; i++)
                    {
                        Dispatcher.BeginInvoke(() =>
                        {
                            txt_download.Text = "Download floor " + (i + 1) + " of " + allbuildingfloors.floors.Count;
                        });

                        foreach (var obj1 in allbuildingfloors.floors)
                        {
                            await CustomPushpinWp8APIClient.GetTiles(obj1.buid, obj1.floor_number);
                        }
                    }
				}

				

				Dispatcher.BeginInvoke(() =>
				{
					txt_download.Text = "Saving data..";
				});

				writeWorldBuildingsToIsolatedStorage(worldbuildings1);
				Helper.worldbuildings = worldbuildings1;
				//Helper.allbuildingfloors = allbuildingfloors1;
				createListPickerItem();
			}
			catch
			{
				downloading.Visibility = Visibility.Collapsed;
				MessageBox.Show("Can not download all buildings. Please check your connection and try again!");
			}
			downloading.Visibility = Visibility.Collapsed;
			
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


		private void Download_Click(object sender, RoutedEventArgs e)
		{
			bool isNetwork = NetworkInterface.GetIsNetworkAvailable();

			if (isNetwork)
			{
				downloaded = true;
				downloading.Visibility = Visibility.Visible;
				downloadAllBuildings();
			}
			else
				MessageBox.Show("No internet connection found. Please check your internet connection and try again later!", "No Internet Connection", MessageBoxButton.OK);
		}


		private async void check_click(object sender, RoutedEventArgs e)
		{
			bool isNetwork = NetworkInterface.GetIsNetworkAvailable();

			if (isNetwork)
			{
				try
				{
					WorldBuilding worldbuildings1 = await CustomPushpinWp8APIClient.GetWorldBuildings();
					if (worldbuildings1.buildings.Count > Helper.worldbuildings.buildings.Count)
					{
						CustomMessageBox messageBox = new CustomMessageBox()
						{
							Caption = "Buildings",
							Message = "New Buildings found. Do you want to download them?",
							LeftButtonContent = "yes",
							RightButtonContent = "no"
						};
						messageBox.Dismissed += (s1, e1) =>
						{
							switch (e1.Result)
							{
								case CustomMessageBoxResult.LeftButton:
									downloading.Visibility = Visibility.Visible;
									downloaded = true;
                                    downloadData();
									break;
								case CustomMessageBoxResult.RightButton:
									// Do something.
									break;
								case CustomMessageBoxResult.None:
									// Do something.
									break;
								default:
									break;
							}
						};

						messageBox.Show();
					}
					else
						MessageBox.Show("No new Buildings found!");
				}
				catch
				{
					MessageBox.Show("Can not check for update. Please check your connection and try again!");
				}
			}
			else
				MessageBox.Show("No internet connection found. Please check your internet connection and try again later!", "No Internet Connection", MessageBoxButton.OK);

		}


		private async void listpicker_selection_change(object sender, SelectionChangedEventArgs e)
		{
			if (listpicker != null)
			{
				if (listpicker.SelectedItem != null)
				{
					string selected = listpicker.SelectedItem.ToString();
					string buid = "";
					if (selected.Equals("None") || selected.Equals("No Buildings to show"))
						return;
					else
					{
						bool isNetwork = NetworkInterface.GetIsNetworkAvailable();

						if (isNetwork)
						{
							downloading.Visibility = Visibility.Visible;
							try
							{
								foreach (var obj in Helper.worldbuildings.buildings)
								{
									if (obj.name.Equals(selected))
									{
										buid = obj.buid;
										break;
									}
								}

                                PoisByBuilding building = await CustomPushpinWp8APIClient.GetPoisByBuilding(buid);

                                AllBuildingFloors allbuildingfloors = await CustomPushpinWp8APIClient.GetAllBuildingFloors(buid);

                                writePoisByBuildingToIsolatedStorage(building, buid);
                                writeAllBuildingFloorsToIsolatedStorage(allbuildingfloors, buid);

                                for (int i = 0; i < allbuildingfloors.floors.Count; i++)
                                {
                                    Dispatcher.BeginInvoke(() =>
                                    {
                                        txt_download.Text = "Download floor " + (i + 1) + " of " + allbuildingfloors.floors.Count;
                                    });

                                    foreach (var obj1 in allbuildingfloors.floors)
                                    {
                                        await CustomPushpinWp8APIClient.GetTiles(obj1.buid, obj1.floor_number);
                                    }
                                }
							}
							catch
							{
								downloading.Visibility = Visibility.Collapsed;
								MessageBox.Show("Can not download selected building. Please check your connection and try again!");
							}

							downloading.Visibility = Visibility.Collapsed;
						}
						else
						{
							MessageBox.Show("No internet connection found. Please check your internet connection and try again later!", "No Internet Connection", MessageBoxButton.OK);
							downloading.Visibility = Visibility.Collapsed;
						}
							
					}
				}
			}
		}
		#endregion

		

		#region anyplace control

		private void showTiles_Checked(object sender, RoutedEventArgs e)
		{
			
		}

		private void showTiles_Unchecked(object sender, RoutedEventArgs e)
		{
			
		}


		private void showZoom_Checked(object sender, RoutedEventArgs e)
		{
			
		}

		private void showZoom_Unchecked(object sender, RoutedEventArgs e)
		{
		   
		}

		private void showTrackme_Checked(object sender, RoutedEventArgs e)
		{
		   
		}

		private void showTrackme_Unchecked(object sender, RoutedEventArgs e)
		{
		   
		}

		private void showOptions_Checked(object sender, RoutedEventArgs e)
		{
			
		}

		private void showOptions_Unchecked(object sender, RoutedEventArgs e)
		{
			
		}

		#endregion

		
	}
}