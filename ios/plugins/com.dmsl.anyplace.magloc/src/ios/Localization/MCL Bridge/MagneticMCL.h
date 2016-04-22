
#ifndef MagneticMCL_h
#define MagneticMCL_h

#ifdef __cplusplus
extern "C" {
#endif
    
/*void mcl_init();
void mcl_add_map(const void* input, size_t size, double width, double height, int floor);
void mcl_remove_map(int floor);
    
void mcl_start(double fraction = 0.5, double alpha_slow = 0.05, double alpha_fast = 0.4);
void mcl_move(double dx, double dy, double distance_variance, double angle_variance_rads);
void mcl_measure_magnitude_based(double f, double variance, double distance_threshold, bool resample);
void mcl_measure_component_based(Field f, Quaternion q, double variance, double distance_threshold, bool resample);
void mcl_measure_angle_based(Field f, Quaternion q, double magnitude_variance, double angle_variance_rads, double dist_threshold, bool resample);
void mcl_pull_particles_to_nearest_milestones(double disance_threshold);

unsigned long mcl_particles_count(int floor);
void mcl_particles(void *buf, size_t size, int floor);
    
void mcl_closest_line(void *input, void *output, int floor);
void mcl_closest_point(void *input, void *output, int floor);
void mcl_closest_milestone(void *input, void *output, int floor);
        
unsigned int mcl_clusters_count(int floor);
void mcl_cluster_sizes(void *output, unsigned int output_size, int floor);

void mcl_most_probable_position(void *output, size_t size, bool pull_to_nearest_milestone, int floor);
    
void mcl_test(void *buf);*/
int mcl_test_ret(int i);
    
#ifdef __cplusplus
}
#endif


#endif /* Magnetic_MCL_h */
