package Spacegame.AvatarService;

import java.util.HashMap;
import java.util.Map;

import com.couchbase.client.java.document.json.JsonObject;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.annotation.OnStop;
import micronet.datastore.DataStore;
import micronet.network.Context;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.script.ScriptExecutor;
import micronet.serialization.Serialization;
import micronet.type.Vector2;

@MessageService(uri="mn://avatar")  
public class AvatarService {

	private DataStore store = new DataStore();
	
	private AvatarDatabase database; 

	// TODO: Avatars could be cached
	private Map<String, String> avatars = new HashMap<>();
	
	@OnStart
	public void onStart(Context context) {
		database = new AvatarDatabase();
		
		context.getAdvisory().listen("User.Disconnected", (String userID) -> {
			avatars.remove(userID);
			System.out.println("User Disconnected" + userID + " player left: " + avatars.size());
		});
	}
	
	@OnStop
	public void onStop(Context context) { 
		database.shutdown();
	}
	
	@MessageListener(uri="/credits/balance")
	public Response balanceCredits(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);
		return new Response(StatusCode.OK, Integer.toString(avatar.getCredits()));
	}
	
	@MessageListener(uri="/credits/add")
	public Response addCredits(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);
		int amount = Integer.parseInt(request.getData());
		int balance = avatar.getCredits() + amount;

		// TODO: Not Acomic Yet because of additional effort for real
		// transactions
		database.updateAvatar(userID, avatar.getName(), (AvatarValues tmp) -> {
			tmp.setCredits(balance);
			return tmp;
		});
		context.sendEvent(userID, "OnCreditsChanged", Integer.toString(balance));
		return new Response(StatusCode.OK, Integer.toString(balance));
	}
	
	@MessageListener(uri="/credits/remove")
	public Response removeCredits(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);

		int amount = Integer.parseInt(request.getData());
		if (avatar.getCredits() >= amount) {
			int balance = avatar.getCredits() - amount;
			database.updateAvatar(userID, avatar.getName(), (AvatarValues tmp) -> {
				tmp.setCredits(balance);
				return tmp;
			});
			context.sendEvent(userID, "OnCreditsChanged", Integer.toString(balance));
			return new Response(StatusCode.OK, Integer.toString(balance));
		} else {
			return new Response(StatusCode.NOT_ACCEPTABLE, "Insufficient Credits");
		}
	}

