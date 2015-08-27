
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
using System.Windows.Navigation;

namespace AnyPlace
{
	class CustomUriMapper : UriMapperBase 
	{
		private string tempUri;
		private string buildid;
		private string poiid;

		public override Uri MapUri(Uri uri)
		{
			// The original URI is encoded prior to URI association. Decode before using it. 
			tempUri = System.Net.HttpUtility.UrlDecode(uri.ToString());

			// URI association launch for contoso handler page. 
			if (tempUri.Contains("anyplace-dmsl-getnavigation:to?"))
			{
				// Get the build ID (after "BuildID=").
				if (tempUri.Contains("bid="))
				{
					int categoryIdIndex = tempUri.IndexOf("bid=") + 4;
					buildid = tempUri.Substring(categoryIdIndex);
					string[] word = buildid.Split('&');
					buildid = word[0];

					// Get the poi ID (after "BuildId="). 
					if (tempUri.Contains("pid="))
					{
						int origUriIndex = tempUri.IndexOf("pid=") + 4;
						poiid = tempUri.Substring(origUriIndex);
						string[] word1 = poiid.Split('&');
						poiid = word1[0];
					}
					else
					{
						poiid = "none";
					}
				}
				else
				{
					buildid = "none";
					poiid = "none";
				}


				// Construct new URI 
				string NewURI = String.Format("/MainPage.xaml?BuildId={0}&PoiId={1}",
											buildid, poiid);

				// Map the show products request to ShowProducts.xaml 
				return new Uri(NewURI, UriKind.Relative);
			}

			// Otherwise perform normal launch. 
			return uri;
		} 
	}
}
