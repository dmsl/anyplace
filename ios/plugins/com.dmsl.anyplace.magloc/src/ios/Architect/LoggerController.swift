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


class LoggerController: UIViewController, UIAlertViewDelegate, UIScrollViewDelegate, ApiClientDelegate, SensorControllerDelegate, PathActionHistoryDelegate {
    private let TAG = "LoggerController: "
    
    private enum LoggerState {
        case Idle, Adding, Selected, Logging
    }
    
    private static let API_TESTER_TOKEN = "api_tester"
    private let MAP_SUBDIR = "map"
    private let LOGS_SUBDIR = "logs"
    private let LOGGING_UPDATE_INTERVAL: Double = 0.5
    
    var buid: String! = nil
    var floorNum: String! = nil
    var token: String! = API_TESTER_TOKEN
    private let floorMap = FloorMap()
    private let apiClient = ApiClient()
    private let sensorController = try! SensorController(options: .OrientationCalibratedReferenceTrueNorth)!
    private var loggingMagneticData = [MagneticData]()
    private var loggingAttitudeData = [AttitudeData]()
    private var pathActionHistory = PathActionHistory() {
        didSet {
            pathActionHistory.delegate = self
        }
    }
    
    @IBOutlet weak var floorMapView: FloorMapView!
    private var image: UIImage! = nil {
        didSet {
            floorMapView.image = image
        }
    }
    private var mapImage: UIImage! = nil {
        didSet {
            image = mapImage
            floorMapView.contentOffset = CGPoint(x: 0, y: 0)
            floorMapView.zoomScale = floorMapView.minimumZoomScale
        }
    }
    private var pathWidth: CGFloat {
        return min(floorMapView.frame.size.width/100, floorMapView.frame.size.height/100)/floorMapView.zoomScale
    }
    private var vertexSize: CGFloat {
        return pathWidth * 2.0
    }
    
    
    private var state: LoggerState = .Idle {
        didSet {
            switch state {
            case .Idle:
                stopLoggingUpdates()
                colorChangeTimer?.invalidate()
                colorChangeTimer = nil
                
                addStopDeleteButton.setTitle("Add", forState: .Normal)
                addStopDeleteButton.enabled = true
                
                loggingButton.setTitle("Record", forState: .Normal)
                loggingButton.enabled = false
                
                uploadButton.setTitle("Synchronize", forState: .Normal)
                uploadButton.enabled = true
                
            case .Adding:
                startVertex = nil
                startedPathFromExistingVertex = false
                newPath = nil
                
                addStopDeleteButton.setTitle("Cancel", forState: .Normal)
                addStopDeleteButton.enabled = true
                
                loggingButton.setTitle("Record", forState: .Normal)
                loggingButton.enabled = false
            
                uploadButton.setTitle("Synchronize", forState: .Normal)
                uploadButton.enabled = false
                
            case .Selected:
                selectedPathId = nil
                
                addStopDeleteButton.setTitle("Delete", forState: .Normal)
                addStopDeleteButton.enabled = true
            
                loggingButton.setTitle("Record", forState: .Normal)
                loggingButton.enabled = true
            
                uploadButton.setTitle("Synchronize", forState: .Normal)
                uploadButton.enabled = false
            case .Logging:
                addStopDeleteButton.setTitle("Cancel", forState: .Normal)
                addStopDeleteButton.enabled = true
                
                loggingButton.setTitle("Finish", forState: .Normal)
                loggingButton.enabled = true
                
                uploadButton.setTitle("Synchronize", forState: .Normal)
                uploadButton.enabled = false
                
                colorChangeTimer = NSTimer(timeInterval: colorChangeInterval, target: self, selector: "onColorChangeTimerFire:", userInfo: nil, repeats: true)
                
                startLoggingUpdates()
            }
        }
    }
    private let colorChangeInterval: NSTimeInterval = 1.0
    private var colorChangeTimer: NSTimer? = nil
    private func onColorChangeTimerFire() {
        let oldColor = addStopDeleteButton.titleLabel!.textColor
        let newColor: UIColor
        if oldColor == UIColor.blackColor() {
            newColor = UIColor.redColor()
        } else {
            newColor = UIColor.blackColor()
        }
        loggingButton.setTitleColor(newColor, forState: .Normal)
    }
    
    @IBOutlet weak var spinner: UIActivityIndicatorView!
    @IBOutlet weak var addStopDeleteButton: UIButton!
    @IBOutlet weak var uploadButton: UIButton!
    @IBOutlet weak var loggingButton: UIButton!
 
    
    
