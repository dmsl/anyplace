/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */

package utils;

import play.Logger;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;

public class Dijkstra {

    public static class Graph{

        public List<DVertex> vertices;
        public HashMap<String, DVertex> hmp;  // to help creating adjacency lists

        public Graph(){
            vertices = new ArrayList<DVertex>();
            hmp = new HashMap<String, DVertex>();
        }

        public void addPois( List<HashMap<String,String>> pois ){
            for( HashMap<String,String> p : pois ){
                DVertex dv = new DVertex(p);
                this.vertices.add( dv );
                hmp.put(p.get("puid"), dv);
            }
        }

        public void addPoi( HashMap<String,String> p ){
            DVertex dv = new DVertex(p);
            this.vertices.add( dv );
            hmp.put(p.get("puid"), dv);
        }

        public void addEdges( List<HashMap<String,String>> conns ){
            NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
            double w =0.0;
            for( HashMap<String,String> e : conns ){
                try {
                    w = nf.parse( e.get("weight") ).doubleValue();
                } catch (ParseException e1) {
                    //
                }
                DVertex a = hmp.get(e.get("pois_a"));
                DVertex b = hmp.get(e.get("pois_b"));
                if( a == null || b == null ){
                    continue;
                }
                a.adjacencies.add(new DEdge( w, b ));
                b.adjacencies.add(new DEdge( w, a ));
            }
        }

        public void addEdge( HashMap<String, String> conn ){
            NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
            double w =0.0;
            try {
                w = nf.parse( conn.get("weight") ).doubleValue();
            } catch (ParseException e1) {
                //
            }
            DVertex a = hmp.get(conn.get("pois_a"));
            DVertex b = hmp.get(conn.get("pois_b"));
            if( a == null || b == null ){
                return;
            }
            a.adjacencies.add(new DEdge( w, b ));
            b.adjacencies.add(new DEdge( w, a ));
        }

        public DVertex getVertex(String puid){
            return hmp.get(puid);
        }

    }

    private static class DVertex implements Comparable<Object>{
        public HashMap<String, String> poi;
        public DVertex previous;
        public double minDistance;
        public List<DEdge> adjacencies;
        public String puid;

        public DVertex(HashMap<String, String> p){
            this.poi = p;
            this.puid = this.poi.get("puid");
            this.previous = null;
            this.minDistance = Double.POSITIVE_INFINITY;
            this.adjacencies = new ArrayList<DEdge>();
        }

        @Override
        public int compareTo(Object o) {
            return Double.compare( this.minDistance, ((DVertex)o).minDistance  );
        }
    }

    private static class DEdge{
        public double weight;
        public DVertex target;
        public DEdge(double weight, DVertex dv){
            this.weight = weight;
            this.target = dv;
        }
    }


    private static List<HashMap<String,String>> computePath( Graph graph, String puid_from, String puid_to ){
        DVertex dv = graph.getVertex( puid_from );
        if( dv == null ){
            return Collections.emptyList();
        }

        // visited structure
        HashSet<String> visited = new HashSet<String>();

        dv.minDistance = 0.0;
        PriorityQueue<DVertex> vqueue = new PriorityQueue<DVertex>();
        vqueue.add( dv );

        DVertex tar;
        while( !vqueue.isEmpty() ){
            dv = vqueue.poll();
            //System.out.println( "current node" + dv.pois);

            if( visited.contains(dv.puid) )
                continue;
            visited.add(dv.puid);

            if( dv.minDistance == Double.POSITIVE_INFINITY ){
                // NO OTHER NODES ARE ACCESSIBLE FROM THIS VERTEX
                return Collections.emptyList();
            }

            //if( dv.pois.getId().equalsIgnoreCase(puid_to) ){
            if( dv.puid.equalsIgnoreCase(puid_to) ){
                // WE FOUND THE PATH SO JUST TERMINATE AND RETURN IT
                //System.out.println("path found: " + dv.pois);
                Logger.info("Path found!");
                return path( dv  );
            }

            for( DEdge e: dv.adjacencies ){
                tar = e.target;

                double dist_no_tar = dv.minDistance + e.weight;
                //System.out.println("vertex:" + dv.pois + "target:" + tar.pois + "dist_no_tar: " + dist_no_tar + "tar.dist: " + tar.minDistance);

                if( dist_no_tar < tar.minDistance ){
                    //vqueue.remove(tar);
                    tar.minDistance = dist_no_tar;
                    tar.previous = dv;
                    //System.out.println("adding back " + tar.pois );
                    vqueue.add(tar);
                    //System.out.println("added back " + tar.pois );
                }
            }// end for each neighbor()
        }// end while nodes exist
        return Collections.emptyList();
    }


    private static List<HashMap<String,String>> path(DVertex dv){
        LinkedList<HashMap<String,String>> final_path = new LinkedList<HashMap<String,String>>();
        final_path.offerFirst(dv.poi);
        while( dv.previous != null ){
            dv = dv.previous;
            final_path.offerFirst(dv.poi);
        }
        return final_path;
    }


    public static List<HashMap<String,String>> getShortestPath( Graph graph, String puid_from, String puid_to ){

        Logger.info("Dijkstra from[" + puid_from + "]");
        Logger.info("Dijkstra to[" + puid_to + "]");

        return computePath(graph, puid_from, puid_to);
    }


}
