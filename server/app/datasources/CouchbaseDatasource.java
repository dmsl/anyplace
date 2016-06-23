/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou, Kyriakos Georgiou
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

package datasources;

import com.avaje.ebeaninternal.server.lib.sql.DataSourceException;
import com.couchbase.client.CouchbaseClient;
import com.couchbase.client.CouchbaseConnectionFactory;
import com.couchbase.client.CouchbaseConnectionFactoryBuilder;
import com.couchbase.client.TapClient;
import com.couchbase.client.protocol.views.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import db_models.Connection;
import db_models.Poi;
import db_models.RadioMapRaw;
import floor_module.IAlgo;
import net.spy.memcached.PersistTo;
import net.spy.memcached.internal.OperationFuture;
import play.Logger;
import play.Play;
import utils.GeoPoint;
import utils.JsonUtils;
import utils.LPLogger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * This is my own client interface for the Couchbase database
 */
public class CouchbaseDatasource implements IDatasource {

    private static CouchbaseDatasource sInstance;
    private static Object sLockInstance = new Object();

    /**
     * Creates and returns a Couchbase Datasource instance with the
     * configuration settings from application.conf.
     * <p>
     * Consecutive calls of this method will always return the same instance.
     * Use the createNewInstance() in order to get a newly created instance with
     * your own configuration settings.
     *
     * @return The CouchbaseDatasource object with the default configuration
     */
    public static CouchbaseDatasource getStaticInstance() {
        synchronized (sLockInstance) {
            if (sInstance == null) {
                String hostname = Play.application().configuration().getString("couchbase.hostname");
                String port = Play.application().configuration().getString("couchbase.port");
                String bucket = Play.application().configuration().getString("couchbase.bucket");
                String password = Play.application().configuration().getString("couchbase.password");
                sInstance = CouchbaseDatasource.createNewInstance(hostname, port, bucket, password);
                try {
                    sInstance.init();
                } catch (DatasourceException e) {
                    LPLogger.error("CouchbaseDatasource::getStaticInstance():: Exception while instantiating Couchbase [" + e.getMessage() + "]");
                }
            }
            return sInstance;
        }
    }

    /**
     * Creates and returns a Couchbase Datasource instance with the
     * passed configuration settings. This call should be followed by
     * an INIT() invocation in order to connect the client to the server.
     *
     * @param hostname The hostname of the Couchbase server
     * @param port     The port of the Couchbase server
     * @param bucket   The bucket you want to connect on the Couchbase server
     * @param password The bucket's password
     * @return The constructed CouchbaseDatasource object with the configuration as set
     */
    public static CouchbaseDatasource createNewInstance(String hostname,
                                                        String port,
                                                        String bucket,
                                                        String password) {
        if (hostname == null || port == null
                || bucket == null || password == null) {
            throw new IllegalArgumentException("[null] parameters are not allowed to create a CouchbaseDatasource");
        }
        hostname = hostname.trim();
        port = port.trim();
        bucket = bucket.trim();
        password = password.trim();
        if (hostname.isEmpty() || port.isEmpty()
                || bucket.isEmpty() || password.isEmpty()) {
            throw new IllegalArgumentException("Empty string configuration are not allowed to create a CouchbaseDatasource");
        }
        return new CouchbaseDatasource(hostname, port, bucket, password);
    }

    // Couchbase server configuration settings
    private String mHostname;
    private String mPort;
    private String mBucket;
    private String mPassword;

    // Holds the actual CouchbaseClient
    private CouchbaseClient mClient = null;
    private TapClient mTapClient = null;

    // creates a Couchbase datasource object with the specified configuration
    // a connect() call should follow to initialize the datasource

    /**
     * Creates a CouchbaseDatasource with the configuration settings passed in.
     *
     * @param hostname The hostname of the Couchbase server
     * @param port     The port of the Couchbase server
     * @param bucket   The bucket you want to connect on the Couchbase server
     * @param password The bucket's password
     */
    private CouchbaseDatasource(String hostname,
                                String port,
                                String bucket,
                                String password) {
        this.mHostname = hostname;
        this.mPort = port;
        this.mBucket = bucket;
        this.mPassword = password;
    }

