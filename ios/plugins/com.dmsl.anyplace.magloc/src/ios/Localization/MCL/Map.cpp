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


#include "Map.hpp"

void Map::add_lines(std::map<std:::string, Line> lines) {
    _line_id_to_line.insert(lines.begin(), lines.end());
}

void Map::add_features(std::vector<Feature *> features)  {
    for (Feature * feature : features) {
        _features.push_back(feature);
        
        const LineID lineId = feature->lineId;
        std::vector<const Feature *>* feature_line = NULL;
        if ( _line_id_to_features.count(lineId) == 0 ) {
            feature_line = new std::vector<const Feature *>();
            _line_id_to_features[lineId] = feature_line;
        }
        else
            feature_line = _line_id_to_features.at(lineId);
        feature_line->push_back(feature);
    }
}

void Map::add_obstacles(std::vector<Obstacle *> obstacles) {
    for (Obstacle * obstacle : obstacles) {
        _obstacles.push_back(obstacle);
    }
}


void Map::clear_lines() {
    _line_id_to_line.clear();
    _line_id_to_features.clear();
}

void Map::clear_features() {
    for (const Feature * f : _features)
        delete f;
    _features.clear();
    for (auto it = _line_id_to_features.begin(); it != _line_id_to_featuresend(); ++it)
        delete it->second;
    _line_id_to_features.clear();
}

void Map::clear_obstacles() {
    for (const Obstacle * o: _obstacles)
        delete o;
    _obstacles.clear();
}

const std::vector<const Feature *> Map::features_at(std::vector<unsigned long> indices) const {
    std::vector<const Feature *> ret; ret.reserve(indices.size());
    for (unsigned long ind : indices)
        ret.push_back(_features.at(ind));
    return ret;
}

const std::vector<const Feature *> Map::find_NN_features(const Point p, const unsigned long k, const double r) const {
    if (r < 0)
        return Map::find_fast_lined_KNN_features(p, k, *this);
    else
        return Map::find_fast_lined_NN_features(p, r, k, *this);
}

const std::vector<const Feature *> Map::find_naive_KNN_features(const Point p, const unsigned long k, const Map & map) {
    if ( k == 0) {
        throw std::invalid_argument("K must be positive");
    }
    
    std::vector<const Feature *> knn;

    for( int i = 0; i < map.features_count(); i++) {
        const Feature * feature = map._features.at(i);
        bool blocked = false;
        for (const Obstacle * obstacle: map._obstacles) {
            if (obstacle->intersects_line(Line(p, feature->pos))) {
                blocked = true;
                break;
            }
        }
        if (!blocked)
            knn.push_back(feature);
//        printf("Feature %d: dist = %f\n", i, (feature->pos - p).norm());
    }
    
    std::sort(knn.begin(), knn.end(), [&](const Feature * f1, const Feature * f2){
        Point p1 = f1->pos;
        Point p2 = f2->pos;
        return (p1 - p).norm() < (p2 - p).norm();
    });
    
    knn.resize(k);
    
    return knn;
}

const std::vector<const Feature *> Map::find_lined_KNN_features(const Point p, const unsigned long k, const Map & map) {
    if ( k == 0) {
        throw std::invalid_argument("K must be positive");
    }
    
    std::vector<const Feature *> knn;
    
    LineID nearest_line = Map::find_NN_line(p, map);
    std::vector<const Feature *> *features = map._line_id_to_features.at(nearest_line);
    
    for( int i = 0; i < features->size(); i++) {
        const Feature * const feature = features->at(i);
        bool blocked = false;
        for (const Obstacle * obstacle: map._obstacles) {
            if (obstacle->intersects_line(Line(p, feature->pos))) {
                blocked = true;
                break;
            }
        }
//        printf("Feature %d: dist = %f\n", i, (feature->pos - p).norm());
        if (!blocked)
            knn.push_back(feature);
    }
    
    std::sort(knn.begin(), knn.end(), [&](const Feature * f1, const Feature * f2){
        Point p1 = f1->pos;
        Point p2 = f2->pos;
        return (p1 - p).norm() < (p2 - p).norm();
    });
    
    knn.resize(k);
    return knn;
}

