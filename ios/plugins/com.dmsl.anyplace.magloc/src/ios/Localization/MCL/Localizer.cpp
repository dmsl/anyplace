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

#include "Localizer.hpp"

using namespace std;

void Localizer::init(double particle_percentage) {
    this->init(particle_percentage, NormalDistribution(_random_generator), NormalDistribution(_random_generator), DEFAULT_K_FOR_KNN_MEASUREMENT, DEFAULT_K_FOR_KNN_CLUSTERING, DEFAULT_D_FOR_NN_CLUSTERING, DEFAULT_ALPHA_SLOW, DEFAULT_ALPHA_FAST);
}

void Localizer::init(double particle_percentage, double alpha_slow, double alpha_fast) {
    this->init(particle_percentage, NormalDistribution(_random_generator), NormalDistribution(_random_generator), DEFAULT_K_FOR_KNN_MEASUREMENT, DEFAULT_K_FOR_KNN_CLUSTERING, DEFAULT_D_FOR_NN_CLUSTERING, alpha_slow, alpha_fast);
}

void Localizer::init(double particle_percentage, Distribution motion_distribution, Distribution measurement_distribution, unsigned long k_for_knn_measurement, unsigned long k_for_knn_clustering, double r_for_nn_clustering, double alpha_slow, double alpha_fast) {
    
    if (alpha_slow < 0 || alpha_fast < 0)
        throw std::invalid_argument("Alpha is incorrect");
    
    const ULONG count = particle_percentage * milestones_count();
    if (particle_percentage < 0 || particle_percentage > 1.0 || count == 0)
        throw std::invalid_argument("Particle percentage incorrect");
    
    _k_for_knn_measurement = k_for_knn_measurement;
    _k_for_knn_clustering = k_for_knn_clustering;
    _r_for_nn_clustering = r_for_nn_clustering;
    _alpha_slow = alpha_slow;
    _alpha_fast = alpha_fast;
    
    particles_init_uniform(count);
    
    *_motion_distribution = motion_distribution;
    *_measurement_distribution = measurement_distribution;
    _w_slow = 0.0;
    _w_fast = 0.0;
}

void Localizer::particles_init_uniform(ULONG count) {
    std::uniform_int_distribution<ULONG> distribution(0, _map.features_count() - 1);
    clear_particles();
    clear_clusters();
    
    int clusterId = 0;
    _clusters[clusterId] = std::vector<Particle *>();
    double weight = 1.0 / count;
    for (int i = 0; i < count; i++)
    {
        //        printf("init_uniform %d begin\n", i);
        ULONG ind = distribution(_random_generator);
        //        printf("index = %lu\n", ind);
        std::vector<const Feature *> knn = _map.find_NN_features(_map.feature_at(ind)->pos, _k_for_knn_clustering, _r_for_nn_clustering);
        //        printf("init_uniform %d after knn\n", i);
        Particle* particle = new Particle{ _map.feature_at(ind)->pos , weight, knn, clusterId };
        _clusters[clusterId].push_back(particle);
        //        printf("init_uniform %d after new\n", i);
        //        if(particle->pos.x == 0.0 || particle->pos.y == 0.0)
        //            printf("hmm");
        _particles.push_back(particle);
    }
}

void Localizer::particles_sample_motion(const Vector2D motion, const double distance_variance, const double angle_variance_rads) {
    printf("Localizer::particles_sample_motion\n");
    
    if (distance_variance < 0)
        throw std::invalid_argument("Variance must be non-negative");
    if (angle_variance_rads < 0 || angle_variance_rads > M_PI)
        throw std::invalid_argument("Incorrect angle variance");
    
    for (Particle* particle : _particles)
    {
        double phi = _motion_distribution->next(0.0, angle_variance_rads);
        double dist = _motion_distribution->next(motion.norm(), distance_variance);
        Vector2D dr = motion.direction().rotate(phi)*dist;
        
        const Milestone * nn = particle->nns[0];
        bool particle_moves_away;
        
        Line nn_line = _map.get_line(nn->lineId);
        
        if ( (nn_line.a + dr > nn_line && particle->pos >= nn_line) ||
            ( nn_line.a + dr < nn_line && particle->pos <= nn_line ) )
            particle_moves_away = true;
        else particle_moves_away = false;
        
        
        
        if (particle_moves_away) {
            double dr_dir_line_dir_dot_product = dr.direction()*nn_line.direction();
            double a_max = M_PI / 3;
            double a_min = 0;
            double a = (a_min - a_max)*abs(dr_dir_line_dir_dot_product) + a_max;
            if ( dr_dir_line_dir_dot_product >= 0 ) {
                if ( nn_line.a + dr > nn_line )
                    a = -a;
            } else {
                if ( nn_line.a + dr < nn_line )
                    a = -a;
            }
            dr.rotate(a);
            //            printf("angle = %f\n", a*180.0/M_PI);
        } else {
            
        }
        
        particle->pos += dr;
    }
}