    ///////////////////////////////////////////////////////////////////////////////
    // Couchbase Connection
    ///////////////////////////////////////////////////////////////////////////////


    /**
     * Connects to Couchbase based on the passed configuration settings.
     * Sets the internal client to the connected client.
     *
     * @return True if the connection succeeded or exception thrown if not
     * @throws DatasourceException
     */
    private boolean connect() throws DatasourceException {
        /*
        if(1==1){
            LPLogger.info("do not connect to couchbase");
            throw new DatasourceException("Cannot connect to Anyplace Database! [Unknown]");
        }
        */

        Logger.info("Trying to connect to: " + mHostname + ":" + mPort + " bucket[" + mBucket + "] password: " + mPassword);

        List<URI> uris = new LinkedList<URI>();
        uris.add(URI.create(mHostname + ":" + mPort + "/pools"));
        try {
            CouchbaseConnectionFactoryBuilder cfb = new CouchbaseConnectionFactoryBuilder();
            //cfb.setMaxReconnectDelay(10000);
            //cfb.setOpQueueMaxBlockTime(5000);
            //cfb.setOpTimeout(20000);
            //cfb.setShouldOptimize(true);
            //cfb.setViewTimeout(6000);
            //CouchbaseConnectionFactory connFactory = cfb.buildCouchbaseConnection(uris, mBucket, mPassword);
            CouchbaseConnectionFactory connFactory = new CouchbaseConnectionFactory(uris, mBucket, mPassword);

            mClient = new CouchbaseClient(connFactory);
            mTapClient = new TapClient(uris, mBucket, mPassword);
        } catch (java.net.SocketTimeoutException e) {
            // thrown by the constructor on timeout
            LPLogger.error("CouchbaseDatasource::connect():: Error connection to Couchbase: " + e.getMessage());
            throw new DatasourceException("Cannot connect to Anyplace Database [SocketTimeout]!");
        } catch (IOException e) {
            LPLogger.error("CouchbaseDatasource::connect():: Error connection to Couchbase: " + e.getMessage());
            throw new DatasourceException("Cannot connect to Anyplace Database [IO]!");
        } catch (Exception e) {
            LPLogger.error("CouchbaseDatasource::connect():: Error connection to Couchbase: " + e.getMessage());
            throw new DatasourceException("Cannot connect to Anyplace Database! [Unknown]");
        }
        return true;
    }

    /**
     * Disconnect from Couchbase server and sets the internal client to null.
     *
     * @return True if disconnected successfully, otherwise False
     * @throws DatasourceException If the internal client is null,
     *                             thus no connection has occurred exception is thrown.
     */
    public boolean disconnect() throws DatasourceException {
        if (mClient == null) {
            LPLogger.error("CouchbaseDatasource::disconnect():: Trying to disconnect from a null client");
            throw new DatasourceException("Trying to disconnect a NULL client!");
        }
        boolean res = mClient.shutdown(3, TimeUnit.SECONDS);
        this.mClient = null;
        return res;
    }

    /**
     * Returns the internal client object if != null, otherwise it calls connect()
     * and returns the newly connected client.
     *
     * @return The connected client
     * @throws DatasourceException If the client cannot connect to the server exception is thrown
     */
    public CouchbaseClient getConnection() throws DatasourceException {
        if (mClient == null) {
            connect();
        }
        return mClient;
    }

    ///////////////////////////////////////////////////////////////////////////////
    // IDatasource Implementations
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * Initializes the Couchbase connection, thus trying to connect to the server
     * and have a connected connection client available.
     *
     * @return True if the connection succeeded
     * @throws DatasourceException If an error occurred while connecting this exception is thrown
     */
    @Override
    public boolean init() throws DatasourceException {
        try {
            this.connect();
        } catch (DatasourceException e) {
            LPLogger.error("CouchbaseDatasource::init():: " + e.getMessage());
            throw new DatasourceException("Cannot establish connection to Anyplace database!");
        }
        return true;
    }

