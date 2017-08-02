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

#include "datatypes.hpp"

//--== Vector2D ==--

//Methods
double Vector2D::norm() const { return sqrt(pow(x, 2) + pow(y, 2)); }
Vector2D  Vector2D::direction() const { return *this / norm(); }
Vector2D & Vector2D::rotate(double angle) {
    double cosa = cos(angle);
    double sina = sin(angle);
    double x = this->x*cosa - this->y*sina;
    double y = this->x*sina + this->y*cosa;
    this->x = x;
    this->y = y;
    return *this;
}
Vector2D Vector2D::rotated(double angle) const { return Vector2D{ x, y }.rotate(angle); }


//Mutationg operators
Vector2D & Vector2D::operator+=(const Vector2D & v) { x += v.x; y += v.y; return *this; }
Vector2D & Vector2D::operator-=(const Vector2D & v) { x -= v.x; y -= v.y; return *this; }
Vector2D & Vector2D::operator*=(double k) { x *= k; y *= k; return *this; }
Vector2D & Vector2D::operator/=(double k) { *this *= (1.0/k); return *this;}

//Immutable operators
Vector2D Vector2D::operator-() const { return Vector2D{-x, -y}; }
Vector2D Vector2D::operator+(const Vector2D & v) const { return Vector2D{x + v.x, y + v.y}; }
Vector2D Vector2D::operator-(const Vector2D & v) const { return Vector2D{x - v.x, y - v.y}; }
Vector2D Vector2D::operator*(const double k) const { return Vector2D{x*k, y*k}; }
double Vector2D::operator*(const Vector2D &v) const { return x*v.x + y*v.y; }
Vector2D Vector2D::operator/(const double k) const { return *this*(1.0/k); }

//Comparison operators
bool Vector2D::operator==(const Vector2D & v) const { return  (*this - v).norm() < PRECISION; }
bool Vector2D::operator!=(const Vector2D & v) const { return !(*this == v); }

//--== Point ==--

double Point::r() const { return Vector2D::norm(); }

Point & Point::rotate(double angle) {
    Vector2D v = Vector2D{x, y};
    v.rotate(angle);
    this->x = v.x;
    this->y = v.y;
    return *this;
}
Point Point::rotated(double angle) const { return Point{x, y}.rotate(angle); }

Point & Point::operator+=(const Point & p) { x += p.x; y += p.y; return *this; }
Point & Point::operator+=(const Vector2D & p) { x += p.x; y += p.y; return *this; }
Point & Point::operator*=(double k) { x *= k; y *= k; return *this; }
Point & Point::operator/=(double k) { *this *= (1.0/k); return *this;}
bool Point::operator==(const Point & p) const { return x == p.x && y == p.y; }
bool Point::operator!=(const Point & p) const { return !(*this == p); }
bool Point::operator<(const Point & p) const { return this->norm() < p.norm(); }
Point Point::operator+(const Point & p) const { return Point{x + p.x, y + p.y}; }
Point Point::operator+(const Vector2D & v) const { return Point{x + v.x, y + v.y}; }
Point Point::operator-(const Vector2D & v) const { return Point{x - v.x, y - v.y}; }
Vector2D Point::operator-(const Point & p) const { return Vector2D{x - p.x, y - p.y}; }
Point Point::operator*(const double k) const { return Point{x*k, y*k}; }
Point Point::operator/(const double k) const { return *this*(1.0/k); }



//--== Vector3D ==--

//Methods
double Vector3D::norm() const { return sqrt(pow(x, 2) + pow(y, 2) + pow(z,2)); }
Vector3D Vector3D::direction() const { return *this / norm(); }
void Vector3D::print(bool pretty) const {
    if (pretty)
        printf("|(%.5f %.5f %.5f)| = %.5f\n", x, y, z, norm());
    else
        printf("|(%f %f %f)| = %f\n", x, y, z, norm());
}

//Mutating operators
Vector3D & Vector3D::operator+=(const Vector3D & v) { x += v.x; y += v.y; z += v.z; return *this; }
Vector3D & Vector3D::operator-=(const Vector3D & v) { x -= v.x; y -= v.y; z -= v.z; return *this; }
Vector3D & Vector3D::operator*=(const double k) { x *= k; y *= k; z *= k; return *this; }
Vector3D & Vector3D::operator/=(const double k) { *this *= (1.0/k); return *this;}

//Immutable operators
Vector3D Vector3D::operator-() const { return Vector3D{-x, -y, -z}; }
Vector3D Vector3D::operator+(const Vector3D & v) const { return Vector3D{x + v.x, y + v.y, z + v.z}; }
Vector3D Vector3D::operator-(const Vector3D & v) const { return Vector3D{x - v.x, y - v.y, z - v.z}; }
Vector3D Vector3D::operator*(const double k) const { return Vector3D{x*k, y*k, z*k}; }
double Vector3D::operator*(const Vector3D &v) const { return x*v.x + y*v.y + z*v.z; }
Vector3D Vector3D::operator/(const double k) const { return *this*(1.0/k); }
Vector3D Vector3D::cross(const Vector3D & v) const { return Vector3D(y*v.z - v.y*z, -x*v.z + v.x*z, x*v.y - v.x*y ); }
double Vector3D::dot(const Vector3D & v) const { return *this*v; }

