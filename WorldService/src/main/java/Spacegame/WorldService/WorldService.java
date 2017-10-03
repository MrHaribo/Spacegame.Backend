package Spacegame.WorldService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
import micronet.type.Vector2;

@MessageService(uri="mn://world")
public class WorldService {
	
	DataStore store;

	public static void main(String[] args) {
		POIValues poi = new POIValues();
		poi.setId("Earth");
		poi.setPosition(new Vector2());
		poi.setSeed(42);
		poi.setType(POIType.Planet);
		
		System.out.println(Serialization.serialize(poi));
	}
	
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
		world.setFactions(Arrays.asList(FactionValues.Confederate, FactionValues.Rebel));
		
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
	
	@MessageListener(uri = "/region/current/set")
	public Response setCurrentRegion(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		store.getSub(playerID).getMap("avatars").get(avatarName).set("regionID", request.getData());
		return new Response(StatusCode.OK);
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
		String[] allRegionIDs = store.getSub("World").get("regions", String[].class);
		List<RegionValues> allRegions = new ArrayList<>();
		for (String regionID : allRegionIDs) {
			RegionValues region = store.getSub(regionID).get("data", RegionValues.class);
			allRegions.add(region);
		}
		return new Response(StatusCode.OK, Serialization.serialize(allRegions));
	}
	
	@MessageListener(uri = "/region/battle/all")
	public Response getAllBattleRegions(Context context, Request request) {
		return new Response(StatusCode.OK, Serialization.serialize(new RegionValues[0]));
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
	

}