void Localizer::particles_sample_measurement(const Field & field, const double & distance_threshold, const bool resample, std::function<double (const Field &, const Milestone *)> similarity ) {
    printf("Localizer::particles_sample_measurement\n");
    
    if (distance_threshold <= 0)
        throw std::invalid_argument("Wrong distance_threshold");
    
    double weights[_particles.size()];
    int i = 0;
    unsigned long k_for_knn = max(_k_for_knn_clustering, _k_for_knn_measurement);
    for (Particle * particle : _particles)
    {
        std::vector<const Milestone *> knn = _map.find_NN_features(particle->pos, k_for_knn, _r_for_nn_clustering);
        particle->nns = knn;
        
        knn.resize(_k_for_knn_measurement);
        
        double weight = 0;
        assert(knn.size() != 0);
        //Weights are calculated as weighted average of probabilities with following coefficients
        //k_i = 1 - \frac{ r_i }{ \sum r_j }, i.e. nearest neighbours (milestones) contribute more
        double sum_r = 0;
        for (const Milestone * milestone : knn)
            sum_r += (milestone->pos - particle->pos).norm();
        double avg_r = sum_r / knn.size();
        for (const Milestone * milestone : knn) {
            double r = (milestone->pos - particle->pos).norm();
            double k = knn.size() == 1 ? 1.0 : 1.0 - r/sum_r;
            weight += similarity(field, milestone)*k;
        }
        
        weight /= 1.0 + avg_r / distance_threshold;
        
        weights[i++] = weight;
        particle->weight = weight;
    }
    
    //    for (int i = 0; i < _particles.size(); i++)
    //        printf("%f\n", weights[i]);
    if (resample)
        particles_importance_resample();
}

void Localizer::particles_sample_measurement_magnitude_based(const double magnitude, const double variance, const double distance_threshold, const bool resample) {
    std::function<double (const Field &, const Milestone *)> similarity = [&](const Field & measured_field, const Milestone * milestone) {
        double w_max = _measurement_distribution->pdf(milestone->field.norm(), milestone->field.norm(), variance);
        return _measurement_distribution->pdf(measured_field.norm(), milestone->field.norm(), variance) / w_max;
    };
    particles_sample_measurement(Field(magnitude, 0.0, 0.0), distance_threshold, resample, similarity);
}

void Localizer::particles_sample_measurement_component_based(const Field field, const double variance, const Quaternion sensor_attitude, const double distance_threshold, const bool resample) {
    
    if (variance < 0)
        throw std::invalid_argument("Incorrect component variance");
    
    std::function<double (const Field &, const Milestone *)> similarity = [&](const Field & measured_field, const Milestone * milestone) {
        
        Quaternion milestone_sensor_attitude = milestone->attitude;
        Field milestone_field = milestone->field;
        //Getting quaternion responsible for rotation from direction during tracking to direction during logging.
        //        Quaternion measurement_to_milestone = sensor_attitude * milestone_sensor_attitude.inverse();
        Quaternion measurement_to_milestone = milestone_sensor_attitude * sensor_attitude.inverse();
        measurement_to_milestone.normalize();
        
        //Correcting measured field direction to logging direction
        Field corrected_field = measurement_to_milestone.rotate(measured_field);
        printf("rotated_measured_field.norm = %f\n", field.norm());
        
        double w_x_max = _measurement_distribution->pdf(milestone_field.x, milestone_field.x, variance);
        double w_y_max = _measurement_distribution->pdf(milestone_field.y, milestone_field.y, variance);
        double w_z_max = _measurement_distribution->pdf(milestone_field.z, milestone_field.z, variance);
        
        double w_x = _measurement_distribution->pdf(corrected_field.x, milestone_field.x, variance) / w_x_max;
        double w_y = _measurement_distribution->pdf(corrected_field.y, milestone_field.y, variance) / w_y_max;
        double w_z = _measurement_distribution->pdf(corrected_field.z, milestone_field.z, variance) / w_z_max;
        
        double w = w_x * w_y * w_z;
        
        return w;
    };
    particles_sample_measurement(field, distance_threshold, resample, similarity);
}