const std::vector<const Feature *> Map::find_fast_lined_KNN_features(const Point p, const unsigned long k, const Map &map) {
    
    if ( k == 0) {
        throw std::invalid_argument("K must be positive");
    }
    
    std::vector<const Feature *> knn;
    
    LineID nearest_line = Map::find_NN_line(p, map);
    std::vector<const Feature *> *features = map._line_id_to_features.at(nearest_line);
    
    unsigned long index = find_nearest_feature_index_to_projection(p, nearest_line, map);
    
//    printf("index = %d, size = %d", index, features->size());
    
    Point projection = features->at(index)->pos;
    unsigned long left = index;
    unsigned long right = index;
    while(right - left + 1 != k && (left != 0 || right != features->size() - 1) ) {
        long new_left = left - 1;
        long new_right = right + 1;
        double left_dist = new_left < 0 ? INFINITY : (projection - features->at(new_left)->pos).norm() ;
        double right_dist = new_right >= features->size() ? INFINITY : (projection - features->at(new_right)->pos).norm();
        
        if (left_dist < right_dist)
            left -= 1;
        else
            right += 1;
    }
    
    for (unsigned long i = left; i <= right; i++)
        knn.push_back(features->at(i));

    return knn;
}

const std::vector<const Feature *> Map::find_fast_lined_NN_features(const Point p, const double r, const unsigned long k_min, const Map &map) {
    
    if ( r <= 0 || k_min <= 0 )
        throw std::invalid_argument("Radius and k_min must be positive");
    
    std::vector<const Feature *> nns;
    
    LineID nearest_line = Map::find_NN_line(p, map);
    std::vector<const Feature *> *features = map._line_id_to_features.at(nearest_line);
    
    unsigned long index = find_nearest_feature_index_to_projection(p, nearest_line, map);
    
    //    printf("index = %d, size = %d", index, features->size());
    
    Point projection = features->at(index)->pos;
    unsigned long left = index;
    unsigned long right = index;
    while( left != 0 || right != features->size() - 1 ) {
        long new_left = left - 1;
        long new_right = right + 1;
        double left_dist = new_left < 0 ? INFINITY : (projection - features->at(new_left)->pos).norm() ;
        double right_dist = new_right >= features->size() ? INFINITY : (projection - features->at(new_right)->pos).norm();
        
        if (left_dist < right_dist)
        {
            if ( (features->at(new_left)->pos - features->at(right)->pos).norm() > r && right - new_left + 1 >= k_min )
                break;
            left = new_left;
        }
        else
        {
            if ( (features->at(left)->pos - features->at(new_right)->pos).norm() > r && new_right - left + 1 >= k_min )
                break;
            right = new_right;
        }
    
    }
    
    for (unsigned long i = left; i <= right; i++)
        nns.push_back(features->at(i));
    
    return nns;
}

const unsigned long Map::find_nearest_feature_index_to_projection(const Point p, const LineID lid, const Map &map) {
    Line l = _line_id_to_line(lid);
    std::vector<const Feature *>* features = map._line_id_to_features.at(lid);
    Point projection = l.projection(p);
    unsigned long index;
    if (l.projection_belongs(p))
        index = (unsigned long) ( (projection - l.a).norm() / l.length() * ( features->size() - 1 ) );
    else if ( (projection - l.a).norm() < (projection - l.b).norm() )
        index = 0;
    else
        index = features->size() - 1;
    
    for(;;) {
        long prev = index - 1;
        long next = index + 1;
        
        //        printf("prev = %d, next = %d size = %d\n", prev, next, features->size());
        
        double prev_dist = prev < 0 ? INFINITY : (projection - features->at(prev)->pos).norm() ;
        double next_dist = next >= features->size() ? INFINITY : (projection - features->at(next)->pos).norm();
        double dist = (projection - features->at(index)->pos).norm();
        
//        printf("prev_dist = %f, next_dist = %f dist = %f\n", prev_dist, next_dist, dist);
        
        if (dist <= prev_dist && dist <= next_dist)
            break;
        else if (prev_dist <= dist && prev_dist <= next_dist)
            index--;
        else if (next_dist <= dist && next_dist <= prev_dist)
            index++;
    }
    
    return index;
}


const std::vector<LineID> Map::find_KNN_lines(const Point p, const unsigned long k, const Map & map) {
    std::vector<LineID> knn; knn.reserve(k);
    
    for (std::map<LineID, std::vector<const  Feature *>*>::const_iterator it = map._line_id_to_features.begin(); it != map._line_id_to_features.end(); ++it) {
        const LineID lineId = it->first;
        const Line line = _line_id_to_line(lineId);
        bool blocked = false;
        for (const Obstacle * obstacle: map._obstacles) {
            if (obstacle->intersects_line(Line(p, line.a)) && obstacle->intersects_line(Line(p, line.b)) ) {
                blocked = true;
                break;
            }
        }
        if (!blocked)
            knn.push_back(lineId);
    }
    
    std::sort(knn.begin(), knn.end(), [&](LineID lid1, LineID lid2){
        return _line_id_to_line(lid1).dist(p) < _line_id_to_line(lid2).dist(p);
    });
    
    knn.resize(k);
    
    return knn;
}

const std:;string Map::find_NN_line(const Point p, const Map & map) {
    return Map::find_KNN_lines(p, 1, map).at(0);
}


