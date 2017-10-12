package Spacegame.GatewayService;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;

import micronet.activemq.AMQGatewayPeer;
import micronet.annotation.MessageListener;
import micronet.annotation.MessageParameter;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.annotation.OnStop;
import micronet.annotation.RequestParameters;
import micronet.datastore.DataStore;
import micronet.network.Context;
import micronet.network.NetworkConstants;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;

@MessageService(uri="mn://gateway")
public class GatewayService {

	private AMQGatewayPeer gatewayPeer;
	
	private boolean useSteam = false;
	
	private static DataStore store = new DataStore();
	private ConnectionStore connections = new ConnectionStore(store.getCluster());

	@OnStart
	public void onStart(Context context) {
		gatewayPeer = new AMQGatewayPeer((String connectionID) -> {
			UserConnection connection = connections.getUserFromConnection(connectionID);
			if (connection == null)
				return;
			connections.remove(connectionID);
            context.getAdvisory().send("User.Disconnected", connection.getUserID());

		});
		gatewayPeer.listen(URI.create(NetworkConstants.COMMAND_QUEUE), (String clientId, Request request) -> clientCmd(context, clientId, request));
		gatewayPeer.listen(URI.create(NetworkConstants.REQUEST_QUEUE), (String clientId, Request request) -> clientRequest(context, clientId, request));
	}
	
	@OnStop
	public void onStop(Context context) {
		
	}
	
	@MessageListener(uri="/forward/event")
	@RequestParameters(@MessageParameter(code=ParameterCode.USER_ID, type=Integer.class))
	public void forwardEvent(Context context, Request request) {
		// TODO: Dont send back userId (security)
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		UserConnection connection = connections.getConnectionFromUser(userID);
		gatewayPeer.sendRequest(URI.create(connections.getConnectionURI(connection) + "/event"), request);
	}
	
	@MessageListener(uri="/broadcast/event")
	public void broadcastEvent(Context context, Request request) {
		for (UserConnection connection : connections.all()) {
			gatewayPeer.sendRequest(URI.create(connections.getConnectionURI(connection) + "/event"), request);
		}
	}

	private void clientCmd(Context context, String connectionID, Request request) {
		UserConnection connection = connections.getUserFromConnection(connectionID);
		if (connection == null)
			return;
		String userRequest = request.getParameters().getString(ParameterCode.USER_REQUEST);
		System.out.println("CMD " + connectionID + " -> " + userRequest + ": " + request.getData());
		request.getParameters().set(ParameterCode.USER_ID, connection.getUserID());
		context.sendRequest(userRequest, request);
	}

	private Response clientRequest(Context context, String connectionID, Request request) {

		String userRequest = request.getParameters().getString(ParameterCode.USER_REQUEST);
		System.out.println("REQUEST " + connectionID + " -> " + userRequest + ": " + request.getData());
		
		UserConnection connection = connections.getUserFromConnection(connectionID);

		switch (userRequest) {
		case "mn://login":
//			if (connection != null)
//				return new Response(StatusCode.FORBIDDEN, "Already logged in");
			
			String userID = request.getData();
			if (useSteam) {
				String steamTicket = request.getData();
				Response steamAuthResponse = context.sendRequestBlocking("mn://steam/login", new Request(steamTicket));
				if (steamAuthResponse.getStatus() != StatusCode.OK)
					return new Response(StatusCode.FORBIDDEN, "Steam Login Failed!");
				userID = steamAuthResponse.getData();				
			}
			
			connection = connections.getConnectionFromUser(userID);
			if (connection != null)
				connections.remove(connection.getConnectionID());
				//return new Response(StatusCode.FORBIDDEN, "User ID already in use");
			connection = connections.add(connectionID, userID);
			
			String playerID = String.format("Player.%s", userID);
			Player player = store.get(playerID, Player.class);
			if (player == null) {
				player = createPlayer(userID);
				store.insert(playerID, player);
			}
			
			return new Response(StatusCode.OK);
		case "mn://logout":
			if (connection == null)
				return new Response(StatusCode.FORBIDDEN, "You are not logged in");
			connections.remove(connectionID);
			return new Response(StatusCode.OK, "Logged Out");
		default:
			if (connection == null)
				return new Response(StatusCode.UNAUTHORIZED, "Not Authenticated: Only register and login possible");
			
			//TODO: Add white- or blacklisting here
			
			request.getParameters().set(ParameterCode.USER_ID, connection.getUserID());
			return context.sendRequestBlocking(userRequest, request);
		}
	}

	private static Player createPlayer(String userID) {
		Player player;
		player = new Player(userID);
		player.setAvatars(new HashMap<>());
		player.setReputation(new HashMap<>());

		player.setVehicles(new VehicleCollection());
		player.getVehicles().setVehicles(new HashMap<>());
		player.getVehicles().setCurrentVehicles(new HashMap<>());
		
		player.setItems(new ItemCollection());
		player.getItems().setInventory(new HashMap<>());
		player.getItems().setStorage(Collections.nCopies(20, null));
		
		return player;
	}
}