//Comparison operators
bool Vector3D::operator==(const Vector3D & v) { return (*this - v).norm() < PRECISION; }
bool Vector3D::operator!=(const Vector3D & v) { return !(*this == v); }



//--== Quaternion ==--

//Methods
double Quaternion::norm() const { return sqrt( pow(w, 2) + pow(u.norm(), 2) ); }
void Quaternion::normalize() { double norm = this->norm(); w /= norm; u /= norm; }
Quaternion Quaternion::inverse() const { return ~(*this)/pow(norm(), 2); }
Vector3D Quaternion::rotate(Vector3D v) const { return (*this*v*this->inverse()).u; }
void Quaternion::print(bool pretty) const {
    if (pretty)
        printf("|(%.5f %.5f %.5f %.5f)| = %.5f\n", w, u.x, u.y, u.z, norm());
    else
        printf("|(%f %f %f %f)| = %f\n", w, u.x, u.y, u.z, norm());
}

//Immutable operators
Quaternion operator~(const Quaternion & q ) { return Quaternion(q.w, -q.u); }
Quaternion operator-(const Quaternion & q) { return Quaternion(-q.w, -q.u); }
Quaternion operator*(const Quaternion & q, const Vector3D & v) { return q * Quaternion(0.0, v);  }
Quaternion operator*(const Vector3D & v, const Quaternion & q) { return Quaternion(0.0, v) * q; }
Quaternion operator*(const Quaternion & q, const double k) { return Quaternion( q.w*k, q.u*k ); }
Quaternion operator/(const Quaternion & q, const double k) { return q*(1.0/k); }
Quaternion operator*(const Quaternion & q1, const Quaternion & q2) {
    double w = q1.w * q2.w - q1.u * q2.u;
    Vector3D u = q2.u * q1.w  + q1.u * q2.w + q1.u.cross(q2.u);
    return Quaternion(w, u);
}


//--== Line ==--

//Methods
bool Line::contains(Point p) const {
    Vector2D v1 = p - a;
    Vector2D v2 = b - a;
    if (v1.norm() > v2.norm())
        return false;
    if ( v1.direction() != v2.direction() )
        return false;
    return true;
}
bool Line::intersects(Line l) const {
    if ( (a >= l && b <= l) || (a <= l && b >= l) ) {
        double ha = l.dist(a);
        double hb = l.dist(b);
        double h = ha + hb;
        if (h == 0.0)
            return l.contains(a) || l.contains(b);
        double wa = 1.0 - ha / h;
        double wb = 1.0 - hb / h;
        Point p = a*wa + b*wb;
        return l.contains(p);
    }
    return false;
    
}
double Line::dist(Point p) const {
    Vector2D AB = this->b - this->a;
    Vector2D BA = -AB;
    Vector2D PA = this->a - p;
    Vector2D PB = this->b - p;
    double cosa = std::abs(AB*PA) / AB.norm() / PA.norm();
    double sina = sqrt(1 - pow(cosa, 2));
    double h = PA.norm()*sina;
    
    if( PA*AB <= 0 && PB*BA <= 0) {
        return h;
    } else if ( PA*AB >= 0 ) {
        return PA.norm();
    } else if ( PB*BA >= 0 ) {
        return PB.norm();
    } else {
        throw std::runtime_error("Problems with geometrical logic");
    }
}
Point Line::projection(Point p) const {
    Vector2D AB = this->b - this->a;
    Vector2D PA = this->a - p;
    return this->a - AB.direction() * (PA*AB) / AB.norm();
}
bool Line::projection_belongs(Point p) const {
    Vector2D AB = this->b - this->a;
    Vector2D BA = -AB;
    Vector2D PA = this->a - p;
    Vector2D PB = this->b - p;
    return PA*AB <= 0 && PB*BA <= 0;
}
double Line::length() const {
    return (a - b).norm();
}

Vector2D Line::direction() const { return (b - a)/length(); }

//Comparison operators
bool Line::operator<(const Line& line) const { return ((this->a + this->b)/2).r() < ((line.a + line.b)/2).r(); }
bool Line::operator==(const Line& line) const { return (this->a == line.a && this->b == line.b) || (this->a == line.b && this->b == line.a); }



bool operator<(const Point & p, const Line &l) {
    Vector3D AP(p - l.a);
    Vector3D AB(l.b - l.a);
    return AB.cross(AP).z > 0;
}
bool operator>(const Point & p, const Line &l) { return !(p < l); }
bool operator>=(const Point & p, const Line &l) { return p > l || l.contains(p); }
bool operator<=(const Point & p, const Line &l)  { return p < l || l.contains(p); }


std::vector<const Milestone *> p_to_milestones(std::vector<const Feature *> features) {
    std::vector<const Milestone *> ret(features.size(), 0);
    for (int i = 0; i < features.size(); i++)
        ret.at(i) = (const Milestone *) features.at(i);
    return ret;
}
std::vector<const Feature *> p_to_features(std::vector<const Milestone *> milestones) {
    return std::vector<const Feature *>(milestones.begin(), milestones.end());
}
