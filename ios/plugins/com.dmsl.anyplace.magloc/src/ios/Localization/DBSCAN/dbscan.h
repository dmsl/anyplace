#include <vector>
#include <boost/numeric/ublas/matrix.hpp>

using namespace boost::numeric;

namespace clustering
{
	class DBSCAN
	{
	public:
		typedef ublas::vector<double> FeaturesWeights;
		typedef ublas::matrix<double> ClusterData;
		typedef ublas::matrix<double> DistanceMatrix;
		typedef std::vector<uint32_t> Neighbors;
		typedef std::vector<int32_t> Labels;

		static ClusterData gen_cluster_data( size_t features_num, size_t elements_num );
		static FeaturesWeights std_weights( size_t s );

		DBSCAN(double eps, size_t min_elems/*, int num_threads=1*/);
		DBSCAN();
		~DBSCAN();

		void init(double eps, size_t min_elems/*, int num_threads=1*/);
		void fit( const ClusterData & C );
		void fit_precomputed( const DistanceMatrix & D );
		void wfit( const ClusterData & C, const FeaturesWeights & W );
		void reset();

		const Labels & get_labels() const;
	
	private:

		void prepare_labels( size_t s );
		const DistanceMatrix calc_dist_matrix( const ClusterData & C, const FeaturesWeights & W );
		Neighbors find_neighbors(const DistanceMatrix & D, uint32_t pid);
		void dbscan( const DistanceMatrix & dm );

		double m_eps;
		size_t m_min_elems;
		//int m_num_threads;
		double m_dmin;
		double m_dmax;

		Labels m_labels;
	};

	std::ostream& operator<<(std::ostream& o, DBSCAN & d);
}