void Localizer::particles_sample_measurement_angle_based(const Field field, const double magnitude_variance, const double angle_variance_rads, const Quaternion sensor_attitude, const double distance_threshold, const bool resample) {
    
    if (angle_variance_rads < 0 || angle_variance_rads >= M_PI)
        throw std::invalid_argument("Incorrect angle variance");
    if (magnitude_variance < 0)
        throw std::invalid_argument("Incorrect magnitude variance");
    
    std::function<double (const Field &, const Milestone *)> similarity = [&](const Field & measured_field, const Milestone * milestone) {
        
        
        Quaternion milestone_sensor_attitude = milestone->attitude;
        Field milestone_field = milestone->field;
        //Getting quaternion responsible for rotation from direction during tracking to direction during logging.
        Quaternion measurement_to_milestone = sensor_attitude * milestone_sensor_attitude.inverse();
        measurement_to_milestone.normalize();
        
        //Correcting measured field direction to logging direction
        Field corrected_field = measurement_to_milestone.rotate(measured_field);
        printf("measured_field\n" );
        measured_field.print();
        printf("corrected_measured_field\n");
        corrected_field.print();
        printf("milestone_field\n");
        milestone_field.print();
        
        double angle = acos(corrected_field.direction() * milestone_field.direction());
        
        printf("angle = %f\n", angle*180.0/M_PI);
        
        double w_magnitude_max = _measurement_distribution->pdf(milestone_field.norm(), milestone_field.norm(), magnitude_variance);
        double w_magnitude = _measurement_distribution->pdf(corrected_field.norm(), milestone_field.norm(), magnitude_variance) / w_magnitude_max;
        
        double w_angle_max = _measurement_distribution->pdf(0.0, 0.0, angle_variance_rads);
        double w_angle = _measurement_distribution->pdf(angle, 0.0, angle_variance_rads) / w_angle_max;
        
        printf("w_mag = %f, w_angle = %f\n", w_magnitude, w_angle);
        
        double w = w_magnitude * w_angle;
        
        return w;
    };
    particles_sample_measurement(field, distance_threshold, resample, similarity);
}

void Localizer::particles_importance_resample() {
    printf("Localizer::particles_importance_resample\n");
    
    std::vector<Particle *> resampled;
    
    unsigned long M = particles_count();
    
    double w_sum = 0;
    for (Particle * particle : _particles)
        w_sum += particle->weight;
    _w_avg = w_sum / M;
    _w_slow += _alpha_slow*(_w_avg - _w_slow);
    _w_fast += _alpha_fast*(_w_avg - _w_fast);
    printf("w_avg = %f, w_slow = %f, w_fast = %f\n", _w_avg, _w_slow, _w_fast);
    
    for (Particle * particle : _particles)
        particle->weight /= w_sum;
    
    
    std::uniform_real_distribution<double> random_add_distribution(0.0, 1.0);
    std::uniform_int_distribution<unsigned long> index_distribution(0, milestones_count() - 1);
    
    double r = std::uniform_real_distribution<double>(0, 1.0 / M)(_random_generator);
    double c = _particles.at(0)->weight;
    unsigned long i = 0;
    
    double probability_add_random = max(0.0, 1 - _w_fast/_w_slow);
    printf("probability_add_random = %f\n", probability_add_random);
    
    for (int m = 0; m < M; m++) {
        double u = r + (double)m / M;
        
        
        if ( random_add_distribution(_random_generator) <= probability_add_random ) {
            //Augmented MCL ( p.206 of Probabilistic Robotoics - http://www.probabilistic-robotics.org/ )
            
            unsigned long ind = index_distribution(_random_generator);
            const Milestone * milestone = _map.feature_at(ind);
            std::vector<const Milestone *> knn = _map.find_NN_features(milestone->pos, _k_for_knn_clustering, _r_for_nn_clustering);
            resampled.push_back(new Particle{ milestone->pos, 1.0 / M, knn, 0 });
            
        } else {
            //Low variance sampling (p.98 of Probabilistic Robotoics - http://www.probabilistic-robotics.org/ )
            while (u > c) {
                i++;
                c += _particles.at(i)->weight;
            }
            Particle * particle = _particles.at(i);
            resampled.push_back( new Particle{ particle->pos, 1.0 / M, particle->nns, particle->clusterId } );
        }
    }
    
    printf("resampled_count = %lu, particles_count = %lu\n", resampled.size(), particles_count());
    
    clear_particles();
    _particles.insert(_particles.end(), resampled.begin(), resampled.end());
}