    @IBAction func onAddStopDeleteButtonClick() {
        switch state {
        case .Idle:
            state = .Adding
        case .Adding:
            if newPath != nil {
                floorMap.removePath(newPath.id)
            } else
            if startVertex != nil && !startedPathFromExistingVertex {
                floorMap.removeVertex(startVertex)
            }
            updateImage()
            state = .Idle
        case .Selected:
            floorMap.removePath(selectedPathId)
            pathActionHistory.addAction(.Delete, pathId: selectedPathId)
            updateImage()
            state = .Idle
        case .Logging:
            state = .Idle
            loggingMagneticData.removeAll(keepCapacity: true)
            loggingAttitudeData.removeAll(keepCapacity: true)
        }
    }
    
    private func pathActionHistory(action: PathActionHistory.PathAction) {
        if pathActionHistory.addedPathIds().count != 0 {
            uploadButton.setTitleColor(UIColor.greenColor(), forState: .Normal)
            loggingButton.enabled = false
        } else {
            uploadButton.setTitleColor(UIColor.blackColor(), forState: .Normal)
            loggingButton.enabled = true
        }
    }
    
    
    @IBAction func onUploadButtonClick() {
//        uploadFloorPlanChanges()
    }
    
    @IBAction func onLoggingButtonClick() {
        switch state {
        case .Selected:
            state = .Logging
        case .Logging:
            saveLoggingData(newPath)
            pathActionHistory.addAction(.Log, pathId: newPath.id)
            state = .Idle
            default: break
        }
    }
    
    override func viewDidLoad() {
        let tapRecognizer = UITapGestureRecognizer(target: self, action: "onTap:")
        tapRecognizer.numberOfTapsRequired = 1
        tapRecognizer.numberOfTouchesRequired = 1
        floorMapView.addGestureRecognizer(tapRecognizer)
        
        assert(buid != nil && floorNum != nil)
        loadFloorPlan()
    }
    
    override func viewDidAppear(animated: Bool) {
        sensorController.attachDelegate(self)
    }
    
    override func viewDidDisappear(animated: Bool) {
        switch state {
        case .Logging:
            floorMap.removePath(newPath.id)
            state = .Idle
        default: break
        }
        sensorController.detachDelegates()
        apiClient.cancelAll()
    }
    
    private func uploadFloorPlanChanges() {
        spinner.startAnimating()
        requestFloorPathsAdd(token, buid: buid, floorNum: floorNum)
    }
    
    private func requestFloorPathsAdd(token: String, buid: String, floorNum: String) {
        let addedPathIds = pathActionHistory.addedPathIds()
        if addedPathIds.count == 0 {
            requestFloorPathsDelete(token, buid: buid, floorNum: floorNum)
        }
        
        var paths = Array<ApiClientDataTypes.Path>()
        for addedPathId in addedPathIds {
            let addedPath = floorMap.pathById(addedPathId)!
            let p1 = coordinatesImageToEarth(addedPath.v1)
            let p2 = coordinatesImageToEarth(addedPath.v2)
            paths.append(ApiClientDataTypes.Path(p1: p1, p2: p2, pathId: addedPathId))
        }
        let tag = try! apiClient.requestAddPaths(token, buid: buid, floor: floorNum, paths: paths)
        apiClient.setCompletionBlock(tag) { [weak self] (success) -> Void in
            if success {
                self?.requestFloorPathsDelete(token, buid: buid, floorNum: floorNum)
            } else {
                self?.onOperationFail()
            }
        }

    }
    
