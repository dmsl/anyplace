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


#ifndef Map_cpp
#define Map_cpp

#include "stdafx.hpp"

/**
 * Map - represents a 2D floor map of one floor in the building. It holds information about the 
 * a) Features (can be Milestones a.k.a. Magnetic Fingerprints) - used for orientation self-localization;
 * b) Feature Lines - features are united into lines, used for KNN approximation;
 * c) Obstacles - to make features invisible, for instance from the walls;
 * d) Direction of the north.
 */

class Map {
private:
    static const Vector2D DEFAULT_NORTH = {0.0, 1.0};
    static const Size DEFAULT_SIZE = {(double)UINT_MAX, (double)UINT_MAX};
    
    std::vector<const Feature *> _features;
    //std::map<Line, std::vector<const Feature *>*> _feature_lines;
    std::map<LineID,  std::vector<const Feature *>*> _line_id_to_features;
    std::map<LineID, Line> _line_id_to_line;
    std::vector<const Obstacle *> _obstacles;
    const Vector2D _north_direction;
    Size _size;

    
    static const unsigned long find_nearest_feature_index_to_projection(const Point p, const LineID lid, const Map & map);
    
public:
    /**
     * Features are stored in a heap to avoid copying during localization. Deletion of the features is done by Map class. Features should not be modifed outside of the Map class.
     */
    
    Map() : Map(DEFAULT_SIZE, DEFAULT_NORTH);
    
    Map(Size size, Vector2D north_direction) : _size(size), _north_direction(north_direction) {};
    
    Map(std::vector<Feature> features, Size size = DEFAULT_SIZE, Vector2D north_direction = DEFAULT_NORTH) : _size(size), _north_direction(north_direction) {
        add_features(features);
    };
    
    void map_set_size(Size size) { _size = size; }
    Size map_get_size() { return _size; }
    
    void add_lines(std::map<LineID, Line> lines);
    void add_features(std::vector<Feature *> features);
    void add_obstacles(std::vector<Obstacle *> obstacles);
    
    
    void clear_lines();
    void clear_features();
    void clear_obstacles();
    
    const unsigned long features_count() const { return _features.size(); }
    const unsigned long obstacles_count() const { return _obstacles.size(); }
    const Vector2D get_north() const { return _north_direction; }
    
    const Feature * const feature_at(unsigned long ind) const { return _features.at(ind); }
    const std::vector<const Feature *> features_at(std::vector<unsigned long> indices) const;
    
    Line get_line(LineID lineId) { return _line_id_to_line.at(lineId); }
    
//    const std::vector<unsigned long> find_KNN_features_indices(const Point p, const unsigned long k) const;
    
    /**
     * find_NN_features() - returns k nearest features to the given point;
     */
    const std::vector<const Feature *> find_NN_features(const Point p, const unsigned long k, const double r = -1.0) const;
    
    //Static KNN methods
    /**
     * find_naive_KNN_features() - returns k nearest features to the given point, using brute-force search
     */
    static const std::vector<const Feature *> find_naive_KNN_features(const Point p, const unsigned long k, const Map & map);
    
    /**
     * find_lined_KNN_features() - returns approximate k nearest features to the given point, using following algorithm:
     * a) Finds closest feature line;
     * b) Brute-force searches for the closest features.
     */
    static const std::vector<const Feature *> find_lined_KNN_features(const Point p, const unsigned long k, const Map & map);
    
    /**
     * find_fast_lined_KNN_features() - returns approximate k nearest features to the given point, using following algorithm:
     * a) Finds closest feature line;
     * b) Finds projection on this lines;
     * c) Finds closest features on lines to this projection.
     */
    static const std::vector<const Feature *> find_fast_lined_KNN_features(const Point p, const unsigned long k, const Map & map);
    
    /**
     * Same as above, additional arguments can be specified:
     * r - desirable radius that include obtained NN features, may be larger
     * k_min - minimal number of NN features
     */
    static const std::vector<const Feature *> find_fast_lined_NN_features(const Point p, const double r, const unsigned long k_min, const Map & map);
    
    /**
     * find_KNN_lines() - returns k nearest lines to the given point (by distance to the endpoint or orthogonal projection)
     */
    static const std::vector<LineID> find_KNN_lines(const Point p, const unsigned long k, const Map & map);
    
    /**
     * find_KNN_lines() - returns nearest lines to the given point (by distance to the endpoint or orthogonal projection)
     */
    static const LineID find_NN_line(const Point p, const Map & map);
    
    
    ~Map() {
        clear_features();
        clear_obstacles();
    };
};

#endif /* Map_cpp */
