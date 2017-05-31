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
using System.IO.IsolatedStorage;

namespace AnyPlace
{
	public class AppSettings
	{
		// Our settings
		IsolatedStorageSettings settings;

		// The key names of our settings
		public const string buildinKeyName = "BuildingSettings";
		public const string countryKeyName = "CountrySettings";
		public const string cityKeyName = "CittySettings";
		public const string streetKeyName = "StreetSettings";
		public const string districtKeyName = "DistrictSettings";
		public const string bingKeyName = "BingSettings";
		public const string showtilesKeyName = "TilesSettings";
		public const string showzoomKeyName = "ZoomSettings";
		public const string showtrackmeKeyName = "TrackmeSettings";
		public const string showoptionsKeyName = "OptionsSettings";

        public const string getServerName = "ServerName";


        public const string bingEnnableKeyName = "BingEnnableSettings";
		public const string buildingEnnableKeyName = "BuildingEnnableSettings";
		public const string countryEnnableKeyName = "CountryEnnableSettings";
		public const string cityEnnableKeyName = "CityEnnableSettings";
		public const string streetEnnableKeyName = "StreetEnnableSettings";
		public const string districtEnnableKeyName = "DistrictEnnableSettings";

		public const string rotateMapEnableKeyName = "RotateMapEnableSettings";


		// The default value of our settings
		const bool building_check = true;
		const bool country_check = false;
		const bool city_check = false;
		const bool street_check = false;
		const bool district_check = false;
		const bool bing_check = true;
		const bool showtiles_check = true;
		const bool showzoom_check = true;
		const bool showtrackme_check = false;
		const bool showoptions_check = true;

		const bool bing_ennable = true;
		const bool building_ennable = true;
		const bool country_ennable = false;
		const bool city_ennable = false;
		const bool street_ennable = false;
		const bool district_ennable = false;

		const bool rotate_map = true;

        const String serverName = "https://anyplace.rayzit.com";


		/// <summary>
		/// Constructor that gets the application settings.
		/// </summary>
		public AppSettings()
		{
			if (!System.ComponentModel.DesignerProperties.IsInDesignTool)
			{
				// Get the settings for this application.
				settings = IsolatedStorageSettings.ApplicationSettings;
			}
		}

		/// <summary>
		/// Update a setting value for our application. If the setting does not
		/// exist, then add the setting.
		/// </summary>
		/// <param name="Key"></param>
		/// <param name="value"></param>
		/// <returns></returns>
		public bool AddOrUpdateValue(string Key, Object value)
		{
			bool valueChanged = false;

			// If the key exists
			if (settings.Contains(Key))
			{
				// If the value has changed
				if (settings[Key] != value)
				{
					// Store the new value
					settings[Key] = value;
					valueChanged = true;
				}
			}
			// Otherwise create the key.
			else
			{
				settings.Add(Key, value);
				valueChanged = true;
			}
			return valueChanged;
		}

		/// <summary>
		/// Get the current value of the setting, or if it is not found, set the 
		/// setting to the default setting.
		/// </summary>
		/// <typeparam name="T"></typeparam>
		/// <param name="Key"></param>
		/// <param name="defaultValue"></param>
		/// <returns></returns>
		public T GetValueOrDefault<T>(string Key, T defaultValue)
		{
			T value;

			// If the key exists, retrieve the value.
			if (settings.Contains(Key))
			{
				value = (T)settings[Key];
			}
			// Otherwise, use the default value.
			else
			{
				settings.Add(Key, defaultValue);
				value = defaultValue;
			}
			return value;
		}

		/// <summary>
		/// Save the settings.
		/// </summary>
		public void Save()
		{
			settings.Save();
		}


		/// <summary>
		/// Property to get and set a CheckBox Setting Key.
		/// </summary>
		public bool ToogleButtonTiles
		{
			get
			{
				return GetValueOrDefault(showtilesKeyName, showtiles_check);
			}
			set
			{
				if (AddOrUpdateValue(showtilesKeyName, value))
				{
					Save();
				}
			}
		}


		/// <summary>
		/// Property to get and set a ListBox Setting Key.
		/// </summary>
		public bool ToggleButtonZoom
		{
			get
			{
				return GetValueOrDefault(showzoomKeyName, showzoom_check);
			}
			set
			{
				if (AddOrUpdateValue(showzoomKeyName, value))
				{
					Save();
				}
			}
		}


		/// <summary>
		/// Property to get and set a RadioButton Setting Key.
		/// </summary>
		public bool ToogleButtonTrackme
		{
			get
			{
				return GetValueOrDefault(showtrackmeKeyName, showtrackme_check);
			}
			set
			{
				if (AddOrUpdateValue(showtrackmeKeyName, value))
				{
					Save();
				}
			}
		}