    private func requestFloorPathsDelete(token: String, buid: String, floorNum: String, pathIds: [String]? = nil) {
        
        var pathIdsToDelete = pathIds ?? pathActionHistory.deletedPathIds()
        
        let completionBlock: (success: Bool) -> Void = { [weak self] (success) -> Void in
            if success {
                if pathIdsToDelete.count != 0 {
                    self?.requestFloorPathsDelete(token, buid: buid, floorNum: floorNum, pathIds: pathIdsToDelete)
                }
                else {
                    self?.uploadFloorLogs(self!.token, buid: self!.buid, floorNum: self!.floorNum)
                }
            } else {
                self?.onOperationFail()
            }
        }
        
        if pathIds == nil && pathIdsToDelete.count == 0 {
            completionBlock(success: true)
            return
        }
        
        
        if let pathIdToDelete = pathIdsToDelete.popLast() {
            let tag = try! apiClient.requestDeletePath(token, buid: buid, floor: floorNum, pathId: pathIdToDelete)
            apiClient.setCompletionBlock(tag, completionBlock: completionBlock)
        } else {
            completionBlock(success: true)
        }
    }
    
    
    private func uploadFloorLogs(token: String, buid: String, floorNum: String, files: [String]? = nil) {
        var loggedFiles: [String]
        if files == nil {
            loggedFiles = [String]()
            let dir = FileHelper.getApplicationTemporaryDirPath(LOGS_SUBDIR)
            if FileHelper.isDirectory(dir) {
                let loggedPathIds = pathActionHistory.loggedPathIds()
                for loggedPathId in loggedPathIds {
                    let filename = buid + "_" + floorNum + "_" + loggedPathId
                    let dir = LOGS_SUBDIR
                    let path = FileHelper.getApplicationTemporaryFilePath(filename, dir: dir)
                    if FileHelper.isFileExist(path) {
                        loggedFiles.append(path)
                    }
                }
            }
        } else {
            loggedFiles = files!
        }
        
        let completionBlock: (success: Bool) -> Void = { [weak self] (success) -> Void in
            if success {
                if loggedFiles.count != 0 {
                    self?.uploadFloorLogs(token, buid: buid, floorNum: floorNum, files: loggedFiles)
                }
                else {
                    self?.onOperationComplete()
                }
            } else {
                self?.onOperationFail()
            }
        }
        if let loggedFileToUpload = loggedFiles.popLast() {
            let tag = try! apiClient.requestUploadMilestones(token, buid: buid, floor: floorNum, path: loggedFileToUpload)
            apiClient.setCompletionBlock(tag, completionBlock: completionBlock)
        } else {
            completionBlock(success: true)
        }
    }
    
    private func loadFloorPlan() {
        spinner.startAnimating()
        apiClient.useCaching = true
        let tag = try! apiClient.requestAllBuildingFloors(token, buid: buid)
        apiClient.setCompletionBlock(tag) { [weak self] (success) -> Void in
            if success {
                self?.loadFloorPaths(self!.token, buid: self!.buid, floorNum: self!.floorNum)
            } else {
                self?.onFloorPlanLoadFail()
            }
        }
    }
    
    private func loadFloorPaths(token: String, buid: String, floorNum: String) {
        let tag = try! apiClient.requestPathsByFloor(token, buid: buid, floor: floorNum)
        apiClient.setCompletionBlock(tag) { [weak self] (success) -> Void in
            if success {
                self?.loadFloorImage(self!.token, buid: self!.buid, floorNum: self!.floorNum)
            } else {
                self?.onFloorPlanLoadFail()
            }
        }
    }
    
    private func loadFloorImage(token: String, buid: String, floorNum: String) {
        let filename = buid + "/" + floorNum
        let dir = MAP_SUBDIR
        let path = FileHelper.getApplicationSupportFilePath(filename, dir: dir)
        let tag = try! apiClient.requestFloorPlanDownloadBase64(token, buid: buid, floorNumber: floorNum, path: path)
        apiClient.setCompletionBlock(tag) { [weak self] (success) -> Void in
            if !success {
                self?.onFloorPlanLoadFail()
            }
        }
    }
    
    private func navigateBack() {
        navigationController!.popViewControllerAnimated(true)
    }
    
    private var floorBottomLeftLatLng: LatLng! = nil
    private var floorTopRightLatLng: LatLng! = nil
    private var realFloorWidth: Double {
        let ll1 = floorBottomLeftLatLng
        let ll2 = LatLng(lat: ll1.lat, lng: floorTopRightLatLng.lng)
        return LatLng.dist(ll1, p2: ll2)
    }
    private var realFloorHeight: Double {
        let ll1 = floorBottomLeftLatLng
        let ll2 = LatLng(lat: floorTopRightLatLng.lat, lng: ll1.lng)
        return LatLng.dist(ll1, p2: ll2)
    }
    
