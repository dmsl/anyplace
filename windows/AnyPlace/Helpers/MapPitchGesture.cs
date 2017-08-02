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

using Microsoft.Phone.Maps.Controls;
using System;
using System.Windows.Input;

namespace AnyPlace
{
  /// <summary>
  /// Adds a three-finger pitch gesture to a Map control.
  /// </summary>
  public class MapPitchGesture : MapGestureBase
  {
    /// <summary>
    /// Gets or sets the sensitivity of this gesture
    /// </summary>
    public double Sensitivity { get; set; }

    private double? _initialPitchYLocation;

    public MapPitchGesture(Map map)
      : base(map)
    {
      Sensitivity = 0.5;
      Touch.FrameReported += Touch_FrameReported;
    }

    private void Touch_FrameReported(object sender, TouchFrameEventArgs e)
    {
        try
        {
            var touchPoints = e.GetTouchPoints(Map);

            SuppressMapGestures = touchPoints.Count == 3;

            if (touchPoints.Count == 3)
            {
                if (!_initialPitchYLocation.HasValue)
                {
                    _initialPitchYLocation = touchPoints[0].Position.Y;
                }

                double delta = touchPoints[0].Position.Y - _initialPitchYLocation.Value;
                double newPitch = Math.Max(0, Math.Min(75, (Map.Pitch + delta * Sensitivity)));
                Map.Pitch = newPitch;
                _initialPitchYLocation = touchPoints[0].Position.Y;
            }
            else
            {
                _initialPitchYLocation = null;
            }
        }
        catch { }
      
    }
  }
}