Point Localizer::most_probable_position(std::function<int ()> most_probable_cluster, bool pull_to_nearest_milestone) const {
    assert(_particles.size() != 0);
    Point position = Point(0.0, 0.0);
    
    int cluster_id = most_probable_cluster();
    
    std::vector<Particle *> cluster = _clusters.at(cluster_id);
    for (Particle * particle: cluster)
        position += particle->pos;
    position /= cluster.size();
    
    if (pull_to_nearest_milestone)
        position = find_nearest_milestone(position)->pos;
    
    return position;
}

Point Localizer::most_probable_position_cluster_size_based(bool pull_to_nearest_milestone) const {
    std::function<int ())> most_probable_cluster = [&]() {
        int max_cluster_id;
        unsigned long max_cluster_size = 0;
        for (std::map<int, std::vector<Particle *>>::const_iterator it = _clusters.begin(); it != _clusters.end(); ++it) {
            unsigned long size = it->second.size();
            if ( size > max_cluster_size )
            {
                max_cluster_size = size;
                max_cluster_id = it->first;
            }
        }
        assert(max_cluster_size != 0);
        return max_cluster_id;
    };
    return most_probable_position(most_probable_cluster, pull_to_nearest_milestone);
}

Point Localizer::most_probable_position_cluster_size_proximity_based(bool pull_to_nearest_milestone) const {
    std::function<int ())> most_probable_cluster = [&]() {
        int max_cluster_id;
        double max_cluster_score = -1;
        std::map<int, ClusterProperties> properties = get_clusters_properties();
        for (std::map<int, ClusterProperties>::const_iterator it = properties.begin(); it != properties.end(); ++it) {
            double score = cluster_score_size_proximity(it->second);
            if ( score > max_cluster_score )
            {
                max_cluster_score = score;
                max_cluster_id = it->first;
            }
        }
        assert(max_cluster_score >= 0);
        return max_cluster_id;
    };
    return most_probable_position(most_probable_cluster, pull_to_nearest_milestone);
}

double Localizer::particles_to_nearest_milestones_average_distance() {
    double dist = 0;
    for (Particle * particle : _particles)
        dist += (particle->pos - particle->nns[0]->pos).norm();
    dist /= _particles.size();
    return dist;
}

void Localizer::particles_pull_to_nearest_milestones(double dist_threshold = 0) {
    if (dist_threshold < 0)
        throw std::invalid_argument("Distance threshold must be non-negative");
    
    for (Particle* particle : _particles)
        if ( (particle->pos - particle->nns[0]->pos).norm() > dist_threshold)
            particle->pos = particle->nns[0]->pos;
}

void Localizer::recalculate_nns() {
    for ( Particle * particle : _particles )
    {
        std::vector<const Milestone *> knn = _map.find_NN_features(particle->pos, _k_for_knn_clustering, _r_for_nn_clustering);
        assert( knn.size() != 0 );
        particle->nns = knn;
    }
}

void Localizer::run_clustering_DBSCAN(bool lined) {
    clear_clusters();
    
    auto cluster = [&](std::vector<Particle *> particles, int firstClusterId = 0) {
        using namespace clustering;
        unsigned long count = particles.size();
        DBSCAN::ClusterData cl_d(count, 2);
        for (int i = 0; i < count; i++) {
            Particle * particle = particles[i];
            cl_d(i, 0) = particle->pos.x;
            cl_d(i, 1) = particle->pos.y;
        }
        DBSCAN dbs(0.1, 20);
        dbs.fit(cl_d);
        DBSCAN::Labels labels = dbs.get_labels();
        
        std::set<int> clusters;
        for (int pid = 0; pid < labels.size(); pid++) {
            int clusterId = labels[pid] == -1 ? -1 : labels[pid] + firstClusterId;
            
            Particle * particle = particles[pid];
            particle->clusterId = clusterId;
            
            if (_clusters.count(clusterId) == 0)
                _clusters[clusterId] = std::vector<Particle *>();
            _clusters[clusterId].push_back(particle);
            
            clusters.insert(clusterId);
        }
        
        //        for (int id : clusters) {
        //            printf("clusterId = %d\n", id);
        //        }
        
        return clusters.size();
    };
    
    if (lined) {
        std::map<LineID, std::vector<Particle *>*> lines;
        
        for (Particle * particle : _particles) {
            
            std::vector<Particle *>* line = NULL;
            LineID lineId = particle->nns[0]->lineId;
            if ( lines.count(lineId) == 0 ) {
                line = new std::vector<Particle *>();
                lines[lineId] = line;
            }
            else
                line = lines.at(lineId);
            line->push_back(particle);

        }
        
        int clustersCount = 0;
        for (std::map<LineID, std::vector<Particle *>*>::iterator it = lines.begin(); it != lines.end(); ++it) {
            clustersCount += cluster(*it->second, clustersCount);
            delete it->second;
        }
    } else {
        cluster(_particles);
    }
}