    func onApiClientDataComplete(error: NSError?, data: NSDictionary?, api: Api, tag: String) {
        if error != nil {

        } else {
            switch api {
            case .AllBuildingFloors:
                let floors = data!.valueForKey(ApiClientParameters.Floors) as! NSArray
                let floor = floors[Int(floorNum)!] as! NSDictionary
                let bottom_left_lat = Double(floor.valueForKey(ApiClientParameters.BottomLeftLat) as! String )!
                let bottom_left_lng = Double(floor.valueForKey(ApiClientParameters.BottomLeftLng) as! String )!
                let top_right_lat = Double(floor.valueForKey(ApiClientParameters.TopRightLat) as! String )!
                let top_right_lng = Double(floor.valueForKey(ApiClientParameters.TopRightLng) as! String )!
                floorBottomLeftLatLng = LatLng(lat: bottom_left_lat, lng: bottom_left_lng)
                floorTopRightLatLng = LatLng(lat: top_right_lat, lng: top_right_lng)

            case .PathsByFloor:
                let responseData = data![ApiClientParameters.Data]! as! [[String: AnyObject]]
                let paths = ApiClientDataTypes.parsePathsDownloadResponseData(responseData)
                floorMap.clear()
                for path in paths {
                    let p1 = coordinatesEarthToImage(path.p1)
                    let p2 = coordinatesEarthToImage(path.p2)
                    floorMap.addPath(Path(v1: p1, v2: p2, id: path.pathId))
                }
                
            case .AddPaths:
                let responseData = data![ApiClientParameters.Data]! as! [[String: AnyObject]]
                let addedPathRealIds = ApiClientDataTypes.parsePathsUploadResponseData(responseData)
                let addedPathTmpIds = pathActionHistory.addedPathIds()
                
                for (tmpPathId, realPathId) in Zip2Sequence(addedPathTmpIds, addedPathRealIds) {
                    var path = floorMap.pathById(tmpPathId)!
                    path.id = realPathId
                    floorMap.removePath(tmpPathId)
                    floorMap.addPath(path)
                }
                
            case .UploadMilestones:
                let dir = LOGS_SUBDIR
                let path = FileHelper.getApplicationTemporaryDirPath(dir)
                if let files = try! FileHelper.listChildren(path) {
                    for file in files {
                        FileHelper.deleteFile(file)
                    }
                }
            
            default: break
            }
        }
    }
    func onApiClientDownloadComplete(error: NSError?, downloadPath: String, api: Api, tag: String) {
        if error != nil {
            onFloorPlanLoadFail()
        } else {
            onFloorPlanLoadSuccess(downloadPath)
        }
    }
    
    private enum AlertViewTag: Int {
        case OperationFail, FloorPlanLoadFail
    }
    
    private func onOperationFail() {
        spinner.stopAnimating()
        let alertTitle = "Architect"
        let alertMessage = "Failed to perform operation."
        let alertActionTitle = "OK"
        
        showDialog(alertTitle, alertMessage: alertMessage, alertActionTitle: alertActionTitle, tag: AlertViewTag.OperationFail.rawValue)
    }
    
    private func onOperationComplete() {
        spinner.stopAnimating()
    }

    
    private func onFloorPlanLoadFail() {
        spinner.stopAnimating()
        let alertTitle = "Architect"
        let alertMessage = "Failed to load floor plan."
        let alertActionTitle = "OK"
        
        showDialog(alertTitle, alertMessage: alertMessage, alertActionTitle: alertActionTitle, tag: AlertViewTag.FloorPlanLoadFail.rawValue)
    }
    
    private func onFloorPlanLoadSuccess(path: String) {
        mapImage = FileHelper.imageFromBase64File(path)
        updateImage()
    }
    
