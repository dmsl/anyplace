/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Artem Nikitin
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
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

import UIKit
import CoreLocation

class NavigatorController: UIViewController, UIWebViewDelegate, CLLocationManagerDelegate, ApiClientDelegate, SensorControllerDelegate {
    
    private var locationManager = CLLocationManager()
    private var approximatePosition: LatLng! = nil {
        didSet {
            if let pos = approximatePosition {
                requestNearbyBuildings(pos)
            }
        }
    }

    private let indexURL: NSURL! = NSBundle.mainBundle().URLForResource("index", withExtension: "html", subdirectory: "Navigator-Website")!
    
    private var token: String = "api_tester" //Get token from signing to G+
    
    private var apiClient = ApiClient() {
        didSet {
            apiClient.delegate = self
        }
    }
    
    private var sensorController: SensorController = try! SensorController(options: [.NavigationCalibratedReferenceTrueNorth])!
    
    enum RequestFromJavaScript {
        case StartMagneticLocalization, StopMagneticLocalization
        
        static func fromString(s: String?) -> RequestFromJavaScript? {
            if let request = s {
                if request == "iOS: requestLocalization" {
                    return .StartMagneticLocalization
                } else if request == "iOS: stopLocalization" {
                    return .StopMagneticLocalization
                }
            }
            return nil
        }
    }
    
    @IBOutlet weak var webView: UIWebView! {
        didSet {
            webView.delegate = self
        }
    }
    
    func webView(webView: UIWebView, shouldStartLoadWithRequest request: NSURLRequest, navigationType: UIWebViewNavigationType) -> Bool {
        print("NavigatorController: shouldStartLoadWithRequest: \(request.URL) \(navigationType.rawValue)")
        
        if let request = RequestFromJavaScript.fromString(request.URL?.description) {
            switch request {
            case .StartMagneticLocalization: requestLocalization()
            case .StopMagneticLocalization: stopLocalization()
            }
            return false
        }
        
        return true
    }
    
    func webView(webView: UIWebView, didFailLoadWithError error: NSError?) {
        print("NavigatorController: didFailLoadWithError: \(error)")
    }
    
    func webViewDidFinishLoad(webView: UIWebView) {
        print("NavigatorController: webViewDidFinishLoad")

    }
    
    func webViewDidStartLoad(webView: UIWebView) {
        print("NavigatorController: webViewDidStartLoad")
    }
    
    private func loadWebViewContents() {
        self.webView.loadRequest(NSURLRequest(URL: indexURL))
    }
    
    override func viewDidAppear(animated: Bool) {
        sensorController.attachDelegate(self)
    }
    
    override func viewDidDisappear(animated: Bool) {
        sensorController.detachDelegates()
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        print("NavigatorController: viewDidLoad")
        
        locationManager.delegate = self
    }
    
    func locationManager(manager: CLLocationManager, didChangeAuthorizationStatus status: CLAuthorizationStatus) {
        print("NavigatorController: didChangeAuthorizationStatus: \(status.rawValue)")
        if status != CLAuthorizationStatus.AuthorizedWhenInUse {
            askEnableLocationServices()
        } else {
            loadWebViewContents()
        }
    }
    
    private func askEnableLocationServices() {
        if IOSVersion.SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO("8.0") && IOSVersion.SYSTEM_VERSION_LESS_THAN("9.0") {
            //I faced the bug, when on iOS 8 locationManager.requestWhenInUseAuthorization() didn't show any dialog to request authorization, even following all instructions from forums. You can try it.
            let title = "Location services".localized
            let message = "Enable Location services \"When In Use\" for Anyplace in Settings -> Privacy -> Location Services.".localized
            let controller = UIAlertController(title: title, message: message, preferredStyle: UIAlertControllerStyle.Alert)
            let action_title = "OK".localized
            let action_ok = UIAlertAction(title: action_title, style: UIAlertActionStyle.Default, handler: { (UIAlertAction) -> Void in
                exit(0)
            })
            controller.addAction(action_ok)
            presentViewController(controller, animated: true, completion: nil)
        } else {
            locationManager.requestWhenInUseAuthorization()
        }
    }
    
    private func askConnectToInternet() {
        let title = "Network".localized
        let message = "Internet connection required! Connect and click OK".localized
        let controller = UIAlertController(title: title, message: message, preferredStyle: UIAlertControllerStyle.Alert)
        let action_title = "OK".localized
        let action_ok = UIAlertAction(title: action_title, style: UIAlertActionStyle.Default, handler: { (UIAlertAction) -> Void in
        })
        controller.addAction(action_ok)
        presentViewController(controller, animated: true, completion: nil)
    }
    
    func locationManager(manager: CLLocationManager, didUpdateToLocation newLocation: CLLocation, fromLocation oldLocation: CLLocation) {
        print("NavigatorController: didUpdateToLocation")
        if approximatePosition == nil {
            approximatePosition = LatLng(lat: newLocation.coordinate.latitude, lng: newLocation.coordinate.longitude)
            locationManager.stopUpdatingLocation()
        }
    }
    
    private func requestApproximatePosition() {
        approximatePosition = nil
        if !Reachability.isConnectedToNetwork() {
            askConnectToInternet()
        } else {
            locationManager.startUpdatingLocation()
        }
    }
    
    private func requestNearbyBuildings(pos: LatLng) {
        let tag = try! apiClient.requestNearbyBuildings(token, coordinates: pos)
        apiClient.setCompletionBlock(tag) { [weak self] (success) -> Void in
            if success {
                
            }
        }
    }
    
    private func requestLocalization() {
        requestApproximatePosition()
    }
    
    private func initLocalization() {
        //Call mcl_init_localization
    }
    
    private func startLocalization() {
        //Call mcl_init_start_localizing
    }
    
    private func stopLocalization() {
        
    }
    
    func apiClient(error: NSError?, data: NSDictionary?, api: Api, tag: String) {
        if error != nil {
            switch api {
            default: break //Show error in JS
            }
        } else {
            switch api {
            case .NearbyBuildings:
                //Parse JSON
                
                break
            
            case .MilestonesByFloor:
                
                
                
                break
                
            default: break
            }
        }
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        print("NavigatorController: didReceiveMemoryWarning")
    }

}
