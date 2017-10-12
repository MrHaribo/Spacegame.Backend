package Spacegame.GatewayService;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.N1qlQueryRow;

import micronet.serialization.Serialization;

public class ConnectionStore {
	
	private static final int connectionTimeout = 3000;
	
	private Bucket bucket;

	public ConnectionStore(Cluster cluster) {
		bucket = cluster.openBucket("user_connections");
        bucket.bucketManager().createN1qlPrimaryIndex(true, false);
	}
	
	public UserConnection getUserFromConnection(String connectionID) {
		JsonDocument connectionDoc = bucket.getAndTouch(connectionID, connectionTimeout);
		if (connectionDoc == null)
			return null;
		return Serialization.deserialize(connectionDoc.content().toString(), UserConnection.class);
	}
	
	public UserConnection getConnectionFromUser(String userID) {

        N1qlQueryResult result = bucket.query(
            N1qlQuery.parameterized("SELECT connectionID, userID FROM user_connections WHERE userID=$1",
            JsonArray.from(userID))
        );

        for (N1qlQueryRow row : result) {
        	return Serialization.deserialize(row.value().toString(), UserConnection.class);
        }
        return null;
	}
	
	public List<UserConnection> all() {

        N1qlQueryResult result = bucket.query(
            N1qlQuery.simple("SELECT connectionID, userID FROM user_connections")
        );

        List<UserConnection> connections = new ArrayList<>();
        for (N1qlQueryRow row : result) {
        	connections.add(Serialization.deserialize(row.value().toString(), UserConnection.class));
        }
        return connections;
	}
	
	public UserConnection add(String connectionID, String userID) {

		UserConnection connection = getConnectionFromUser(userID);
		if (connection != null) {
			System.out.println("Removing old connection: " + connection.getConnectionID());
			remove(connection.getConnectionID());
		}
		
        System.out.println("Add Player Connection: " + connectionID);
		connection = createConnection(connectionID, userID);
        JsonObject connectionObj = JsonObject.fromJson(Serialization.serialize(connection));
        bucket.insert(JsonDocument.create(connectionID, connectionTimeout, connectionObj));
        
        return connection;
	}

	public void remove(String connectionID) {
		System.out.println("Remove Player Connection: " + connectionID);
		bucket.remove(connectionID);
	}
	
	private UserConnection createConnection(String connectionID, String userID) {
		UserConnection connection = new UserConnection();
		connection.setConnectionID(connectionID);
		connection.setUserID(userID);
		return connection;
	}
	
	public URI getConnectionURI(UserConnection connection) {
		return URI.create("mn://" + connection.getConnectionID());
	}
}