    private func showDialog(alertTitle: String, alertMessage: String, alertActionTitle: String, tag: Int) {
        if #available(iOS 8, *) {
            let alertController = UIAlertController(title: alertTitle, message: alertMessage, preferredStyle: UIAlertControllerStyle.Alert)
            let actionOk = UIAlertAction(title: alertActionTitle, style: UIAlertActionStyle.Default, handler: { (UIAlertAction) -> Void in
                self.navigateBack()
            })
            alertController.addAction(actionOk)
            presentViewController(alertController, animated: true, completion: nil)
        } else {
            let alertDialog = UIAlertView(title: alertTitle, message: alertMessage, delegate: self, cancelButtonTitle: alertActionTitle)
            alertDialog.tag = tag
            alertDialog.show()
        }
    }

    func alertView(alertView: UIAlertView, clickedButtonAtIndex buttonIndex: Int) {
        assert(buttonIndex == 0)
        switch alertView.tag {
        case AlertViewTag.FloorPlanLoadFail.rawValue:
            self.navigateBack()
        default: break
        }
    }
    
    
    private func updateImage() {
        self.image = floorMap.drawMap(mapImage, vertexSize: vertexSize, pathWidth: pathWidth)
    }
    
    private var startVertex: Vertex! = nil
    private var startedPathFromExistingVertex: Bool = false
    private var newPath: Path! = nil
    private var selectedPathId: String! = nil {
        didSet {
            if oldValue != nil {
                let color = FloorMap.DEFAULT_PATH_COLOR
                let path = floorMap.pathById(oldValue)!
                floorMap.setPathColor(oldValue, color: color)
                floorMap.setVertexColor(path.v1, color: color)
                floorMap.setVertexColor(path.v2, color: color)
            }
            if selectedPathId != nil {
                let color = UIColor.redColor()
                let path = floorMap.pathById(selectedPathId)!
                floorMap.setPathColor(selectedPathId, color: color)
                floorMap.setVertexColor(path.v1, color: color)
                floorMap.setVertexColor(path.v2, color: color)
            }
        }
    }
    
    private func onTap(recognizer: UITapGestureRecognizer) {
        if mapImage == nil {
            return
        }
        if state == .Logging {
            return
        }
        
        let position = floorMapView.locationOnMap(recognizer.locationInView(floorMapView))
        let vertex = floorMap.vertexAtPoint(position, maxDist: vertexSize / 2.0) ?? Vertex( x: Double(position.x), y: Double(position.y) )
        
        switch state {
        case .Idle:
            if let pathId = floorMap.pathIdAtPoint(position, maxDist: pathWidth / 2.0) {
                selectedPathId = pathId
                state = .Selected
            }
        case .Adding:
            if startVertex == nil {
                startVertex = vertex
                if floorMap.vertexExists(vertex) {
                    startedPathFromExistingVertex = true
                } else {
                    startedPathFromExistingVertex = false
                    floorMap.addVertex(vertex)
                }
            } else {
                if startVertex == vertex {
                    return
                }
                let v1 = startVertex
                let v2 = vertex
                let tmpId = v1.description + "_" + v2.description
                newPath = Path(v1: v1, v2: v2, id: tmpId)
                floorMap.addPath(newPath)
                pathActionHistory.addAction(.Add, pathId: tmpId)
                state = .Idle
            }
        case .Selected:
            if let pathId = floorMap.pathIdAtPoint(position, maxDist: pathWidth) {
                selectedPathId = pathId
            } else {
                selectedPathId = nil
                state = .Idle
            }
        default: return
        }
        updateImage()
    }
    
    func scrollViewDidEndZooming(scrollView: UIScrollView, withView view: UIView?, atScale scale: CGFloat) {
        updateImage()
    }

    private func startLoggingUpdates() {
        loggingMagneticData.removeAll(keepCapacity: true)
        loggingAttitudeData.removeAll(keepCapacity: true)
        sensorController.setUpdateInterval(LOGGING_UPDATE_INTERVAL)
        try! sensorController.start()
    }
    
    private func stopLoggingUpdates() {
        if sensorController.active {
            sensorController.stop()
        }
    }
    
    func handleUpdate(updateError error: NSError!, magneticData data: MagneticData!) {
        assert(error == nil && data != nil)
        if data.accuracy != Accuracy.Uncalibrated {
            self.loggingMagneticData.append(data)
        }
    }
    
    func handleUpdate(updateError error: NSError!, attitudeData data: AttitudeData!) {
        assert(error == nil && data != nil)
        if data.accuracy != Accuracy.Uncalibrated {
            self.loggingAttitudeData.append(data)
        }
    }
    
    
    
    private func coordinatesImageToEarth(p: Point) -> LatLng {
        let x = p.x
        let y = p.y
        
        let real_x =  x / Double(image.size.width) * realFloorWidth
        let real_y =  ( 1.0 - y / Double(image.size.height) ) * realFloorHeight
        
        let R = LatLng.radiusOfEarthAtLat(floorBottomLeftLatLng.lat)
        let d_lat = real_y / R
        let d_lng = real_x / R
        
        let lat = floorBottomLeftLatLng.lat + d_lat
        let lng = floorBottomLeftLatLng.lng + d_lng
        
        return LatLng(lat: lat, lng: lng)
    }
    
    private func coordinatesEarthToImage(ll: LatLng) -> Point {
        let R = LatLng.radiusOfEarthAtLat(floorBottomLeftLatLng.lat)
        let d_lat = ll.lat - floorBottomLeftLatLng.lat
        let d_lng = ll.lng - floorBottomLeftLatLng.lng
        let real_x = R * d_lng
        let real_y = R * d_lat
        let x = real_x / realFloorWidth * Double(image.size.width)
        let y = (1.0 - real_y / realFloorHeight) * Double(image.size.height)
        return Point(x: x, y: y)
    }
    
    private func saveLoggingData(path: Path) {
        assert(loggingMagneticData.count > 0 && loggingAttitudeData.count == loggingMagneticData.count)
        
        print("loggingDataToMilestones: count = \(loggingMagneticData.count)")
        
        let v1 = path.v1
        let v2 = path.v2
        
        let timeStart = loggingMagneticData.first!.timestamp
        let timeEnd = loggingMagneticData.last!.timestamp
        
        let translation = v2 - v1
        let period = timeEnd - timeStart
        let velocityAvg = (translation / period).norm
        let direction = translation.direction
        
        var milestones = Array<ApiClientDataTypes.Milestone>()
        
        for (m_datum, a_datum) in Zip2Sequence(loggingMagneticData, loggingAttitudeData) {
            let dt = m_datum.timestamp - timeStart
            //            let min = SensorController.minField
            //            let max = SensorController.maxField
            let field = m_datum.field
            //            let val = (datum.field_magnitude - min)/(max - min)
            let x = v1.x + direction.x*velocityAvg*dt
            let y = v1.y + direction.y*velocityAvg*dt

            let pos = coordinatesImageToEarth(Point(x: x, y: y))
            
            let q_ref = Quaternion(w: a_datum.attitude.quaternion.w, x: a_datum.attitude.quaternion.x, y: a_datum.attitude.quaternion.y, z: a_datum.attitude.quaternion.z)
            
            let milestone = ApiClientDataTypes.Milestone(pos: pos, orientation: q_ref, field: field)
            milestones.append(milestone)
        }
        let data = JSONToString(ApiClientDataTypes.constructMilestonesUploadRequestData([path.id : milestones]))!
        
        let filename = buid + "_" + floorNum + "_" + path.id
        let dir = LOGS_SUBDIR
        let path = FileHelper.getApplicationTemporaryFilePath(filename, dir: dir)
        
        assert(FileHelper.writeFile(path, data: data))
    }
    
}


