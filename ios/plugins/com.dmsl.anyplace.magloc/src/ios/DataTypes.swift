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

struct ApiClientDataTypes {
    
    struct Milestone {
        let pos: LatLng
        let orientation: Quaternion
        let field: Vector3D
        
        init(pos: LatLng, orientation: Quaternion, field: Vector3D) {
            self.pos = pos
            self.orientation = orientation
            self.field = field
        }
        
        init(JSON: [String: AnyObject]) {
            let pos = JSON[ApiClientParameters.Position]!
            let lat = pos[ApiClientParameters.Lat]! as! Double
            let lng = pos[ApiClientParameters.Lng]! as! Double
            
            let orientation = JSON[ApiClientParameters.Orientation]!
            let w = orientation[ApiClientParameters.w]! as! Double
            let x = orientation[ApiClientParameters.x]! as! Double
            let y = orientation[ApiClientParameters.y]! as! Double
            let z = orientation[ApiClientParameters.z]! as! Double
            
            let field = JSON[ApiClientParameters.Field]!
            let f_x = field[ApiClientParameters.x]! as! Double
            let f_y = field[ApiClientParameters.y]! as! Double
            let f_z = field[ApiClientParameters.z]! as! Double
            
            self.init(pos: LatLng(lat: lat, lng: lng), orientation: Quaternion(w: w, x: x, y: y, z: z), field: Vector3D(x: f_x, y: f_y, z: f_z))
        }
        
        var JSON: [String: AnyObject] {
            var dict = [String: AnyObject]()
            
            var pos = [String: Double]()
            pos[ApiClientParameters.Lat] = self.pos.lat
            pos[ApiClientParameters.Lng] = self.pos.lng
            dict[ApiClientParameters.Position] = pos
            
            var orientation = [String: Double]()
            orientation[ApiClientParameters.w] = self.orientation.w
            orientation[ApiClientParameters.x] = self.orientation.x
            orientation[ApiClientParameters.y] = self.orientation.y
            orientation[ApiClientParameters.z] = self.orientation.z
            dict[ApiClientParameters.Orientation] = orientation
            
            var field = [String: Double]()
            field[ApiClientParameters.x] = self.field.x
            field[ApiClientParameters.y] = self.field.y
            field[ApiClientParameters.z] = self.field.z
            dict[ApiClientParameters.Field] = field
            
            return dict
        }
    }
    
    static func parseMilestonesDownloadResponseData(jsonArray: [[String: AnyObject]]) -> [String: [Milestone]] {
        var ret = [String: [Milestone]]()
        for json in jsonArray {
            let pathId = json[ApiClientParameters.PathId]! as! String
            let milestones_json_arr = json[ApiClientParameters.Milestones]! as! [[String: AnyObject]]
            var milestones = [Milestone]()
            for milestone_json in milestones_json_arr {
                milestones.append(Milestone(JSON: milestone_json))
            }
            ret[pathId] = milestones
        }
        return ret
    }
    
    static func constructMilestonesUploadRequestData(mapPathIdMilestones: [String: [Milestone]] ) -> [[String: AnyObject]] {
        var ret = [[String: AnyObject]]()
        for (pathId, milestones) in mapPathIdMilestones {
            var json = [String: AnyObject]()
            json[ApiClientParameters.PathId] = pathId
            var milestones_json_arr = [[String: AnyObject]]()
            for milestone in milestones {
                let milestone_json = milestone.JSON
                milestones_json_arr.append(milestone_json)
            }
            json[ApiClientParameters.Milestones] = milestones_json_arr
            ret.append(json)
        }
        return ret
    }
    
    
    
    struct Path {
        let pathId: String
        let p1: LatLng
        let p2: LatLng
        
        init(p1: LatLng, p2: LatLng, pathId: String) {
            self.pathId = pathId
            self.p1 = p1
            self.p2 = p2
        }
        
        init(JSON: [String: AnyObject]) {
            let pathId = JSON[ApiClientParameters.PathId]! as! String
            
            let line = JSON[ApiClientParameters.Line]!
            let endpoints = line[ApiClientParameters.Endpoints]! as! [[String: AnyObject]]
            let lat_a = endpoints[0][ApiClientParameters.Lat]! as! Double
            let lng_a = endpoints[0][ApiClientParameters.Lng]! as! Double
            
            let lat_b = endpoints[1][ApiClientParameters.Lat]! as! Double
            let lng_b = endpoints[1][ApiClientParameters.Lng]! as! Double

            self.init(p1: LatLng(lat: lat_a, lng: lng_a), p2: LatLng(lat: lat_b, lng: lng_b), pathId: pathId)
        }
        
        var JSON: [String: AnyObject] {
            var dict = [String: AnyObject]()
            
            dict[ApiClientParameters.PathId] = pathId
            
            var line = [String: AnyObject]()
            var endpoints = [[String: AnyObject]]()
            
            var a = [String: AnyObject]()
            a[ApiClientParameters.Lat] = p1.lat
            a[ApiClientParameters.Lng] = p1.lng
            endpoints.append(a)
            
            var b = [String: AnyObject]()
            b[ApiClientParameters.Lat] = p2.lat
            b[ApiClientParameters.Lng] = p2.lng
            endpoints.append(b)
            
            line[ApiClientParameters.Endpoints] = endpoints
            dict[ApiClientParameters.Line] = line
            
            return dict
        }
    }
    
    static func parsePathsUploadResponseData(jsonArray: [[String: AnyObject]]) -> [String] {
        var ret = [String]()
        for jsonPath in jsonArray {
            ret.append(jsonPath[ApiClientParameters.PathId]! as! String)
        }
        return ret
    }
    
    static func parsePathsDownloadResponseData(jsonArray: [[String: AnyObject]]) -> [Path] {
        var ret = [Path]()
        for jsonPath in jsonArray {
            ret.append(Path(JSON: jsonPath))
        }
        return ret
    }
    
    static func constructPathsUploadRequestData(paths: [Path] ) -> [[String: AnyObject]] {
        var ret = [[String: AnyObject]]()
        for path in paths {
            ret.append(path.JSON)
        }
        return ret
    }
    
    
}

