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

let PRECISION = 1e-15

@objc class Vector2D: NSObject, NSCoding {
    private(set) var x: Double
    private(set) var y: Double
    
    convenience override init() {
        self.init(x: 0, y: 0)
    }
    
    init(x: Double, y: Double) {
        self.x = x
        self.y = y
    }
    
    required convenience init?(coder aDecoder: NSCoder) {
        let x = aDecoder.decodeDoubleForKey("x")
        let y = aDecoder.decodeDoubleForKey("y")
        self.init(x: x, y: y)
    }
    
    func rotate(angle: Double) {
        let cosa = cos(angle)
        let sina = sin(angle)
        let x = self.x*cosa - self.y*sina
        let y = self.x*sina + self.y*cosa
        self.x = x
        self.y = y
    }
    
    func rotated(angle: Double) -> Vector2D {
        let v = Vector2D(x: x, y: y);
        v.rotate(angle)
        return v;
    }
    
    var norm: Double { return sqrt(pow(x,2) + pow(y,2)) }
    var direction: Vector2D { return self/norm }
    
    func normalize() { x /= norm; y /= norm }
    
    override var description: String {
        return String(format: "|(%.5f %.5f)| = %.5f", x, y, norm)
    }
    
    func encodeWithCoder(aCoder: NSCoder) {
        aCoder.encodeDouble(x, forKey: "x")
        aCoder.encodeDouble(y, forKey: "y")
    }
    
    override var hashValue: Int {
        return Int(x) + Int(y)
    }
}

prefix func -(vec: Vector2D) -> Vector2D { return Vector2D(x: -vec.x, y: -vec.y) }
func +(left: Vector2D, right: Vector2D) -> Vector2D { return Vector2D(x: left.x + right.x, y: left.y + right.y) }
func +=(inout left: Vector2D, right: Vector2D) { left.x += right.x; left.y += right.y }
func -(left: Vector2D, right: Vector2D) -> Vector2D { return left + -right }
func -=(inout left: Vector2D, right: Vector2D) { left += (-right) }
func *(left: Vector2D, right: Vector2D) -> Double { return left.x*right.x + left.y*right.y}
func *(vec: Vector2D, val: Double) -> Vector2D { return Vector2D(x: vec.x*val, y: vec.y*val) }
func *=(inout vec: Vector2D, val: Double) { vec.x *= val; vec.y *= val }
func /(vec: Vector2D, val: Double) -> Vector2D { return vec*(1.0/val) }
func /=(inout vec: Vector2D, val: Double) { vec *= 1.0/val }
func ==(lhs: Vector2D, rhs: Vector2D) -> Bool { return (lhs - rhs).norm < PRECISION }


@objc class Vector3D: NSObject, NSCoding {
    private(set) var x: Double
    private(set) var y: Double
    private(set) var z: Double
    
    convenience override init() {
        self.init(x: 0, y: 0,z : 0)
    }
    
    init(x: Double, y: Double, z: Double) {
        self.x = x
        self.y = y
        self.z = z
    }
    
    convenience init(v: Vector2D) {
        self.init(x: v.x, y: v.y, z: 0.0)
    }
    
    required convenience init?(coder aDecoder: NSCoder) {
        let x = aDecoder.decodeDoubleForKey("x")
        let y = aDecoder.decodeDoubleForKey("y")
        let z = aDecoder.decodeDoubleForKey("z")
        self.init(x: x, y: y, z: z)
    }
    
    var norm: Double { return sqrt(pow(x,2) + pow(y,2) + pow(z,2)) }
    var direction: Vector3D { return self/norm }
    
    func normalize() { x /= norm; y /= norm; z /= norm }
    
    override var description: String {
        return String(format: "|(%.5f %.5f %.5f)| = %.5f", x, y, z, norm)
    }
    
    func encodeWithCoder(aCoder: NSCoder) {
        aCoder.encodeDouble(x, forKey: "x")
        aCoder.encodeDouble(y, forKey: "y")
        aCoder.encodeDouble(z, forKey: "z")
    }
    
    func cross(v: Vector3D) -> Vector3D {
        return Vector3D(x: y*v.z - v.y*z, y: -x*v.z + v.x*z, z: x*v.y - v.x*y )
    }
    
    
}

prefix func -(vec: Vector3D) -> Vector3D { return Vector3D(x: -vec.x, y: -vec.y, z: -vec.z) }
func +(left: Vector3D, right: Vector3D) -> Vector3D { return Vector3D(x: left.x + right.x, y: left.y + right.y, z: left.z + right.z) }
func +=(inout left: Vector3D, right: Vector3D) { left.x += right.x; left.y += right.y; left.z += right.z; }