//	@MessageListener(uri="/reputation/add")
//	public void addReputation(Context context, Request request) {
//		String userID = request.getParameters().getString(ParameterCode.USER_ID);
//		String attitude = request.getParameters().getString(ParameterCode.FACTION);
//		float amount = Float.parseFloat(request.getData());
//
//		if (!avatars.containsKey(userID))
//			return;
//		String avatarName = avatars.get(userID);
//
//		database.updateAvatar(userID, avatarName, (AvatarValues tmp) -> {
//			if (attitude.equals(FactionValues.AttitudeConfederate))
//				tmp.getFaction().setConfederateReputation(tmp.getFaction().getConfederateReputation() + amount);
//			if (attitude.equals(FactionValues.AttitudeRebel))
//				tmp.getFaction().setRebelReputation(tmp.getFaction().getRebelReputation() + amount);
//			return tmp;
//		});
//		
//		sendReputationChangedEvent(context, userID, avatarName);
//	}
	
	@MessageListener(uri="/land")
	public Response land(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);

		if (avatar.getLanded())
			return new Response(StatusCode.BAD_REQUEST, "Already Landed");

		
		String regionId = avatar.getRegionID();
		String queue = ScriptExecutor.INSTANCE.invokeFunction("regionAddress", regionId).toString() + "/land";
		System.out.println("SENDING LAND: " + queue);
		Response landResponse = context.sendRequestBlocking(queue, request);

		if (landResponse.getStatus() != StatusCode.OK)
			return landResponse;

		database.updateAvatar(userID, avatar.getName(), (AvatarValues tmp) -> {
			tmp.setPosition(landResponse.getParameters().getVector2(ParameterCode.POSITION));
			tmp.setPoiID(landResponse.getParameters().getString(ParameterCode.POI_ID));
			tmp.setLanded(true);
			return tmp;
		});

		System.out.println(avatar.getName() + " lands on " + landResponse.getParameters().getString(ParameterCode.POI_ID));
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri="/takeoff")
	public Response takeoff(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);

		if (!avatar.getLanded())
			return new Response(StatusCode.BAD_REQUEST, "Already InSpace");

		String regionId = avatar.getRegionID();
		String queue = ScriptExecutor.INSTANCE.invokeFunction("regionAddress", regionId).toString() + "/takeoff";
		System.out.println("SENDING TAKEOFF: " + queue);
		Response takeoffResponse = context.sendRequestBlocking(queue, request);

		if (takeoffResponse.getStatus() != StatusCode.OK)
			return takeoffResponse;

		database.updateAvatar(userID, avatar.getName(), (AvatarValues tmp) -> {
			tmp.setLanded(false);
			return tmp;
		});

		System.out.println(avatar.getName() + " takeoff ");
		return new Response(StatusCode.OK);
	}


	@MessageListener(uri="/current/name/get")
	public Response getCurrentAvatarName(Context context, Request request) {
		String playerID = getPlayerID(request);
		AvatarValues avatar = getCurrentAvatar(playerID);
		if (avatar == null)
			return new Response(StatusCode.GONE);
		return new Response(StatusCode.OK, avatar.getName());
	}
	
	@MessageListener(uri="/current/get")
	public Response getCurrentAvatar(Context context, Request request) {
		String playerID = getPlayerID(request);
		AvatarValues avatar = getCurrentAvatar(playerID);
		if (avatar == null)
			return new Response(StatusCode.GONE);
		return new Response(StatusCode.OK, Serialization.serialize(avatar));
	}

	@MessageListener(uri="/current/set")
	public Response setCurrentAvatar(Context context, Request request) {
		String playerID = getPlayerID(request);
		
		Player player = store.get(playerID, Player.class);
		player.setCurrentAvatar(request.getData());
		store.upsert(playerID, player);
		
		AvatarValues avatar = getCurrentAvatar(playerID);
		return new Response(StatusCode.OK, Serialization.serialize(avatar));
	}
	
	@MessageListener(uri="/current/update")
	public void updateAvatar(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = getPlayerID(request);

		AvatarValues avatar = getCurrentAvatar(playerID);
		if (avatar == null)
			return;
		persistAvatar(request, userID, avatar.getName());
	}

	@MessageListener(uri="/get")
	public Response setAvatar(Context context, Request request) {
		String playerID = getPlayerID(request);
		
		AvatarValues avatar = store.getSub(playerID).getMap("avatars").get(request.getData(), AvatarValues.class);
		if (avatar == null)
			return new Response(StatusCode.NOT_FOUND);
		
		return new Response(StatusCode.OK, Serialization.serialize(avatar));
	}
	
	@MessageListener(uri="/all")
	public Response getAllAvatars(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		
		Player player = store.get(playerID, Player.class);
		return new Response(StatusCode.OK, Serialization.serialize(player.getAvatars()));
	}

	@MessageListener(uri="/create")
	public Response createAvatar(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		
		AvatarValues avatar = Serialization.deserialize(request.getData(), AvatarValues.class);

		if (avatar.getName() == null || avatar.getName().length() < 2)
			return new Response(StatusCode.BAD_REQUEST);
		avatar.setCredits(1000);

		String faction = request.getParameters().getString(ParameterCode.FACTION);
		if (faction.equals(FactionValues.AttitudeRebel)) {
			avatar.setFaction(FactionValues.AttitudeRebel);
			avatar.setRegionID("Region.Heulion");
			avatar.setHomeRegionID("Region.Heulion");
		} else if (faction.equals(FactionValues.AttitudeConfederate)) {
			avatar.setFaction(FactionValues.AttitudeConfederate);
			avatar.setRegionID("Region.Central");
			avatar.setHomeRegionID("Region.Central");
		} else if (faction.equals(FactionValues.AttitudeNeutral)) {
			avatar.setFaction(FactionValues.AttitudeNeutral);
			avatar.setRegionID("Region.Sol");
			avatar.setHomeRegionID("Region.Sol");
		}

		store.getSub(playerID).getMap("avatars").add(avatar.getName(), avatar);

		Request createCollectionRequest = new Request(avatar.getName());
		createCollectionRequest.getParameters().set(ParameterCode.USER_ID, userID);
		createCollectionRequest.getParameters().set(ParameterCode.FACTION, faction);
		context.sendRequest("mn://vehicle/collection/create", createCollectionRequest);

		sendAvailableAvatarsChangedEvent(context, userID);
		return new Response(StatusCode.OK);
	}
	
	@MessageListener(uri="/delete")
	public Response deleteAvatar(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		
		store.getSub(playerID).getMap("avatars").remove(request.getData());
		context.sendRequest("mn://vehicle/collection/remove", request);

		sendAvailableAvatarsChangedEvent(context, userID);
		return new Response(StatusCode.OK);
	}
	
	@MessageListener(uri="/persist")
	public void persistAvatar(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String avatarName = request.getParameters().getString(ParameterCode.NAME);
		persistAvatar(request, userID, avatarName);
	}

	private void persistAvatar(Request request, String userID, String avatarName) {
		String playerID = String.format("Player.%s", userID);

		if (request.getParameters().containsParameter(ParameterCode.REGION_ID)) {
			String regionID = request.getParameters().getString(ParameterCode.REGION_ID);
			store.getSub(playerID).set(String.format("avatars.%s.regionID", avatarName), regionID);
			
		}
		if (request.getParameters().containsParameter(ParameterCode.POSITION)) {
			Vector2 position = request.getParameters().getVector2(ParameterCode.POSITION);
			store.getSub(playerID).set(String.format("avatars.%s.position", avatarName), position);
		}
	}
	
	private String getPlayerID(Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		if (request.getParameters().containsParameter(ParameterCode.ID)) {
			userID = request.getParameters().getString(ParameterCode.ID);
		}
		return String.format("Player.%s", userID);
	}
	
	private AvatarValues getCurrentAvatar(String playerID) {
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		if (avatarName == null)
			return null;

		AvatarValues avatar = store.getSub(playerID).getMap("avatars").get(avatarName, AvatarValues.class);
		return avatar;
	}

	private void sendAvailableAvatarsChangedEvent(Context context, String userID) {
		String playerID = String.format("Player.%s", userID);
		Player player = store.get(playerID, Player.class);
		
		String data = Serialization.serialize(player.getAvatars());
		context.sendEvent(userID, Event.AvailableAvatarsChanged, data);
	}
	
//	private void sendReputationChangedEvent(Context context, String userID, String avatarName) {
//		AvatarValues avatar = database.getAvatar(userID, avatarName);
//		String data = Serialization.serialize(avatar.getFaction());
//		context.sendEvent(userID, Event.ReputationChanged, data);
//	}
}