void Localizer::run_clustering_KNN_milestones() {
    clear_clusters();
    
    auto cluster = [&] (std::vector<Particle *> particles, int first_cluster_id) {
        std::unordered_map<const Milestone *, std::vector<Particle *>*> map;
        
        for (Particle * particle : particles) {
            particle->clusterId = -1;
            for (const Milestone * nn : particle->nns) {
                std::vector<Particle *>* milestone_nns = NULL;
                if ( map.count(nn) == 0 ) {
                    milestone_nns  = new std::vector<Particle *>();
                    map[nn] = milestone_nns;
                }
                else
                    milestone_nns = map[nn];
                milestone_nns->push_back(particle);
            }
        }
        
        //        printf("first_cluster_id = %d\n", first_cluster_id);
        int clusterId = first_cluster_id;
        for (std::unordered_map<const Milestone *, std::vector<Particle *>*>::iterator it = map.begin(); it != map.end(); ++it) {
            std::vector<Particle *>* candidates = it->second;
            
            int commonClusterId = clusterId;
            bool all_without_cluster_id = true;
            for (Particle * candidate : * candidates)
                if ( candidate->clusterId != -1) {
                    commonClusterId = candidate->clusterId;
                    all_without_cluster_id = false;
                    break;
                }
            //            printf("all_without_cluster_id %d, clusterId = %d\n", all_without_cluster_id, commonClusterId);
            for (Particle * candidate : * candidates) {
                if(candidate->clusterId == -1) {
                    candidate->clusterId = commonClusterId;
                    if (_clusters.count(commonClusterId) == 0)
                    {
                        _clusters[commonClusterId] = std::vector<Particle *>();
                        //                        printf("_clusters.size() = %d, clusterId = %d\n", _clusters.size(), commonClusterId);
                    }
                    _clusters[commonClusterId].push_back(candidate);
                }
            }
            if (all_without_cluster_id) {
                clusterId++;
                //                printf("clusterId = %d\n", clusterId);
            }
            
            delete it->second;
        }
        
        //        printf("Return clusterId = %d\n", clusterId);
        return clusterId;
    };
    
    
    std::map<LineID, std::vector<Particle *>*> nn_lines;
    
    for (Particle * particle : _particles) {
        
        std::vector<Particle *>* line = NULL;
        LineID lineId = particle->nns[0]->lineId;
        if ( nn_lines.count(lineId) == 0 ) {
            line = new std::vector<Particle *>();
            nn_lines[lineId] = line;
        }
        else
            line = nn_lines.at(lineId);
        line->push_back(particle);
    }
    
    int clusterId = 0;
    for (std::map<LineID, std::vector<Particle *>*>::iterator it = nn_lines.begin(); it != nn_lines.end(); ++it) {
        clusterId = cluster(*it->second, clusterId);
        delete it->second;
    }
    
    
}

const std::vector<const Particle *> Localizer::get_particles() const {
    std::vector<const Particle *> ret;
    for (Particle* particle : _particles)
        ret.push_back(particle);
    return ret;
}

void Localizer::clear_particles() {
    for (Particle * particle : _particles)
        delete particle;
    _particles.clear();
}

void Localizer::clear_clusters() {
    _clusters.clear();
}

void Localizer::clear_distributions() {
    delete _measurement_distribution;
    delete _motion_distribution;
}



