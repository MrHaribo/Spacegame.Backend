package Spacegame.AvatarService;

import java.util.HashMap;
import java.util.Map;

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

@MessageService(uri="mn://avatar")  
public class AvatarService {

	private DataStore store = new DataStore();

	// TODO: Avatars could be cached
	private Map<String, String> avatars = new HashMap<>();
	
	@OnStart
	public void onStart(Context context) {
		context.getAdvisory().listen("User.Disconnected", (String userID) -> {
			avatars.remove(userID);
			System.out.println("User Disconnected" + userID + " player left: " + avatars.size());
		});
	}
	
	@MessageListener(uri="/credits/balance")
	public Response balanceCredits(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		AvatarValues avatar = getCurrentAvatar(playerID);
		return new Response(StatusCode.OK, Integer.toString(avatar.getCredits()));
	}
	
	@MessageListener(uri="/credits/add")
	public Response addCredits(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		AvatarValues avatar = getCurrentAvatar(playerID);
		int amount = Integer.parseInt(request.getData());
		int balance = avatar.getCredits() + amount;
		
		store.getSub(playerID).getMap("avatars").get(avatar.getName()).set("credits", balance);

		context.sendEvent(userID, Event.CreditsChanged, Integer.toString(balance));
		return new Response(StatusCode.OK, Integer.toString(balance));
	}
	
	@MessageListener(uri="/credits/remove")
	public Response removeCredits(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		AvatarValues avatar = getCurrentAvatar(playerID);

		int amount = Integer.parseInt(request.getData());
		if (avatar.getCredits() >= amount) {
			int balance = avatar.getCredits() - amount;
			store.getSub(playerID).getMap("avatars").get(avatar.getName()).set("credits", balance);
			context.sendEvent(userID, Event.CreditsChanged, Integer.toString(balance));
			return new Response(StatusCode.OK, Integer.toString(balance));
		} else {
			return new Response(StatusCode.NOT_ACCEPTABLE, "Insufficient Credits");
		}
	}
	
	@MessageListener(uri="/land")
	public Response land(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		AvatarValues avatar = getCurrentAvatar(playerID);

		if (avatar.getLanded())
			return new Response(StatusCode.BAD_REQUEST, "Already Landed");

		avatar.setLanded(true);
		avatar.setPoiID(request.getData());
		
		store.getSub(playerID).getMap("avatars").put(avatar.getName(), avatar);
		sendAvatarChangedEvent(context, userID);
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri="/takeoff")
	public Response takeoff(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		AvatarValues avatar = getCurrentAvatar(playerID);

		if (!avatar.getLanded())
			return new Response(StatusCode.BAD_REQUEST, "Already InSpace");

		avatar.setLanded(false);
		avatar.setPoiID(null);

		store.getSub(playerID).getMap("avatars").put(avatar.getName(), avatar);
		sendAvatarChangedEvent(context, userID);
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
		if (faction.equals(FactionValues.Rebel)) {
			avatar.setFaction(FactionValues.Rebel);
			avatar.setRegionID("Region.Heulion");
			avatar.setHomeRegionID("Region.Heulion");
		} else if (faction.equals(FactionValues.Confederate)) {
			avatar.setFaction(FactionValues.Confederate);
			avatar.setRegionID("Region.Central");
			avatar.setHomeRegionID("Region.Central");
		} else if (faction.equals(FactionValues.Neutral)) {
			avatar.setFaction(FactionValues.Neutral);
			avatar.setRegionID("Region.Sol");
			avatar.setHomeRegionID("Region.Sol");
		}

		store.getSub(playerID).getMap("avatars").add(avatar.getName(), avatar);

		Request createCollectionRequest = new Request(avatar.getName());
		createCollectionRequest.getParameters().set(ParameterCode.USER_ID, userID);
		createCollectionRequest.getParameters().set(ParameterCode.FACTION, faction);
		context.sendRequest("mn://vehicle/collection/create", createCollectionRequest);
		context.sendRequest("mn://item/inventory/create", createCollectionRequest);
		context.sendRequest("mn://faction/reputation/create", createCollectionRequest);

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
		context.sendEvent(userID, Event.AvailableAvatarsChanged, Serialization.serialize(player.getAvatars()));
	}
	
	private void sendAvatarChangedEvent(Context context, String userID) {
		String playerID = String.format("Player.%s", userID);
		AvatarValues avatar = getCurrentAvatar(playerID);
		context.sendEvent(userID, Event.AvatarChanged, Serialization.serialize(avatar));
	}
}
