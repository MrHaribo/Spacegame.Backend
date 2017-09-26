package Spacegame.WorldService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.datastore.DataStore;
import micronet.network.Context;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.serialization.Serialization;

@MessageService(uri="mn://world")
public class WorldService {
	
	DataStore store;

	@OnStart
	public void onStart(Context context) {
		store = new DataStore();
		
		List<String> allRegions = new ArrayList<>();
		
		try {
			String regionData = new String(Files.readAllBytes(Paths.get("defaultRegions.json")));
			RegionValues[] regions = Serialization.deserialize(regionData, RegionValues[].class);
			
			for (RegionValues data : regions) {
				Region region = new Region();
				region.setStatus(RegionStatus.CLOSED);
				region.setQueue(new HashSet<>());
				region.setUsers(new HashSet<>());
				region.setData(data);
				
				store.upsert(data.getId(), region);
				allRegions.add(data.getId());
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		World world = new World();
		world.setRegions(allRegions);
		
		store.upsert("World", world);

		context.getAdvisory().listen("User.Disconnected", userID -> {
			removeUserFromRegion(userID);
			removeUserFromQueue(userID);
		});
	}

	@MessageListener(uri = "/join")
	public Response joinWorld(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		
		String regionID = request.getData();
		Region region = null;
		if (regionID == null) {
			region = getCurrentRegion(userID);
		} else {
			region = store.get(regionID, Region.class);
		}
		
		if (region == null)
			return new Response(StatusCode.NOT_FOUND);
		
		removeUserFromRegion(userID);
		removeUserFromQueue(userID);
		
		switch (region.getStatus()) {
		case OPEN:
			store.getSub(region.getData().getId()).getList("users").append(userID);
			return new Response(StatusCode.OK, Serialization.serialize(region.getHost()));
		case CLOSED:
			//TODO: Timeout if User does not create Map
			store.getSub(region.getData().getId()).set("status", RegionStatus.OPENING);
			store.getSub(region.getData().getId()).getList("queue").append(userID);
			return new Response(StatusCode.ACCEPTED);
		case OPENING:
			region.getQueue().add(region.getData().getId());
			store.getSub(region.getData().getId()).set("queue", region.getQueue());
			return new Response(StatusCode.TEMPORARY_REDIRECT);
		default:
			return new Response(StatusCode.NOT_IMPLEMENTED);
		}
	}
	

	
	@MessageListener(uri = "/region/current/get")
	public Response getCurrentRegion(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		Region region = getCurrentRegion(userID);
		if (region == null)
			return new Response(StatusCode.NOT_FOUND);
		return new Response(StatusCode.OK, Serialization.serialize(region.getData()));
	}
	
	@MessageListener(uri = "/leave")
	public void leaveWorld(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		removeUserFromRegion(userID);
	}
		
	@MessageListener(uri = "/region/ready")
	public void readyRegion(Context context, Request request) {
		System.out.println("Region Ready");
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		
		String query = "SELECT b.* FROM entities b WHERE ARRAY_CONTAINS(queue, $1)";
		Region region = store.query(query, Region.class, userID);
		
		if (region == null) {
			query = "SELECT b.* FROM entities b WHERE ARRAY_CONTAINS(users, $1)";
			region = store.query(query, Region.class, userID);
		}
		
		if (region == null)
			return;
		
		Host host = Serialization.deserialize(request.getData(), Host.class);
		if (host == null)
			return;
		
		region.setStatus(RegionStatus.OPEN);
		region.setHost(host);
		
		for (String queuedUserID : region.getQueue()) {
			region.getUsers().add(queuedUserID);
			if (!queuedUserID.equals(userID))
				context.sendEvent(queuedUserID, Event.RegionReady, request.getData());
		}
		
		region.getQueue().clear();
		store.upsert(region.getData().getId(), region);
	}
	
	@MessageListener(uri = "/region/all")
	public Response getAllRegions(Context context, Request request) {
		Object allRegions = store.getSub("World").get("regions", Object.class);
		return new Response(StatusCode.OK, allRegions.toString());
	}
	
	private void removeUserFromRegion(String userID) {
		String query = "SELECT b.* FROM entities b WHERE ARRAY_CONTAINS(users, $1)";
		Region region = store.query(query, Region.class, userID);
		if (region == null)
			return;
		
		region.getUsers().remove(userID);
		if (region.getUsers().size() <= 0) {
			region.setHost(null);
			region.setStatus(RegionStatus.CLOSED);
		}
		store.upsert(region.getData().getId(), region);
	}
	
	private Region getCurrentRegion(String userID) {
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		AvatarValues avatar = store.getSub(playerID).getMap("avatars").get(avatarName, AvatarValues.class);
		return store.get(avatar.getRegionID(), Region.class);
	}
	
	private void removeUserFromQueue(String userID) {
		String query = "SELECT b.* FROM entities b WHERE ARRAY_CONTAINS(queue, $1)";
		Region region = store.query(query, Region.class, userID);
		if (region == null)
			return;
		
		region.getQueue().remove(userID);
		if (region.getQueue().size() <= 0)
			region.setStatus(RegionStatus.CLOSED);
		store.upsert(region.getData().getId(), region);
	}
	
//	@MessageListener(uri = "/join")
//	public Response joinWorld(Context context, Request request) {
//		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
//		Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/set", request);
//		AvatarValues avatar = Serialization.deserialize(avatarResponse.getData(), AvatarValues.class);
//		
//		if (avatar.getRegionID() != null) {
//			return joinWorld(context, userID, avatar.getRegionID(), avatarResponse.getData());
//		}
//		return joinWorld(context, userID, avatar.getHomeRegionID(), avatarResponse.getData());
//	}
//	
//	@MessageListener(uri = "/travel/home")
//	public Response homestone(Context context, Request request) {
//		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
//		Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/get", request);
//		AvatarValues avatar = Serialization.deserialize(avatarResponse.getData(), AvatarValues.class);
//		return joinWorld(context, userID, avatar.getHomeRegionID(), avatarResponse.getData());
//	}
//
//	@MessageListener(uri = "/travel")
//	public Response travel(Context context, Request request) {
//		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
//		Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/get", request);
//		AvatarValues avatar = Serialization.deserialize(avatarResponse.getData(), AvatarValues.class);
//		if (avatar.getLanded())
//			return new Response(StatusCode.FORBIDDEN, "Must be in Space to Travel");
//		return joinWorld(context, userID, request.getData(), avatarResponse.getData());
//	}


//	@MessageListener(uri = "/instance/open")
//	public Response readyInstance(Context context, Request request) {
//
//		String instanceID = request.getParameters().getString(ParameterCode.ID);
//		String regionID = request.getParameters().getString(ParameterCode.REGION_ID);
//		String host = request.getParameters().getString(ParameterCode.HOST);
//		int port = request.getParameters().getInt(ParameterCode.PORT);
//		
//		if (!store.existsInstance(new String(regionID))) {
//			context.sendRequest("mn://port/release", new Request(Integer.toString(port)));
//			return new Response(StatusCode.RESET_CONTENT, "Instance not used any longer");
//		}
//				
//		store.openInstance(new String(regionID), instanceID, host, port);
//
//		//TODO: Notify Player that they can join
//		for (int userID : store.getQueuedUsers(new String(regionID))) {
//			
//			Request avatarRequest = new Request();
//			avatarRequest.getParameters().set(ParameterCode.USER_ID, userID);
//			Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/get", avatarRequest);
//			
//			Response joinRegionResponse = joinRegion(context, userID, new String(regionID), avatarResponse.getData());
//			
//			Request regionReadyRequest = new Request(avatarResponse.getData());
//			regionReadyRequest.getParameters().set(ParameterCode.TOKEN, joinRegionResponse.getParameters().getString(ParameterCode.TOKEN));
//			regionReadyRequest.getParameters().set(ParameterCode.REGION_ID, regionID);
//			regionReadyRequest.getParameters().set(ParameterCode.HOST, host);
//			regionReadyRequest.getParameters().set(ParameterCode.PORT, port);
//			context.sendEvent(userID, "OnRegionReady", regionReadyRequest);
//		}
//		
//		System.out.println("Instance Opened: " + regionID + " on: " + instanceID);
//		return new Response(StatusCode.OK, "Instance Added");
//	}
//
//
//	@MessageListener(uri = "/instance/close")
//	public Response resetInstance(Context context, Request request) {
//		String regionID = new String(request.getParameters().getString(ParameterCode.REGION_ID));
//		int port = request.getParameters().getInt(ParameterCode.PORT);
//		
//		store.removeInstance(regionID);
//		context.sendRequest("mn://port/release", new Request(Integer.toString(port)));
//		return new Response(StatusCode.OK, "Instance Closed");
//	}
//
//	private Response joinWorld(Context context, int userID, String regionID, String avatarData) {
//		// RegionID id = new RegionID(124554051589L);
//		// RegionValues region = new RegionValues(id);
//
//		System.out.println("World Join: " + regionID);
//		store.unqueueUser(userID);
//		
//		// Check if player is waiting for another instance
//
//		if (store.isRegionOpen(regionID)) {
//			store.queueUser(regionID, userID);
//			return joinRegion(context, userID, regionID, avatarData);
//		} else if (store.isRegionOpening(regionID)) {
//			store.queueUser(regionID, userID);
//		} else {
//			Response regionResponse = context.sendRequestBlocking("mn://region/get", new Request(regionID.toString()));
//			if (regionResponse.getStatus() != StatusCode.OK) 
//				return new Response(StatusCode.NOT_FOUND, "Region unknown");
//			
//			store.addInstance(regionID);
//			store.queueUser(regionID, userID);
//			
//			Response portResponse = context.sendRequestBlocking("mn://port/reserve", new Request());
//			if (portResponse.getStatus() != StatusCode.OK)
//				return new Response(StatusCode.NO_CONTENT, "No free Ports available");
//			
//			Request openRequest = new Request(regionResponse.getData());
//			openRequest.getParameters().set(ParameterCode.REGION_ID, regionID);
//			openRequest.getParameters().set(ParameterCode.PORT, portResponse.getData());
//			context.sendRequest("mn://instance-open", openRequest, response-> { 
//				System.err.println("No free Instances available");
//			});
//		}
//		return new Response(StatusCode.TEMPORARY_REDIRECT, "Wait for Region to be loaded"); 
//	}
//	
//
//	private Response joinRegion(Context context, int userID, String regionID, String avatarData) {
//		// Generate PlayerToken (random UUIDs)
//		UUID token = UUID.randomUUID();
//		Request joinRequest = new Request(avatarData);
//		joinRequest.getParameters().set(ParameterCode.USER_ID, userID);
//		joinRequest.getParameters().set(ParameterCode.TOKEN, token.toString());
//
//		Object address = ScriptExecutor.INSTANCE.invokeFunction("regionAddress", regionID) + "/join";
//		System.out.println("SENDING JOIN: " + address);
//		URI destination = URI.create(address.toString());
//		Response joinResponse = context.sendRequestBlocking(destination.toString(), joinRequest);
//		if (joinResponse.getStatus() != StatusCode.OK)
//			return new Response(StatusCode.UNAUTHORIZED);
//
//		Request setRegionRequest = new Request();
//		setRegionRequest.getParameters().set(ParameterCode.USER_ID, userID);
//		setRegionRequest.getParameters().set(ParameterCode.REGION_ID, regionID.toString());
//		context.sendRequest("mn://avatar/current/update", setRegionRequest);
//
//		Response response = new Response(StatusCode.OK, avatarData);
//		response.getParameters().set(ParameterCode.REGION_ID, regionID.toString());
//		response.getParameters().set(ParameterCode.TOKEN, token.toString());
//		response.getParameters().set(ParameterCode.HOST, store.getInstanceHost(regionID));
//		response.getParameters().set(ParameterCode.PORT, store.getInstancePort(regionID));
//
//		return response;
//	}
}
