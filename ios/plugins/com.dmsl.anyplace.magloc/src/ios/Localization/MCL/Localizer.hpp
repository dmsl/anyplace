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

#pragma once

#include "stdafx.hpp"
#include "Distributions.hpp"
#include "Map.hpp"
#include "dbscan.h"

using namespace std;

/**
 * Localizer - represents an algorithm for localization on the 2D flor map with magnetic fingerprints.
 * Central probabilistic algorithm used is Monte Carlo (or Particle Filter) localization. 
 * Details on the theory can be found here http://www.probabilistic-robotics.org
 */
class Localizer
{
private:
    
    /**
     * Constants used for localization algorithm. Should be tuned up.
     * Picked in reasonable range, but random now. Proper investigation must be conducted.
     * ALPHA_FAST, ALPHA_SLOW originates from MCL directly, details can be found here http://www.probabilistic-robotics.org
     */
    const unsigned long DEFAULT_K_FOR_KNN_MEASUREMENT = 1;
    const unsigned long DEFAULT_K_FOR_KNN_CLUSTERING = 10;
    const double DEFAULT_D_FOR_NN_CLUSTERING = 5.0;
    const double DEFAULT_D_FOR_NN_PULL = 2.0;
    const double DEFAULT_ALPHA_FAST = 0.2;
    const double DEFAULT_ALPHA_SLOW = 0.05;
    
    std::default_random_engine _random_generator = std::default_random_engine((unsigned int)time(0));
    
    /**
     * _particles - list of active particles
     * _clusters - map of clusters in which particles are united. Used for estimation of the real position.
     */
    std::vector<Particle *> _particles;
    std::map<int, std::vector<Particle *>> _clusters;
    Map _map;
    
    unsigned long _k_for_knn_measurement;
    unsigned long _k_for_knn_clustering;
    double _r_for_nn_clustering;
    double _alpha_slow = 0;
    double _alpha_fast = 0;
    double _w_fast = 0;
    double _w_slow = 0;
    double _w_avg = 0;

    Distribution* _motion_distribution = new NormalDistribution();
    Distribution* _measurement_distribution = new NormalDistribution();
    
	void particles_init_uniform(unsigned long count);
    
    
    void clear_particles();
    void clear_clusters();
    void clear_distributions();
    
public:
    Localizer(const std::vector<Milestone> & milestones, Size map_size) {
        std::vector<Feature *> features;
        for (Milestone milestone : milestones) {
            features.push_back((Feature *) new Milestone(milestone));
        }
        _map.set_size(map_size);
        _map.add_features(features);
        printf("Localizer(): feautures = %lu, obstacles = %lu\n", _map.features_count(), _map.obstacles_count());
	};

    const unsigned long particles_count() const { return _particles.size(); }
    const unsigned long milestones_count() const { return _map.features_count(); }
    const std::vector<const Particle *> get_particles() const;
    
    
    void init(double particle_percentage);
    void init(double particle_percentage, double alpha_slow, double alpha_fast);
	void init(double particle_percentage, Distribution motion_axis_distribution, Distribution measurement_magnitude_distribution, unsigned long k_for_knn_measurement, unsigned long k_for_knn_clustering, double r_for_nn_clustering, double alpha_slow, double alpha_fast);
	void particles_sample_motion(const Vector2D motion, const double distance_variance, const double angle_variance);
    
private:
    void particles_sample_measurement(const Field & field, const double & distance_threshold, const bool resample, std::function<double (const Field &, const Milestone *)> similarity);
    
    
    double localization_score(std::function<double (const ClusterProperties &)> cluster_score) const;
    double localization_score_cluster_size_proximity) const;

    Point most_probable_position(std::function<int ()> most_probable_cluster, bool pull_to_nearest_milestone = false) const;
    
protected:
    double cluster_score_size_proximity(const ClusterProperties &) const;
    
public:
    void particles_sample_measurement_magnitude_based(const double field_magnitude, const double magnitude_variance, const double distance_threshold, const bool resample);
    void particles_sample_measurement_component_based(const Field field, const double component_variance, const Quaternion attitude, const double distance_threshold, const bool resample);
    void particles_sample_measurement_angle_based(const Field field, const double magnitude_variance, const double angle_variance_rads, const Quaternion attitude, const double distance_threshold, const bool resample);
	void particles_importance_resample();
    Point most_probable_position(bool pull_to_nearest_milestone = false) const;
    
    void particles_pull_to_nearest_milestones(double distance_threshold);
    double particles_to_nearest_milestones_average_distance();
    void recalculate_nns();
    
    Line find_nearest_line(Point p) const { return _map.get_line(Map::find_NN_line(p, _map)); }
    Point find_nearest_point(Point p) const { return _map.find_NN_features(p, 1).at(0)->pos; }
    const Milestone * find_nearest_milestone(Point p) const { return (Milestone *) _map.find_NN_features(p, 1).at(0); }
    
    void run_clustering_DBSCAN(bool lined);
    void run_clustering_KNN_milestones();
    
    unsigned int clusters_count() const { return _clusters.size(); }
    std::map<int, std::vector<Particle *>> get_clusters() const { return _clusters; }
    std::map<int, ClusterProperties> get_clusters_properties() const;
    
    enum LocalizerAccruacy{ LOW, MEDIUM, HIGH; }
    int localization_accuracy() const;
    
    Point most_probable_position_cluster_size_based(bool pull_to_nearest_milestone = false) const;
    Point most_probable_position_cluster_size_proximity_based(bool pull_to_nearest_milestone = false) const;
    
    ~Localizer();
};