    @Override
    public boolean addJsonDocument(String key, int expiry, String document) throws DatasourceException {
        // verifies that a connection exists or an exception is thrown
        CouchbaseClient client = getConnection();
        OperationFuture<Boolean> db_res = client.add(key, expiry, document, PersistTo.ONE);
        try {
            return db_res.get();
        } catch (InterruptedException e) {
            throw new DataSourceException("Document storing interrupted!");
        } catch (ExecutionException e) {
            throw new DataSourceException("Document storing had an exception!");
        }
    }

    @Override
    public boolean replaceJsonDocument(String key, int expiry, String document) throws DatasourceException {
        // verifies that a connection exists or an exception is thrown
        CouchbaseClient client = getConnection();
        OperationFuture<Boolean> db_res = client.replace(key, expiry, document, PersistTo.ONE);
        try {
            return db_res.get();
        } catch (InterruptedException e) {
            throw new DataSourceException("Document replace interrupted!");
        } catch (ExecutionException e) {
            throw new DataSourceException("Document replace had an exception!");
        }
    }

    /**
     * Returns the JSON document from the Couchbase
     *
     * @param key The key of the document required
     * @return Returns the Json document as String, or null if no document exists with the key
     * @throws DatasourceException This exception is only thrown if the connection failed
     */
    @Override
    public boolean deleteFromKey(String key) throws DatasourceException {
        // verifies that a connection exists or an exception is thrown
        CouchbaseClient client = getConnection();
        OperationFuture<Boolean> db_res = client.delete(key, PersistTo.ONE);
        try {
            return db_res.get();
        } catch (InterruptedException e) {
            throw new DataSourceException("Document delete interrupted!");
        } catch (ExecutionException e) {
            throw new DataSourceException("Document delete had an exception!");
        }
    }

    /**
     * Returns the JSON document from the Couchbase
     *
     * @param key The key of the document required
     * @return Returns the Json document as String, or null if no document exists with the key
     * @throws DatasourceException This exception is only thrown if the connection failed
     */
    @Override
    public Object getFromKey(String key) throws DatasourceException {
        // verifies that a connection exists or an exception is thrown
        CouchbaseClient client = getConnection();
        Object db_res = client.get(key);
        return db_res;
    }

    /**
     * The Json document with the specified key is returned as a JsonNode.
     *
     * @param key The key of the document required
     * @return Returns the Json document as JsonNode, or null if no document exists with the key
     * @throws DatasourceException      This exception is only thrown if the connection failed
     * @throws IllegalArgumentException if empty/null key is specified
     */
    @Override
    public JsonNode getFromKeyAsJson(String key) throws DatasourceException {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("No null or empty string allowed as key!");
        }
        Object db_res = getFromKey(key);
        // document does not exist
        if (db_res == null) {
            return null;
        }
        try {
            JsonNode jsonNode = JsonUtils.getJsonTree((String) db_res);
            return jsonNode;
        } catch (IOException e) {
            LPLogger.error("CouchbaseDatasource::getFromKeyAsJson():: Could not convert document from Couchbase into JSON!");
            return null;
        }
    }

    @Override
    public JsonNode buildingFromKeyAsJson(String key) throws DatasourceException {
        // fetch the building
        ObjectNode building = (ObjectNode) getFromKeyAsJson(key);
        if (building == null) {
            return null;
        }
        // fetch the floors
        ArrayNode floors = building.putArray("floors");
        for (JsonNode f : floorsByBuildingAsJson(key)) {
            floors.add(f);
        }
        // fetch the Pois
        ArrayNode pois = building.putArray("pois");
        for (JsonNode p : poisByBuildingAsJson(key)) {
            if (p.path("pois_type").textValue().equals(Poi.POIS_TYPE_NONE))
                continue;
            pois.add(p);
        }
        return building;
    }

    /**
     * The POI with specified key is returned as JsonNode
     *
     * @param key The key of the POI required
     * @return Returns the Json document as JsonNode, or null if no document exists with the key
     * @throws DatasourceException      This exception is only thrown if the connection failed
     * @throws IllegalArgumentException if empty/null key is specified
     */
    @Override
    public JsonNode poiFromKeyAsJson(String key) throws DatasourceException {
        return getFromKeyAsJson(key);
    }

    @Override
    public List<JsonNode> poisByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                json.remove("geometry");