func -(left: Vector3D, right: Vector3D) -> Vector3D { return left + -right }
func -=(inout left: Vector3D, right: Vector3D) { left += (-right) }
func *(left: Vector3D, right: Vector3D) -> Double { return left.x*right.x + left.y*right.y + left.z*right.z;}
func cross( left: Vector3D, _ right: Vector3D ) -> Vector3D {  return Vector3D(x: left.y*right.z - right.y*left.z, y: -left.x*right.z + right.x*left.z, z: left.x*right.y - right.x*left.y )  }
func *(vec: Vector3D, val: Double) -> Vector3D { return Vector3D(x: vec.x*val, y: vec.y*val, z: vec.z*val); }
func *(val: Double, vec: Vector3D) -> Vector3D { return vec*val; }
func *=(inout vec: Vector3D, val: Double) { vec.x *= val; vec.y *= val; vec.z *= val; }
func /(vec: Vector3D, val: Double) -> Vector3D { return vec*(1.0/val) }
func /=(inout vec: Vector3D, val: Double) { vec *= 1.0/val }
func ==(lhs: Vector3D, rhs: Vector3D) -> Bool { return (lhs - rhs).norm < PRECISION }


@objc class Quaternion: NSObject, NSCoding {
    private(set) var w: Double
    private(set) var u: Vector3D
    
    var x: Double { return u.x }
    var y: Double { return u.y }
    var z: Double { return u.z }
    
    convenience init(v1: Vector3D, v2: Vector3D) {
        let k = v2.norm / v1.norm
        let d1 = v1.direction
        let d2 = v2.direction
        if (d1 + d2).norm < PRECISION {
            var n: Vector3D
            repeat {
                let x = Double(arc4random()) / Double(UINT32_MAX)
                let y = Double(arc4random()) / Double(UINT32_MAX)
                let z = Double(arc4random()) / Double(UINT32_MAX)
                let v = Vector3D(x: x, y: y, z: z)
                n = v - v1.direction*(v*v1)/v1.norm
            } while n.norm < PRECISION
            self.init(w: 0.0, u: n.direction)
        } else if (d1 - d2).norm < PRECISION {
            self.init()
        } else {
            let phi = acos( v1.direction*v2.direction)
            let a = cross(v1, v2).direction
            assert(a.norm > PRECISION)
            let w = cos(phi/2) * sqrt(k)
            let u = sin(phi/2) * a * sqrt(k)
            self.init(w: w, u: u)
        }
    }
    
    convenience override init() {
        self.init(w: 1.0, x: 0.0, y: 0.0, z: 0.0)
    }
    
    convenience init(w: Double, x: Double, y: Double, z: Double) {
        self.init(w: w, u: Vector3D(x: x, y: y, z: z))
    }
    
    init(w: Double, u: Vector3D) {
        self.w = w
        self.u = u
    }
    
    required convenience init?(coder aDecoder: NSCoder) {
        let w = aDecoder.decodeDoubleForKey("w")
        let u = aDecoder.decodeObjectForKey("u") as! Vector3D
        self.init(w: w, u: u)
    }
    
    func encodeWithCoder(aCoder: NSCoder) {
        aCoder.encodeDouble(w, forKey: "w")
        aCoder.encodeObject(u, forKey: "u")
    }
    
    override var description: String {
        return String(format: "(%.5f %.5f %.5f %.5f)", w, x, y, z)
    }
    
//    override var description: String {
//        return "|\(w) \(u.x) \(u.y) \(u.z)| = \(norm) "
//    }
//    
    

    var norm: Double { return sqrt( pow(w, 2) + pow(u.norm, 2) ) }
    func normalize() { w /= norm; u /= norm }
    
    func inverse() -> Quaternion { return (~self)/pow(norm, 2) }
    
    func rotate(v: Vector3D) -> Vector3D {
        return (self*v*inverse()).u
    }
}

prefix func ~(q: Quaternion) -> Quaternion { return Quaternion(w: q.w, u: -q.u) }
prefix func -(q: Quaternion) -> Quaternion { return Quaternion(w: -q.w, u: -q.u) }
func *(q: Quaternion, v: Vector3D) -> Quaternion { return q * Quaternion(w: 0.0, u: v)  }
func *(v: Vector3D, q: Quaternion) -> Quaternion { return Quaternion(w: 0.0, u: v) * q }
func *(q: Quaternion, k: Double) -> Quaternion { return Quaternion(w: q.w*k, u: q.u*k) }
func /(q: Quaternion, k: Double) -> Quaternion { return q*(1.0/k) }
func *(q1: Quaternion, q2: Quaternion) -> Quaternion {
    let w = q1.w * q2.w - q1.u * q2.u;
    let u = q1.w * q2.u + q2.w * q1.u + cross(q1.u, q2.u)
    return Quaternion(w: w, u: u)
}


