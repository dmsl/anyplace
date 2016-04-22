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


#ifndef datatypes_h
#define datatypes_h

#include "stdafx.hpp"

#define ULONG unsigned long
#define UINT unsigned int
#define Field Vector3D
#define PRECISION 1e-12
typedef UINT LineID

typedef struct Vector2D {
    double x, y;
    double norm() const;
    Vector2D  direction() const;
    
    Vector2D(double x, double y) { this->x = x; this->y = y; }
    Vector2D() : Vector2D(0.0, 0.0) {}
    
    Vector2D & rotate(double angle);
    Vector2D rotated(double angle) const;
    Vector2D & operator+=(const Vector2D & v);
    Vector2D & operator-=(const Vector2D & v);
    Vector2D & operator*=(double k);
    Vector2D & operator/=(double k);
    bool operator==(const Vector2D & v) const;
    bool operator!=(const Vector2D & v) const;
    
    Vector2D operator-() const;
    Vector2D operator+(const Vector2D & v) const;
    Vector2D operator-(const Vector2D & v) const;
    Vector2D operator*(const double k) const;
    double operator*(const Vector2D &v) const;
    Vector2D operator/(const double k) const;
    
} Vector2D;


typedef struct Vector3D {
    double x,y,z;
    double norm() const;
    Vector3D direction() const;
    void print(bool pretty = true) const;
    
    Vector3D(double x, double y, double z) { this->x = x; this->y = y; this->z = z; }
    Vector3D() : Vector3D(0.0, 0.0, 0.0) {}
    Vector3D(Vector2D v) : Vector3D(v.x, v.y, 0.0) {}
    
    Vector3D & operator+=(const Vector3D & v);
    Vector3D & operator-=(const Vector3D & v);
    Vector3D & operator*=(const double k);
    Vector3D & operator/=(const double k);
    bool operator==(const Vector3D & v);
    bool operator!=(const Vector3D & v);
    
    Vector3D operator-() const;
    Vector3D operator+(const Vector3D & v) const;
    Vector3D operator-(const Vector3D & v) const ;
    Vector3D operator*(const double k) const;
    double operator*(const Vector3D &v) const;
    Vector3D operator/(const double k) const;
    Vector3D cross(const Vector3D & v) const;
    double dot(const Vector3D & v) const;
    
} Vector3D;


typedef struct Quaternion {
private:
    void init(double w, Vector3D u) {
        this->w = w;
        this->u = u;
    }
    
    
public:
    double w;
    Vector3D u;
    
    Quaternion(Vector3D v1, Vector3D v2) {
        const double k = v2.norm() / v1.norm();
        const Vector3D d1 = v1.direction();
        const Vector3D d2 = v2.direction();
        if ( (d1 + d2).norm() < PRECISION ) {
            Vector3D n;
            srand( (unsigned int) time(0));
            do {
                double x = (double) rand() / RAND_MAX;
                double y = (double) rand() / RAND_MAX;
                double z = (double) rand() / RAND_MAX;
                Vector3D v = Vector3D(x, y, z);
                n = v - v1.direction()*(v*v1)/v1.norm();
            } while (n.norm() < PRECISION );
            init( 0.0, n.direction() );
        } else if ( (d1 - d2).norm() < PRECISION ) {
            init( 1.0, Vector3D(0.0, 0.0, 0.0) );
        } else {
            double phi = acos( v1.direction()*v2.direction() );
            Vector3D a = v1.cross(v2).direction();
            assert(a.norm() > PRECISION);
            double w = cos(phi/2) * sqrt(k);
            Vector3D u = a * sin(phi/2) * sqrt(k);
            init(w, u);
        }
    }
    
    Quaternion() : Quaternion(1.0, 0.0, 0.0, 0.0) {}
    
    Quaternion(double w, double x, double y, double z) : Quaternion(w, Vector3D(x, y, z)) {}
    
    Quaternion(double w, Vector3D u) {
        init(w, u);
    }
    

    double norm() const;
    void normalize();
    Quaternion inverse() const;
    Vector3D rotate(Vector3D v) const;
    void print(bool pretty = true) const;
    
} Quaternion;