//                json.remove("image");
//                json.remove("is_door");
//                json.remove("is_published");
//                json.remove("floor_name");
//                json.remove("url");
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<HashMap<String, String>> poisByBuildingFloorAsMap(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<HashMap<String, String>> result = new ArrayList<HashMap<String, String>>();
        for (ViewRow row : res) {
            result.add(JsonUtils.getHashMapStrStr(row.getDocument().toString()));
        }
        return result;
    }

    @Override
    public List<JsonNode> poisByBuildingAsJson(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        List<JsonNode> pois = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                json.remove("geometry");
//                json.remove("image");
//                json.remove("is_door");
//                json.remove("is_published");
//                json.remove("floor_name");
//                json.remove("url");
                pois.add(json);
            } catch (IOException e) {
                // skip this one since not a valid Json object
            }
        }
        return pois;
    }

    @Override
    public List<HashMap<String, String>> poisByBuildingAsMap(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "pois_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        List<HashMap<String, String>> pois = new ArrayList<HashMap<String, String>>();
        for (ViewRow row : res) {
            pois.add(JsonUtils.getHashMapStrStr(row.getDocument().toString()));
        }
        return pois;
    }

    @Override
    public List<JsonNode> floorsByBuildingAsJson(String buid) throws DatasourceException {
        List<JsonNode> floors = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "floor_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        if (res.getErrors().size() > 0) {
            throw new DatasourceException("Error retrieving floors from database!");
        }

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                floors.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return floors;
    }

    @Override
    public List<JsonNode> connectionsByBuildingAsJson(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "connection_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        JsonNode json;
        List<JsonNode> conns = new ArrayList<JsonNode>();
        for (ViewRow row : res) {
            try {
                json = JsonUtils.getJsonTree(row.getDocument().toString());
                if (json.path("edge_type").textValue().equalsIgnoreCase(Connection.EDGE_TYPE_OUTDOOR))
                    continue;
                conns.add(json);
            } catch (IOException e) {
                // skip this one since not a valid Json object
            }
        }
        return conns;
    }

    @Override
    public List<HashMap<String, String>> connectionsByBuildingAsMap(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "connection_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        HashMap<String, String> hm;
        List<HashMap<String, String>> conns = new ArrayList<HashMap<String, String>>();
        for (ViewRow row : res) {
            hm = JsonUtils.getHashMapStrStr(row.getDocument().toString());
            if (hm.get("edge_type").equalsIgnoreCase(Connection.EDGE_TYPE_OUTDOOR))
                continue;
            conns.add(hm);
        }
        return conns;
    }

    @Override
    public List<JsonNode> connectionsByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "connection_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();
        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("owner_id");
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<String> deleteAllByBuilding(String buid) throws DatasourceException {
        List<String> all_items_failed = new ArrayList<String>();
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "all_by_buid");
        Query query = new Query();
        query.setKey(buid);
        ViewResponse res = couchbaseClient.query(view, query);
        for (ViewRow row : res) {
            String id = row.getValue();
            // we have the id so try to delete it

            OperationFuture<Boolean> db_res = couchbaseClient.delete(id, PersistTo.ONE);
            try {
                if (db_res.get().booleanValue() == false) {
                    all_items_failed.add(id);
                } else {
                    // document deleted just fine
                }
            } catch (Exception e) {
                all_items_failed.add(id);
            }
        }
        return all_items_failed;
    }

    @Override
    public List<String> deleteAllByFloor(String buid, String floor_number) throws DatasourceException {
        List<String> all_items_failed = new ArrayList<String>();
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "all_by_floor");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        for (ViewRow row : res) {
            String id = row.getValue();
            // we have the id so try to delete it
            OperationFuture<Boolean> db_res = couchbaseClient.delete(id, PersistTo.ONE);
            try {
                if (db_res.get().booleanValue() == false) {
                    all_items_failed.add(id);
                } else {
                    // document deleted just fine
                }
            } catch (Exception e) {
                all_items_failed.add(id);
            }
        }
        return all_items_failed;
    }

    @Override
    public List<String> deleteAllByConnection(String cuid) throws DatasourceException {
        List<String> all_items_failed = new ArrayList<String>();
        if (!this.deleteFromKey(cuid)) {
            all_items_failed.add(cuid);
        }
        return all_items_failed;
    }

    @Override
    public List<String> deleteAllByPoi(String puid) throws DatasourceException {
        List<String> all_items_failed = new ArrayList<String>();
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "all_by_pois");
        Query query = new Query();
        query.setKey(puid);
        ViewResponse res = couchbaseClient.query(view, query);
        for (ViewRow row : res) {
            String id = row.getValue();
            // we have the id so try to delete it
            OperationFuture<Boolean> db_res = couchbaseClient.delete(id, PersistTo.ONE);
            try {
                if (db_res.get().booleanValue() == false) {
                    all_items_failed.add(id);
                } else {
                    // document deleted just fine
                }
            } catch (Exception e) {
                all_items_failed.add(id);
            }
        }
        return all_items_failed;
    }

    @Override
    public List<JsonNode> getRadioHeatmap() throws DatasourceException {
        List<JsonNode> points = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("radio", "radio_new_campus_experiment");
        Query query = new Query();
        query.setGroup(true);
        query.setReduce(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getKey());
                json.put("weight", row.getValue());
                points.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return points;
    }

    @Override
    public List<JsonNode> getRadioHeatmapByBuildingFloor(String buid, String floor) throws DatasourceException {
        List<JsonNode> points = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("radio", "radio_heatmap_building_floor");
        Query query = new Query();
        query.setGroup(true);
        query.setReduce(true);
        query.setRangeStart(ComplexKey.of(buid, floor));
        query.setRangeEnd(ComplexKey.of(buid, floor, ComplexKey.emptyObject(), ComplexKey.emptyObject()));

        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree("{}");
                String k = row.getKey();
                k = k.substring(1, k.length() - 1);
                String[] array = k.split(",");
                json.put("x", array[2].substring(1, array[2].length() - 1));
                json.put("y", array[3].substring(1, array[3].length() - 1));
                json.put("w", row.getValue());
                points.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return points;
    }

    @Override
    public List<JsonNode> getAllBuildings() throws DatasourceException {
        List<JsonNode> buildings = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "building_all");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("geometry");
                json.remove("owner_id");
                json.remove("co_owners");
