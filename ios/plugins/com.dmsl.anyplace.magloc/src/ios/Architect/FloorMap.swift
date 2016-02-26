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
import CoreGraphics
import UIKit

typealias Vertex = Point

struct Path {
    init(v1: Vertex, v2: Vertex, id: String) {
        self.v1 = v1
        self.v2 = v2
        self.id = id
    }
    
    var v1: Vertex
    var v2: Vertex
    var id: String
}

class FloorMap {
    private let TAG = "FloorMap: "
    
    var vertexSize: CGFloat = 0.0 {
        didSet {
            if vertexSize < 0 {
                vertexSize = oldValue
            }
        }
    }
    var pathWidth: CGFloat = 0.0 {
        didSet {
            if pathWidth < 0 {
                pathWidth = oldValue
            }
        }
    }
    
    private var pairsVertexPathList = [Vertex: [String: Path]]()
    private var paths = [String: Path]()
    static let DEFAULT_VERTEX_COLOR = UIColor.blueColor()
    private var verticesColors = [Vertex: UIColor]()
    static let DEFAULT_PATH_COLOR = UIColor.blueColor()
    private var pathsColors = [String: UIColor]()
    
    var pathsAll: [Path] {
        var arr = [Path]()
        for (_, path) in paths {
            arr.append(path)
        }
        return arr
    }
    
    func addVertex(v: Vertex) {
        if pairsVertexPathList[v] == nil {
            pairsVertexPathList[v] = [String: Path]()
        }
    }
    
    func removeVertex(v: Vertex) {
        if let paths = pairsVertexPathList[v] {
            for (_, path) in paths {
                self.paths.removeValueForKey(path.id)
            }
            pairsVertexPathList.removeValueForKey(v)
        }
    }
    func vertexExists(v: Vertex) -> Bool {
        return pairsVertexPathList[v] != nil
    }
    func setVertexColor(v: Vertex, color: UIColor) {
        if pairsVertexPathList[v] != nil {
            verticesColors[v] = color
        }
    }
    
    
    func addPath(path: Path) {
        addVertex(path.v1)
        addVertex(path.v2)
        pairsVertexPathList[path.v1]![path.id] = path
        pairsVertexPathList[path.v2]![path.id] = path
        paths[path.id] = path
    }
    
    func removePath(id: String) {
        if let path = paths[id] {
            if let paths = pairsVertexPathList[path.v1] {
                if paths.count == 1 {
                    pairsVertexPathList.removeValueForKey(path.v1)
                }
            }
            if let paths = pairsVertexPathList[path.v2] {
                if paths.count == 1 {
                    pairsVertexPathList.removeValueForKey(path.v2)
                }
            }
            paths.removeValueForKey(id)
            pathsColors.removeValueForKey(id)
        }
    }
    
    func pathById(id: String) -> Path? {
        return paths[id]
    }
    
    func setPathColor(id: String, color: UIColor) {
        if paths[id] != nil {
            pathsColors[id] = color
        }
    }
    
    func vertexAtPoint(point: CGPoint, maxDist: CGFloat) -> Vertex? {
        var closestDist: Double = Double.infinity
        var closestVertex: Vertex?
        for (vertex, _) in pairsVertexPathList {
            let cgVertex = CGPoint(x: vertex.x, y: vertex.y)
            let dist = sqrt(pow(Double(cgVertex.x - point.x), 2) + pow(Double(cgVertex.y - point.y), 2))
            if dist <= Double(maxDist) {
                if dist < closestDist {
                    closestDist = dist
                    closestVertex = vertex
                }
            }
        }
        return closestVertex
    }
    
    func pathIdAtPoint(point: CGPoint, maxDist: CGFloat) -> String? {
        var closestDist: Double = Double.infinity
        var closestPathId: String?
        let p = Point(x: Double(point.x), y: Double(point.y))
        for (id, path) in paths {
            let line = Line(a: path.v1, b: path.v2)!
            let dist = line.dist(p)
            if dist < Double(maxDist) && dist < closestDist {
                closestDist = dist
                closestPathId = id
            }
        }
        return closestPathId
    }
    
    func clear() {
        pairsVertexPathList.removeAll(keepCapacity: true)
        paths.removeAll(keepCapacity: true)
        verticesColors.removeAll(keepCapacity: true)
        pathsColors.removeAll(keepCapacity: true)
    }

    
    func drawMap(mapImage: UIImage, vertexSize: CGFloat, pathWidth: CGFloat) -> UIImage {
        print(TAG + "drawMap")
        let image: UIImage!
        
        let rect = CGRectMake(0, 0, mapImage.size.width, mapImage.size.height)
        UIGraphicsBeginImageContextWithOptions(CGSize(width: rect.width, height: rect.height), false, 0)
        mapImage.drawInRect(rect)
        
        //Draw paths
        for (id, path) in paths {
            let p1 = CGPoint(x: path.v1.x, y: path.v1.y)
            let p2 = CGPoint(x: path.v2.x, y: path.v2.y)
            let color = pathsColors[id] ?? FloorMap.DEFAULT_PATH_COLOR
            drawLine(p1, p2: p2, color: color, size: CGFloat(pathWidth))
        }
        
        //Draw vertices of paths over paths
        for (v, _) in pairsVertexPathList {
            let color = verticesColors[v] ?? FloorMap.DEFAULT_VERTEX_COLOR
            let pos = CGPoint(x: v.x, y: v.y)
            drawPoint(pos, color: color, size: CGFloat(vertexSize))
        }
        
        image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        
        return image
    }
    
    private func drawPoint(pos: CGPoint, color: UIColor, size: CGFloat) {
        let path = UIBezierPath(arcCenter: pos, radius: size/2.0, startAngle: 0, endAngle: CGFloat(2*M_PI), clockwise: true)
        color.setFill()
        path.fill()
    }
    
    private func drawLine(p1: CGPoint, p2: CGPoint, color: UIColor, size: CGFloat) {
        let path = UIBezierPath()
        path.moveToPoint(p1)
        path.addLineToPoint(p2)
        path.lineWidth = size
        color.setStroke()
        path.stroke()
    }

    
    
    
}
