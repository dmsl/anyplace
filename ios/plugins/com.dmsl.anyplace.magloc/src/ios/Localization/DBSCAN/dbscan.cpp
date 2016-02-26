#include <iostream>
#include <boost/numeric/ublas/matrix.hpp>
#include <boost/numeric/ublas/matrix_proxy.hpp>
#include <boost/numeric/ublas/io.hpp>
#include <boost/algorithm/minmax.hpp>
#include <vector>

#include "dbscan.h"

namespace clustering
{
	DBSCAN::ClusterData DBSCAN::gen_cluster_data( size_t features_num, size_t elements_num )
	{
		DBSCAN::ClusterData cl_d( elements_num, features_num );

		for (size_t i = 0; i < elements_num; ++i)
		{
			for (size_t j = 0; j < features_num; ++j)	
			{
				cl_d(i, j) = (-1.0 + rand() * (2.0) / RAND_MAX);
			}
		}

		return cl_d;
	}

	DBSCAN::FeaturesWeights DBSCAN::std_weights( size_t s )
	{
		// num cols
		DBSCAN::FeaturesWeights ws( s );

		for (size_t i = 0; i < s; ++i)
		{
			ws(i) = 1.0;
		}

		return ws;
	}

	DBSCAN::DBSCAN()
	{

	}

	void DBSCAN::init(double eps, size_t min_elems/*, int num_threads*/)
	{
		m_eps = eps;
		m_min_elems = min_elems;
	}

	DBSCAN::DBSCAN(double eps, size_t min_elems/*, int num_threads*/)
	: m_eps( eps )
	, m_min_elems( min_elems )
	, m_dmin(0.0)
	, m_dmax(0.0)
	{
		reset();
	}

	DBSCAN::~DBSCAN()
	{

	}

	void DBSCAN::reset()
	{
		m_labels.clear();
	}

	void DBSCAN::prepare_labels( size_t s )
	{
		m_labels.resize(s);

		for( auto & l : m_labels)
		{
			l = -1;
		}
	}

	const DBSCAN::DistanceMatrix DBSCAN::calc_dist_matrix( const DBSCAN::ClusterData & C, const DBSCAN::FeaturesWeights & W )
	{
		DBSCAN::ClusterData cl_d = C;

		for (size_t i = 0; i < cl_d.size2(); ++i)
		{
			ublas::matrix_column<DBSCAN::ClusterData>col(cl_d, i);

			const auto r = minmax_element( col.begin(), col.end() );

			double data_min = *r.first;
			double data_range = *r.second - *r.first;

			if (data_range == 0.0) { data_range = 1.0; }

			const double scale = 1/data_range;
			const double min = -1.0*data_min*scale;

			col *= scale;
			col.plus_assign( ublas::scalar_vector< typename ublas::matrix_column<DBSCAN::ClusterData>::value_type >(col.size(), min) );
		}

		// rows x rows
		DBSCAN::DistanceMatrix d_m( cl_d.size1(), cl_d.size1() );
		ublas::vector<double> d_max( cl_d.size1() );
		ublas::vector<double> d_min( cl_d.size1() );

		for (size_t i = 0; i < cl_d.size1(); ++i)
		{
			for (size_t j = i; j < cl_d.size1(); ++j)	
			{
				d_m(i, j) = 0.0;

				if (i != j)
				{
					ublas::matrix_row<DBSCAN::ClusterData> U (cl_d, i);
					ublas::matrix_row<DBSCAN::ClusterData> V (cl_d, j);

					int k = 0;
					for (const auto e : ( U-V ) )
					{
						d_m(i, j) += fabs(e)*W[k++];
					}

					d_m(j, i) = d_m(i, j);
				}
			}

			const auto cur_row = ublas::matrix_row<DBSCAN::DistanceMatrix>(d_m, i);
			const auto mm = minmax_element( cur_row.begin(), cur_row.end() );

			d_max(i) = *mm.second;
			d_min(i) = *mm.first;
		}

		m_dmin = *(min_element( d_min.begin(), d_min.end() ));
		m_dmax = *(max_element( d_max.begin(), d_max.end() ));

		m_eps = (m_dmax - m_dmin) * m_eps + m_dmin;

		return d_m;
	}

	DBSCAN::Neighbors DBSCAN::find_neighbors(const DBSCAN::DistanceMatrix & D, uint32_t pid)
	{
		Neighbors ne;

		for (uint32_t j = 0; j < D.size1(); ++j)
		{
			if 	( D(pid, j) <= m_eps )
			{
				ne.push_back(j);
			}
		}
		return ne;
	}

	void DBSCAN::dbscan( const DBSCAN::DistanceMatrix & dm )
	{
		std::vector<uint8_t> visited( dm.size1() );

		uint32_t cluster_id = 0;

		for (uint32_t pid = 0; pid < dm.size1(); ++pid)
		{
			if ( !visited[pid] )
			{  
				visited[pid] = 1;

				Neighbors ne = find_neighbors(dm, pid );

				if (ne.size() >= m_min_elems)
				{
					m_labels[pid] = cluster_id;

					for (uint32_t i = 0; i < ne.size(); ++i)
					{
						uint32_t nPid = ne[i];

						if ( !visited[nPid] )
						{
							visited[nPid] = 1;

							Neighbors ne1 = find_neighbors(dm, nPid);

							if ( ne1.size() >= m_min_elems )
							{
								for (const auto & n1 : ne1)
								{
									ne.push_back(n1);
								}
							}
						}

						if ( m_labels[nPid] == -1 )
						{
							m_labels[nPid] = cluster_id;
						}
					}

					++cluster_id;
				}
			}
		}
	}

	void DBSCAN::fit( const DBSCAN::ClusterData & C ) 
	{
		const DBSCAN::FeaturesWeights W = DBSCAN::std_weights( C.size2() );
		wfit( C, W );
	}
	void DBSCAN::fit_precomputed( const DBSCAN::DistanceMatrix & D ) 
	{
		prepare_labels( D.size1() );
		dbscan( D );
	}

	void DBSCAN::wfit( const DBSCAN::ClusterData & C, const DBSCAN::FeaturesWeights & W )
	{
		prepare_labels( C.size1() );
		const DBSCAN::DistanceMatrix D = calc_dist_matrix( C, W );
		dbscan( D );
	}

	const DBSCAN::Labels & DBSCAN::get_labels() const
	{
		return m_labels;
	}

	std::ostream& operator<<(std::ostream& o, DBSCAN & d)
	{
		o << "[ ";
		for ( const auto & l : d.get_labels() )
		{
			o << " " << l;
		}
		o << " ] " << std::endl;

		return o;
	}
}
