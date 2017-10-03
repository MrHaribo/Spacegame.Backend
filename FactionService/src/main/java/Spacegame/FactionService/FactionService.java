package Spacegame.FactionService;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.datastore.DataStore;
import micronet.network.Context;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.serialization.Serialization;

@MessageService(uri = "mn://faction")
public class FactionService {
	
	DataStore store = new DataStore();
	
	@OnStart
	public void onStart(Context context) {
		FactionValues faction = new FactionValues(FactionValues.Rebel);
		faction.setHostileFactions(new HashSet<>(Arrays.asList(FactionValues.Confederate)));
		String id = String.format("Faction.%s", FactionValues.Rebel);
		store.upsert(id, faction);
		
		faction = new FactionValues(FactionValues.Confederate);
		faction.setHostileFactions(new HashSet<>(Arrays.asList(FactionValues.Rebel)));
		id = String.format("Faction.%s", FactionValues.Confederate);
		store.upsert(id, faction);
	}
	
	@MessageListener(uri = "/reputation/create")
	public void createReputation(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String faction = request.getParameters().getString(ParameterCode.FACTION);
		
		switch (faction) {
		case FactionValues.Rebel:
			store.getSub(playerID).getSub("reputation").getMap("reputation").put(FactionValues.Rebel, 0.3f);
			store.getSub(playerID).getSub("reputation").getMap("reputation").put(FactionValues.Confederate, -0.5f);
			break;
		case FactionValues.Confederate:
			store.getSub(playerID).getSub("reputation").getMap("reputation").put(FactionValues.Confederate, 0.3f);
			store.getSub(playerID).getSub("reputation").getMap("reputation").put(FactionValues.Rebel, -0.5f);
			break;
			default:
		}
	}
	
//	@MessageListener(uri = "/reputation/add")
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
	
	@MessageListener(uri = "/reputation/remove")
	public void removeReputation(Context context, Request request) {

	}
	
	@MessageListener(uri = "/get")
	public Response getFaction(Context context, Request request) {
		String id = String.format("Faction.%s", request.getData());
		FactionValues faction = store.get(id, FactionValues.class);
		return new Response(StatusCode.OK, Serialization.serialize(faction));
	}
	
	@MessageListener(uri = "/change")
	public void changeFaction(Context context, Request request) {

	}
	
//	private void sendReputationChangedEvent(Context context, String userID, String avatarName) {
//		AvatarValues avatar = database.getAvatar(userID, avatarName);
//		String data = Serialization.serialize(avatar.getFaction());
//		context.sendEvent(userID, Event.ReputationChanged, data);
//	}
}

