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

import Foundation

@objc enum Api: Int {
    // NAVIGATION
    case PoiDetails, NavigationRouteCoordinatesToPoi, NavigationRoutePoiToPoi
    //MAPPING
    case WorldBuildings, NearbyBuildings, AllBuildingFloors, PoisByBuilding, PoisByFloor, ConnectionsByFloor, RadioHeatMapByFloor
    //BLUEPRINTS
    case FloorPlanDownloadBase64, FloorPlanDownloadTiles
    //POSITION
    case UploadRSSLog, RadiomapByCoordinatesAndFloor, RadiomapByBuildingAndFloor
    //Paths
    case PathsByFloor, AddPaths, DeletePath
    
    case UploadMilestones, MilestonesByFloor
    
    private static let host = "http://anyplace.rayzit.com/anyplace"
    
    var url: String {
        let subdir: String
        switch(self) {
        case .PoiDetails: subdir = "/navigation/pois/id"
        case .NavigationRouteCoordinatesToPoi: subdir = "/navigation/route_xy"
        case .NavigationRoutePoiToPoi: subdir = "/navigation/route"
            
        case .WorldBuildings: subdir = "/mapping/building/all"
        case .NearbyBuildings: subdir = "/mapping/building/coordinates"
        case .AllBuildingFloors: subdir = "/mapping/floor/all"
        case .PoisByBuilding: subdir = "/mapping/pois/all_building"
        case .PoisByFloor: subdir = "/mapping/pois/all_floor"
        case .ConnectionsByFloor: subdir = "/mapping/connection/all_floor"
        case .RadioHeatMapByFloor: subdir = "/mapping/radio/heatmap_building_floor"
            
        case .FloorPlanDownloadBase64: subdir = "/floorplans64"
        case .FloorPlanDownloadTiles: subdir = "/floortiles"
            
        case .UploadRSSLog: subdir = "/position/radio_upload"
        case .RadiomapByCoordinatesAndFloor: subdir = "/position/radio_download_floor"
        case .RadiomapByBuildingAndFloor: subdir = "/position/radio_by_building_floor"
            
        case .PathsByFloor: subdir = "???" //STUB
        case .AddPaths: subdir = "???" //STUB
        case .DeletePath: subdir = "???" //STUB
            
        case .UploadMilestones: subdir = "???" //STUB
        case .MilestonesByFloor: subdir = "???" //STUB
        }
        return Api.host + subdir
    }
    
}

@objc protocol ApiClientDelegate {
    optional func apiClient( error: NSError?, data: NSDictionary?, api: Api, tag: String )
    optional func apiClient( error: NSError?, downloadPath path: String, api: Api, tag: String )
    optional func apiClient( error: NSError?, uploadPath path: String, api: Api, tag: String )
}

private struct RequestParameters {
    static let Data = "data"
    static let Token = "access_token"
    static let Pois = "pois"
    static let PoisFrom = "pois_from"
    static let PoisTo = "pois_to"
    static let BuildingID = "buid"
    static let FloorNumber = "floor_number"
    static let Floor = "floor" //Why another name???
    static let PathId = "path_id"
    static let CoordinatesLat = "coordinates_lat"
    static let CoordinatesLng = "coordinates_lon"
    static let Radiomap = "radiomap"
}

struct ApiClientParameters {
    static let Data = "data"
    static let Error = "error"
    static let Floors = "floors"
    static let FloorNumber = RequestParameters.FloorNumber
    static let PathId = RequestParameters.PathId
    static let BuildingID = RequestParameters.BuildingID
    static let BottomLeftLat = "bottom_left_lat"
    static let BottomLeftLng = "bottom_left_lng"
    static let TopRightLat = "top_right_lat"
    static let TopRightLng = "top_right_lng"
    static let Milestones = "milestones"
    static let Position = "position"
    static let Orientation = "orientation"
    static let Field = "field"
    static let Lat = "lat"
    static let Lng = "lng"
    static let w = "w"
    static let x = "x"
    static let y = "y"
    static let z = "z"
    static let Line = "line"
    static let Endpoints = "endpoints"
    static let Paths = "paths"
}


private class TaskInfo {
    let api: Api
    let params: Dictionary<String, String>
    