		/// <summary>
		/// Property to get and set a RadioButton Setting Key.
		/// </summary>
		public bool ToggleButtonMapOptions
		{
			get
			{
				return GetValueOrDefault(showoptionsKeyName, showoptions_check);
			}
			set
			{
				if (AddOrUpdateValue(showoptionsKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a RadioButton Setting Key.
		/// </summary>
		public bool CheckBoxBing
		{
			get
			{
				return GetValueOrDefault(bingKeyName, bing_check);
			}
			set
			{
				if (AddOrUpdateValue(bingKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Username Setting Key.
		/// </summary>
		public bool CheckBoxBuildings
		{
			get
			{
				return GetValueOrDefault(buildinKeyName, building_check);
			}
			set
			{
				if (AddOrUpdateValue(buildinKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool CheckBoxCountry
		{
			get
			{
				return GetValueOrDefault(countryKeyName, country_check);
			}
			set
			{
				if (AddOrUpdateValue(countryKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool CheckBoxCity
		{
			get
			{
				return GetValueOrDefault(cityKeyName, city_check);
			}
			set
			{
				if (AddOrUpdateValue(cityKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool CheckBoxStreet
		{
			get
			{
				return GetValueOrDefault(streetKeyName, street_check);
			}
			set
			{
				if (AddOrUpdateValue(streetKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool CheckBoxDistrict
		{
			get
			{
				return GetValueOrDefault(districtKeyName, district_check);
			}
			set
			{
				if (AddOrUpdateValue(districtKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool BingEnnable
		{
			get
			{
				return GetValueOrDefault(bingEnnableKeyName, bing_ennable);
			}
			set
			{
				if (AddOrUpdateValue(bingEnnableKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool BuildingEnnable
		{
			get
			{
				return GetValueOrDefault(buildingEnnableKeyName, building_ennable);
			}
			set
			{
				if (AddOrUpdateValue(buildingEnnableKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool CountryEnnable
		{
			get
			{
				return GetValueOrDefault(countryEnnableKeyName, country_ennable);
			}
			set
			{
				if (AddOrUpdateValue(countryEnnableKeyName, value))
				{
					Save();
				}
			}
		}


		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool CityEnnable
		{
			get
			{
				return GetValueOrDefault(cityEnnableKeyName, city_ennable);
			}
			set
			{
				if (AddOrUpdateValue(cityEnnableKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool StreetEnnable
		{
			get
			{
				return GetValueOrDefault(streetEnnableKeyName, street_ennable);
			}
			set
			{
				if (AddOrUpdateValue(streetEnnableKeyName, value))
				{
					Save();
				}
			}
		}

		/// <summary>
		/// Property to get and set a Password Setting Key.
		/// </summary>
		public bool DistrictEnnable
		{
			get
			{
				return GetValueOrDefault(districtEnnableKeyName, district_ennable);
			}
			set
			{
				if (AddOrUpdateValue(districtEnnableKeyName, value))
				{
					Save();
				}
			}
		}

		public bool ToggleButtonMapRotation
		{
			get
			{
				return GetValueOrDefault(rotateMapEnableKeyName, rotate_map);
			}
			set
			{
				if (AddOrUpdateValue(rotateMapEnableKeyName, value))
				{
					Save();
				}
			}
		}

        public String ServerName
        {
            get
            {
                return GetValueOrDefault(getServerName, serverName);
            }
            set
            {
                if (AddOrUpdateValue(getServerName, value))
                {
                    Save();
                }
            }
        }

        //public static void LoadSettings(){
        //	IsolatedStorageSettings set = IsolatedStorageSettings.ApplicationSettings;
        //	set.Add(buildinKeyName, building_check);
        //	set.Add(countryKeyName, country_check);
        //	set.Add(cityKeyName, city_check);
        //	set.Add(streetKeyName, street_check);
        //	set.Add(districtKeyName, district_check);
        //	set.Add(bingKeyName, bing_check);
        //	set.Add(showtilesKeyName, showtiles_check);
        //	set.Add(showzoomKeyName, showzoom_check);
        //	set.Add(showtrackmeKeyName, showtrackme_check);
        //	set.Add(showoptionsKeyName, showoptions_check);
        //	set.Add(bingEnnableKeyName, bing_ennable);
        //	set.Add(buildingEnnableKeyName, building_ennable);
        //	set.Add(countryEnnableKeyName, country_ennable);
        //	set.Add(cityEnnableKeyName, city_ennable);
        //	set.Add(streetEnnableKeyName, street_ennable);
        //	set.Add(districtEnnableKeyName, district_ennable);
        //	set.Add(rotateMapEnableKeyName, rotate_map);
        //}
    }
}