class LocalizerMultipleMap {
private:
    std::map<int, Localizer *> _localizers;

public:
    
    void init(double particle_percentage) {
        for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
            it->second->init(particle_percentage);
        }
    }
    
    void init(double particle_percentage, double alpha_slow, double alpha_fast) {
        for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
            it->second->init(particle_percentage, alpha_slow, alpha_fast);
        }
    }
    
    void init(double particle_percentage, Distribution motion_axis_distribution, Distribution measurement_magnitude_distribution, unsigned long k_for_knn_measurement, unsigned long k_for_knn_clustering, double r_for_nn_clustering, double alpha_slow, double alpha_fast) {
        for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
            it->second->init(particle_percentage, motion_axis_distribution, measurement_magnitude_distribution, k_for_knn_measurement, k_for_knn_clustering, r_for_nn_clustering, alpha_slow, alpha_fast);
        }

    }
    
    void particles_sample_motion(const Vector2D motion, const double distance_variance, const double angle_variance) {
        for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
            it->second->particles_sample_motion(motion, distance_variance, angle_variance);
        }
    }
    
    void particles_sample_measurement_magnitude_based(const double field_magnitude, const double magnitude_variance, const double distance_threshold, const bool resample) {
        for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
            it->second->particles_sample_measurement_magnitude_based(field_magnitude, magnitude_variance, distance_threshold, resample);
        }
    }
    
    void particles_sample_measurement_component_based(const Field field, const double component_variance, const Quaternion attitude, const double distance_threshold, const bool resample) {
        for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
            it->second->particles_sample_measurement_component_based(field, component_variance, attitude, distance_threshold, resample);
        }
    }
    
    void particles_sample_measurement_angle_based(const Field field, const double magnitude_variance, const double angle_variance_rads, const Quaternion attitude, const double distance_threshold, const bool resample) {
        for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
            it->second->particles_sample_measurement_angle_based(field, magnitude_variance, angle_variance_rads, attitude, distance_threshold, resample);
        }
    }
    
    void particles_pull_to_nearest_milestones(double distance_threshold) {
        for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
            it->second->particles_pull_to_nearest_milestones(distance_threshold);
        }
    }
    
    Point most_probable_position_cluster_size_based(int id, bool pull_to_nearest_milestone = false) const {
        return _localizers.at(id)->most_probable_position_cluster_size_based(pull_to_nearest_milestone);
    }
    
    Point most_probable_position_cluster_size_proximity_based(int id, bool pull_to_nearest_milestone = false) const {
        return _localizers.at(id)->most_probable_position_cluster_size_proximity_based(pull_to_nearest_milestone);
    }
    
    //
    //Need to add multiple map most_probable_position support. It can be done using
    //localization_score functions in Localizer class
    //
    
    
    const unsigned long particles_count(int id) const {
        return _localizers.at(id)->particles_count();
    }
    
    const std::vector<const Particle *> get_particles(int id) const {
        return _localizers.at(id)->get_particles();
    }
    
    Line find_nearest_line(Point p, int id) const {
        return _localizers.at(id)->find_nearest_line(p);
    }
    Point find_nearest_point(Point p, int id) const {
        return _localizers.at(id)->find_nearest_point(p);
    }
    const Milestone * find_nearest_milestone(Point p, int id) const {
        return _localizers.at(id)->find_nearest_milestone(p);
    }

    const unsigned int clusters_count(int id) const {
        return _localizers.at(id)->clusters_count();
    }
    std::map<int, std::vector<Particle *>> get_clusters(int id) const {
        return _localizers.at(id)->get_clusters();
    }
    std::map<int, ClusterProperties> get_clusters_properties(int id) const {
        return _localizers.at(id)->get_clusters_properties();
    }
    
    void set_map(const std::vector<Milestone> milestones, Size map_size, int id);
    void remove_map(int id);
    void clear();
    const int map_id_by_best_localization_density_score();
    ~LocalizerMultipleMap();

};