    var tag: String {
        return url + " " + params.description
    }
    
    var url: String {
        switch(api) {
        case .FloorPlanDownloadBase64: fallthrough
        case .FloorPlanDownloadTiles:
            let buid = params[RequestParameters.BuildingID]!
            let floorNumber = params[RequestParameters.FloorNumber]!
            return api.url + "/" + buid + "/" + floorNumber
        default:
            return api.url
        }
    }
    
    init(api: Api, params: Dictionary<String, String>) {
        self.api = api
        self.params = params
    }
}

private class DataTaskInfo: TaskInfo {
    let data = NSMutableData()
    override var tag: String {
        return "DataTask" + " " + super.tag
    }
}

private class DownloadTaskInfo: TaskInfo {
    let path: String
    init(path: String, api: Api, params: Dictionary<String, String>) {
        self.path = path
        super.init(api: api, params: params)
    }
    
    override var tag: String {
        return "DownloadTask" + " " + super.tag + " " + path
    }
}

private class UploadTaskInfo: TaskInfo {
    let path: String
    init(path: String, api: Api, params: Dictionary<String, String>) {
        self.path = path
        super.init(api: api, params: params)
    }
    
    override var tag: String {
        return "UploadTask" + " " + super.tag + " " + path
    }
}

enum ApiClientError {
    
    case CacheEmpty, CacheOutdated
    
    private var domain: String {
        return "com.dmsl.Anyplace"
    }
    
    private var code: Int {
        switch self {
        case .CacheEmpty: return 100
        case .CacheOutdated: return 101
        }
    }
    
    var error: NSError {
        return NSError(domain: domain, code: code, userInfo: nil)
    }
}

class ApiClient: NSObject, NSURLSessionTaskDelegate, NSURLSessionDataDelegate, NSURLSessionDownloadDelegate
{
    private static let MINUTE: Double = 60.0
    private static let HOUR: Double = 60.0 * MINUTE
    private static let DAY: Double = 24.0 * HOUR
    private static let WEEK: Double = 7.0 * DAY
    
    weak var delegate: ApiClientDelegate?
    
    private var completionBlocks = [String: Any]()
    func setCompletionBlock(tag: String, completionBlock: (success: Bool) -> Void ) {
        if tasks[tag] != nil {
            completionBlocks[tag] = completionBlock
        }
    }
    
    private var tasks = [String: NSURLSessionTask]()
    private var infos = [NSURLSessionTask: TaskInfo]()
    
    var timeout: NSTimeInterval = 60 {
        didSet {
            timeout = max(0, timeout)
        }
    }
    
    var useCaching: Bool = true
    private static let DEFAULT_CACHE_UPDATE_INTERVAL: Double = WEEK
    var cacheUpdateInterval: Double = ApiClient.DEFAULT_CACHE_UPDATE_INTERVAL {
        didSet {
            if cacheUpdateInterval < 0 {
                cacheUpdateInterval = oldValue
            }
        }
    }
    
    required init( delegate: ApiClientDelegate ) {
        self.delegate = delegate
    }
    