Quaternion operator~(const Quaternion & q);
Quaternion operator-(const Quaternion & q);
Quaternion operator*(const Quaternion & q, const Vector3D & v);
Quaternion operator*(const Vector3D & v, const Quaternion & q);
Quaternion operator*(const Quaternion & q, const double k);
Quaternion operator/(const Quaternion & q, const double k);
Quaternion operator*(const Quaternion & q1, const Quaternion & q2);



typedef struct Point : private Vector2D {
public:
    using Vector2D::x;
    using Vector2D::y;
    double r() const;
    Point() : Vector2D() {}
    Point(double x, double y) : Vector2D(x,y) {}
    
    Point & rotate(double angle);
    Point rotated(double angle) const;
    
    Point & operator+=(const Point & p);
    Point & operator+=(const Vector2D & p);
    using Vector2D::operator-=;
    Point & operator*=(double k);
    Point & operator/=(double k);
    bool operator==(const Point & p) const;
    bool operator!=(const Point & p) const;
    bool operator<(const Point & p) const;
    Point operator+(const Point & p) const;
    Point operator+(const Vector2D & v) const;
    Point operator-(const Vector2D & v) const;
    Vector2D operator-(const Point & p) const;
    using Vector2D::operator-;
    Point operator*(const double k) const;
    Point operator/(const double k) const;
} Point;

typedef struct {
    Point bottom_left;
    Point top_right() { return Point(bottom_left.x + width, bottom_left.y + height); }
    double height;
    double width;
    bool contains(Point p) {
        Point b_l = bottom_left;
        Point t_r = top_right();
        return b_l.x < p.x && p.x < t_r.x && b_l.y < p.y && p.y < t_r.y;
    }
} Rect;

typedef struct Size {
    double width;
    double height;
    Size(double w, double h) {
        if (w <= 0 || h <= 0)
            throw std::invalid_argument("Size dimensions must be positive.");
        width = w;
        height = h;
    }
} Size;

typedef struct Line {
    Point a;
    Point b;
    Line() : Line(Point{0.0, 0.0}, Point{1.0, 0.0}) {}
    Line(Point a, Point b) : a(a), b(b) {
        if( a == b ) throw std::invalid_argument("Endpoints must differ");
    }
    bool contains(Point p) const;
    bool intersects(Line l) const;
    double dist(Point p) const;
    Point projection(Point p) const;
    bool projection_belongs(Point p) const;
    double length() const;
    Vector2D direction() const;
    
    bool operator<(const Line& line) const;
    bool operator==(const Line& line) const;
} Line;

bool operator<(const Point & p, const Line &l);
bool operator>(const Point & p, const Line &l);
bool operator>=(const Point & p, const Line &l);
bool operator<=(const Point & p, const Line &l);

typedef struct Obstacle {
    virtual std::vector<Line> contour() const = 0;
    virtual bool intersects_line(Line l) const = 0;
    virtual ~Obstacle();
} Obstacle;

typedef struct: Obstacle {
    Line line;
    std::vector<Line> contour() const {
        std::vector<Line> contour;
        contour.push_back(line);
        return contour;
    }
    bool intersects_line(Line l) const {
        return line.intersects(l);
    }
} Wall;

typedef struct {
    Point pos;
    UINT lineId;
} Feature __attribute__ ((aligned(8)));

typedef struct: Feature {
    Field field;
    Quaternion attitude;
} Milestone __attribute__ ((aligned(8)));

typedef struct {
    Point pos;
    double weight;
    std::vector<const Milestone *> nns;
    int clusterId;
} Particle;

typedef struct {
    unsigned long size;
    double min_radius;
    double max_radius;
    double avg_radius;
    double dev_radius;
} ClusterProperties;




#endif /* datatypes_h */