private protocol PathActionHistoryDelegate {
    func pathActionHistory(action: PathActionHistory.PathAction)
}

private class PathActionHistory {
    enum PathAction {
        case Add, Delete, Log
    }
    
    var delegate: PathActionHistoryDelegate? = nil
    
    private var mapPathIdPathActions = [String: [PathAction]]()
    
    func clear() { mapPathIdPathActions.removeAll() }
    
    func addAction(action: PathAction, pathId: String) {
        if mapPathIdPathActions[pathId] == nil {
            mapPathIdPathActions[pathId] = [PathAction]()
        }
        mapPathIdPathActions[pathId]!.append(action)
    }
    
    func addedPathIds() -> [String] {
        var ret = [String]()
        for (pathId, _) in mapPathIdPathActions {
            if wasAdded(pathId) {
                ret.append(pathId)
            }
        }
        return ret
    }
    
    func deletedPathIds() -> [String] {
        var ret = [String]()
        for (pathId, _) in mapPathIdPathActions {
            if wasDeleted(pathId) {
                ret.append(pathId)
            }
        }
        return ret
    }
    
    func loggedPathIds() -> [String] {
        var ret = [String]()
        for (pathId, _) in mapPathIdPathActions {
            if wasLogged(pathId) {
                ret.append(pathId)
            }
        }
        return ret
    }
    
    private func wasAdded(pathId: String) -> Bool {
        let actions = mapPathIdPathActions[pathId]!
        var addedTimes = 0
        var deletedTimes = 0
        for action in actions {
            switch action {
            case .Add: addedTimes++
            case .Delete: deletedTimes++
            default: break
            }
        }
        return deletedTimes < addedTimes
    }
    
    private func wasDeleted(pathId: String) -> Bool {
        let actions = mapPathIdPathActions[pathId]!
        var addedTimes = 0
        var deletedTimes = 0
        for action in actions {
            switch action {
            case .Add: addedTimes++
            case .Delete: deletedTimes++
            default: break
            }
        }
        return deletedTimes > addedTimes
    }
    
    private func wasLogged(pathId: String) -> Bool {
        let actions = mapPathIdPathActions[pathId]!
        var wasLogged: Bool = false
        for action in actions {
            switch action {
            case .Log: wasLogged = true
            case .Add: wasLogged = false
            case .Delete: wasLogged = false
            default: break
            }
        }
        return wasLogged
    }
}
