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

class ArchitectController: UIViewController, UIWebViewDelegate {
    
    private let TAG = "ArchitectController: "
    
    private let BUID = "buid"
    private let FLOOR_NUMBER = "floor_number"
    private let SEGUE_IDENTIFIER_LOGGER_CONTROLLER = "showLoggerController"
    
    private let indexURL: NSURL! = NSBundle.mainBundle().URLForResource("index", withExtension: "html", subdirectory: "Architect-Website")!
    
    struct RequestFromJavaScript {
        
        enum RequestType: CustomStringConvertible {
            case ShowLogger
            
            var description: String {
                switch self {
                case .ShowLogger: return "iOS: showLoggerController: "
                }
            }
            
            static func fromString(s: String) -> RequestType? {
                if s == "iOS: showLoggerController: "{
                    return RequestType.ShowLogger
                }
                return nil
            }
            
            static var key: String { return "request_type" }
        }
        
        static let data_key = "data"
        
        let type: RequestType
        let data: String?
        
        init(type: RequestType, data: String?) {
            self.type = type
            self.data = data
        }
        
        static func fromString(s: String?) -> RequestFromJavaScript? {
            guard let request = s else { return nil }
            guard let json = StringToJSON(request) else { return nil }
            guard let typeField = json[RequestType.key] else { return nil }
            guard let type = RequestType.fromString(typeField as! String) else { return nil }
                
            switch type {
            case .ShowLogger:
                let data = JSONToString(json[data_key]!)
                return RequestFromJavaScript(type: type, data: data)
            default: return nil
            }
        }
    }
    
    @IBOutlet weak var webView: UIWebView! {
        didSet{
           webView.loadRequest(NSURLRequest(URL: indexURL))
        }
    }
    
    private var buid: String!
    private var floorNum: String!
    
    func webView(webView: UIWebView, shouldStartLoadWithRequest request: NSURLRequest, navigationType: UIWebViewNavigationType) -> Bool {
        print(TAG + "shouldStartLoadWithRequest")
        
        print(request.URL?.description)
        
        if let request = RequestFromJavaScript.fromString(request.URL?.description) {
            switch request.type {
            case .ShowLogger:
                let json = StringToJSON(request.data!)!
                buid = (json[BUID]! as! String)
                floorNum = (json[FLOOR_NUMBER]! as! String)
                performSegueWithIdentifier(SEGUE_IDENTIFIER_LOGGER_CONTROLLER, sender: nil)
            }
            return false
        }
        
        return true
    }

    
    func webViewDidStartLoad(webView: UIWebView) {
        print(TAG + "webViewDidStartLoad")
    }

    func webViewDidFinishLoad(webView: UIWebView) {
        print(TAG + "webViewDidFinishLoad")
        
    }
    
    func webView(webView: UIWebView, didFailLoadWithError error: NSError?) {
        print(TAG + "didFailLoadWithError: \(error)")

    }

    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        if let loggerViewController = segue.destinationViewController as? LoggerController {
            assert(buid != nil && floorNum != nil)
            loggerViewController.buid = buid
            loggerViewController.floorNum = floorNum
        }
    }
}