//                json.remove("is_published");
//                json.remove("url");
//                json.remove("address");
                buildings.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return buildings;
    }

    @Override
    public List<JsonNode> getAllBuildingsByOwner(String oid) throws DatasourceException {
        List<JsonNode> buildings = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "building_all_by_owner");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(oid);
        query.setKey(key);
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("geometry");
                json.remove("owner_id");
                json.remove("co_owners");
//                json.remove("is_published");
//                json.remove("url");
//                json.remove("address");
                buildings.add(json);
            } catch (Exception e) {
                // skip this NOT-JSON document
            }
        }
        return buildings;
    }

    @Override
    public List<JsonNode> getAllBuildingsByBucode(String bucode) throws DatasourceException {
        List<JsonNode> buildings = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "building_all_by_bucode");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(bucode);
        query.setKey(key);
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("geometry");
                json.remove("owner_id");
                json.remove("co_owners");
//                json.remove("is_published");
//                json.remove("url");
//                json.remove("address");
                buildings.add(json);
            } catch (Exception e) {
                // skip this NOT-JSON document
            }
        }
        return buildings;
    }

    @Override
    public List<JsonNode> getAllBuildingsNearMe(double lat, double lng) throws DatasourceException {
        List<JsonNode> buildings = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        SpatialView view = couchbaseClient.getSpatialView("nav", "building_coordinates");
        Query query = new Query();
        query.setIncludeDocs(true);

        GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(lat, lng, 50); // 50 meters radius
        //query.setBbox(-180, -90, 180, 90);
        query.setBbox(bbox[0].dlat, bbox[0].dlon, bbox[1].dlat, bbox[1].dlon);

        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        /*
        if( res.getErrors().size() > 0 ){
            throw new DatasourceException("Error retrieving buildings from database!");
        }
        */

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("geometry");
                json.remove("owner_id");
                json.remove("co_owners");
//                json.remove("is_published");
//                json.remove("url");
//                json.remove("address");
                buildings.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return buildings;
    }

    @Override
    public JsonNode getBuildingByAlias(String alias) throws DatasourceException {
        JsonNode jsn = null;

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("nav", "building_by_alias");
        Query query = new Query();
        ComplexKey key = ComplexKey.of(alias);
        query.setKey(key);
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        if (!res.iterator().hasNext()) {
            return null;
        }
        try {
            jsn = (ObjectNode) JsonUtils.getJsonTree(res.iterator().next().getDocument().toString());
        } catch (IOException ioe) {

        }

        return jsn;
    }

    @Override
    public long dumpRssLogEntriesSpatial(FileOutputStream outFile, GeoPoint[] bbox, String floor_number) throws DatasourceException {
        PrintWriter writer = new PrintWriter(outFile);
        CouchbaseClient couchbaseClient = getConnection();
        SpatialView view;
        Query query;

        int queryLimit = 5000;
        int totalFetched = 0;
        int currentFetched;
        int floorFetched = 0;
        JsonNode rssEntry;

        view = couchbaseClient.getSpatialView("radio", "raw_radio");
        do {
            query = new Query();
            //query.setDescending(true);
            query.setLimit(queryLimit);
            query.setSkip(totalFetched);
            //query.setStale(Stale.FALSE);
            query.setBbox(bbox[0].dlat, bbox[0].dlon, bbox[1].dlat, bbox[1].dlon);

            ViewResponse res = couchbaseClient.query(view, query);
            currentFetched = 0;
            for (ViewRow row : res) {
                // handle each raw radio entry
                currentFetched++;
                try {
                    rssEntry = JsonUtils.getJsonTree(row.getValue());
                } catch (IOException e) {
                    // skip documents not in Json-format
                    continue;
                }
                if (!rssEntry.path("floor").textValue().equals(floor_number)) {
                    // skip rss logs of another floor
                    continue;
                }
                floorFetched++;
                // write this entry into the file
                writer.println(RadioMapRaw.toRawRadioMapRecord(rssEntry));
            } // end while paginator
            totalFetched += currentFetched;

            LPLogger.info("total fetched: " + totalFetched);
        } while (currentFetched >= queryLimit && floorFetched < 100000);

        // flush and close the files
        writer.flush();
        writer.close();
        return floorFetched;
    }

    @Override
    public long dumpRssLogEntriesByBuildingFloor(FileOutputStream outFile, String buid, String floor_number) throws DatasourceException {
        PrintWriter writer = new PrintWriter(outFile);
        CouchbaseClient couchbaseClient = getConnection();
        View view;
        Query query;
// multi key
        // couch view
        int queryLimit = 10000;
        int totalFetched = 0;
        int currentFetched;
        JsonNode rssEntry;

        ComplexKey key = ComplexKey.of(buid, floor_number);


        view = couchbaseClient.getView("radio", "raw_radio_building_floor");
        do {
            query = new Query();
            query.setKey(key);
            query.setIncludeDocs(true);
            //query.setDescending(true);
            query.setLimit(queryLimit);
            query.setSkip(totalFetched);
            //query.setStale(Stale.FALSE);

            ViewResponse res = couchbaseClient.query(view, query);

            if (res == null)
                return totalFetched;

            currentFetched = 0;
            for (ViewRow row : res) {
                // handle each raw radio entry
                currentFetched++;
                try {
                    rssEntry = JsonUtils.getJsonTree(row.getDocument().toString());
                } catch (IOException e) {
                    // skip documents not in Json-format
                    continue;
                }

                // write this entry into the file
                writer.println(RadioMapRaw.toRawRadioMapRecord(rssEntry));
            } // end while paginator
            totalFetched += currentFetched;

            LPLogger.info("total fetched: " + totalFetched);

            // basically, ==
        } while (currentFetched >= queryLimit);

        // flush and close the files
        writer.flush();
        writer.close();
        return totalFetched;
    }


    /**
     * ***************************************************************
     * ACCOUNTS METHODS
     */

    @Override
    public List<JsonNode> getAllAccounts() throws DatasourceException {
        List<JsonNode> accounts = new ArrayList<JsonNode>();

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("accounts", "accounts_all");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);
        System.out.println("couchbase results: " + res.size());

        if (res.getErrors().size() > 0) {
            throw new DatasourceException("Error retrieving accounts from database!");
        }

        ObjectNode json;
        for (ViewRow row : res) {
            // handle each building entry
            try {
                // try to avoid documents that are fetched without documents
                if (row.getDocument() == null)
                    continue;
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                json.remove("doctype");
                accounts.add(json);
            } catch (IOException e) {
                // skip this NOT-JSON document
            }
        }
        return accounts;
    }


    @Override
    public boolean deleteRadiosInBox() throws DatasourceException {

        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("radio", "tempview");
        Query query = new Query();
        query.setIncludeDocs(true);
        ViewResponse res = couchbaseClient.query(view, query);

        for (ViewRow row : res) {
            deleteFromKey(row.getKey());
        }

        return true;
    }


    @Override
    public boolean predictFloor(IAlgo algo, GeoPoint[] bbox, String[] strongestMAC)
            throws DatasourceException {
        //predictFloorSlow(algo, bbox);
        return predictFloorFast(algo, bbox, strongestMAC);
    }

    private boolean predictFloorFast(IAlgo algo, GeoPoint[] bbox,
                                     String[] strongestMACs) throws DatasourceException {

        String designDoc = "floor";
        String viewName = "group_wifi";
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView(designDoc, viewName);
        int totalFetched = 0;

        for (String strongestMAC : strongestMACs) {
            Query query = new Query();
            ComplexKey startkey = ComplexKey.of(strongestMAC, bbox[0].dlat,
                    bbox[0].dlon, null);
            ComplexKey endkey = ComplexKey.of(strongestMAC, bbox[1].dlat,
                    bbox[1].dlon, "\u0fff");

            query.setRange(startkey, endkey);
            ViewResponse response = couchbaseClient.query(view, query);

            String _timestamp = "";
            String _floor = "0";
            ArrayList<JsonNode> bucket = new ArrayList<JsonNode>(10);
            for (ViewRow row : response) {
                try {
                    String timestamp = row.getKey();
                    JsonNode value = JsonUtils.getJsonTree(row.getValue());

                    if (!_timestamp.equals(timestamp)) {
                        if (!_timestamp.equals("")) {
                            algo.proccess(bucket, _floor);
                        }

                        bucket.clear();
                        _timestamp = timestamp;
                        _floor = value.get("floor").textValue();
                    }

                    bucket.add(value);
                    totalFetched++;
                } catch (IOException e) {
                    // skip documents not in Json-format
                    continue;
                }

            }
        }

        LPLogger.info("total fetched: " + totalFetched);
        if (totalFetched > 10) {
            return true;
        } else {
            return false;
        }

    }

    /*
        MAGNETIC
     */

    @Override
    public List<JsonNode> magneticPathsByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("magnetic", "mpaths_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<JsonNode> magneticPathsByBuildingAsJson(String buid) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("magnetic", "mpaths_by_buid");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

    @Override
    public List<JsonNode> magneticMilestonesByBuildingFloorAsJson(String buid, String floor_number) throws DatasourceException {
        CouchbaseClient couchbaseClient = getConnection();
        View view = couchbaseClient.getView("magnetic", "mmilestones_by_buid_floor");
        Query query = new Query();
        query.setIncludeDocs(true);
        ComplexKey key = ComplexKey.of(buid, floor_number);
        query.setKey(key);
        ViewResponse res = couchbaseClient.query(view, query);
        if (0 == res.size()) {
            return Collections.emptyList();
        }
        List<JsonNode> result = new ArrayList<JsonNode>();

        ObjectNode json;
        for (ViewRow row : res) {
            try {
                json = (ObjectNode) JsonUtils.getJsonTree(row.getDocument().toString());
                result.add(json);
            } catch (IOException e) {
                // skip this document since it is not a valid Json object
            }
        }
        return result;
    }

}
