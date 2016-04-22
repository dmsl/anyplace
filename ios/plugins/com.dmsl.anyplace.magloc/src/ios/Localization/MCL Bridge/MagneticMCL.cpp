
#include "MagneticMCL.h"
/*#include "Localizer.hpp"

LocalizerMultipleMap localizer;

void mcl_init() {
    localizer.clear();
}

void mcl_add_map(const void* input, size_t size, double width, double height, int floor) {
    printf("mcl_init_localization: count = %d\n", count);
    Milestone* milestones = (Milestone*) input;
    assert(size % sizeof(Milestone) = 0);
    unsigned int count = size / sizeof(Milestone);
	std::vector < Milestone > vec = std::vector<Milestone>();
	for (int i = 0; i < count; i++)
		vec.push_back(milestones[i]);
    localizer.set_map(vec, Size(width, height), floor);
}

void mcl_remove_map(int floor) {
    localizer.remove_map(floor);
}

void mcl_start(double fraction, double alpha_slow, double alpha_fast) {
    assert(fraction <= 1.0 & fraction >= 0);
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

void mcl_measure_component_based(Field f, Quaternion q, double variance, double distance_threshold, bool resample = false) {
    printf("mcl_measure_component_based\n");
    localizer.particles_sample_measurement_component_based(f, variance, q, distance_threshold, resample);
}

void mcl_measure_angle_based(Field f, Quaternion q, double magnitude_variance, double angle_variance_rads, double distance_threshold, bool resample = false) {
    printf("mcl_measure_component_based\n");
    localizer.particles_sample_measurement_angle_based(f, magnitude_variance, angle_variance_rads, q, distance_threshold, resample);
}

void mcl_pull_particles_to_nearest_milestones(double dist_threshold) {
    printf("mcl_pull\n");
    localizer.particles_pull_to_nearest_milestones(dist_threshold);
}

unsigned long mcl_particles_count(int floor) {
    return localizer.particles_count(floor);
}

void mcl_particles(void *buf, size_t size, int floor) {
    printf("mcl_particles\n");
    struct OutStruct {
        Point pos;
        double weight;
        Point nn;
        //Why double? Because else is not working! Swift's sizeof cosiders 44 bytes if Int, but in memory aligns as 48 bytes => getting garbage on return in swift. Need to play with alignment properties of C++ compiler, so it will align struct to multiple of 8 always, or keep it such manually.
        // Added __attribute__ keyword to fx alignment, however, it can be an issue on another platorm, i.e. 32-bit
        int clusterId;
    } __attribute__ ((aligned(8)));
//    printf("sizeof outstruct = %d\n", sizeof(OutStruct));
    OutStruct * particles = (OutStruct *) buf;
    const std::vector<const Particle *> vec = localizer.get_particles(floor);
//    printf("vecsize = %lu\n",vec.size());
    assert(size >= vec.size()*sizeof(struct OutStruct));

    for (int i = 0; i < vec.size(); i++) {
        const Particle * p = vec.at(i);
//        printf("dist = %.2f\n", (p->nn->pos - p->pos).magnitude());
        particles[i] = OutStruct{ p->pos, p->weight, p->nns[0]->pos, p->clusterId};
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

void mcl_cluster_sizes(void *output, unsigned int output_size, int floor) {
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

void mcl_most_probable_position(void *output, size_t size, bool pull_to_nearest_milestone, int floor) {
    printf("mcl_most_probable_position");
    if (size != sizeof(Point))
        std::invalid_argument("Wrong buffer size");
    Point *p = (Point *) output;
    *p = localizer.most_probable_position_cluster_size_proximity_based(floor, pull_to_nearest_milestone);
}

void mcl_test(void *buf) {
    struct Out {
        int i1;
        Point p1;
        double d;
        Point p2;
 
        //int i2;
    };
    printf("c++: sizeof = %lu\n", sizeof(Out));
    Out * outs = (Out *) buf;
    
    for (int i = 0; i < 3; i++) {
        printf("sizeof = %lu\n", sizeof(Out{i, Point{(double)i, (double)i}, (double)i, Point{(double)i, (double)i}}));
        outs[i] = Out{i, Point{(double)i, (double)i}, (double)i, Point{(double)i, (double)i} };
        printf("c++: %d (%f %f) %f (%f %f)\n", outs[i].i1, outs[i].p1.x, outs[i].p1.y, outs[i].d, outs[i].p2.x, outs[i].p2.y);
    }
}
*/
int mcl_test_ret(int i) {
    return i + 1;
}