class Point : Vector2D {
    
    override init(x: Double, y: Double) {
        super.init(x: x, y: y)
    }
    
    convenience init(_ v: Vector2D) {
        self.init(x: v.x, y: v.y)
    }
    
    override internal var norm: Double {
        return super.norm
    }
    
    var r: Double {
        return norm
    }
}

func -(p: Point, v: Vector2D) -> Point { return Point(x: p.x - v.x, y: p.y - v.y) }
func -(left: Point, right: Point) -> Vector2D { return Vector2D(x: left.x - right.x, y: left.y - right.y) }
prefix func -(p: Point) -> Point { return Point(x: -p.x, y: -p.y) }
func +(left: Point, right: Point) -> Point { return Point(x: left.x + right.x, y: left.y + right.y) }
func +=(inout left: Point, right: Point) { left.x += right.x; left.y += right.y; }
private func -=(inout left: Point, right: Point) {  }
func *(p: Point, val: Double) -> Point { return Point(x: p.x*val, y: p.y*val); }
func *=(inout p: Point, val: Double) { p.x *= val; p.y *= val;}
func /(p: Point, val: Double) -> Point { return p*(1.0/val) }
func /=(inout p: Point, val: Double) { p *= 1.0/val }
func ==(lhs: Point, rhs: Point) -> Bool { return (lhs - rhs).norm < PRECISION }

struct Line {
    
    private(set) var a: Point
    private(set) var b: Point
    
    init?(a: Point, b: Point) {
        if( a == b ) {
            return nil
        }
        
        self.a = a
        self.b = b
    }
    
    init() {
        self.a = Point(x: 0.0, y: 0.0)
        self.b = Point(x: 1.0, y: 0.0)
    }
    
    func contains(p: Point) -> Bool {
        let v1 = p - a
        let v2 = b - a
        if v1.norm > v2.norm {
            return false
        }
        if v1.direction != v2.direction {
            return false
        }
        return true
    }
    
    func dist(p: Point) -> Double {
        let AB = b - a
        let BA = -AB
        let PA = a - p
        let PB = b - p
        let cosa = abs(AB*PA) / AB.norm / PA.norm
        let sina = sqrt(1 - pow(cosa, 2))
        let h = PA.norm*sina
    
        if( PA*AB <= 0 && PB*BA <= 0) {
            return h;
        } else if ( PA*AB >= 0 ) {
            return PA.norm
        } else if ( PB*BA >= 0 ) {
            return PB.norm
        } else {
            assertionFailure("Geometrical logic error")
            return -1 //To shut up error
        }
    }
    
    func intersects(l: Line) -> Bool {
        if (a >= l && b <= l) || (a <= l && b >= l)  {
            let ha = l.dist(a)
            let hb = l.dist(b)
            let h = ha + hb
            if h == 0.0 {
                return l.contains(a) || l.contains(b)
            }
            let wa = 1.0 - ha / h
            let  wb = 1.0 - hb / h
            let p = a*wa + b*wb
            return l.contains(p)
        }
        return false
    }

    func projection(p: Point) -> Point {
        let AB = b - a
        let PA = a - p
        return a - AB.direction * (PA*AB) / AB.norm
    }
    
    func projection_belongs(p: Point) -> Bool {
        let AB = b - a
        let BA = -AB
        let PA = a - p
        let PB = b - p;
        return PA*AB <= 0 && PB*BA <= 0
    }
    
    var length: Double {
        return (a-b).norm
    }

    var direction: Vector2D {
        var v = b - a
        v /= length
        return v
    }

}


//Comparison operators
func <(left: Line, right: Line) -> Bool { return ((left.a + left.b)/2).r < ((right.a + right.b)/2).r }
func ==(left: Line, right: Line) -> Bool { return (left.a == right.a && left.b == right.b) || (left.a == right.b && left.b == right.a) }
func <(p: Point, l: Line) -> Bool {
    let AP = Vector3D(v: p - l.a)
    let AB = Vector3D(v: l.b - l.a)
    return AB.cross(AP).z > 0
}
func >(p: Point, l: Line) -> Bool { return !(p < l) }
func >=(p: Point, l: Line) -> Bool { return p > l || l.contains(p) }
func <=(p: Point, l: Line) -> Bool  { return p < l || l.contains(p) }


