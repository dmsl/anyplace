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
  /// Adds a two-finger rotation gesture to a Map control.
  /// </summary>
  public class MapRotationGesture : MapGestureBase
  {
    /// <summary>
    /// Gets or sets the minimuum rotation that the user must apply in order to initiate this gesture.
    /// </summary>
    public double MinimumRotation { get; set; }

    private double? _previousAngle;

    private bool _isRotating;
    
    public MapRotationGesture(Map map)
      : base(map)
    {
      MinimumRotation = 10.0;
      Touch.FrameReported += Touch_FrameReported;
      rotate = true;
    }
    

    private void Touch_FrameReported(object sender, TouchFrameEventArgs e)
    {
        if(rotate){
            TouchPointCollection touchPoints;
            try
            {
                touchPoints = e.GetTouchPoints(Map);
            }
            catch
            {
                return;
            }

            if (touchPoints.Count == 2)
            {
                // for the initial touch, record the angle between the fingers
                if (!_previousAngle.HasValue)
                {
                    _previousAngle = AngleBetweenPoints(touchPoints[0], touchPoints[1]);
                }

                // should we rotate?
                if (!_isRotating)
                {
                    double angle = AngleBetweenPoints(touchPoints[0], touchPoints[1]);
                    double delta = angle - _previousAngle.Value;
                    if (Math.Abs(delta) > MinimumRotation)
                    {
                        _isRotating = true;
                        SuppressMapGestures = true;
                    }
                }

                // rotate me
                if (_isRotating && rotate)
                {
                    double angle = AngleBetweenPoints(touchPoints[0], touchPoints[1]);
                    double delta = angle - _previousAngle.Value;
                    Map.Heading -= delta;
                    _previousAngle = angle;
                }
            }
            else
            {
                _previousAngle = null;
                _isRotating = false;
                SuppressMapGestures = false;
            }
        }
      
    }

    bool rotate;

    public void rotation(bool rot)
    {
       this.rotate = rot;
    }

    private double AngleBetweenPoints(TouchPoint p1, TouchPoint p2)
    {
      return Math.Atan2(p1.Position.Y - p2.Position.Y, p1.Position.X - p2.Position.X)
              *(180 / Math.PI);
    }
  }
}