std::map<int, ClusterProperties> Localizer::get_clusters_properties() const {
    std::map<int, ClusterProperties> properties;
    for (std::map<int, std::vector<Particle *>>::const_iterator it = _clusters.begin(); it != _clusters.end(); ++it) {
        int clusterId = it->first;
        std::vector<Particle *> particles = it->second;
        double min_radius = 0;
        double max_radius = 0;
        double avg_radius = 0;
        double dev_radius = 0;
        unsigned long n = particles.size();
        Point center = {0.0, 0.0};
        for ( Particle * particle : particles )
            center += particle->pos / n;
        for ( Particle * particle : particles ) {
            double radius = (particle->pos - center).norm();
            if ( radius > max_radius )
                max_radius = radius;
            if (radius < min_radius)
                min_radius = radius;
            avg_radius += radius / n;
        }
        for ( Particle * particle : particles ) {
            double radius = (particle->pos - center).norm();
            dev_radius += pow(radius - avg_radius, 2.0);
        }
        dev_radius = sqrt(dev_radius / n);
        
        properties[clusterId] = ClusterProperties{ particles.size(), , min_radius, max_radius, avg_radius, dev_radius};
    }
    return properties;
}

void Localizer::particles_sample_measurement_magnitude_based(const double magnitude, const double variance, const double distance_threshold, const bool resample) {
    std::function<double (const Field &, const Milestone *)> similarity = [&](const Field & measured_field, const Milestone * milestone) {
        double w_max = _measurement_distribution->pdf(milestone->field.norm(), milestone->field.norm(), variance);
        return _measurement_distribution->pdf(measured_field.norm(), milestone->field.norm(), variance) / w_max;
    };
    particles_sample_measurement(Field(magnitude, 0.0, 0.0), distance_threshold, resample, similarity);
}

//May be it is better to come up with another score function. This one is not tested.
//Relative size of cluster (normalized to [0,1]) + deviation from the center (normalized to [0,1]) (if bulding is very narrow, then might have problems!)
double Localizer::cluster_score_size_proximity(const ClusterProperties & p) const {
    
    double cluster_score_size = (double)p.size / _particles.size();
    
    //Need to normalize proximity score. Largest penalty for largest cluster, i.e. whole map.
    //1.0 / [ 1.0 + Avg. cluster radius * (Diameter / Max Map Dimension) ]
    //Larger cluster -> smaller score, more scattered cluster -> smaller score
    Size map_size = _map.get_size();
    double normalizer = 2.0 * p.max_radius / max(map_size.width, map_size.height) ;
    double cluster_score_proximity = 1.0 / (1.0 + p.avg_radius * normalizer);
    
    return cluster_score_size * cluster_score_proximity;
}

//To compare scores between different maps.
//1.0 / [ 1.0 + sum_i(1 - score_i) ]
//More clusters -> worse
double Localizer::localization_score(std::function<double (const ClusterProperties &)> cluster_score) const {
    double score = 0.0;
    std::map<int, ClusterProperties> properties = get_clusters_properties();
    for (std::map<int, ClusterProperties>::const_iterator it = properties.begin(); it != properties.end(); ++it)
        score += (1.0 - cluster_score(it->second));
    score = 1.0 / ( 1.0 + score );
    return score;
}

double Localizer::localization_score_size_proximity() const {
    std::function<double (const ClusterProperties &)> cluster_score = [&](const ClusterProperties & p) {
        return cluster_score_size_proximity(p);
    };
    return localization_score(cluster_score);
}


Localizer::~Localizer()
{
    clear_particles();
    clear_distributions();
}


//
//Multiple Map Extension
//

void LocalizerMultipleMap::set_map(const std::vector<Milestone> milestones, Size map_size, int id) {
    if (_localizers.count(id) != 0) {
        delete _localizers[id];
        _localizers.erase(id);
    }
    _localizers[id] = new Localizer(milestones, map_size);
}

void LocalizerMultipleMap::remove_map(int id) {
    if (_localizers.count(id) != 0)
        delete _localizers[id];
    _localizers.erase(id);
}

void LocalizerMultipleMap::clear() {
    for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
        delete it->second;
    }
    _localizers.clear();
}

LocalizerMultipleMap::~LocalizerMultipleMap() {
    clear();
}

const int LocalizerMultipleMap::map_id_by_best_localization_density_score() {
    double max_score = 0;
    int max_ind = NAN;
    for (std::map<int, Localizer *>::const_iterator it = _localizers.begin(); it != _localizers.end(); ++it) {
        double score = it->second->calculate_localization_density_score();
        if (score > max_score) {
            max_score = score;
            max_ind = it->first;
        }
    }
    return max_ind;
}


