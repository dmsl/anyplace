
#include "MagneticMCL.h"
#include "Localizer.hpp"

LocalizerMultipleMap localizer;

void mcl_init() {
    localizer.clear();
}

void mcl_add_map(const void* input, unsigned int count, int floor) {
    printf("mcl_init_localization: count = %d\n", count);
    Milestone* milestones = (Milestone*) input;
	std::vector < Milestone > vec = std::vector<Milestone>();
	for (int i = 0; i < count; i++)
		vec.push_back(milestones[i]);
    localizer.set_map(vec, floor);
}

void mcl_remove_map(int floor) {
    localizer.remove_map(floor);
}

void mcl_start(double fraction, double alpha_slow, double alpha_fast) {
    printf("mcl_start_localizing\n");
	localizer.init(fraction, alpha_slow, alpha_fast);
}

void mcl_move(double dx, double dy, double distance_variance, double angle_variance_rads) {
    printf("mcl_move\n");
	localizer.particles_sample_motion(Vector2D(dx, dy), distance_variance, angle_variance_rads);
}

void mcl_measure_magnitude_based(double f, double variance, double distance_threshold, bool resample = false) {
    printf("mcl_measure_magnitude_based\n");
    localizer.particles_sample_measurement_magnitude_based(f, variance, distance_threshold, resample);
}

void mcl_measure_component_based(double f_x, double f_y, double f_z, double w, double x, double y, double z, double variance, double distance_threshold, bool resample = false) {
    printf("mcl_measure_component_based\n");
    localizer.particles_sample_measurement_component_based(Field(f_x,f_y,f_z), variance, Quaternion(w, x, y, z), distance_threshold, resample);
}

void mcl_measure_angle_based(double f_x, double f_y, double f_z, double w, double x, double y, double z, double magnitude_variance, double angle_variance_rads, double distance_threshold, bool resample = false) {
    printf("mcl_measure_component_based\n");
    localizer.particles_sample_measurement_angle_based(Field(f_x,f_y,f_z), magnitude_variance, angle_variance_rads, Quaternion(w, x, y, z), distance_threshold, resample);
}

void mcl_pull_particles_to_nearest_milestones(double dist_threshold) {
    printf("mcl_pull\n");
    localizer.particles_pull_to_nearest_milestones(dist_threshold);
}

unsigned long mcl_particles_count(int floor) {
    return localizer.particles_count(floor);
}

void mcl_particles(void *buf, int floor) {
    printf("mcl_particles\n");
    struct OutStruct {
        Point pos;
        double weight;
        Point nn;
        //Why double? Because else is not working! Swift's sizeof cosiders 44 bytes if Int, but in memory aligns as 48 bytes => getting garbage on return in swift. Need to play with alignment properties of C++ compiler, so it will align struct to multiple of 8 always, or keep it such manually.
        double clusterId;
    };
//    printf("sizeof outstruct = %d\n", sizeof(OutStruct));
    OutStruct * particles = (OutStruct *) buf;
    const std::vector<const Particle *> vec = localizer.get_particles(floor);
//    printf("vecsize = %lu\n",vec.size());
    for (int i = 0; i < vec.size(); i++) {
        const Particle * p = vec.at(i);
//        printf("dist = %.2f\n", (p->nn->pos - p->pos).magnitude());
        particles[i] = OutStruct{ p->pos, p->weight, p->nns[0]->pos, (double)p->clusterId};
    }
    
}

void mcl_closest_line(void *input, void *output, int floor) {
    struct InStruct {
        Point p;
    };
    struct OutStruct {
        Point a;
        Point b;
    };
    InStruct * point = (InStruct*) input;
    OutStruct * line = (OutStruct*) output;
    
    Line l = localizer.find_nearest_line(point->p, floor);
    *line = OutStruct{ l.a, l.b };
}

void mcl_closest_point(void *input, void *output, int floor) {
    Point * in = (Point*) input;
    Point * out = (Point*) output;
    
    Point p = localizer.find_nearest_point(*in, floor);
    *out = p;
}

void mcl_closest_milestone(void *input, void *output, int floor) {
    Point * in = (Point*) input;
    Milestone * out = (Milestone*) output;
    
    const Milestone * m = localizer.find_nearest_milestone(*in, floor);
    *out = *m;
}

unsigned int mcl_clusters_count(int floor) { return localizer.clusters_count(floor); }

void mcl_cluster_sizes(void *output, int output_size, int floor) {
    struct ClusterInfo {
        int id;
        unsigned int count;
    };
    ClusterInfo * infos = (ClusterInfo *) output;
    
    if (output_size < sizeof(ClusterInfo)*localizer.clusters_count(floor) )
        throw std::invalid_argument("Buffer is too small");
    
    std::map<int, std::vector<Particle *>> clusters = localizer.get_clusters(floor);
    
    int i = 0;
    for (std::map<int, std::vector<Particle *>>::const_iterator it = clusters.begin(); it != clusters.end(); ++it) {
        int id = it->first;
        unsigned int size = (unsigned int) it->second.size();
        infos[i++] = ClusterInfo{ id, size };
    }
}

void mcl_most_probable_position(void *output, int size, bool pull_to_nearest_milestone, int floor) {
    printf("mcl_most_probable_position");
    if (size < sizeof(Point))
        std::invalid_argument("Buffer is too small");
    Point *p = (Point *) output;
    *p = localizer.most_probable_position(floor, pull_to_nearest_milestone);
}

void mcl_test(void *buf) {
    struct Out {
        int i1;
        Point p1;
        double d;
        Point p2;
        
        /*int i2;*/
    };
    printf("c++: sizeof = %lu\n", sizeof(Out));
    Out * outs = (Out *) buf;
    
    for (int i = 0; i < 3; i++) {
        printf("sizeof = %lu\n", sizeof(Out{i, Point{(double)i, (double)i}, (double)i, Point{(double)i, (double)i}/*, i */}));
        outs[i] = Out{i, Point{(double)i, (double)i}, (double)i, Point{(double)i, (double)i}/*, i*/ };
        printf("c++: %d (%f %f) %f (%f %f)\n", outs[i].i1, outs[i].p1.x, outs[i].p1.y, outs[i].d, outs[i].p2.x, outs[i].p2.y  /*, outs[i].i2*/);
    }
}