    override init() {}
    
    
    func URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didBecomeDownloadTask downloadTask: NSURLSessionDownloadTask) {
        print("didBecomeDownloadTask")
    }
    
    func URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveResponse response: NSURLResponse, completionHandler: (NSURLSessionResponseDisposition) -> Void) {
        completionHandler(NSURLSessionResponseDisposition.Allow)
        print("didReceiveResponse")
    }
    
    func URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, didReceiveData data: NSData) {
        print("didReceiveData")
        (self.infos[dataTask]! as! DataTaskInfo).data.appendData(data)
    }
    
    func URLSession(session: NSURLSession, task: NSURLSessionTask, didCompleteWithError error: NSError?) {
        print("didCompleteWithError")
        
        let info = infos[task]!
        let api = info.api
        let tag = info.tag
        
        popTask(task)
        
        if task is NSURLSessionDataTask {
            print("Complete: Data Task: success = \(error == nil)")
            
            let data = NSString( data: (info as! DataTaskInfo).data, encoding: NSUTF8StringEncoding)!
            let json = StringToJSON(data)
            
            delegate?.apiClient?(error, data: json, api: api, tag: tag)
        } else if task is NSURLSessionDownloadTask {
            print("Complete: Download Task: success = \(error == nil)")
            
            let path = (info as! DownloadTaskInfo).path
            switch(api)
            {
            case .FloorPlanDownloadBase64:
                delegate?.apiClient?(error, downloadPath: path, api: api, tag: tag)
            case .FloorPlanDownloadTiles:
                if error != nil {
                    delegate?.apiClient?(error, downloadPath: path, api: api, tag: tag)
                } else {
                    let currentQueue = NSOperationQueue.currentQueue() ?? NSOperationQueue.mainQueue()
                    dispatch_async(dispatch_get_global_queue(QOS_CLASS_DEFAULT, 0)) {

                        //Implement depending on ZIP lib
                        let error: NSError?
                        if !FileHelper.unzip(path) {
                            error = NSError(domain: "FileSystem", code: 123, userInfo: [:])
                        } else {
                            error = nil
                        }
                        currentQueue.addOperationWithBlock { [weak self] in
                            self?.delegate?.apiClient?(error, downloadPath: path, api: api, tag: tag)
                        }
                    }
                }
            default: break
            }
            
        } else if task is NSURLSessionUploadTask {
            print("Complete: Upload Task: success = \(error == nil)")
            
            let path = (info as! UploadTaskInfo).path
            delegate?.apiClient?(error, uploadPath: path, api: api, tag: tag)
        }
        
        if let completionBlock = completionBlocks[tag] {
            (completionBlock as! (success: Bool) -> Void)(success: error == nil)
            completionBlocks.removeValueForKey(tag)
        }
    }
    
    
    @objc func URLSession(session: NSURLSession, downloadTask: NSURLSessionDownloadTask, didFinishDownloadingToURL location: NSURL) {
        print("didFinishDownloadingToURL")
        
        //Probably should do asycnhronously
        let tmpPath = location.path!
        let path = (infos[downloadTask]! as! DownloadTaskInfo).path
        print(tmpPath + " -> " + path)
        print("didFinishDownloadingToURL: moved \(FileHelper.moveFile(tmpPath, newPath: path))")
        print("didFinishDownloadingToURL: exists \(NSFileManager.defaultManager().fileExistsAtPath(path))")
        print("didFinishDownloadingToURL: size tmp = \(try! FileHelper.sizeOfFile(tmpPath))")
        print("didFinishDownloadingToURL: size permanent = \(try! FileHelper.sizeOfFile(path))")
    }
    
    func URLSession(session: NSURLSession, downloadTask: NSURLSessionDownloadTask, didWriteData bytesWritten: Int64, totalBytesWritten: Int64, totalBytesExpectedToWrite: Int64) {
        print("didWriteData: \(totalBytesWritten/1024) of \(totalBytesExpectedToWrite/1024)" )
    }
    
    func URLSession(session: NSURLSession, dataTask: NSURLSessionDataTask, willCacheResponse proposedResponse: NSCachedURLResponse, completionHandler: (NSCachedURLResponse?) -> Void) {
        
    }
    
    func isTaskActive(tag tag: String) -> Bool {
        return tasks[tag] != nil
    }
    
    func cancel(tag: String) {
        tasks[tag]?.cancel()
    }
    
    func cancelAll() {
        for (_, task) in tasks {
            task.cancel()
        }
    }
    
    private func pushTask(task: NSURLSessionTask, info: TaskInfo) {
        let tag = info.tag
        tasks[tag] = task
        infos[task] = info
    }
    
    private func popTask(task: NSURLSessionTask) {
        let info = infos[task]!
        let tag = info.tag
        infos.removeValueForKey(task)
        tasks.removeValueForKey(tag)
    }
    
    private func createTask(info: TaskInfo, cache: Bool) throws -> NSURLSessionTask! {
        
        let url = info.url
        let params = info.params
        let request = NSMutableURLRequest(URL: NSURL(string: url)!)
        let config = NSURLSessionConfiguration.defaultSessionConfiguration()
        config.timeoutIntervalForRequest = timeout
        config.timeoutIntervalForResource = timeout
        let session = NSURLSession(configuration: config, delegate: self, delegateQueue: NSOperationQueue.currentQueue())
        request.HTTPMethod = "POST"
        
        switch info
        {
        case _ as DataTaskInfo :
            print("createTask: DataTaskInfo")
            
            request.HTTPBody = try NSJSONSerialization.dataWithJSONObject(params, options: [])
            request.addValue("application/json", forHTTPHeaderField: "Content-Type")
            request.addValue("application/json", forHTTPHeaderField: "Accept")
            request.cachePolicy = cache ? NSURLRequestCachePolicy.ReturnCacheDataElseLoad : NSURLRequestCachePolicy.ReloadIgnoringCacheData
            
            return session.dataTaskWithRequest(request)
            
        case let downloadTaskInfo as DownloadTaskInfo:
            print("createTask: DownloadTaskInfo")
            
            if cache {
                let path = downloadTaskInfo.path
                if FileHelper.isFileExist(path) {
                    let time = NSDate().timeIntervalSince1970
                    let timeModified = try! FileHelper.dateModified(path)!.timeIntervalSince1970
                    let elapsed = time - timeModified
                    if elapsed < cacheUpdateInterval {
                        delegate?.apiClient?(nil, downloadPath: path, api: downloadTaskInfo.api, tag: info.tag)
                    } else {
                         delegate?.apiClient?(ApiClientError.CacheOutdated.error, downloadPath: path, api: downloadTaskInfo.api, tag: downloadTaskInfo.tag)
                    }
                } else {
                    delegate?.apiClient?(ApiClientError.CacheEmpty.error, downloadPath: path, api: downloadTaskInfo.api, tag: downloadTaskInfo.tag)
                }
                return nil
            }
            
            
            //NEED TO CHANGE API! (Set buid and floor_num as http body arguments)
            switch info.api
            {
            case .FloorPlanDownloadBase64:
                request.HTTPBody = try NSJSONSerialization.dataWithJSONObject(params, options: [])
                request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                request.addValue("application/octet-stream", forHTTPHeaderField: "Accept")
                
            default: break
            }
            return session.downloadTaskWithRequest(request)
            
        case let uploadTaskInfo as UploadTaskInfo:
            print("createTask: UploadTaskInfo")
            
            switch info.api
            {
            case .UploadRSSLog:
                request.HTTPBody = try NSJSONSerialization.dataWithJSONObject(params, options: [])
                request.addValue("application/json", forHTTPHeaderField: "Content-Type")
            default: break
            }
            
            let fileURL = NSURL.fileURLWithPath(uploadTaskInfo.path)
            return session.uploadTaskWithRequest(request, fromFile: fileURL)
            
        default:
            return nil
        }
    }
    
    
    private func request(info: TaskInfo, cache: Bool) throws -> String {
        
        if !isTaskActive(tag: info.tag) {
            print("request: task is new: \(info.tag)")
            let task = try createTask(info, cache: cache)
            if task == nil {
                return info.tag
            }
            pushTask(task, info: info)
            task.resume()
        } else {
            print("request: task was already launched: \(info.tag)")
        }
        return info.tag
    }
    
    private func requestFetchJSON(params: Dictionary<String, String>, api: Api) throws -> String {
        let info = DataTaskInfo(api: api, params: params)
        return try request(info, cache: useCaching)
    }
    
    private func requestDownloadFile(path: String, params: Dictionary<String, String>, api: Api) throws -> String {
        let info = DownloadTaskInfo(path: path, api: api, params: params)
        return try request(info, cache: useCaching)
    }
    
    private func requestUploadFile(path: String, params: Dictionary<String, String>, api: Api) throws -> String {
        let info = UploadTaskInfo(path: path, api: api, params: params)
        return try request(info, cache: false)
    }
    
    func requestPoiDetails(token: String, puid: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.Pois] = puid
        return try requestFetchJSON(params, api: Api.PoiDetails)
    }
    
    func requestNavigationRouteCoordinatesToPoi(token: String, poisTo: String, buid: String! = nil, floorNumber: String, coordinates: LatLng) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.PoisTo] = poisTo
        if buid != nil {
            params[RequestParameters.BuildingID] = buid
        }
        params[RequestParameters.FloorNumber] = floorNumber
        params[RequestParameters.CoordinatesLat] = String(coordinates.lat)
        params[RequestParameters.CoordinatesLng] = String(coordinates.lng)
        return try requestFetchJSON(params, api: Api.NavigationRouteCoordinatesToPoi)
    }
    
    func requestNavigationRoutePoiToPoi(token: String, poisFrom: String, poisTo: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.PoisFrom] = poisFrom
        params[RequestParameters.PoisTo] = poisTo
        return try requestFetchJSON(params, api: Api.NavigationRoutePoiToPoi)
    }
    
    func requestWorldBuildings(token: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        return try requestFetchJSON(params, api: Api.WorldBuildings)
    }
    
    func requestNearbyBuildings(token: String, coordinates: LatLng) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.CoordinatesLat] = String(coordinates.lat)
        params[RequestParameters.CoordinatesLng] = String(coordinates.lng)
        return try requestFetchJSON(params, api: Api.NearbyBuildings)
    }
    
    func requestAllBuildingFloors(token: String, buid: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        return try requestFetchJSON(params, api: Api.AllBuildingFloors)
    }
    
    func requestPoisByBuilding(token: String, buid: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        return try requestFetchJSON(params, api: Api.PoisByBuilding)
    }
    
    func requestPoisByFloor(token: String, buid: String, floorNumber: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.FloorNumber] = floorNumber
        return try requestFetchJSON(params, api: Api.PoisByFloor)
    }
    
    func requestConnectionsByFloor(token: String, buid: String, floorNumber: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.FloorNumber] = floorNumber
        return try requestFetchJSON(params, api: Api.ConnectionsByFloor)
    }
    
    func requestRadioHeatMapByFloor(token: String, buid: String, floorNumber: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.Floor] = floorNumber
        return try requestFetchJSON(params, api: Api.RadioHeatMapByFloor)
    }
    
    func requestFloorPlanDownloadBase64(token: String, buid: String, floorNumber: String, path: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.FloorNumber] = floorNumber
        return try requestDownloadFile(path, params: params, api: Api.FloorPlanDownloadBase64)
    }
    
    func requestFloorPlanDownloadTiles(token: String, buid: String, floorNumber: String, path: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.FloorNumber] = floorNumber
        return try requestDownloadFile(path, params: params, api: Api.FloorPlanDownloadTiles)
    }
    
    func requestUploadRSSLog(token: String, path: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.Radiomap] = path
        return try requestUploadFile(path, params: params, api: Api.UploadRSSLog)
    }
    
    func requestRadiomapByCoordinatesAndFloor(token: String, coordinates: LatLng, floorNumber: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.CoordinatesLat] = String(coordinates.lat)
        params[RequestParameters.CoordinatesLng] = String(coordinates.lng)
        params[RequestParameters.FloorNumber] = floorNumber
        return try requestFetchJSON(params, api: Api.RadiomapByCoordinatesAndFloor)
    }
    
    func requestRadiomapByBuildingAndFloor(token: String, buid: String, floor: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.Floor] = floor
        return try requestFetchJSON(params, api: Api.RadiomapByBuildingAndFloor)
    }
    
    func requestAddPaths(token: String, buid: String, floor: String, paths: [ApiClientDataTypes.Path]) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.Floor] = floor
        params[RequestParameters.Data] = JSONToString(ApiClientDataTypes.constructPathsUploadRequestData(paths))
        return try requestFetchJSON(params, api: Api.AddPaths)
    }
    
    func requestDeletePath(token: String, buid: String, floor: String, pathId: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.Floor] = floor
        params[RequestParameters.PathId] = pathId
        return try requestFetchJSON(params, api: Api.DeletePath)
    }
    
    func requestPathsByFloor(token: String, buid: String, floor: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.Floor] = floor
        return try requestFetchJSON(params, api: Api.PathsByFloor)
    }
    
    func requestUploadMilestones(token: String, buid: String, floor: String, path: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.Floor] = floor
        return try requestUploadFile(path, params: params, api: Api.UploadMilestones)
    }
    
    func requestMilestonesByFloor(token: String, buid: String, floor: String) throws -> String {
        var params = [String: String]()
        params[RequestParameters.Token] = token
        params[RequestParameters.BuildingID] = buid
        params[RequestParameters.Floor] = floor
        return try requestFetchJSON(params, api: Api.MilestonesByFloor)
    }
    
    
}
